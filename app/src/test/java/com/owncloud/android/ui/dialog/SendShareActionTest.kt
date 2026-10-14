/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.owncloud.android.ui.dialog

import org.junit.Assert.assertEquals
import org.junit.Test

class SendShareActionTest {

    @Test
    fun downloadedFileIsSentImmediately() {
        assertEquals(SendShareAction.SEND_NOW, SendShareAction.resolve(isDownloaded = true, hostCanDownload = true))
    }

    @Test
    fun downloadedFileIsSentImmediatelyEvenWhenHostCannotDownload() {
        assertEquals(SendShareAction.SEND_NOW, SendShareAction.resolve(isDownloaded = true, hostCanDownload = false))
    }

    @Test
    fun notDownloadedFileIsDownloadedWhenHostCanDownload() {
        assertEquals(
            SendShareAction.DOWNLOAD_FIRST,
            SendShareAction.resolve(isDownloaded = false, hostCanDownload = true)
        )
    }

    @Test
    fun notDownloadedFileIsRejectedWhenHostCannotDownload() {
        assertEquals(
            SendShareAction.DOWNLOAD_UNAVAILABLE,
            SendShareAction.resolve(isDownloaded = false, hostCanDownload = false)
        )
    }
}
