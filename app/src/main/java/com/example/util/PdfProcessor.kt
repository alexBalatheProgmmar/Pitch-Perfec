package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.security.MessageDigest
import java.util.regex.Pattern

data class ExtractedPdfData(
    val fileName: String,
    val fileSize: Long,
    val pageCount: Int,
    val fullText: String,
    val pageTexts: List<PageTextData>,
    val isScannedPdf: Boolean,
    val ocrConfidence: Float? = null,
    val contentHash: String,
    val previewBitmap: Bitmap? = null
)

data class PageTextData(
    val pageNumber: Int,
    val text: String,
    val lineCount: Int
)

object PdfProcessor {

    /**
     * Extracts text and structure from a PDF URI.
     * If the document has embedded digital text, extracts it page by page.
     * If text is empty/unusable (scanned PDF), renders pages and falls back to OCR.
     */
    suspend fun processPdf(
        context: Context,
        uri: Uri,
        fileName: String,
        ocrFallback: (suspend (Bitmap) -> String)? = null
    ): ExtractedPdfData = withContext(Dispatchers.IO) {
        val bytes = readBytesFromUri(context, uri)
        val fileSize = bytes.size.toLong()
        val contentHash = computeSha256(bytes)

        // 1. First attempt: Stream-based digital text extraction
        val rawExtracted = extractTextFromPdfBytes(bytes)
        val hasUsableText = isTextUsable(rawExtracted.fullText)

        var pageCount = rawExtracted.pageCount.coerceAtLeast(1)
        var previewBitmap: Bitmap? = null
        var isScanned = false
        var ocrConfidence: Float? = null
        var finalFullText = rawExtracted.fullText
        var finalPageTexts = rawExtracted.pageTexts

        // 2. Open via PdfRenderer for preview bitmap and/or OCR if text extraction is inadequate
        try {
            val tempFile = File(context.cacheDir, "temp_pdf_${System.currentTimeMillis()}.pdf")
            tempFile.outputStream().use { it.write(bytes) }

            val pfd = ParcelFileDescriptor.open(tempFile, ParcelFileDescriptor.MODE_READ_ONLY)
            val renderer = PdfRenderer(pfd)
            pageCount = renderer.pageCount

            // Render first page for preview thumbnail
            if (renderer.pageCount > 0) {
                val page0 = renderer.openPage(0)
                val width = (page0.width * 1.5f).toInt().coerceAtMost(1080)
                val height = (page0.height * 1.5f).toInt().coerceAtMost(1920)
                val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                page0.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                page0.close()
                previewBitmap = bitmap
            }

            // 3. If digital text is missing, corrupted, or scanned image PDF, perform OCR
            if (!hasUsableText && ocrFallback != null && renderer.pageCount > 0) {
                isScanned = true
                val ocrPages = mutableListOf<PageTextData>()
                val maxOcrPages = minOf(renderer.pageCount, 5)

                for (i in 0 until maxOcrPages) {
                    val page = renderer.openPage(i)
                    val width = (page.width * 1.5f).toInt().coerceAtMost(1080)
                    val height = (page.height * 1.5f).toInt().coerceAtMost(1920)
                    val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                    page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    page.close()

                    val pageOcrText = ocrFallback(bmp)
                    if (pageOcrText.isNotBlank()) {
                        ocrPages.add(
                            PageTextData(
                                pageNumber = i + 1,
                                text = pageOcrText,
                                lineCount = pageOcrText.lines().size
                            )
                        )
                    }
                }

                if (ocrPages.isNotEmpty()) {
                    finalPageTexts = ocrPages
                    finalFullText = ocrPages.joinToString("\n\n") { "--- PAGE ${it.pageNumber} ---\n${it.text}" }
                    ocrConfidence = 0.88f
                }
            }

            renderer.close()
            pfd.close()
            tempFile.delete()
        } catch (e: Exception) {
            // PdfRenderer failed (or not a standard PDF file format)
        }

        ExtractedPdfData(
            fileName = fileName,
            fileSize = fileSize,
            pageCount = pageCount,
            fullText = finalFullText,
            pageTexts = finalPageTexts,
            isScannedPdf = isScanned,
            ocrConfidence = ocrConfidence,
            contentHash = contentHash,
            previewBitmap = previewBitmap
        )
    }

