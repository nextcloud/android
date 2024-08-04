/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2023 Álvaro Brey <alvaro@alvarobrey.com>
 * SPDX-FileCopyrightText: 2023 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
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
