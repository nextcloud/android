/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.nextcloud.client.e2ee.vault

import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import com.nextcloud.client.account.MockUser
import com.nextcloud.client.core.Clock
import com.owncloud.android.datamodel.FileDataStorageManager
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.lib.common.OwnCloudClient
import java.util.Date
import java.util.TimeZone
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@OptIn(UnstableApi::class)
class E2eeVideoMediaSourceProviderTest {
    private val clock = TestClock()
    private val storageManager: FileDataStorageManager = mock()
    private val client: OwnCloudClient = mock()
    private val user = MockUser(ACCOUNT_NAME, ACCOUNT_TYPE)
    private val parent = OCFile(PARENT_REMOTE_PATH).apply {
        localId = VAULT_LOCAL_ID
    }
    private val session = E2eeVaultSession(clock, E2eeVaultSessionConfig())
    private val loader = FakePlaintextMediaLoader()
    private val provider = E2eeVideoMediaSourceProvider(user, storageManager, session, loader) { true }

    @Before
    fun setUp() {
        clock.now = START_TIME
        loader.calls = 0
        loader.plaintext = PLAINTEXT.copyOf()
        whenever(storageManager.getFileByEncryptedRemotePath(any())).thenReturn(parent)
    }

    @Test
    fun loadMediaSourceReturnsNullForNormalVideoWithoutRequestingPlaintext() {
        val handle = provider.loadMediaSource(normalVideo(), client)

        assertNull(handle)
        assertEquals(0, loader.calls)
    }

    @Test
    fun loadMediaSourceReturnsNullWhenVaultIsLockedWithoutRequestingPlaintext() {
        val handle = provider.loadMediaSource(encryptedVideo(), client)

        assertNull(handle)
        assertEquals(0, loader.calls)
    }

    @Test
    fun loadMediaSourceReturnsNullWhenVaultSessionExpired() {
        session.unlock(E2eeVaultSessionKey(ACCOUNT_NAME, VAULT_LOCAL_ID))
        clock.now += E2eeVaultSessionConfig().unlockDurationMillis + ONE_MILLISECOND

        val handle = provider.loadMediaSource(encryptedVideo(), client)

        assertNull(handle)
        assertEquals(0, loader.calls)
    }

    @Test
    fun loadMediaSourceReturnsNullWhenEncryptedParentIsMissing() {
        whenever(storageManager.getFileByEncryptedRemotePath(any())).thenReturn(null)
        session.unlock(E2eeVaultSessionKey(ACCOUNT_NAME, VAULT_LOCAL_ID))

        val handle = provider.loadMediaSource(encryptedVideo(), client)

        assertNull(handle)
        assertEquals(0, loader.calls)
    }

    @Test
    fun loadMediaSourceReturnsNullWhenPlaintextLoadFails() {
        loader.plaintext = null
        session.unlock(E2eeVaultSessionKey(ACCOUNT_NAME, VAULT_LOCAL_ID))

        val handle = provider.loadMediaSource(encryptedVideo(), client)

        assertNull(handle)
        assertEquals(1, loader.calls)
    }

    @Test
    fun loadMediaSourceCreatesHandleWhenVaultIsUnlocked() {
        session.unlock(E2eeVaultSessionKey(ACCOUNT_NAME, VAULT_LOCAL_ID))

        val handle = provider.loadMediaSource(encryptedVideo(), client)

        assertNotNull(handle)
        assertEquals(1, loader.calls)
        handle?.close()
    }

    @Test
    fun loadMediaSourceReturnsNullWhenVideoIsNotRenderable() {
        val provider = E2eeVideoMediaSourceProvider(user, storageManager, session, loader) { false }
        session.unlock(E2eeVaultSessionKey(ACCOUNT_NAME, VAULT_LOCAL_ID))

        val handle = provider.loadMediaSource(encryptedVideo(), client)

        assertNull(handle)
        assertEquals(1, loader.calls)
    }

    @Test
    fun closeWipesPlaintextBuffer() {
        val plaintext = PLAINTEXT.copyOf()
        val handle = E2eeVideoMediaSourceHandle(encryptedVideo(), plaintext)

        handle.close()

        assertArrayEquals(ByteArray(PLAINTEXT.size), plaintext)
    }

    @Test
    fun normalizedMimeTypeUsesProjectAviMimeTypeForAviExtension() {
        val file = encryptedVideo().apply {
            remotePath = "/encrypted/video.AVI"
            mimeType = LEGACY_AVI_MIME_TYPE
        }

        assertEquals(AVI_MIME_TYPE, E2eeVideoMediaSourceHandle.normalizedMimeType(file))
    }

    @Test
    fun normalizedMimeTypeKeepsNonAviMimeType() {
        assertEquals(VIDEO_MIME_TYPE, E2eeVideoMediaSourceHandle.normalizedMimeType(encryptedVideo()))
    }

    private fun encryptedVideo(): OCFile = OCFile(ENCRYPTED_VIDEO_REMOTE_PATH).apply {
        isEncrypted = true
        mimeType = VIDEO_MIME_TYPE
    }

    private fun normalVideo(): OCFile = OCFile(ENCRYPTED_VIDEO_REMOTE_PATH).apply {
        isEncrypted = false
        mimeType = VIDEO_MIME_TYPE
    }

    private class FakePlaintextMediaLoader : E2eePlaintextMediaLoader {
        var calls = 0
        var plaintext: ByteArray? = PLAINTEXT.copyOf()

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

    private class TestClock : Clock {
        var now = START_TIME
        override val currentTime: Long
            get() = now
        override val currentDate: Date
            get() = Date(now)
        override val millisSinceBoot: Long
            get() = now
        override val tz: TimeZone = TimeZone.getTimeZone("UTC")
    }

    companion object {
        private const val ACCOUNT_NAME = "user@example.org"
        private const val ACCOUNT_TYPE = "nextcloud"
        private const val PARENT_REMOTE_PATH = "/encrypted/"
        private const val ENCRYPTED_VIDEO_REMOTE_PATH = "/encrypted/video.mp4"
        private const val VIDEO_MIME_TYPE = "video/mp4"
        private const val LEGACY_AVI_MIME_TYPE = "video/avi"
        private const val AVI_MIME_TYPE = "video/x-msvideo"
        private const val START_TIME = 1_000L
        private const val ONE_MILLISECOND = 1L
        private const val VAULT_LOCAL_ID = 42L
        private val PLAINTEXT = byteArrayOf(1, 2, 3, 4)
    }
}
