/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.nextcloud.utils.thumbnail

import android.widget.ImageView
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.datamodel.UploadsStorageManager.UploadStatus
import com.owncloud.android.db.OCUpload
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.mockito.kotlin.clearInvocations
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions

class ThumbnailGeneratorTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private val files: FileThumbnailGenerator = mock()
    private val folders: FolderThumbnailGenerator = mock()
    private val view: ImageView = mock()
    private val generator = ThumbnailGenerator(files, folders)

    @Test
    fun uploadsInEveryStateGenerateLocalThumbnailsDespiteHavingNoServerId() {
        val local = temporaryFolder.newFile("photo.jpg")
        val remote = OCFile("/encrypted/cipher").apply { remoteId = "server-id" }
        for (status in UploadStatus.entries) {
            clearInvocations(files)
            val upload = mock<OCUpload> {
                on { localPath } doReturn local.absolutePath
                on { remotePath } doReturn remote.remotePath
                on { mimeType } doReturn "image/jpeg"
                on { uploadStatus } doReturn status
            }
            val source = UploadThumbnailSource.forUpload(upload, remote)
            assertTrue(source.isOfflineOperation)

            generator.setUploadThumbnail(source, view)

            verify(files).setThumbnail(source, view, ThumbnailArguments.none)
            verify(files, never()).setOfflineOperationThumbnail(source, view)
        }
        verifyNoInteractions(folders)
    }

    @Test
    fun uploadWithoutLocalOriginalUsesTheExistingRemoteThumbnailPath() {
        val remote = OCFile("/photo.jpg").apply { remoteId = "server-id" }
        val upload = mock<OCUpload> {
            on { localPath } doReturn temporaryFolder.root.resolve("missing.jpg").absolutePath
        }
        val source = UploadThumbnailSource.forUpload(upload, remote)

        generator.setUploadThumbnail(source, view)

        verify(files).setThumbnail(remote, view, ThumbnailArguments.none)
        verifyNoInteractions(folders)
    }

    @Test
    fun fileListKeepsTheOfflineOperationThumbnailBehavior() {
        val offline = OCFile("/photo.jpg")

        generator.setThumbnail(offline, view)

        verify(files).setOfflineOperationThumbnail(offline, view)
        verify(files, never()).setThumbnail(offline, view, ThumbnailArguments.none)
        verifyNoInteractions(folders)
    }
}
