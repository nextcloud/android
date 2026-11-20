/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.owncloud.android.operations

import android.content.Context
import com.nextcloud.client.account.User
import com.owncloud.android.datamodel.ArbitraryDataProviderImpl
import com.owncloud.android.datamodel.FileDataStorageManager
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.datamodel.e2e.v1.decrypted.DecryptedFolderMetadataFileV1
import com.owncloud.android.datamodel.e2e.v2.decrypted.DecryptedFolderMetadataFile
import com.owncloud.android.datamodel.e2e.v2.decrypted.DecryptedMetadata
import com.owncloud.android.lib.common.OwnCloudClient
import com.owncloud.android.lib.common.operations.RemoteOperationResult
import com.owncloud.android.lib.common.operations.RemoteOperationResult.ResultCode
import com.owncloud.android.lib.resources.status.E2EVersion
import com.owncloud.android.lib.resources.status.OCCapability
import com.owncloud.android.utils.EncryptionUtils
import com.owncloud.android.utils.EncryptionUtilsV2
import org.apache.commons.httpclient.Header
import org.apache.jackrabbit.webdav.client.methods.DeleteMethod
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.MockedStatic
import org.mockito.Mockito.mockConstruction
import org.mockito.Mockito.mockStatic
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.eq
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.io.IOException

@Suppress("DEPRECATION", "NestedBlockDepth", "MagicNumber")
class RemoveRemoteEncryptedFileOperationTest {
    private val context: Context = mock()
    private val user: User = mock { on { accountName } doReturn "test" }
    private val client: OwnCloudClient = mock()
    private val parent: OCFile = mock {
        on { remotePath } doReturn "/secret/"
        on { e2eCounter } doReturn 2L
    }
    private val capability: OCCapability = mock {
        on { endToEndEncryptionApiVersion } doReturn E2EVersion.V2_0
    }
    private val metadata = DecryptedFolderMetadataFile(
        DecryptedMetadata(counter = 48L, metadataKey = byteArrayOf()),
        version = "2.0"
    )
    private val metadataV1: DecryptedFolderMetadataFileV1 = mock()
    private val ok = RemoteOperationResult<Void>(ResultCode.OK)
    private val failed = RemoteOperationResult<Void>(ResultCode.UNKNOWN_ERROR)
    private var deleteStatus = 204
    private var failMetadata = false
    private var unlockResult = ok
    private val events = mutableListOf<String>()

    @Test
    fun usesLatestVerifiedMetadataCounterAndRereadsInsideLock() = withOperations { storage, _, crypto ->
        assertTrue(operation().execute(client).isSuccess)
        assertEquals(listOf("metadata", "lock", "metadata"), events)
        assertEquals(49L, metadata.metadata.counter)
        crypto.verify { EncryptionUtils.lockFolder(parent, client, 49L) }
        verify(storage.single()).updateE2EECounter(parent, 49L)
    }