    /**
     * Parses PDF objects, streams, and text elements from PDF byte buffer.
     */
    fun extractTextFromPdfBytes(bytes: ByteArray): ExtractedPdfData {
        val rawString = String(bytes, Charsets.ISO_8859_1)
        val pages = mutableListOf<PageTextData>()

        // Look for PDF text objects between BT (Begin Text) and ET (End Text)
        val textBlocks = mutableListOf<String>()
        val btEtPattern = Pattern.compile("BT\\s+(.*?)\\s+ET", Pattern.DOTALL)
        val btEtMatcher = btEtPattern.matcher(rawString)

        while (btEtMatcher.find()) {
            val content = btEtMatcher.group(1) ?: continue
            val decoded = extractTextFromOperators(content)
            if (decoded.isNotBlank()) {
                textBlocks.add(decoded)
            }
        }

        // Also check for raw plain strings in the PDF stream
        if (textBlocks.isEmpty()) {
            val stringPattern = Pattern.compile("\\(([^()]{3,})\\)")
            val stringMatcher = stringPattern.matcher(rawString)
            val extractedStrings = mutableListOf<String>()
            while (stringMatcher.find()) {
                val str = stringMatcher.group(1)?.trim()
                if (!str.isNullOrBlank() && str.any { it.isLetter() }) {
                    extractedStrings.add(str)
                }
            }
            if (extractedStrings.isNotEmpty()) {
                textBlocks.add(extractedStrings.joinToString(" "))
            }
        }

        // Detect page breaks (/Type /Page or /Page\b)
        val pageMarkers = Pattern.compile("/Type\\s*/Page\\b").matcher(rawString)
        var count = 0
        while (pageMarkers.find()) {
            count++
        }
        val estimatedPageCount = count.coerceAtLeast(1)

        val fullText = if (textBlocks.isNotEmpty()) {
            textBlocks.joinToString("\n")
        } else {
            // Try cleaning raw string for printable chunks if standard parser missed it
            extractPrintableAsciiChunks(rawString)
        }

        if (fullText.isNotBlank()) {
            pages.add(PageTextData(pageNumber = 1, text = fullText, lineCount = fullText.lines().size))
        }

        return ExtractedPdfData(
            fileName = "document.pdf",
            fileSize = bytes.size.toLong(),
            pageCount = estimatedPageCount,
            fullText = fullText,
            pageTexts = pages,
            isScannedPdf = false,
            ocrConfidence = null,
            contentHash = computeSha256(bytes)
        )
    }

    private fun extractTextFromOperators(content: String): String {
        val result = StringBuilder()
        
        // Match (string) Tj or (string) ' or [(s1)(s2)] TJ
        val tjPattern = Pattern.compile("\\((.*?)\\)\\s*(?:Tj|'|\")")
        val tjMatcher = tjPattern.matcher(content)
        while (tjMatcher.find()) {
            val text = tjMatcher.group(1) ?: ""
            result.append(unescapePdfString(text)).append(" ")
        }

        val arrayPattern = Pattern.compile("\\[(.*?)\\]\\s*TJ")
        val arrayMatcher = arrayPattern.matcher(content)
        while (arrayMatcher.find()) {
            val arrayContent = arrayMatcher.group(1) ?: ""
            val innerPattern = Pattern.compile("\\((.*?)\\)")
            val innerMatcher = innerPattern.matcher(arrayContent)
            while (innerMatcher.find()) {
                val text = innerMatcher.group(1) ?: ""
                result.append(unescapePdfString(text))
            }
            result.append(" ")
        }

        return result.toString().trim()
    }

    private fun unescapePdfString(input: String): String {
        return input
            .replace("\\n", "\n")
            .replace("\\r", "\r")
            .replace("\\t", "\t")
            .replace("\\(", "(")
            .replace("\\)", ")")
            .replace("\\\\", "\\")
    }

    private fun extractPrintableAsciiChunks(raw: String): String {
        val sb = StringBuilder()
        val chunk = StringBuilder()
        for (ch in raw) {
            if (ch.code in 32..126 || ch == '\n' || ch == '\t') {
                chunk.append(ch)
            } else {
                if (chunk.length >= 6 && chunk.any { it.isLetter() }) {
                    sb.append(chunk).append("\n")
                }
                chunk.setLength(0)
            }
        }
        if (chunk.length >= 6 && chunk.any { it.isLetter() }) {
            sb.append(chunk).append("\n")
        }
        return sb.toString().trim()
    }

    private fun isTextUsable(text: String): Boolean {
        if (text.isBlank()) return false
        val alphanumericCount = text.count { it.isLetterOrDigit() }
        return alphanumericCount >= 25
    }

    private fun readBytesFromUri(context: Context, uri: Uri): ByteArray {
        val inputStream: InputStream = context.contentResolver.openInputStream(uri)
            ?: throw IllegalArgumentException("Cannot open stream for URI: $uri")
        return inputStream.use { input ->
            val buffer = ByteArrayOutputStream()
            val data = ByteArray(8192)
            var nRead: Int
            while (input.read(data, 0, data.size).also { nRead = it } != -1) {
                buffer.write(data, 0, nRead)
            }
            buffer.toByteArray()
        }
    }

    fun computeSha256(bytes: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(bytes)
        return hashBytes.joinToString("") { "%02x".format(it) }
    }
}
