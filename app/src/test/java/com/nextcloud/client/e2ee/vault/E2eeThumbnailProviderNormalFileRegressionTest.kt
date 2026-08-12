/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.nextcloud.client.e2ee.vault

import com.nextcloud.client.account.MockUser
import com.nextcloud.client.core.Clock
import com.owncloud.android.datamodel.FileDataStorageManager
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.lib.common.OwnCloudClient
import java.util.Date
import java.util.TimeZone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify

class E2eeThumbnailProviderNormalFileRegressionTest {
    private val storageManager: FileDataStorageManager = mock()
    private val client: OwnCloudClient = mock()
    private val loader = FakePlaintextMediaLoader()
    private val provider = E2eeThumbnailProvider(
        MockUser(ACCOUNT_NAME, ACCOUNT_TYPE),
        storageManager,
        E2eeVaultSession(TestClock(), E2eeVaultSessionConfig()),
        loader
    )

    @Test
    fun normalImageDoesNotUseE2eeThumbnailPath() {
        val thumbnail = provider.loadThumbnail(normalImage(), client, THUMBNAIL_SIZE, THUMBNAIL_SIZE)

        assertNull(thumbnail)
        assertEquals(0, loader.calls)
        verify(storageManager, never()).getFileByEncryptedRemotePath(any())
    }

    @Test
    fun normalVideoDoesNotUseE2eeThumbnailPath() {
        val thumbnail = provider.loadThumbnail(normalVideo(), client, THUMBNAIL_SIZE, THUMBNAIL_SIZE)

        assertNull(thumbnail)
        assertEquals(0, loader.calls)
        verify(storageManager, never()).getFileByEncryptedRemotePath(any())
    }

    private fun normalImage(): OCFile = OCFile("/Photos/normal.jpg").apply {
        isEncrypted = false
        mimeType = "image/jpeg"
    }

    private fun normalVideo(): OCFile = OCFile("/Photos/normal.mp4").apply {
        isEncrypted = false
        mimeType = "video/mp4"
    }

    private class FakePlaintextMediaLoader : E2eePlaintextMediaLoader {
        var calls = 0

        override fun <T> withPlaintext(
            file: OCFile,
            parent: OCFile,
            client: OwnCloudClient,
            block: (ByteArray) -> T?
        ): T? {
            calls++
            return block(byteArrayOf())
        }
    }

    private class TestClock : Clock {
        override val currentTime: Long = START_TIME
        override val currentDate: Date = Date(START_TIME)
        override val millisSinceBoot: Long = START_TIME
        override val tz: TimeZone = TimeZone.getTimeZone("UTC")
    }

    companion object {
        private const val ACCOUNT_NAME = "user@example.org"
        private const val ACCOUNT_TYPE = "nextcloud"
        private const val START_TIME = 1_000L
        private const val THUMBNAIL_SIZE = 256
    }
}
