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
        val channel = NotificationChannel(
            channelId,
            "File Downloads",
            NotificationManager.IMPORTANCE_DEFAULT
        )
        notificationManager.createNotificationChannel(channel)
    }

    suspend fun saveFileToDisk(filename: String, body: ResponseBody): Boolean =
        withContext(Dispatchers.IO) {
            val uniqueFileName = getUniqueFileName(filename)
            val notificationId = filename.hashCode()

            val builder = NotificationCompat.Builder(context, channelId)
                .setSmallIcon(android.R.drawable.stat_sys_download)
                .setContentTitle("Downloading $filename")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setProgress(100, 0, true)

            notificationManager.notify(notificationId, builder.build())

            try {
                val customFolderName = (dataStoreManager.downloadPath.first() ?: "ROOM307").trim()
                val mimeType = getMimeType(filename)

                val outputStream: OutputStream?
                val resolver = context.contentResolver

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val relativePath = "${Environment.DIRECTORY_DOWNLOADS}/$customFolderName"

                    val contentValues = ContentValues().apply {
                        put(MediaStore.MediaColumns.DISPLAY_NAME, uniqueFileName)
                        put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                        put(MediaStore.MediaColumns.RELATIVE_PATH, relativePath)
                        put(MediaStore.MediaColumns.IS_PENDING, 1)
                    }

                    val collection = MediaStore.Downloads.EXTERNAL_CONTENT_URI
                    val uri = resolver.insert(collection, contentValues)

                    outputStream = uri?.let { resolver.openOutputStream(it) }

                    if (outputStream != null) {
                        writeFile(body, outputStream, builder, notificationId)

                        contentValues.clear()
                        contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                        resolver.update(uri, contentValues, null, null)

                        showFinishedNotification(notificationId, filename, true)
                        return@withContext true
                    }
                } else {
                    // Legacy API 27, 28
                    val baseDir =
                        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                    val targetDir = File(baseDir, customFolderName)
                    if (!targetDir.exists()) targetDir.mkdirs()

                    val targetFile = File(targetDir, uniqueFileName)
                    outputStream = FileOutputStream(targetFile)

                    writeFile(body, outputStream, builder, notificationId)
                    showFinishedNotification(notificationId, filename, true)
                    return@withContext true
                }

                showFinishedNotification(notificationId, filename, false)
                false
            } catch (e: Exception) {
                Log.e("FileDownloader", "Error saving file", e)
                showFinishedNotification(notificationId, filename, false)
                false
            }
        }

    private fun writeFile(
        body: ResponseBody,
        outputStream: OutputStream,
        builder: NotificationCompat.Builder,
        notificationId: Int
    ) {
        val inputStream = body.byteStream()
        val totalBytes = body.contentLength()
        var bytesDownloaded = 0L
        val buffer = ByteArray(8192)

        inputStream.use { input ->
            outputStream.use { output ->
                var bytesRead: Int
                while (input.read(buffer).also { bytesRead = it } != -1) {
                    output.write(buffer, 0, bytesRead)
                    bytesDownloaded += bytesRead

                    if (totalBytes > 0) {
                        val progress = (bytesDownloaded * 100 / totalBytes).toInt()
                        builder.setProgress(100, progress, false)
                        notificationManager.notify(notificationId, builder.build())
                    }
                }
            }
        }
    }

    private fun getUniqueFileName(filename: String): String {
        val name = filename.substringBeforeLast('.')
        val extension = filename.substringAfterLast('.', "")
        val timestamp = System.currentTimeMillis() / 1000
        return if (extension.isNotEmpty()) "${name}_$timestamp.$extension" else "${name}_$timestamp"
    }

    private fun getMimeType(filename: String): String {
        val extension = filename.substringAfterLast('.', "")
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
            ?: "application/octet-stream"
    }

    private fun showFinishedNotification(id: Int, filename: String, success: Boolean) {
        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(if (success) android.R.drawable.stat_sys_download_done else android.R.drawable.stat_notify_error)
            .setContentTitle(if (success) "Download Complete" else "Download Failed")
            .setContentText(filename)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setOngoing(false)
            .setProgress(0, 0, false)
            .build()
        notificationManager.notify(id, notification)
    }
}
