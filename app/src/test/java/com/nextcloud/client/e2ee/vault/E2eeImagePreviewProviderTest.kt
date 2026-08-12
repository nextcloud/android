/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.nextcloud.client.e2ee.vault

import android.graphics.Bitmap
import com.nextcloud.client.account.MockUser
import com.nextcloud.client.core.Clock
import com.owncloud.android.datamodel.FileDataStorageManager
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.lib.common.OwnCloudClient
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class E2eeImagePreviewProviderTest {
    private val clock = TestClock()
    private val storageManager: FileDataStorageManager = mock()
    private val client: OwnCloudClient = mock()
    private val user = MockUser(ACCOUNT_NAME, ACCOUNT_TYPE)
    private val parent = OCFile(PARENT_REMOTE_PATH).apply {
        localId = VAULT_LOCAL_ID
    }
    private val session = E2eeVaultSession(clock, E2eeVaultSessionConfig())
    private val loader = FakePlaintextMediaLoader()
    private val decoder = FakeImageDecoder()
    private val provider = E2eeImagePreviewProvider(user, storageManager, session, loader, decoder)

    @Before
    fun setUp() {
        whenever(storageManager.getFileByEncryptedRemotePath(any())).thenReturn(parent)
    }

    @Test
    fun loadBitmapReturnsNullForNormalImageWithoutRequestingPlaintext() {
        val bitmap = provider.loadBitmap(normalImage(), client, PREVIEW_WIDTH, PREVIEW_HEIGHT)

        assertNull(bitmap)
        assertEquals(0, loader.calls)
        assertEquals(0, decoder.calls)
    }

    @Test
    fun loadBitmapReturnsNullWhenVaultIsLockedWithoutRequestingPlaintext() {
        val bitmap = provider.loadBitmap(encryptedImage(), client, PREVIEW_WIDTH, PREVIEW_HEIGHT)

        assertNull(bitmap)
        assertEquals(0, loader.calls)
        assertEquals(0, decoder.calls)
    }

    @Test
    fun loadBitmapReturnsNullWhenEncryptedParentIsMissing() {
        whenever(storageManager.getFileByEncryptedRemotePath(any())).thenReturn(null)
        session.unlock(E2eeVaultSessionKey(ACCOUNT_NAME, VAULT_LOCAL_ID))

        val bitmap = provider.loadBitmap(encryptedImage(), client, PREVIEW_WIDTH, PREVIEW_HEIGHT)

        assertNull(bitmap)
        assertEquals(0, loader.calls)
        assertEquals(0, decoder.calls)
    }

    @Test
    fun loadBitmapRequestsPlaintextAndDecodesItWhenVaultIsUnlocked() {
        session.unlock(E2eeVaultSessionKey(ACCOUNT_NAME, VAULT_LOCAL_ID))

        val bitmap = provider.loadBitmap(encryptedImage(), client, PREVIEW_WIDTH, PREVIEW_HEIGHT)
        val parentPath = argumentCaptor<String>()

        assertNull(bitmap)
        verify(storageManager).getFileByEncryptedRemotePath(parentPath.capture())
        assertEquals(encryptedImage().parentRemotePath, parentPath.firstValue)
        assertEquals(1, loader.calls)
        assertEquals(1, decoder.calls)
        assertArrayEquals(PLAINTEXT, decoder.bytes)
        assertEquals(PREVIEW_WIDTH, decoder.requestedWidth)
        assertEquals(PREVIEW_HEIGHT, decoder.requestedHeight)
    }

    @Test
    fun loadBitmapReturnsNullWhenPlaintextLoadFails() {
        loader.plaintext = null
        session.unlock(E2eeVaultSessionKey(ACCOUNT_NAME, VAULT_LOCAL_ID))

        val bitmap = provider.loadBitmap(encryptedImage(), client, PREVIEW_WIDTH, PREVIEW_HEIGHT)

        assertNull(bitmap)
        assertEquals(1, loader.calls)
        assertEquals(0, decoder.calls)
    }

    private fun encryptedImage(): OCFile = OCFile(ENCRYPTED_IMAGE_REMOTE_PATH).apply {
        isEncrypted = true
        mimeType = IMAGE_MIME_TYPE
    }

    private fun normalImage(): OCFile = OCFile(ENCRYPTED_IMAGE_REMOTE_PATH).apply {
        isEncrypted = false
        mimeType = IMAGE_MIME_TYPE
    }

    private class FakePlaintextMediaLoader : E2eePlaintextMediaLoader {
        var calls = 0
        var plaintext: ByteArray? = PLAINTEXT

        override fun <T> withPlaintext(
            file: OCFile,
            parent: OCFile,
            client: OwnCloudClient,
            block: (ByteArray) -> T?
        ): T? {
            calls++
            return plaintext?.let { block(it) }
        }
    }

    private class FakeImageDecoder : E2eeImageDecoder {
        var calls = 0
        var bytes: ByteArray? = null
        var requestedWidth = 0
        var requestedHeight = 0

        override fun decode(bytes: ByteArray, requestedWidth: Int, requestedHeight: Int): Bitmap? {
            calls++
            this.bytes = bytes
            this.requestedWidth = requestedWidth
            this.requestedHeight = requestedHeight
            return null
        }
    }

    private class TestClock : Clock {
        override val currentTime: Long = START_TIME
        override val currentDate = java.util.Date(START_TIME)
        override val millisSinceBoot: Long = START_TIME
        override val tz = java.util.TimeZone.getTimeZone("UTC")
    }

    companion object {
        private const val ACCOUNT_NAME = "user@example.org"
        private const val ACCOUNT_TYPE = "nextcloud"
        private const val PARENT_REMOTE_PATH = "/encrypted/"
        private const val ENCRYPTED_IMAGE_REMOTE_PATH = "/encrypted/photo.jpg"
        private const val IMAGE_MIME_TYPE = "image/jpeg"
        private const val START_TIME = 1_000L
        private const val VAULT_LOCAL_ID = 42L
        private const val PREVIEW_WIDTH = 320
        private const val PREVIEW_HEIGHT = 240
        private val PLAINTEXT = byteArrayOf(1, 2, 3)
    }
}