    @Test
    fun rejectedConditionalDeleteDoesNotChangeMetadata() {
        deleteStatus = 412
        withOperations { storage, utils, _ ->
            assertFalse(operation().execute(client).isSuccess)
            assertEquals(48L, metadata.metadata.counter)
            verify(utils.last(), never()).removeFileFromMetadata(any(), any())
            verify(utils.last(), never()).serializeAndUploadMetadata(
                any<OCFile>(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any()
            )
            verify(storage.single(), never()).updateE2EECounter(any(), any<Long>())
        }
    }

    @Test
    fun metadataFailureDoesNotCommitStagedDelete() {
        failMetadata = true
        withOperations { _, _, crypto ->
            assertFalse(operation().execute(client).isSuccess)
            crypto.verify({ EncryptionUtils.unlockFolder(parent, client, "token") }, never())
        }
    }

    @Test
    fun unlockFailureIsReportedWithoutUpdatingLocalCounter() {
        unlockResult = failed
        withOperations { storage, _, _ ->
            assertFalse(operation().execute(client).isSuccess)
            verify(storage.single(), never()).updateE2EECounter(any(), any<Long>())
        }
    }

    @Test
    fun deletingEncryptedRootDoesNotRemoveItFromItsOwnFolderMap() = withOperations { _, utils, _ ->
        assertTrue(operation("/secret/", true).execute(client).isSuccess)
        verify(utils.last(), never()).removeFolderFromMetadata(any(), any())
    }

    @Test
    fun deletingV1SubfolderRemovesItsEntryFromParentMetadata() {
        doReturn(E2EVersion.V1_2).whenever(capability).endToEndEncryptionApiVersion
        withOperations { _, _, crypto ->
            assertTrue(operation(folder = true).execute(client).isSuccess)
            crypto.verify { EncryptionUtils.removeFileFromMetadata("blob", metadataV1) }
        }
    }

    @Test
    fun rejectedV1DeletePreservesItsMetadataEntry() {
        doReturn(E2EVersion.V1_2).whenever(capability).endToEndEncryptionApiVersion
        deleteStatus = 412
        withOperations { _, _, crypto ->
            assertFalse(operation(folder = true).execute(client).isSuccess)
            crypto.verify({ EncryptionUtils.removeFileFromMetadata("blob", metadataV1) }, never())
        }
    }

    private fun operation(path: String = "/secret/blob", folder: Boolean = false) =
        RemoveRemoteEncryptedFileOperation(path, user, context, "blob", parent, folder, "original-version")

    private fun withOperations(
        test: (List<FileDataStorageManager>, List<EncryptionUtilsV2>, MockedStatic<EncryptionUtils>) -> Unit
    ) {
        mockConstruction(FileDataStorageManager::class.java) { storage, _ ->
            doReturn(capability).whenever(storage).getCapability(user)
        }.use { storage ->
            mockConstruction(EncryptionUtilsV2::class.java) { utils, _ ->
                doAnswer {
                    events.add("metadata")
                    Pair(true, metadata)
                }.whenever(utils).retrieveMetadata(parent, client, user, context)
                doAnswer {
                    if (failMetadata) throw IOException("metadata write failed")
                    Unit
                }.whenever(utils).serializeAndUploadMetadata(
                    any<OCFile>(),
                    any(),
                    any(),
                    any(),
                    any(),
                    any(),
                    any(),
                    any()
                )
            }.use { utils ->
                mockConstruction(DeleteMethod::class.java) { delete, _ ->
                    doReturn(deleteStatus in 200..299).whenever(delete).succeeded()
                    doReturn(deleteStatus).whenever(delete).statusCode
                    doReturn(emptyArray<Header>()).whenever(delete).responseHeaders
                }.use { deletes ->
                    doReturn(deleteStatus).whenever(client).executeMethod(any<DeleteMethod>(), any(), any())
                    mockConstruction(ArbitraryDataProviderImpl::class.java) { provider, _ ->
                        doReturn("private").whenever(provider).getValue("test", EncryptionUtils.PRIVATE_KEY)
                        doReturn("public").whenever(provider).getValue("test", EncryptionUtils.CERTIFICATE)
                    }.use {
                        mockStatic(EncryptionUtils::class.java).use { crypto ->
                            val v1Pair = mock<android.util.Pair<Boolean, DecryptedFolderMetadataFileV1>>()
                            android.util.Pair::class.java.getField("first").set(v1Pair, true)
                            android.util.Pair::class.java.getField("second").set(v1Pair, metadataV1)
                            crypto.`when`<android.util.Pair<Boolean, DecryptedFolderMetadataFileV1>> {
                                EncryptionUtils.retrieveMetadataV1(
                                    eq(parent),
                                    eq(client),
                                    eq("private"),
                                    eq("public"),
                                    any(),
                                    eq(user),
                                    eq("1.2")
                                )
                            }.thenReturn(v1Pair)
                            crypto.`when`<String> {
                                EncryptionUtils.lockFolder(eq(parent), eq(client), any())
                            }.thenAnswer {
                                events.add("lock")
                                "token"
                            }
                            crypto.`when`<RemoteOperationResult<Void>> {
                                EncryptionUtils.unlockFolder(parent, client, "token")
                            }.thenReturn(unlockResult)
                            crypto.`when`<RemoteOperationResult<Void>> {
                                EncryptionUtils.unlockFolderV1(parent, client, "token")
                            }.thenReturn(unlockResult)
                            test(storage.constructed(), utils.constructed(), crypto)
                        }
                    }
                    deletes.constructed().forEach { delete ->
                        verify(delete).setRequestHeader("If-Match", "\"original-version\"")
                        verify(delete).releaseConnection()
                    }
                }
            }
        }
    }
}
