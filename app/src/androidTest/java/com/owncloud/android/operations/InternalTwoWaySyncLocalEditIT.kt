/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.owncloud.android.operations

import android.media.MediaScannerConnection
import android.net.Uri
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

class InternalTwoWaySyncLocalEditIT : AbstractInternalTwoWaySyncIT() {

    companion object {
        private const val IMAGE_ASSET = "image.jpg"
        private const val IMAGE_NAME = "edited.jpg"
        private const val MEDIA_SCAN_TIMEOUT_SECONDS = 10L
        private val EDITED_BYTES = byteArrayOf(0, 1, 2, 3)
    }

    @Test
    fun testWhenLocalImageEditedGivenShouldKeepLocalAndServerCopyAcrossSyncs() {
        val folder = setUpTwoWaySyncFolder("/twoWaySyncEditedImage/")
        val localImage = File(folder.storagePath, IMAGE_NAME)
        getFile(IMAGE_ASSET).copyTo(localImage)
        syncAndAssertSuccess(folder.remotePath)

        val before = storageManager.getFileByPath(folder.childPath(IMAGE_NAME))
        assertNotNull(before)
        assertEquals(localImage, File(before.storagePath))
        scanIntoMediaStore(localImage)

        shortSleep()
        localImage.appendBytes(EDITED_BYTES)
        syncAndAssertSuccess(folder.remotePath)

        assertTrue(
            "Edited image was not uploaded within timeout",
            waitUntil { storageManager.getFileByPath(folder.childPath(IMAGE_NAME))?.etag != before.etag }
        )
        assertTrue("Edited image was removed locally after upload", localImage.exists())

        syncAndAssertSuccess(folder.remotePath)

        assertTrue("Edited image was removed locally after next sync", localImage.exists())
        assertTrue("Edited image was removed from server after next sync", existsOnServer(before.remotePath))
    }

    @Test
    fun testWhenLocalFileMissingAndServerChangedGivenShouldDownloadInsteadOfDeletingOnServer() {
        val folder = setUpTwoWaySyncFolderWithFile("/twoWaySyncMissingLocallyChangedOnServer/")
        val localFile = File(storageManager.getFileByPath(folder.childPath(FILE_NAME)).storagePath)
        assertTrue(localFile.delete())
        val newVersion = getDummyFile(LARGE_DUMMY_FILE)
        uploadDirectlyToServer(newVersion, folder.childPath(FILE_NAME))

        syncAndAssertSuccess(folder.remotePath)

        assertTrue(existsOnServer(folder.childPath(FILE_NAME)))
        assertTrue(localFile.exists())
        assertEquals(newVersion.length(), localFile.length())
    }

    private fun scanIntoMediaStore(file: File) {
        val latch = CountDownLatch(1)
        val scannedUri = AtomicReference<Uri?>()
        MediaScannerConnection.scanFile(targetContext, arrayOf(file.absolutePath), null) { _, uri ->
            scannedUri.set(uri)
            latch.countDown()
        }

        assertTrue("Media scan did not finish", latch.await(MEDIA_SCAN_TIMEOUT_SECONDS, TimeUnit.SECONDS))
        assertNotNull("Image is not indexed by MediaStore", scannedUri.get())
    }
}
