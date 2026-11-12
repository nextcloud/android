/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.owncloud.android.operations.upload

import com.nextcloud.client.account.User
import com.nextcloud.utils.extensions.isNonRetryable
import com.owncloud.android.datamodel.ArbitraryDataProvider
import com.owncloud.android.datamodel.FileDataStorageManager
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.db.UploadResult
import com.owncloud.android.lib.common.OwnCloudClient
import com.owncloud.android.lib.common.operations.RemoteOperationResult
import com.owncloud.android.lib.common.operations.RemoteOperationResult.ResultCode
import com.owncloud.android.lib.resources.files.ReadFileRemoteOperation
import com.owncloud.android.lib.resources.e2ee.GetMetadataRemoteOperation
import com.owncloud.android.lib.resources.files.model.RemoteFile
import com.owncloud.android.operations.CreateFolderOperation
import com.owncloud.android.operations.RefreshFolderOperation
import com.owncloud.android.operations.UploadFileOperation
import com.owncloud.android.utils.MimeType
import com.owncloud.android.utils.EncryptionUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.Mockito.mockConstruction
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class UploadFolderBackendTest {
    private val context: android.content.Context = mock()
    private val user: User = mock { on { accountName } doReturn "A" }
    private val storage: FileDataStorageManager = mock()
    private val client: OwnCloudClient = mock()
    private val keys: ArbitraryDataProvider = mock {
        on { getValue(any<String>(), any()) } doReturn ""
    }
    private val operation: UploadFileOperation = mock {
        on { user } doReturn this@UploadFolderBackendTest.user
        on { storageManager } doReturn storage
        on { context } doReturn this@UploadFolderBackendTest.context
    }

    @Test
    fun missingTaskAccountKeysStopsBeforeCreatingChildrenAndRemainsRetryable() {
        val root = OCFile("/").apply { mimeType = MimeType.DIRECTORY }
        val encrypted = OCFile("/private/").apply { mimeType = MimeType.DIRECTORY }
        whenever(storage.getFileByEncryptedRemotePath("/")).thenReturn(root)
        whenever(storage.getFolderContent(root, false)).thenReturn(listOf(encrypted))
        val properties: RemoteFile = mock {
            on { remoteId } doReturn "encrypted-id"
            on { isEncrypted } doReturn true
        }
        val rootProperties: RemoteFile = mock { on { remoteId } doReturn "root-id" }
        val rootResult: RemoteOperationResult<Any> = mock {
            on { isSuccess } doReturn true
            on { data } doReturn arrayListOf(rootProperties)
        }
        val encryptedResult: RemoteOperationResult<Any> = mock {
            on { isSuccess } doReturn true
            on { data } doReturn arrayListOf(properties)
        }
        mockConstruction(ReadFileRemoteOperation::class.java) { read, construction ->
            doReturn(if (construction.arguments()[0] == "/") rootResult else encryptedResult)
                .whenever(read).execute(client)
        }.use {
            mockConstruction(RefreshFolderOperation::class.java) { refresh, _ ->
                doReturn(RemoteOperationResult<Any>(ResultCode.OK)).whenever(refresh).execute(client)
            }.use { refreshes ->
                mockConstruction(CreateFolderOperation::class.java).use { creations ->
                    val error = assertThrows(UploadFolderException::class.java) {
                        UploadFolderBackend(operation, client, keys).resolve("/private/new/")
                    }

                    assertTrue(error.result.exception is UploadFileException.EncryptionKeysUnavailable)
                    assertFalse(UploadResult.fromOperationResult(error.result).isNonRetryable())
                    assertEquals(1, refreshes.constructed().size)
                    assertTrue(creations.constructed().isEmpty())
                }
            }
        }
        verify(keys, never()).getValue(org.mockito.kotlin.eq("B"), any())
    }

    @Test
    fun remoteReadFailureDoesNotUseCachedEncryptedFolderOrCreateAnything() {
        val root = OCFile("/").apply { mimeType = MimeType.DIRECTORY }
        whenever(storage.getFileByEncryptedRemotePath("/")).thenReturn(root)
        mockConstruction(ReadFileRemoteOperation::class.java) { read, _ ->
            doReturn(RemoteOperationResult<Any>(ResultCode.UNAUTHORIZED)).whenever(read).execute(client)
        }.use {
            mockConstruction(CreateFolderOperation::class.java).use { creations ->
                val error = assertThrows(UploadFolderException::class.java) {
                    UploadFolderBackend(operation, client, keys).resolve("/private/new/")
                }

                assertEquals(ResultCode.UNAUTHORIZED, error.result.code)
                assertTrue(creations.constructed().isEmpty())
                verify(storage, never()).getFolderContent(any<OCFile>(), any())
            }
        }
    }

    @Test
    fun cancelledPreparationStopsBeforeNetworkAccessOrFolderCreation() {
        val root = OCFile("/").apply { mimeType = MimeType.DIRECTORY }
        whenever(storage.getFileByEncryptedRemotePath("/")).thenReturn(root)
        whenever(operation.isCancellationRequested).thenReturn(true)
        mockConstruction(ReadFileRemoteOperation::class.java).use { reads ->
            mockConstruction(CreateFolderOperation::class.java).use { creations ->
                val error = assertThrows(UploadFolderException::class.java) {
                    UploadFolderBackend(operation, client, keys).resolve("/private/new/")
                }

                assertEquals(ResultCode.USER_CANCELLED, error.result.code)
                assertTrue(reads.constructed().isEmpty())
                assertTrue(creations.constructed().isEmpty())
            }
        }
    }

    @Test
    fun deletedEncryptedTargetIsRecreatedAsOrdinaryWithoutEncryptionKeys() {
        val root = OCFile("/").apply { mimeType = MimeType.DIRECTORY }
        val target = OCFile("/private/").apply { mimeType = MimeType.DIRECTORY }
        whenever(storage.getFileByEncryptedRemotePath("/")).thenReturn(root)
        whenever(storage.getFolderContent(root, false)).thenReturn(emptyList(), listOf(target))
        val properties: RemoteFile = mock { on { remoteId } doReturn "00000042instance" }
        val readResult: RemoteOperationResult<Any> = mock {
            on { isSuccess } doReturn true
            on { data } doReturn arrayListOf(properties)
        }
        mockConstruction(ReadFileRemoteOperation::class.java) { read, _ ->
            doReturn(readResult).whenever(read).execute(client)
        }.use {
            mockConstruction(RefreshFolderOperation::class.java) { refresh, _ ->
                doReturn(RemoteOperationResult<Any>(ResultCode.OK)).whenever(refresh).execute(client)
            }.use {
                mockConstruction(CreateFolderOperation::class.java) { create, construction ->
                    assertEquals("/private/", construction.arguments()[0])
                    doReturn(RemoteOperationResult<Any>(ResultCode.OK)).whenever(create).execute(client)
                }.use { creations ->
                    val resolved = UploadFolderBackend(operation, client, keys).resolve("/private/")

                    assertEquals(target, resolved)
                    assertFalse(resolved.isEncrypted)
                    assertEquals(1, creations.constructed().size)
                }
            }
        }
        verify(keys, never()).getValue("A", EncryptionUtils.PRIVATE_KEY)
        verify(keys, never()).getValue("A", EncryptionUtils.PUBLIC_KEY)
    }

    @Test
    fun encryptedFolderRefreshUsesExistingMetadataValidationWithoutDownloadingAgain() {
        val root = OCFile("/").apply { mimeType = MimeType.DIRECTORY }
        val target = OCFile("/private/").apply { mimeType = MimeType.DIRECTORY }
        whenever(storage.getFileByEncryptedRemotePath("/")).thenReturn(root)
        whenever(storage.getFolderContent(root, false)).thenReturn(listOf(target))
        whenever(keys.getValue("A", EncryptionUtils.PRIVATE_KEY)).thenReturn("key")
        whenever(keys.getValue("A", EncryptionUtils.PUBLIC_KEY)).thenReturn("key")
        mockConstruction(ReadFileRemoteOperation::class.java) { read, construction ->
            val properties: RemoteFile = mock {
                on { remoteId } doReturn "00000042instance"
                on { isEncrypted } doReturn (construction.arguments()[0] != "/")
            }
            val result: RemoteOperationResult<Any> = mock {
                on { isSuccess } doReturn true
                on { data } doReturn arrayListOf(properties)
            }
            doReturn(result).whenever(read).execute(client)
        }.use {
            mockConstruction(RefreshFolderOperation::class.java) { refresh, _ ->
                doReturn(RemoteOperationResult<Any>(ResultCode.OK)).whenever(refresh).execute(client)
            }.use { refreshes ->
                mockConstruction(GetMetadataRemoteOperation::class.java).use { metadata ->
                    assertEquals(target, UploadFolderBackend(operation, client, keys).resolve("/private/"))
                    assertEquals(2, refreshes.constructed().size)
                    assertTrue(metadata.constructed().isEmpty())
                }
            }
        }
    }
}
