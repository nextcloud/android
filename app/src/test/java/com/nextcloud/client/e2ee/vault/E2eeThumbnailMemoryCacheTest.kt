/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.nextcloud.client.e2ee.vault

import android.graphics.Bitmap
import com.owncloud.android.datamodel.OCFile
import io.mockk.every
import io.mockk.mockk
import org.junit.After
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class E2eeThumbnailMemoryCacheTest {
    @Before
    fun setUp() {
        E2eeThumbnailMemoryCache.clear()
    }

    @After
    fun tearDown() {
        E2eeThumbnailMemoryCache.clear()
    }

    @Test
    fun getReturnsCachedThumbnailForSameFileVersion() {
        val file = encryptedFile(etag = "etag-a")
        val thumbnail = thumbnail()

        E2eeThumbnailMemoryCache.put(ACCOUNT_NAME, file, thumbnail)

        assertSame(thumbnail, E2eeThumbnailMemoryCache.get(ACCOUNT_NAME, file))
    }

    @Test
    fun getMissesWhenFileVersionChanges() {
        val file = encryptedFile(etag = "etag-a")

        E2eeThumbnailMemoryCache.put(ACCOUNT_NAME, file, thumbnail())
        file.setEtag("etag-b")

        assertNull(E2eeThumbnailMemoryCache.get(ACCOUNT_NAME, file))
    }

    @Test
    fun vaultLockEvictsAccountThumbnails() {
        val accountThumbnail = thumbnail()
        val otherAccountThumbnail = thumbnail()

        E2eeThumbnailMemoryCache.put(ACCOUNT_NAME, encryptedFile(), accountThumbnail)
        E2eeThumbnailMemoryCache.put(OTHER_ACCOUNT_NAME, encryptedFile(), otherAccountThumbnail)

        E2eeThumbnailMemoryCache.onVaultLocked(E2eeVaultSessionKey(ACCOUNT_NAME, 1L))

        assertNull(E2eeThumbnailMemoryCache.get(ACCOUNT_NAME, encryptedFile()))
        assertSame(otherAccountThumbnail, E2eeThumbnailMemoryCache.get(OTHER_ACCOUNT_NAME, encryptedFile()))
    }

    @Test
    fun lockAllEvictsAllThumbnails() {
        E2eeThumbnailMemoryCache.put(ACCOUNT_NAME, encryptedFile(), thumbnail())
        E2eeThumbnailMemoryCache.put(OTHER_ACCOUNT_NAME, encryptedFile(), thumbnail())

        E2eeThumbnailMemoryCache.onAllVaultsLocked()

        assertNull(E2eeThumbnailMemoryCache.get(ACCOUNT_NAME, encryptedFile()))
        assertNull(E2eeThumbnailMemoryCache.get(OTHER_ACCOUNT_NAME, encryptedFile()))
    }

    @Test
    fun putFailureMarksFileAsRecentlyFailed() {
        val file = encryptedFile()

        E2eeThumbnailMemoryCache.putFailure(ACCOUNT_NAME, file)

        assertTrue(E2eeThumbnailMemoryCache.hasRecentFailure(ACCOUNT_NAME, file))
    }

    @Test
    fun putClearsRecentFailure() {
        val file = encryptedFile()

        E2eeThumbnailMemoryCache.putFailure(ACCOUNT_NAME, file)
        E2eeThumbnailMemoryCache.put(ACCOUNT_NAME, file, thumbnail())

        assertFalse(E2eeThumbnailMemoryCache.hasRecentFailure(ACCOUNT_NAME, file))
    }

    @Test
    fun vaultLockEvictsRecentFailuresForAccount() {
        E2eeThumbnailMemoryCache.putFailure(ACCOUNT_NAME, encryptedFile())
        E2eeThumbnailMemoryCache.putFailure(OTHER_ACCOUNT_NAME, encryptedFile())

        E2eeThumbnailMemoryCache.onVaultLocked(E2eeVaultSessionKey(ACCOUNT_NAME, 1L))

        assertFalse(E2eeThumbnailMemoryCache.hasRecentFailure(ACCOUNT_NAME, encryptedFile()))
        assertTrue(E2eeThumbnailMemoryCache.hasRecentFailure(OTHER_ACCOUNT_NAME, encryptedFile()))
    }

    private fun encryptedFile(etag: String = "etag"): OCFile = OCFile("/Photos/secret.jpg").apply {
        isEncrypted = true
        setRemoteId("remote-id")
        setEtag(etag)
        fileLength = FILE_LENGTH
        modificationTimestamp = MODIFICATION_TIMESTAMP
    }

    private fun thumbnail(): Bitmap = mockk {
        every { byteCount } returns BITMAP_BYTES
        every { isRecycled } returns false
    }

    companion object {
        private const val ACCOUNT_NAME = "user@example.org"
        private const val OTHER_ACCOUNT_NAME = "other@example.org"
        private const val BITMAP_BYTES = 4096
        private const val FILE_LENGTH = 123L
        private const val MODIFICATION_TIMESTAMP = 456L
    }
}
