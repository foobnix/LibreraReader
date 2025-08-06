package mobi.librera.lib.gdrive

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.util.Log
import androidx.core.graphics.createBitmap
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

object CoverGenerator {
    private const val TAG = "CoverGenerator"

    /**
     * Generate a cover image from a PDF file and save it as PNG
     * @param context Android context
     * @param pdfFile The PDF file to generate cover from
     * @param outputFileName The name for the output cover file (without extension)
     * @return Uri of the generated cover file, or null if failed
     */
    fun generateCoverFromPDF(
        context: Context,
        pdfFile: File,
        outputFileName: String
    ): Uri? {
        return try {
            val coverBitmap = renderPdfFirstPage(pdfFile) ?: return null
            saveBitmapAsPng(context, coverBitmap, outputFileName)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to generate cover from PDF", e)
            null
        }
    }

    /**
     * Render the first page of a PDF as a bitmap
     */
    private fun renderPdfFirstPage(pdfFile: File): Bitmap? {
        return try {
            if (!pdfFile.exists()) {
                Log.e(TAG, "PDF file does not exist: ${pdfFile.absolutePath}")
                return null
            }

            val fileDescriptor =
                ParcelFileDescriptor.open(pdfFile, ParcelFileDescriptor.MODE_READ_ONLY)
            val pdfRenderer = PdfRenderer(fileDescriptor)

            if (pdfRenderer.pageCount == 0) {
                pdfRenderer.close()
                fileDescriptor.close()
                return null
            }

            val page = pdfRenderer.openPage(0)

            // Calculate appropriate bitmap size (max width 300px for covers)
            val maxWidth = 300
            val aspectRatio = page.height.toFloat() / page.width.toFloat()
            val bitmapWidth = maxWidth
            val bitmapHeight = (maxWidth * aspectRatio).toInt()

            val bitmap = createBitmap(bitmapWidth, bitmapHeight)

            // Render the page to bitmap
            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)

            page.close()
            pdfRenderer.close()
            fileDescriptor.close()

            Log.d(TAG, "Successfully rendered PDF cover: ${bitmapWidth}x${bitmapHeight}")
            bitmap

        } catch (e: Exception) {
            Log.e(TAG, "Error rendering PDF first page", e)
            null
        }
    }

    /**
     * Save a bitmap as PNG file and return its Uri
     */
    private fun saveBitmapAsPng(
        context: Context,
        bitmap: Bitmap,
        fileName: String
    ): Uri? {
        return try {
            // Create covers directory in internal storage
            val coversDir = File(context.filesDir, "covers")
            if (!coversDir.exists()) {
                coversDir.mkdirs()
            }

            val coverFile = File(coversDir, "$fileName.png")

            // Save bitmap to file
            FileOutputStream(coverFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }

            Log.d(TAG, "Cover saved to: ${coverFile.absolutePath}")
            Uri.fromFile(coverFile)

        } catch (e: IOException) {
            Log.e(TAG, "Error saving bitmap as PNG", e)
            null
        }
    }

    /**
     * Generate cover name from file name (same name + .png extension)
     */
    fun generateCoverName(fileName: String): String {
        return fileName
    }

    /**
     * Clean up generated cover files (optional utility)
     */
    fun cleanupCovers(context: Context) {
        try {
            val coversDir = File(context.filesDir, "covers")
            if (coversDir.exists()) {
                coversDir.listFiles()?.forEach { file ->
                    if (file.isFile && file.name.endsWith(".png")) {
                        file.delete()
                        Log.d(TAG, "Deleted cover file: ${file.name}")
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error cleaning up covers", e)
        }
    }
}
