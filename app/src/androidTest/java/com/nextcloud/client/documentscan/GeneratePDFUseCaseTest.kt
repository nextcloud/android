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

import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import com.nextcloud.client.logger.Logger
import com.owncloud.android.AbstractIT
import io.mockk.MockKAnnotations
import io.mockk.impl.annotations.MockK
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File

internal class GeneratePDFUseCaseTest : AbstractIT() {

    @MockK
    private lateinit var logger: Logger

    private lateinit var sut: GeneratePDFUseCase

    @Before
    fun setUp() {
        MockKAnnotations.init(this, relaxed = true)
        sut = GeneratePDFUseCase(logger)
    }

    @Test
    fun invalidArguments_shouldReturnFalse() {
        var result = sut.execute(emptyList(), "/test/foo.pdf")
        assertFalse("Usecase does not indicate failure with invalid arguments", result)
        result = sut.execute(listOf("/test.jpg"), "")
        assertFalse("Usecase does not indicate failure with invalid arguments", result)
    }

    @Test
    fun generatePdf_checkPages() {
        // can't think of how to test the _content_ of the pages
        val images = listOf(
            getFile("image.jpg"),
            getFile("christine.jpg")
        ).map { it.path }

        val output = "/sdcard/test.pdf"

        val result = sut.execute(images, output)

        assertTrue("Usecase does not indicate success", result)

        val outputFile = File(output)

        assertTrue("Output file does not exist", outputFile.exists())

        ParcelFileDescriptor.open(outputFile, ParcelFileDescriptor.MODE_READ_ONLY).use {
            PdfRenderer(it).use { renderer ->
                val pageCount = renderer.pageCount
                assertTrue("Page count is not correct", pageCount == 2)
            }
        }

        // clean up
        outputFile.delete()
    }
}
