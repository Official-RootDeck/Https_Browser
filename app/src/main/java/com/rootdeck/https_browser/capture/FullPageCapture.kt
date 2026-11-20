// Built by MD.Nirob
package com.rootdeck.https_browser.capture

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.webkit.WebView
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream

/**
 * Built by MD.Nirob — Captures full-page WebView screenshots and exports paginated PDFs.
 */
object FullPageCapture {

    private const val MAX_CAPTURE_HEIGHT_PX = 8192

    fun captureFullPageBitmap(webView: WebView): Bitmap? {
        val width = webView.width
        if (width <= 0) return null

        val contentHeightPx = (webView.contentHeight * webView.scale).toInt()
            .coerceAtLeast(webView.height)
            .coerceAtMost(MAX_CAPTURE_HEIGHT_PX)

        return try {
            val bitmap = Bitmap.createBitmap(width, contentHeightPx, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            canvas.drawColor(android.graphics.Color.WHITE)
            val oldScrollX = webView.scrollX
            val oldScrollY = webView.scrollY
            webView.scrollTo(0, 0)
            webView.draw(canvas)
            webView.scrollTo(oldScrollX, oldScrollY)
            bitmap
        } catch (_: OutOfMemoryError) {
            null
        } catch (_: Exception) {
            null
        }
    }

    fun saveBitmapToCache(context: Context, bitmap: Bitmap): File? {
        return try {
            val dir = File(context.cacheDir, "captures").apply { mkdirs() }
            val file = File(dir, "capture_${System.currentTimeMillis()}.png")
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 95, out)
            }
            file
        } catch (_: Exception) {
            null
        }
    }

    fun saveBitmapToGallery(context: Context, bitmap: Bitmap, title: String): Boolean {
        return try {
            val safeName = title.replace(Regex("[^a-zA-Z0-9_-]"), "_").take(32).ifBlank { "page" }
            val filename = "https_browser_${safeName}_${System.currentTimeMillis()}.png"
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, filename)
                    put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                    put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/HttpsBrowser")
                }
                val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                    ?: return false
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 95, out)
                }
                true
            } else {
                val dir = File(context.cacheDir, "captures").apply { mkdirs() }
                val file = File(dir, filename)
                FileOutputStream(file).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 95, out)
                }
                true
            }
        } catch (_: Exception) {
            false
        }
    }

    fun exportBitmapToPdf(context: Context, bitmap: Bitmap, title: String): File? {
        return try {
            val pdfDir = File(context.cacheDir, "pdfs").apply { mkdirs() }
            val safeName = title.replace(Regex("[^a-zA-Z0-9_-]"), "_").take(32).ifBlank { "webpage" }
            val pdfFile = File(pdfDir, "${safeName}_${System.currentTimeMillis()}.pdf")

            val document = PdfDocument()
            val pageWidth = 595 // A4 width in points
            val pageHeight = 842 // A4 height in points
            val scale = pageWidth.toFloat() / bitmap.width.toFloat()
            val sliceHeightOnBitmap = (pageHeight / scale).toInt().coerceAtLeast(1)

            var y = 0
            var pageNum = 1
            while (y < bitmap.height) {
                val chunkHeight = minOf(sliceHeightOnBitmap, bitmap.height - y)
                val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNum).create()
                val page = document.startPage(pageInfo)
                val destHeight = (chunkHeight * scale).toInt()
                val srcRect = android.graphics.Rect(0, y, bitmap.width, y + chunkHeight)
                val dstRect = android.graphics.Rect(0, 0, pageWidth, destHeight)
                page.canvas.drawColor(android.graphics.Color.WHITE)
                page.canvas.drawBitmap(bitmap, srcRect, dstRect, null)
                document.finishPage(page)
                y += chunkHeight
                pageNum++
            }

            FileOutputStream(pdfFile).use { out ->
                document.writeTo(out)
            }
            document.close()
            pdfFile
        } catch (_: Exception) {
            null
        }
    }

    fun getShareUri(context: Context, file: File): Uri {
        return FileProvider.getUriForFile(
            context,
            "${context.applicationContext.packageName}.fileprovider",
            file
        )
    }
}
