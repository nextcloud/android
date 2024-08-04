/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2023 Álvaro Brey <alvaro@alvarobrey.com>
 * SPDX-FileCopyrightText: 2023 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.nextcloud.client.documentscan

import android.graphics.BitmapFactory
import android.graphics.pdf.PdfDocument
import com.nextcloud.client.logger.Logger
import java.io.FileOutputStream
import java.io.IOException
import javax.inject.Inject

/**
 * This class takes a list of images and generates a PDF file.
 */
class GeneratePDFUseCase @Inject constructor(private val logger: Logger) {
    /**
     * @param imagePaths list of image paths
     * @return `true` if the PDF was generated successfully, `false` otherwise
     */
    fun execute(imagePaths: List<String>, filePath: String): Boolean {
        return if (imagePaths.isEmpty() || filePath.isBlank()) {
            logger.w(TAG, "Invalid parameters: imagePaths: $imagePaths, filePath: $filePath")
            false
        } else {
            val document = PdfDocument()
            fillDocumentPages(document, imagePaths)
            writePdfToFile(filePath, document)
        }
    }

    /**
     * @return `true` if the PDF was generated successfully, `false` otherwise
     */
    private fun writePdfToFile(filePath: String, document: PdfDocument): Boolean {
        return try {
            val fileOutputStream = FileOutputStream(filePath)
            document.writeTo(fileOutputStream)
            fileOutputStream.close()
            document.close()
            true
        } catch (ex: IOException) {
            logger.e(TAG, "Error generating PDF", ex)
            false
        }
    }

    private fun fillDocumentPages(document: PdfDocument, imagePaths: List<String>) {
        imagePaths.forEach { path ->
            val bitmap = BitmapFactory.decodeFile(path)
            val pageInfo = PdfDocument.PageInfo.Builder(bitmap.width, bitmap.height, 1).create()
            val page = document.startPage(pageInfo)
            page.canvas.drawBitmap(bitmap, 0f, 0f, null)
            document.finishPage(page)
        }
    }

    companion object {
        private const val TAG = "GeneratePDFUseCase"
    }
}
