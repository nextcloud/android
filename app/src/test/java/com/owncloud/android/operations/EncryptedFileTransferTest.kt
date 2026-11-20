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
import com.owncloud.android.lib.resources.files.ReadFileRemoteOperation
import com.owncloud.android.lib.resources.files.model.RemoteFile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.Mockito.mockConstruction
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.inOrder
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@Suppress("DEPRECATION")
class EncryptedFileTransferTest {
    private val storage: FileDataStorageManager = mock()
    private val client: OwnCloudClient = mock()
    private val source = node("/secret/id", "/secret/report.txt", 2, 1, false)
    private val destination = node("/public/", "/public/", 3, 0, true)
    private val ok = RemoteOperationResult<Any>(ResultCode.OK)
    private val failed = RemoteOperationResult<Any>(ResultCode.UNKNOWN_ERROR)
    private var sourceEtag = "original-version"

    @Test
    fun moveDeletesSourceOnlyAfterSuccessfulCopy() = withOperations { content, remover ->
        doReturn(ok).whenever(content).copy(source, destination, "report.txt", client)
        doReturn(ok).whenever(remover).remove(source, "original-version", client)

        assertTrue(transfer().move(source, destination, client).isSuccess)

        inOrder(content, remover) {
            verify(content).copy(source, destination, "report.txt", client)
            verify(remover).remove(source, "original-version", client)
        }
    }

    @Test
    fun uploadFailurePreservesSource() = withOperations { content, remover ->
        doReturn(failed).whenever(content).copy(source, destination, "report.txt", client)

        assertFalse(transfer().move(source, destination, client).isSuccess)

        verify(remover, never()).remove(any(), any(), any())
    }

    @Test
    fun copyDoesNotDeleteSource() = withOperations { content, remover ->
        doReturn(ok).whenever(content).copy(source, destination, "report.txt", client)

        assertTrue(transfer().copy(source, destination, client).isSuccess)

        verify(remover, never()).remove(any(), any(), any())
    }

    @Test
    fun rejectedConditionalDeleteIsReportedAsFailure() = withOperations { content, remover ->
        doReturn(ok).whenever(content).copy(source, destination, "report.txt", client)
        doReturn(failed).whenever(remover).remove(source, "original-version", client)

        assertFalse(transfer().move(source, destination, client).isSuccess)
    }

    @Test
    fun moveWithoutSourceVersionDoesNotStartCopying() = withOperations { content, remover ->
        sourceEtag = ""
        assertFalse(transfer().move(source, destination, client).isSuccess)
        verify(content, never()).copy(any(), any(), any(), any())
        verify(remover, never()).remove(any(), any(), any())
    }

    @Test
    fun unsyncedLocalChangesPreventCopyingAnOlderServerVersion() = withOperations { content, remover ->
        doReturn(true).whenever(remover).hasLocalChanges(source)

        assertEquals(ResultCode.SYNC_CONFLICT, transfer().move(source, destination, client).code)

        verify(content, never()).copy(any(), any(), any(), any())
        verify(remover, never()).remove(any(), any(), any())
    }

    @Test
    fun sameFolderCopyRenamesOnlyTheFilenameWhenParentContainsDots() = withOperations { content, _ ->
        doReturn(destination.fileId).whenever(source).parentId
        doReturn("README").whenever(source).fileName
        doReturn("README").whenever(source).decryptedFileName
        doReturn("/secret.v2/").whenever(destination).remotePath
        doReturn(listOf(source)).whenever(storage).getFolderContent(destination, false)
        doReturn(ok).whenever(content).copy(source, destination, "README (2)", client)

        assertTrue(transfer().copy(source, destination, client).isSuccess)

        verify(content).copy(source, destination, "README (2)", client)
    }

    @Test
    fun folderMoveKeepsWholeSourceWhenAChildFails() = withOperations { content, remover ->
        val folder = node("/secret/folder/", "/secret/photos/", 4, 1, true)
        val target = node("/public/photos/", "/public/photos/", 5, 3, true)
        val second = node("/secret/folder/second", "/secret/photos/second.txt", 6, 4, false)
        doReturn(listOf(source, second)).whenever(storage).getFolderContent(folder, false)
        doReturn(target).whenever(storage).getFileByDecryptedRemotePath("/public/photos/")
        doReturn(ok).whenever(content).copy(source, target, "report.txt", client)
        doReturn(failed).whenever(content).copy(second, target, "second.txt", client)

        mockConstruction(CreateFolderOperation::class.java) { create, _ ->
            doReturn(ok).whenever(create).execute(client)
        }.use {
            assertFalse(transfer().move(folder, destination, client).isSuccess)
        }

        verify(content).copy(source, target, "report.txt", client)
        verify(remover, never()).remove(any(), any(), any())
    }

