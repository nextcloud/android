/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.owncloud.android.operations.upload

import android.content.Context
import com.nextcloud.client.account.User
import com.owncloud.android.datamodel.ArbitraryDataProviderImpl
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.datamodel.e2e.v2.decrypted.DecryptedFile
import com.owncloud.android.datamodel.e2e.v2.decrypted.DecryptedFolderMetadataFile
import com.owncloud.android.datamodel.e2e.v2.decrypted.DecryptedMetadata
import com.owncloud.android.lib.common.OwnCloudClient
import com.owncloud.android.lib.common.operations.RemoteOperationResult
import com.owncloud.android.lib.resources.files.DownloadFileRemoteOperation
import com.owncloud.android.lib.resources.files.ReadFileRemoteOperation
import com.owncloud.android.lib.resources.files.model.RemoteFile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.mockito.Mockito.mockConstruction
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class EncryptedUploadConflictCheckerTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun unchangedVerifiedFileDoesNotDownloadCiphertextAgain() {
        verifyCachedComparison(true)
    }

    @Test
    fun unchangedKnownConflictDoesNotDownloadDuringBackgroundRetry() {
        verifyCachedComparison(false)
    }

    @Test
    fun acceptingVerifiedServerCopyDoesNotDownloadOnNextScan() {
        verifyCachedComparison(false, adoptCopy = true)
    }

    @Suppress("NestedBlockDepth")
    private fun verifyCachedComparison(same: Boolean, adoptCopy: Boolean = false) {
        val local = temporaryFolder.newFile().apply { writeText("abc") }
        val context: Context = mock { on { cacheDir } doReturn temporaryFolder.root }
        val user: User = mock { on { accountName } doReturn "account-A" }
        val client: OwnCloudClient = mock()
        val parent = OCFile("/private/").apply {
            isEncrypted = true
            decryptedRemotePath = "/private/"
        }
        val metadata = DecryptedFolderMetadataFile(
            metadata = DecryptedMetadata(
                files = mutableMapOf("cipher" to DecryptedFile("photo.jpg", "image/jpeg", "nonce", "tag", "key")),
                metadataKey = ByteArray(0)
            ),
            version = "2.0"
        )
        val remote = RemoteFile("/private/cipher").apply {
            length = 19
            etag = "version-A"
            remoteId = "00000042instance"
            mimeType = "application/octet-stream"
            modifiedTimestamp = 1_700_000_000_000
        }
        val remoteVersion = EncryptedFileComparisonCache.fingerprint(
            remote.etag.orEmpty(),
            remote.remoteId.orEmpty(),
            remote.length.toString(),
            remote.modifiedTimestamp.toString(),
            "key",
            "nonce"
        )
        val version = EncryptedFileComparisonCache.version(local.length(), local.lastModified(), remoteVersion)
        var record = "$version:$same:3"
        val result: RemoteOperationResult<Any> = mock {
            on { isSuccess } doReturn true
            on { data } doReturn arrayListOf(remote)
        }
        mockConstruction(ReadFileRemoteOperation::class.java) { read, _ ->
            doReturn(result).whenever(read).execute(client)
        }.use {
            mockConstruction(ArbitraryDataProviderImpl::class.java) { provider, _ ->
                whenever(provider.getValue(any<String>(), any())).thenAnswer { record }
                doAnswer {
                    record = it.getArgument(2)
                    null
                }.whenever(provider).storeOrUpdateKeyValue(any<String>(), any(), any<String>())
            }.use {
                mockConstruction(DownloadFileRemoteOperation::class.java).use { downloads ->
                    val compared = requireNotNull(
                        EncryptedUploadConflictChecker(
                            context,
                            user
                        ).compare(client, parent, metadata, local, "photo.jpg")
                    )

                    assertEquals(same, compared.sameContent)
                    assertEquals("/private/photo.jpg", compared.remoteFile.decryptedRemotePath)
                    assertEquals("image/jpeg", compared.remoteFile.mimeType)
                    assertEquals(3L, compared.remoteFile.fileLength)
                    if (adoptCopy) {
                        local.writeText("new")
                        local.setLastModified(local.lastModified() + 1_000)
                        EncryptedUploadConflictChecker.rememberVerifiedCopy(
                            context,
                            user,
                            compared.remoteFile,
                            local,
                            compared.remoteVersion
                        )
                        val afterCopy = requireNotNull(
                            EncryptedUploadConflictChecker(context, user)
                                .compare(client, parent, metadata, local, "photo.jpg")
                        )
                        assertTrue(afterCopy.sameContent)
                    }
                    assertTrue(downloads.constructed().isEmpty())
                    assertTrue(temporaryFolder.root.listFiles()!!.none { it.isDirectory })
                }
            }
        }
    }
}
