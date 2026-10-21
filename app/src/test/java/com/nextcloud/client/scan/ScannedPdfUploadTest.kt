/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.client.scan

import android.net.Uri
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Test

class ScannedPdfUploadTest {

    private fun uri(scheme: String?, path: String?): Uri = mockk {
        every { this@mockk.scheme } returns scheme
        every { this@mockk.path } returns path
    }

    @Test
    fun fileSchemeUriIsTreatedAsEmbeddedScan() {
        val result = ScannedPdfUpload.from(uri("file", "/data/files/fairscan/scan.pdf"), "/Documents/", "scan.pdf")

        assertEquals(ScannedPdfUpload.Embedded("/data/files/fairscan/scan.pdf", "/Documents/scan.pdf"), result)
    }

    @Test
    fun embeddedScanInRootFolderUsesRootPath() {
        val result = ScannedPdfUpload.from(uri("file", "/data/scan.pdf"), "/", "20260101_scan.pdf")

        assertEquals(ScannedPdfUpload.Embedded("/data/scan.pdf", "/20260101_scan.pdf"), result)
    }

    @Test
    fun contentSchemeUriIsTreatedAsExternalScan() {
        val uri = uri("content", "/scan.pdf")

        assertEquals(ScannedPdfUpload.External(uri, "/Documents/"), ScannedPdfUpload.from(uri, "/Documents/", "scan.pdf"))
    }

    @Test
    fun uriWithoutSchemeIsTreatedAsExternalScan() {
        val uri = uri(null, null)

        assertEquals(ScannedPdfUpload.External(uri, "/Documents/"), ScannedPdfUpload.from(uri, "/Documents/", "scan.pdf"))
    }

    @Test
    fun fileSchemeUriWithoutPathIsInvalid() {
        assertEquals(ScannedPdfUpload.Invalid, ScannedPdfUpload.from(uri("file", null), "/", "scan.pdf"))
    }
}
