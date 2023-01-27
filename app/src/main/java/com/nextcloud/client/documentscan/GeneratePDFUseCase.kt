/*
 * Nextcloud Android client application
 *
 *  @author Álvaro Brey
 *  Copyright (C) 2023 Álvaro Brey
 *  Copyright (C) 2023 Nextcloud GmbH
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU AFFERO GENERAL PUBLIC LICENSE
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU AFFERO GENERAL PUBLIC LICENSE for more details.
 *
 * You should have received a copy of the GNU Affero General Public
 * License along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
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
    private fun writePdfToFile(
        filePath: String,
        document: PdfDocument
    ): Boolean {
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

    private fun fillDocumentPages(
        document: PdfDocument,
        imagePaths: List<String>
    ) {
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
