package com.example.room307.files.domain

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.webkit.MimeTypeMap
import androidx.core.app.NotificationCompat
import com.example.room307.data.local.DataStoreManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import okhttp3.ResponseBody
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import javax.inject.Inject

class FileDownloader @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val dataStoreManager: DataStoreManager
) {
    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private val channelId = "file_download_channel"

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        val channel =
            NotificationChannel(channelId, "File Downloads", NotificationManager.IMPORTANCE_LOW)
        notificationManager.createNotificationChannel(channel)
    }

    suspend fun saveFileToDisk(filename: String, body: ResponseBody): Boolean =
        withContext(Dispatchers.IO) {
            val uniqueFileName = getUniqueFileName(filename)
            val notificationId = filename.hashCode()
            val builder = createProgressNotification(filename)
            var uri: Uri? = null

            try {
                notificationManager.notify(notificationId, builder.build())

                val folderName = (dataStoreManager.downloadPath.first() ?: "ROOM307").trim()

                val outputStreamResult = getOutputStreamAndUri(uniqueFileName, folderName)
                val outputStream = outputStreamResult.first
                uri = outputStreamResult.second

                if (outputStream != null) {
                    var lastProgress = -1 // Add this to track progress changes
                    val success = body.use { responseBody ->
                        writeStream(responseBody, outputStream) { progress ->
                            // Only notify when percentage actually increments
                            if (progress > lastProgress) {
                                lastProgress = progress
                                notificationManager.notify(
                                    notificationId,
                                    builder.setProgress(100, progress, false).build()
                                )
                            }
                        }
                    }

                    if (success && uri != null) {
                        finalizeDownload(uri)
                    } else if (!success && uri != null) {
                        context.contentResolver.delete(uri, null, null)
                    }

                    showFinishedNotification(notificationId, filename, success)
                    success
                } else {
                    showFinishedNotification(notificationId, filename, false)
                    false
                }
            } catch (e: Exception) {
                Log.e("FileDownloader", "Download failed: $filename", e)
                uri?.let { context.contentResolver.delete(it, null, null) }
                showFinishedNotification(notificationId, filename, false)
                false
            }
        }

    private fun getOutputStreamAndUri(
        fileName: String,
        folderName: String
    ): Pair<OutputStream?, Uri?> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, getMimeType(fileName))
                // Ensure the path ends with a slash and is relative to Downloads
                put(
                    MediaStore.MediaColumns.RELATIVE_PATH,
                    "${Environment.DIRECTORY_DOWNLOADS}/$folderName/"
                )
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
            val uri =
                context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
            val stream = uri?.let { context.contentResolver.openOutputStream(it) }
            Pair(stream, uri)
        } else {
            val dir = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                folderName
            )
            if (!dir.exists()) dir.mkdirs()
            val file = File(dir, fileName)
            Pair(FileOutputStream(file), Uri.fromFile(file))
        }
    }

    private fun writeStream(
        body: ResponseBody,
        outputStream: OutputStream,
        onProgress: (Int) -> Unit
    ): Boolean {
        return try {
            outputStream.use { output ->
                body.byteStream().use { input ->
                    val buffer = ByteArray(8192)
                    val totalBytes = body.contentLength()
                    var bytesRead: Int
                    var bytesDownloaded = 0L

                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        bytesDownloaded += bytesRead
                        if (totalBytes > 0) {
                            onProgress((bytesDownloaded * 100 / totalBytes).toInt())
                        }
                    }
                    output.flush()
                }
            }
            true
        } catch (e: Exception) {
            Log.e("FileDownloader", "Stream write error", e)
            false
        }
    }

    private fun finalizeDownload(uri: Uri) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) }
            context.contentResolver.update(uri, values, null, null)
        }
    }

    private fun createProgressNotification(filename: String) =
        NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle("Downloading $filename")
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setProgress(100, 0, false)

    private fun showFinishedNotification(id: Int, filename: String, success: Boolean) {
        notificationManager.cancel(id)

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(if (success) android.R.drawable.stat_sys_download_done else android.R.drawable.stat_notify_error)
            .setContentTitle(if (success) "Download Complete" else "Download Failed")
            .setContentText(filename)
            .setAutoCancel(true)
            .setOngoing(false)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        notificationManager.notify(id, notification)
    }

    private fun getUniqueFileName(filename: String): String {
        val name = filename.substringBeforeLast('.')
        val ext = filename.substringAfterLast('.', "")
        val ts = System.currentTimeMillis() / 1000
        return if (ext.isNotEmpty()) "${name}_$ts.$ext" else "${name}_$ts"
    }

    private fun getMimeType(filename: String): String = MimeTypeMap.getSingleton()
        .getMimeTypeFromExtension(filename.substringAfterLast('.', ""))
        ?: "application/octet-stream"
}