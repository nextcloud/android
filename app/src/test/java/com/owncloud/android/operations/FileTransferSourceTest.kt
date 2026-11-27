/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.owncloud.android.operations

import com.owncloud.android.datamodel.FileDataStorageManager
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.lib.common.OwnCloudClient
import com.owncloud.android.lib.common.operations.RemoteOperationResult
import com.owncloud.android.lib.common.operations.RemoteOperationResult.ResultCode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.Mockito.mockConstruction
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@Suppress("DEPRECATION")
class FileTransferSourceTest {
    private val storage: FileDataStorageManager = mock()
    private val client: OwnCloudClient = mock()
    private val source: OCFile = mock {
        on { remotePath } doReturn "/secret/"
        on { encryptedFileName } doReturn "secret"
        on { parentId } doReturn 1L
        on { isEncrypted } doReturn true
        on { isFolder } doReturn true
    }
    private val parent: OCFile = mock()

    @Test
    fun encryptedRootIsLockedThroughItsOwnMetadata() {
        doReturn(parent).whenever(storage).getFileById(1L)
        doReturn(true).whenever(storage).removeFile(source, true, true)
        mockConstruction(RemoveRemoteEncryptedFileOperation::class.java) { removal, construction ->
            assertSame(source, construction.arguments()[4])
            assertEquals("original-etag", construction.arguments()[6])
            doReturn(RemoteOperationResult<Void>(ResultCode.OK)).whenever(removal).execute(client)
        }.use {
            assertTrue(FileTransferSource(mock(), mock(), storage).remove(source, "original-etag", client).isSuccess)
        }
        verify(storage).removeFile(source, true, true)
    }

    @Test
    fun encryptedSubfolderUsesItsEncryptedParentMetadata() {
        doReturn(true).whenever(parent).isEncrypted
        doReturn(parent).whenever(storage).getFileById(1L)
        doReturn(true).whenever(storage).removeFile(source, true, true)
        mockConstruction(RemoveRemoteEncryptedFileOperation::class.java) { removal, construction ->
            assertSame(parent, construction.arguments()[4])
            doReturn(RemoteOperationResult<Void>(ResultCode.OK)).whenever(removal).execute(client)
        }.use {
            assertTrue(FileTransferSource(mock(), mock(), storage).remove(source, "original-etag", client).isSuccess)
        }
    }

    @Test
    fun failedRemoteDeleteDoesNotRemoveLocalSource() {
        doReturn(parent).whenever(storage).getFileById(1L)
        mockConstruction(RemoveRemoteEncryptedFileOperation::class.java) { removal, _ ->
            doReturn(RemoteOperationResult<Void>(ResultCode.UNKNOWN_ERROR)).whenever(removal).execute(client)
        }.use {
            assertFalse(FileTransferSource(mock(), mock(), storage).remove(source, "original-etag", client).isSuccess)
        }
        verify(storage, never()).removeFile(eq(source), any(), any())
    }

    @Test
    fun editingADownloadedChildDuringTransferPreservesTheWholeSource() {
        val child: OCFile = mock {
            on { localModificationTimestamp } doReturn 2L
            on { lastSyncDateForData } doReturn 1L
        }
        doReturn(listOf(child)).whenever(storage).getFolderContent(source, false)

        val result = FileTransferSource(mock(), mock(), storage).remove(source, "original-etag", client)

        assertEquals(ResultCode.SYNC_CONFLICT, result.code)
        verify(storage, never()).removeFile(eq(source), any(), any())
        verify(storage, never()).getFileById(any())
    }

    @Test
    fun unresolvedConflictPreventsSourceDeletion() {
        doReturn(true).whenever(source).isInConflict

        val result = FileTransferSource(mock(), mock(), storage).remove(source, "original-etag", client)

        assertEquals(ResultCode.SYNC_CONFLICT, result.code)
        verify(storage, never()).removeFile(eq(source), any(), any())
    }

    @Test
    fun conditionalDeleteQuotesBothRawAndAlreadyQuotedEtags() {
        assertEquals("\"version\"", FileTransferSource.quotedEtag("version"))
        assertEquals("\"version\"", FileTransferSource.quotedEtag("\"version\""))
    }
}
