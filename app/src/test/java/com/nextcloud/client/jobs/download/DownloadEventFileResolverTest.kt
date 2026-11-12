/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.nextcloud.client.jobs.download

import com.nextcloud.client.account.User
import com.owncloud.android.datamodel.FileDataStorageManager
import com.owncloud.android.datamodel.OCFile
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class DownloadEventFileResolverTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private val storage: FileDataStorageManager = mock()
    private val user: User = mock()

    @Before
    fun setUp() {
        whenever(user.accountName).thenReturn("account-A")
        whenever(storage.user).thenReturn(user)
    }

    @Test
    fun encryptedDownloadCompletionResolvesDownloadedPdfByPhysicalPath() {
        val file = OCFile("/cipher-folder/cipher-file").apply {
            isEncrypted = true
            decryptedRemotePath = "/private/document.pdf"
            mimeType = "application/pdf"
            storagePath = temporaryFolder.newFile("document.pdf").absolutePath
        }
        whenever(storage.getFileByEncryptedRemotePath(file.remotePath)).thenReturn(file)

        assertSame(file, DownloadEventFileResolver.resolve(storage, "account-A", file.remotePath))
        assertTrue(file.isDown)
        verify(storage, never()).getFileByDecryptedRemotePath(any())
    }

    @Test
    fun ordinaryDownloadUsesTheSamePhysicalPathLookup() {
        val file = OCFile("/document.pdf")
        whenever(storage.getFileByEncryptedRemotePath(file.remotePath)).thenReturn(file)

        assertSame(file, DownloadEventFileResolver.resolve(storage, "account-A", file.remotePath))
    }

    @Test
    fun anotherAccountsDownloadCannotOpenCurrentAccountsFileAtTheSamePath() {
        assertNull(DownloadEventFileResolver.resolve(storage, "account-B", "/document.pdf"))
        verify(storage, never()).getFileByEncryptedRemotePath(any())
    }

    @Test
    fun missingAccountOrPathCannotResolveAFile() {
        assertNull(DownloadEventFileResolver.resolve(storage, null, "/document.pdf"))
        assertNull(DownloadEventFileResolver.resolve(storage, "account-A", null))
        assertNull(DownloadEventFileResolver.resolve(storage, "account-A", ""))
        verify(storage, never()).getFileByEncryptedRemotePath(any())
    }

    @Test
    fun absentPhysicalFileCannotResolveToAnotherFilesDecryptedName() {
        whenever(storage.getFileByDecryptedRemotePath("/cipher-file"))
            .thenReturn(OCFile("/unrelated-file"))

        assertNull(DownloadEventFileResolver.resolve(storage, "account-A", "/cipher-file"))
        verify(storage, never()).getFileByDecryptedRemotePath(any())
    }
}
