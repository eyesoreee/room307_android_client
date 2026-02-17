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
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.ResponseBody
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import javax.inject.Inject

class FileDownloader @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private val channelId = "file_download_channel"

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "File Downloads",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationManager.createNotificationChannel(channel)
        }
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
                val mimeType = getMimeType(filename)
                val outputStream: OutputStream?

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val contentValues = ContentValues().apply {
                        put(MediaStore.MediaColumns.DISPLAY_NAME, uniqueFileName)
                        put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                        put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                    }
                    val resolver = context.contentResolver
                    val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                    outputStream = uri?.let { resolver.openOutputStream(it) }
                } else {
                    val target = File(
                        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                        uniqueFileName
                    )
                    outputStream = FileOutputStream(target)
                }

                if (outputStream == null) {
                    showFinishedNotification(notificationId, filename, false)
                    return@withContext false
                }

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

                showFinishedNotification(notificationId, filename, true)
                true
            } catch (e: Exception) {
                Log.e("FileDownloader", "Error saving file", e)
                showFinishedNotification(notificationId, filename, false)
                false
            }
        }

    private fun getUniqueFileName(filename: String): String {
        val name = filename.substringBeforeLast('.')
        val extension = filename.substringAfterLast('.', "")
        val timestamp = System.currentTimeMillis() / 1000 // Shorter timestamp
        return if (extension.isNotEmpty()) "${name}_$timestamp.$extension" else "${name}_$timestamp"
    }

    private fun getMimeType(filename: String): String {
        val extension = filename.substringAfterLast('.', "")
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension) ?: "application/octet-stream"
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
