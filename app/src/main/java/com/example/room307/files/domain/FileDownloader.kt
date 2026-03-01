package com.example.room307.files.domain

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.ContentValues
import android.content.Context
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
            NotificationChannel(channelId, "File Downloads", NotificationManager.IMPORTANCE_DEFAULT)
        notificationManager.createNotificationChannel(channel)
    }

    suspend fun saveFileToDisk(filename: String, body: ResponseBody): Boolean =
        withContext(Dispatchers.IO) {
            val uniqueFileName = getUniqueFileName(filename)
            val notificationId = filename.hashCode()
            val builder = createProgressNotification(filename)

            try {
                notificationManager.notify(notificationId, builder.build())

                val folderName = (dataStoreManager.downloadPath.first() ?: "ROOM307").trim()
                val outputStream = getOutputStream(uniqueFileName, folderName)

                if (outputStream != null) {
                    val success = writeStream(body, outputStream) { progress ->
                        notificationManager.notify(
                            notificationId,
                            builder.setProgress(100, progress, false).build()
                        )
                    }

                    finalizeDownload(uniqueFileName, folderName)
                    showFinishedNotification(notificationId, filename, success)
                    success
                } else {
                    showFinishedNotification(notificationId, filename, false)
                    false
                }
            } catch (e: Exception) {
                Log.e("FileDownloader", "Download failed: $filename", e)
                showFinishedNotification(notificationId, filename, false)
                false
            }
        }

    private fun getOutputStream(fileName: String, folderName: String): OutputStream? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, getMimeType(fileName))
                put(
                    MediaStore.MediaColumns.RELATIVE_PATH,
                    "${Environment.DIRECTORY_DOWNLOADS}/$folderName"
                )
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
            val uri =
                context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
            uri?.let { context.contentResolver.openOutputStream(it) }
        } else {
            val dir = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                folderName
            )
            if (!dir.exists()) dir.mkdirs()
            FileOutputStream(File(dir, fileName))
        }
    }

    private fun writeStream(
        body: ResponseBody,
        outputStream: OutputStream,
        onProgress: (Int) -> Unit
    ): Boolean {
        return try {
            body.byteStream().use { input ->
                outputStream.use { output ->
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
                }
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun finalizeDownload(fileName: String, folderName: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val selection =
                "${MediaStore.MediaColumns.DISPLAY_NAME} = ? AND ${MediaStore.MediaColumns.RELATIVE_PATH} = ?"
            val args = arrayOf(fileName, "${Environment.DIRECTORY_DOWNLOADS}/$folderName")
            val values = ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) }
            context.contentResolver.update(
                MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                values,
                selection,
                args
            )
        }
    }

    private fun createProgressNotification(filename: String) =
        NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle("Downloading $filename")
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setProgress(100, 0, true)

    private fun showFinishedNotification(id: Int, filename: String, success: Boolean) {
        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(if (success) android.R.drawable.stat_sys_download_done else android.R.drawable.stat_notify_error)
            .setContentTitle(if (success) "Download Complete" else "Download Failed")
            .setContentText(filename)
            .setAutoCancel(true)
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