    @Test
    fun keepingEncryptionEncryptsNewFolderBeforeCopyingChildren() = withOperations { content, remover ->
        val folder = node("/secret/folder/", "/secret/photos/", 4, 1, true)
        val target = node("/public/photos/", "/public/photos/", 5, 3, true)
        doReturn(listOf(source)).whenever(storage).getFolderContent(folder, false)
        doReturn(target).whenever(storage).getFileByDecryptedRemotePath("/public/photos/")
        doReturn(ok).whenever(remover).encrypt(target, client)
        doReturn(ok).whenever(content).copy(source, target, "report.txt", client)

        mockConstruction(CreateFolderOperation::class.java) { create, _ ->
            doReturn(ok).whenever(create).execute(client)
        }.use {
            assertTrue(transfer(keepEncryption = true).copy(folder, destination, client).isSuccess)
        }

        inOrder(remover, content) {
            verify(remover).encrypt(target, client)
            verify(content).copy(source, target, "report.txt", client)
        }
    }

    @Test
    fun encryptedFolderCopyUsesDisplayPathsForCreationAndDisplayNamesForChildren() = withOperations { content, _ ->
        val folder = node("/secret/source-id/", "/保险箱/项目资料/", 4, 1, true)
        val parent = node("/secret/target-id/", "/加密区/目标/", 5, 1, true)
        val target = node("/secret/target-id/new-id/", "/加密区/目标/项目资料/", 6, 5, true)
        doReturn(listOf(source)).whenever(storage).getFolderContent(folder, false)
        doReturn(target).whenever(storage).getFileByDecryptedRemotePath("/加密区/目标/项目资料/")
        doReturn(ok).whenever(content).copy(source, target, "report.txt", client)

        mockConstruction(CreateFolderOperation::class.java) { create, construction ->
            assertEquals("/加密区/目标/项目资料/", construction.arguments()[0])
            doReturn(ok).whenever(create).execute(client)
        }.use {
            assertTrue(transfer().copy(folder, parent, client).isSuccess)
        }

        verify(content).copy(source, target, "report.txt", client)
    }

    @Test
    fun folderCannotBeCopiedIntoItsOwnDescendant() = withOperations { content, remover ->
        val folder = node("/secret/folder/", "/secret/photos/", 4, 1, true)
        val descendant = node("/secret/folder/child/", "/secret/photos/child/", 5, 4, true)

        val result = transfer().copy(folder, descendant, client)

        assertEquals(ResultCode.INVALID_COPY_INTO_DESCENDANT, result.code)
        verify(content, never()).copy(any(), any(), any(), any())
        verify(remover, never()).remove(any(), any(), any())
    }

    private fun transfer(keepEncryption: Boolean = false) = EncryptedFileTransfer(
        storage,
        mock(),
        mock(),
        mock(),
        keepEncryption
    )

    private fun withOperations(test: (FileTransferContent, FileTransferSource) -> Unit) {
        doReturn(emptyList<OCFile>()).whenever(storage).getFolderContent(destination, false)
        val content: FileTransferContent = mock()
        val remover: FileTransferSource = mock()
        mockConstruction(FileTransferContent::class.java) { copy, _ ->
            doAnswer {
                content.copy(it.getArgument(0), it.getArgument(1), it.getArgument(2), it.getArgument(3))
            }.whenever(copy).copy(any(), any(), any(), any())
        }.use {
            mockConstruction(FileTransferSource::class.java) { source, _ ->
                doAnswer { remover.hasLocalChanges(it.getArgument(0)) }
                    .whenever(source).hasLocalChanges(any())
                doAnswer {
                    remover.remove(it.getArgument(0), it.getArgument(1), it.getArgument(2))
                }.whenever(source).remove(any(), any(), any())
                doAnswer {
                    remover.encrypt(it.getArgument(0), it.getArgument(1))
                }.whenever(source).encrypt(any(), any())
            }.use {
                mockConstruction(RefreshFolderOperation::class.java) { refresh, _ ->
                    doReturn(ok).whenever(refresh).execute(client)
                }.use {
                    withStat { test(content, remover) }
                }
            }
        }
    }

    private fun withStat(test: () -> Unit) {
        val remote: RemoteFile = mock { on { etag } doAnswer { sourceEtag } }
        val stat: RemoteOperationResult<Any> = mock {
            on { isSuccess } doReturn true
            on { data } doReturn arrayListOf(remote)
        }
        mockConstruction(ReadFileRemoteOperation::class.java) { read, _ ->
            doReturn(stat).whenever(read).execute(client)
        }.use { test() }
    }

    private fun node(path: String, displayPath: String, id: Long, parent: Long, folder: Boolean): OCFile = mock {
        on { remotePath } doReturn path
        on { decryptedRemotePath } doReturn displayPath
        on { fileName } doReturn displayPath.trimEnd('/').substringAfterLast('/')
        on { decryptedFileName } doReturn displayPath.trimEnd('/').substringAfterLast('/')
        on { fileId } doReturn id
        on { parentId } doReturn parent
        on { isFolder } doReturn folder
        on { isEncrypted } doReturn path.startsWith("/secret/")
    }
}
