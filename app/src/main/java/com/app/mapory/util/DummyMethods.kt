package com.app.mapory.util

import android.app.Activity
import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.content.res.ResourcesCompat
import com.app.mapory.R
import www.sanju.motiontoast.MotionToast
import www.sanju.motiontoast.MotionToastStyle
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.core.net.toUri
import org.apache.poi.hwpf.HWPFDocument
import org.apache.poi.xwpf.usermodel.XWPFDocument
import java.io.InputStream

class DummyMethods {

    companion object{

        fun showMotionToast(context: Context, title :String, message: String, style: MotionToastStyle ){

            MotionToast.createColorToast(
                context as Activity,
                title,
                message,
                style,
                MotionToast.GRAVITY_BOTTOM,
                MotionToast.LONG_DURATION,
                ResourcesCompat.getFont(context, www.sanju.motiontoast.R.font.montserrat_regular))
        }

        fun getCurrentFormattedDate(): String {
            val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.ENGLISH)
            return dateFormat.format(Date())
        }


        fun createImageUri(context: Context): Uri {
            val timestamp = System.currentTimeMillis()
            val imageFileName = "JPEG_${timestamp}_"
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, imageFileName)
                put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            }
            return context.contentResolver.insert(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                contentValues
            ) ?: throw IllegalStateException("Failed to create image URI")
        }

        fun createVideoUri(context: Context): Uri? {
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val fileName = "VIDEO_${timeStamp}.mp4"

            return try {
                val values = ContentValues().apply {
                    put(MediaStore.Video.Media.DISPLAY_NAME, fileName)
                    put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
                }
                context.contentResolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values)
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }


         fun formatTime(millis: Long): String {
            val totalSeconds = millis / 1000
            val minutes = totalSeconds / 60
            val seconds = totalSeconds % 60
            return String.format("%02d:%02d", minutes, seconds)
        }

        fun dateToMillis(dateStr: String): Long {
            val format = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            format.isLenient = false
            val date = format.parse(dateStr)
            return date!!.time
        }

        fun getFileType(uri: Uri, context: Context): String {
            val mimeType = context.contentResolver.getType(uri)
            return when {
                mimeType?.contains("text/plain") == true -> "txt"
                mimeType?.contains("application/pdf") == true -> "pdf"
                mimeType?.contains("application/msword") == true ||
                        mimeType?.contains("application/vnd.openxmlformats-officedocument.wordprocessingml.document") == true -> "word"
                else -> "unknown"
            }
        }




        fun extractWordContent(uri: Uri, context: Context): String {
            return try {
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    try {
                        val document = XWPFDocument(inputStream)
                        val output = StringBuilder()

                        document.paragraphs.forEach { paragraph ->
                            val style = paragraph.style ?: ""
                            when {
                                style.contains("Heading1") ->
                                    output.append("<h1>${paragraph.text}</h1>")
                                style.contains("Heading2") ->
                                    output.append("<h2>${paragraph.text}</h2>")
                                else -> output.append("<p>${paragraph.text}</p>")
                            }
                        }

                        document.tables.forEach { table ->
                            output.append("<table border='1'>")
                            table.rows.forEach { row ->
                                output.append("<tr>")
                                row.tableCells.forEach { cell ->
                                    output.append("<td>${cell.text}</td>")
                                }
                                output.append("</tr>")
                            }
                            output.append("</table>")
                        }

                        document.close()
                        output.toString()
                    } catch (e: Exception) {
                        context.contentResolver.openInputStream(uri)?.use { newInputStream ->
                            try {
                                val document = HWPFDocument(newInputStream)
                                val output = StringBuilder()

                                val range = document.range
                                for (i in 0 until range.numParagraphs()) {
                                    output.append("<p>${range.getParagraph(i).text()}</p>")
                                }

                                document.close()
                                output.toString()
                            } catch (e2: Exception) {
                                throw Exception("File format not supported. Neither DOC nor DOCX format could be read.")
                            }
                        } ?: throw IllegalStateException("Could not open file")
                    }
                } ?: throw IllegalStateException("Could not open file")
            } catch (e: Exception) {
                e.printStackTrace()
                "<p>Error: ${e.message}</p>"
            }
        }



    }


}
