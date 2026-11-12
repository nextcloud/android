/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.owncloud.android.operations.upload

import android.content.Context
import com.nextcloud.client.account.User
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.operations.DownloadFileOperation
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import java.io.File
import java.io.IOException

class EncryptedConflictFilesTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun verifiedServerCopyBecomesLocalCopyWithoutAnotherDownload() {
        val local = temporaryFolder.newFile("local.jpg").apply { writeText("older local content") }
        val snapshot = temporaryFolder.newFile("snapshot.jpg").apply { writeText("new server content") }
        val file = OCFile("/private/ciphertext").apply { storagePath = local.absolutePath }
        val verified = OCFile(file.remotePath).apply {
            isEncrypted = true
            storagePath = snapshot.absolutePath
            modificationTimestamp = 1_700_000_000_000
            etag = "verified-etag"
            mimeType = "image/jpeg"
            remoteId = "remote-id"
        }
        val user: User = mock { on { accountName } doReturn "account-A" }
        val operation = DownloadFileOperation(user, file, mock())

        operation.useVerifiedCopy(verified)

        assertArrayEquals(snapshot.readBytes(), local.readBytes())
        assertEquals(verified.modificationTimestamp, local.lastModified())
        assertEquals(verified.modificationTimestamp, operation.getModificationTimestamp())
        assertEquals(verified.etag, operation.etag)
        assertEquals(verified.mimeType, operation.mimeType)
        assertEquals(verified.remoteId, file.remoteId)
        assertFalse(temporaryFolder.root.listFiles()!!.any { it.name.startsWith("verified-download-") })
    }

    @Test
    fun missingSnapshotCannotReplaceExistingLocalFileAndStagingIsCleaned() {
        val local = temporaryFolder.newFile().apply { writeText("preserve this") }
        val file = OCFile("/private/ciphertext").apply { storagePath = local.absolutePath }
        val verified = OCFile(file.remotePath).apply {
            isEncrypted = true
            storagePath = File(temporaryFolder.root, "missing").absolutePath
        }
        val operation = DownloadFileOperation(mock(), file, mock())

        assertThrows(IOException::class.java) { operation.useVerifiedCopy(verified) }
        assertEquals("preserve this", local.readText())
        assertFalse(temporaryFolder.root.listFiles()!!.any { it.name.startsWith("verified-download-") })
    }

    @Test
    fun previewReleaseDeletesOnlyOwnedTemporaryFiles() {
        val context: Context = mock { on { cacheDir } doReturn temporaryFolder.root }
        val preview = File(temporaryFolder.newFolder("encrypted-conflicts"), "conflict.jpg").apply {
            writeText("private")
        }
        val personal = temporaryFolder.newFile("personal.jpg").apply { writeText("preserve") }
        val file = OCFile("/ciphertext").apply { isEncrypted = true }

        file.storagePath = personal.absolutePath
        EncryptedUploadConflictChecker.releasePreview(context, file)
        assertTrue(personal.exists())
        file.storagePath = preview.absolutePath
        EncryptedUploadConflictChecker.releasePreview(context, file)
        assertFalse(preview.exists())
    }

    @Test
    fun processRestartCleansOrphansAndPreservesOtherCacheFiles() {
        val context: Context = mock { on { cacheDir } doReturn temporaryFolder.root }
        val preview = File(temporaryFolder.newFolder("encrypted-conflicts"), "conflict.jpg").apply {
            writeText("private")
        }
        val abandoned = temporaryFolder.newFolder("encrypted-comparison-abandoned")
        File(abandoned, "partial").writeText("partial download")
        val unrelated = temporaryFolder.newFile("other-cache")

        EncryptedUploadConflictChecker.clearOrphanedPreviews(context)

        assertFalse(preview.exists())
        assertFalse(abandoned.exists())
        assertTrue(unrelated.exists())
    }
}
