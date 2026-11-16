/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.owncloud.android.operations

import android.content.Context
import com.nextcloud.client.account.User
import com.nextcloud.common.NextcloudClient
import com.nextcloud.utils.extensions.toNextcloudClient
import com.owncloud.android.datamodel.FileDataStorageManager
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.datamodel.e2e.v2.decrypted.DecryptedFile
import com.owncloud.android.datamodel.e2e.v2.decrypted.DecryptedFolderMetadataFile
import com.owncloud.android.datamodel.e2e.v2.decrypted.DecryptedMetadata
import com.owncloud.android.db.OCUpload
import com.owncloud.android.files.services.NameCollisionPolicy
import com.owncloud.android.lib.common.OwnCloudClient
import com.owncloud.android.lib.common.operations.RemoteOperationResult
import com.owncloud.android.lib.common.operations.RemoteOperationResult.ResultCode
import com.owncloud.android.lib.resources.files.DownloadFileRemoteOperation
import com.owncloud.android.operations.factory.UploadFileOperationFactory
import com.owncloud.android.utils.EncryptionUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.mockito.Mockito.mockConstruction
import org.mockito.Mockito.mockStatic
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.isNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.io.File
import javax.crypto.AEADBadTagException
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

@Suppress("DEPRECATION", "NestedBlockDepth", "MagicNumber")
class FileTransferContentTest {
    @get:Rule
    val temporary = TemporaryFolder()

    private val key = ByteArray(16) { it.toByte() }
    private val nonce = ByteArray(12) { it.toByte() }
    private val content = "Content requiring authenticated decryption"
    private val client: OwnCloudClient = mock()
    private val nextcloud: NextcloudClient = mock()
    private val storage: FileDataStorageManager = mock()
    private val factory: UploadFileOperationFactory = mock()
    private val user: User = mock { on { accountName } doReturn "test-account" }
    private val context: Context = mock { on { cacheDir } doAnswer { temporary.root } }
    private val source: OCFile = mock {
        on { remotePath } doReturn "/secret/blob"
        on { encryptedFileName } doReturn "blob"
        on { parentId } doReturn 1L
        on { isEncrypted } doReturn true
        on { mimeType } doReturn "text/plain"
    }
    private val destination: OCFile = mock { on { remotePath } doReturn "/public/" }

    @Test
    fun authenticatedPlaintextIsUploadedWithoutRetainingTemporaryContent() {
        val uploaded = RemoteOperationResult<Any>(ResultCode.OK)
        val operation: UploadFileOperation = mock()
        doReturn(mock<OCFile>()).whenever(operation).file
        doReturn(uploaded).whenever(operation).execute(client)
        doAnswer {
            val upload = it.getArgument<OCUpload>(1)
            assertEquals(content, File(upload.localPath).readText())
            assertEquals("/public/report.txt", upload.remotePath)
            verify(upload).setNameCollisionPolicy(NameCollisionPolicy.ASK_USER)
            operation
        }.whenever(factory).create(eq(context), any(), isNull(), eq(true), eq(user), eq(storage))

        withDownload(cipher(Cipher.ENCRYPT_MODE).doFinal(content.toByteArray())) {
            assertTrue(
                FileTransferContent(context, user, storage, factory).copy(
                    source,
                    destination,
                    "report.txt",
                    client
                ).isSuccess
            )
        }

        assertTrue(temporary.root.listFiles().orEmpty().isEmpty())
        verify(operation).setKeepE2EELockOnFailure(true)
    }

    @Test
    fun corruptedCiphertextIsNeverUploadedAndTemporaryPlaintextIsRemoved() {
        val encrypted = cipher(Cipher.ENCRYPT_MODE).doFinal(content.toByteArray())
        encrypted[encrypted.lastIndex] = (encrypted.last().toInt() xor 1).toByte()

        withDownload(encrypted) {
            assertThrows(AEADBadTagException::class.java) {
                FileTransferContent(context, user, storage, factory).copy(source, destination, "report.txt", client)
            }
        }

        verifyNoInteractions(factory)
        assertTrue(temporary.root.listFiles().orEmpty().isEmpty())
    }

    @Test
    fun encryptedDestinationUsesItsStoragePathWithTheDecryptedFilename() {
        doReturn("/target/opaque-folder/").whenever(destination).remotePath
        doReturn("/资料/加密目录/").whenever(destination).decryptedRemotePath
        doReturn(true).whenever(destination).isEncrypted
        val operation: UploadFileOperation = mock()
        doReturn(mock<OCFile>()).whenever(operation).file
        doReturn(RemoteOperationResult<Any>(ResultCode.OK)).whenever(operation).execute(client)
        doAnswer {
            val upload = it.getArgument<OCUpload>(1)
            assertEquals("/target/opaque-folder/季度报告.txt", upload.remotePath)
            assertEquals(content, File(upload.localPath).readText())
            operation
        }.whenever(factory).create(eq(context), any(), isNull(), eq(true), eq(user), eq(storage))

        withDownload(cipher(Cipher.ENCRYPT_MODE).doFinal(content.toByteArray())) {
            assertTrue(
                FileTransferContent(context, user, storage, factory).copy(
                    source,
                    destination,
                    "季度报告.txt",
                    client
                ).isSuccess
            )
        }
        assertTrue(temporary.root.listFiles().orEmpty().isEmpty())
    }

    private fun withDownload(encrypted: ByteArray, test: () -> Unit) {
        val parent: OCFile = mock()
        doReturn(parent).whenever(storage).getFileById(1L)
        val metadata = DecryptedFolderMetadataFile(
            metadata = DecryptedMetadata(
                files = mutableMapOf("blob" to DecryptedFile("report.txt", "text/plain", "nonce", "tag", "key"))
            ),
            version = "2.0"
        )
        mockStatic(EncryptionUtils::class.java).use { crypto ->
            crypto.`when`<Any> { EncryptionUtils.downloadFolderMetadata(parent, client, context, user) }
                .thenReturn(metadata)
            crypto.`when`<ByteArray> { EncryptionUtils.decodeStringToBase64Bytes("key") }.thenReturn(key)
            crypto.`when`<ByteArray> { EncryptionUtils.decodeStringToBase64Bytes("nonce") }.thenReturn(nonce)
            crypto.`when`<Cipher> { EncryptionUtils.getCipher(Cipher.DECRYPT_MODE, key, nonce) }
                .thenAnswer { cipher(Cipher.DECRYPT_MODE) }
            mockStatic(Class.forName("com.nextcloud.utils.extensions.OwnCloudClientExtensionsKt")).use { extensions ->
                extensions.`when`<NextcloudClient> { client.toNextcloudClient(context) }.thenReturn(nextcloud)
                mockConstruction(OCUpload::class.java) { upload, construction ->
                    doReturn(construction.arguments()[0]).whenever(upload).localPath
                    doReturn(construction.arguments()[1]).whenever(upload).remotePath
                }.use {
                    mockConstruction(DownloadFileRemoteOperation::class.java) { download, construction ->
                        doAnswer {
                            File(construction.arguments()[1].toString() + "/secret/blob").apply {
                                parentFile.mkdirs()
                                writeBytes(encrypted)
                            }
                            RemoteOperationResult<Unit>(ResultCode.OK)
                        }.whenever(download).execute(nextcloud)
                    }.use { test() }
                }
            }
        }
    }

    private fun cipher(mode: Int): Cipher = Cipher.getInstance("AES/GCM/NoPadding").apply {
        init(mode, SecretKeySpec(key, "AES"), GCMParameterSpec(128, nonce))
    }
}
