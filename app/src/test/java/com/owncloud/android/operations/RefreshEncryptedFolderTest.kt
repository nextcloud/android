/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.owncloud.android.operations

import android.content.Context
import com.nextcloud.client.account.User
import com.nextcloud.utils.extensions.saveFolder
import com.nextcloud.utils.share.UnifiedShareSharees
import com.owncloud.android.datamodel.FileDataStorageManager
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.datamodel.e2e.v1.decrypted.DecryptedFolderMetadataFileV1
import com.owncloud.android.datamodel.e2e.v2.decrypted.DecryptedFolderMetadataFile
import com.owncloud.android.datamodel.e2e.v2.decrypted.DecryptedMetadata
import com.owncloud.android.lib.resources.files.model.RemoteFile
import com.owncloud.android.lib.resources.status.E2EVersion
import com.owncloud.android.lib.resources.status.OCCapability
import com.owncloud.android.utils.EncryptionUtils
import com.owncloud.android.utils.FileStorageUtils
import com.owncloud.android.utils.MimeType
import com.owncloud.android.utils.theme.CapabilityUtils
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.Mockito.mockStatic
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.any
import org.mockito.kotlin.argThat
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.lang.reflect.InvocationTargetException

class RefreshEncryptedFolderTest {
    @Test
    fun v1RefreshPreservesFolderNameFromParentMetadata() {
        assertFolderMappingPreserved(DecryptedFolderMetadataFileV1(), E2EVersion.V1_2)
    }

    @Test
    fun v2RefreshPreservesFolderNameAndInheritedEncryption() {
        assertFolderMappingPreserved(
            DecryptedFolderMetadataFile(metadata = DecryptedMetadata(metadataKey = ByteArray(0)), version = "2.0"),
            E2EVersion.V2_0
        )
    }

    private fun assertFolderMappingPreserved(metadata: Any, version: E2EVersion) {
        val folder = OCFile("/private/cipher/").apply {
            mimeType = MimeType.DIRECTORY
            decryptedRemotePath = "/private/photos/"
            isEncrypted = true
        }
        val remoteFolder = OCFile(folder.remotePath).apply {
            mimeType = MimeType.DIRECTORY
            decryptedRemotePath = remotePath
        }
        val context: Context = mock()
        val user: User = mock()
        val storage: FileDataStorageManager = mock()
        whenever(storage.getFileByPath(folder.remotePath)).thenReturn(folder)
        whenever(storage.getFolderContent(folder, false)).thenReturn(emptyList())
        whenever(storage.getE2EEVersionObject(user)).thenReturn(version)
        val capability: OCCapability = mock { on { endToEndEncryptionApiVersion } doReturn version }
        val remote: RemoteFile = mock()
        val operation = RefreshFolderOperation(folder, storage, user, context)
        val synchronize = RefreshFolderOperation::class.java.getDeclaredMethod("synchronizeData", List::class.java)
            .apply { isAccessible = true }

        val files = mockStatic(FileStorageUtils::class.java)
        val capabilities = mockStatic(CapabilityUtils::class.java)
        val encryption = mockStatic(EncryptionUtils::class.java)
        val shares = mockStatic(UnifiedShareSharees::class.java)
        val persistence = mockStatic(Class.forName("com.nextcloud.utils.extensions.FileDataStorageManagerExtensionsKt"))
        try {
            files.`when`<OCFile> { FileStorageUtils.fillOCFile(remote) }.thenReturn(remoteFolder)
            files.`when`<Boolean> { FileStorageUtils.checkEncryptionStatus(folder, storage) }.thenReturn(true)
            capabilities.`when`<OCCapability> { CapabilityUtils.getCapability(user, context) }.thenReturn(capability)
            encryption.`when`<Any> { EncryptionUtils.downloadFolderMetadata(folder, null, context, user) }
                .thenReturn(metadata)
            synchronize.invoke(operation, listOf(remote))

            persistence.verify {
                eq(storage).saveFolder(
                    argThat {
                        remotePath == "/private/cipher/" &&
                            decryptedRemotePath == "/private/photos/" && isEncrypted
                    },
                    any(),
                    any()
                )
            }
        } finally {
            persistence.close()
            shares.close()
            encryption.close()
            capabilities.close()
            files.close()
        }
    }

    @Test
    fun v1MetadataFailureCannotSaveCiphertextChildrenAsLogicalNames() {
        val folder = OCFile("/private/").apply {
            mimeType = MimeType.DIRECTORY
            isEncrypted = true
        }
        val context: Context = mock()
        val user: User = mock()
        val storage: FileDataStorageManager = mock()
        whenever(storage.getFileByPath(folder.remotePath)).thenReturn(folder)
        val capability: OCCapability = mock { on { endToEndEncryptionApiVersion } doReturn E2EVersion.V1_2 }
        val remote: RemoteFile = mock()
        val operation = RefreshFolderOperation(folder, storage, user, context)
        val synchronize = RefreshFolderOperation::class.java.getDeclaredMethod("synchronizeData", List::class.java)
            .apply { isAccessible = true }

        mockStatic(FileStorageUtils::class.java).use { files ->
            files.`when`<OCFile> { FileStorageUtils.fillOCFile(remote) }.thenReturn(folder)
            files.`when`<Boolean> { FileStorageUtils.checkEncryptionStatus(folder, storage) }.thenReturn(true)
            mockStatic(CapabilityUtils::class.java).use { capabilities ->
                capabilities.`when`<OCCapability> { CapabilityUtils.getCapability(user, context) }
                    .thenReturn(capability)
                mockStatic(EncryptionUtils::class.java).use {
                    val error = assertThrows(InvocationTargetException::class.java) {
                        synchronize.invoke(operation, listOf(remote, mock<RemoteFile>()))
                    }
                    assertTrue(error.cause is IllegalStateException)
                }
            }
        }
    }
}
