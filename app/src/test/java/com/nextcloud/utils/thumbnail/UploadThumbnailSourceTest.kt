/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.utils.thumbnail

import com.owncloud.android.datamodel.UploadsStorageManager.UploadStatus
import com.owncloud.android.db.OCUpload
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.io.File

class UploadThumbnailSourceTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun readableLocalPhotoIsUsedAcrossUploadStates() {
        val source = temporaryFolder.newFile("photo.jpg")
        val upload = upload(source)

        for (state in UploadStatus.entries) {
            whenever(upload.uploadStatus).thenReturn(state)
            val thumbnail = requireNotNull(localUploadThumbnail(upload))
            assertEquals(source.absolutePath, thumbnail.storagePath)
            assertNull(thumbnail.remoteId)
        }
    }

    @Test
    fun missingSourceOrUnsupportedMimeTypeUsesTheExistingFallback() {
        assertNull(localUploadThumbnail(upload(File(temporaryFolder.root, "missing.jpg"))))
        assertNull(localUploadThumbnail(upload(temporaryFolder.newFile("marker.txt"), "text/plain")))
    }

    private fun upload(source: File, mime: String = "image/jpeg"): OCUpload = mock {
        on { localPath } doReturn source.absolutePath
        on { remotePath } doReturn "/uploads/photo.jpg"
        on { mimeType } doReturn mime
    }
}
