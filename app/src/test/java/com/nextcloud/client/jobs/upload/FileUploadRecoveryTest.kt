/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.nextcloud.client.jobs.upload

import android.content.Context
import androidx.work.Data
import androidx.work.ListenableWorker.Result
import androidx.work.WorkerParameters
import com.nextcloud.client.account.User
import com.nextcloud.client.account.UserAccountManager
import com.nextcloud.client.database.dao.FileSystemDao
import com.nextcloud.client.database.dao.UploadDao
import com.nextcloud.client.database.entity.FilesystemEntity
import com.nextcloud.client.jobs.BackgroundJobManager
import com.nextcloud.client.device.PowerManagementService
import com.nextcloud.client.jobs.autoUpload.FileSystemRepository
import com.nextcloud.client.jobs.autoUpload.SyncFolderHelper
import com.nextcloud.client.network.ConnectivityService
import com.nextcloud.client.preferences.AppPreferences
import com.owncloud.android.datamodel.SyncedFolder
import com.owncloud.android.datamodel.SyncedFolderProvider
import com.owncloud.android.datamodel.UploadsStorageManager
import com.owncloud.android.datamodel.UploadsStorageManager.UploadStatus
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.datamodel.FileDataStorageManager
import com.owncloud.android.datamodel.ThumbnailsCacheManager
import com.owncloud.android.db.OCUpload
import com.owncloud.android.db.UploadResult
import com.owncloud.android.operations.factory.UploadFileOperationFactory
import com.owncloud.android.operations.UploadFileOperation
import com.owncloud.android.lib.common.OwnCloudClient
import com.owncloud.android.lib.common.OwnCloudAccount
import com.owncloud.android.lib.common.OwnCloudClientManager
import com.owncloud.android.lib.common.OwnCloudClientManagerFactory
import com.owncloud.android.lib.common.operations.RemoteOperationResult
import com.owncloud.android.lib.common.operations.RemoteOperationResult.ResultCode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.mockito.Mockito.mockConstruction
import org.mockito.Mockito.mockStatic
import org.mockito.Mockito.withSettings
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import java.io.File
import java.util.Optional

class FileUploadRecoveryTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private val context: Context = mock()
    private val filesystem: FileSystemDao = mock()
    private val uploadDao: UploadDao = mock()
    private val storage: UploadsStorageManager = mock {
        on { this.uploadDao } doReturn uploadDao
    }
    private val manager: BackgroundJobManager = mock()
    private val accounts: UserAccountManager = mock()
    private val connectivity: ConnectivityService = mock()
    private val preferences: AppPreferences = mock()
    private val operations: UploadFileOperationFactory = org.mockito.Mockito.mock(
        UploadFileOperationFactory::class.java,
        withSettings().useConstructor(storage, connectivity, mock<PowerManagementService>(), accounts)
    )
    private val folders: SyncedFolderProvider = mock()
    private val folder: SyncedFolder = mock {
        on { id } doReturn FOLDER_ID
        on { account } doReturn ACCOUNT
        on { isEnabled } doReturn true
    }
    private lateinit var upload: OCUpload

    @Before
    fun setUp() = runTest {
        upload = OCUpload(File.separator + "photo.jpg", REMOTE_PATH, ACCOUNT).apply {
            localPath = temporaryFolder.newFile("photo.jpg").absolutePath
            uploadId = UPLOAD_ID
            uploadStatus = UploadStatus.UPLOAD_IN_PROGRESS
            lastResult = UploadResult.NETWORK_CONNECTION
            folderUnlockToken = "recovery-token"
        }
        val user: User = mock { on { accountName } doReturn ACCOUNT }
        whenever(accounts.user).thenReturn(user)
        whenever(accounts.getUser(ACCOUNT)).thenReturn(Optional.of(user))
        whenever(storage.getUploadsByIds(any(), any())).thenReturn(listOf(upload))
        whenever(folders.syncedFolders).thenReturn(mutableListOf(folder))
        whenever(filesystem.getFileByPathAndFolder(upload.localPath, FOLDER_ID.toString())).thenReturn(
            FilesystemEntity(1, upload.localPath, REMOTE_PATH, 0, 0L, 0, FOLDER_ID.toString(), null, 0L)
        )
    }

    @Test
    fun `recovering a network failure schedules auto upload and preserves its record and token`() = runTest {
        assertEquals(Result.success(), runWorker())

        verify(manager).startAutoUpload(folder, false)
        verify(uploadDao, never()).deleteByRemotePathAndAccountName(any(), any())
        verifyNoInteractions(operations)
        assertEquals("recovery-token", upload.folderUnlockToken)
        assertEquals(UPLOAD_ID, upload.uploadId)
    }

    @Test
    fun `a conflict is left for user resolution without scheduling auto upload`() = runTest {
        upload.uploadStatus = UploadStatus.UPLOAD_FAILED
        upload.lastResult = UploadResult.SYNC_CONFLICT

        assertEquals(Result.success(), runWorker())

        verify(manager, never()).startAutoUpload(any(), any())
        verifyNoInteractions(uploadDao, operations)
    }

    @Test
    fun `a cancelled upload is not handed back to auto upload`() = runTest {
        upload.uploadStatus = UploadStatus.UPLOAD_CANCELLED

        assertEquals(Result.success(), runWorker())

        verify(manager, never()).startAutoUpload(any(), any())
        verifyNoInteractions(uploadDao, operations)
    }

    @Test
    fun `a disabled folder cannot take ownership of a failed upload`() = runTest {
        whenever(folder.isEnabled).thenReturn(false)

        assertEquals(Result.retry(), runWorker())

        verify(manager, never()).startAutoUpload(any(), any())
        verifyNoInteractions(uploadDao, operations)
    }

    @Test
    fun `another account cannot take ownership of a failed upload`() = runTest {
        whenever(folder.account).thenReturn("another@nextcloud.localhost")

        assertEquals(Result.retry(), runWorker())

        verify(manager, never()).startAutoUpload(any(), any())
        verifyNoInteractions(uploadDao, operations)
    }

    @Test
    fun `a handled file cannot take ownership of a failed upload`() = runTest {
        whenever(filesystem.getFileByPathAndFolder(upload.localPath, FOLDER_ID.toString())).thenReturn(
            FilesystemEntity(1, upload.localPath, REMOTE_PATH, 0, 0L, 1, FOLDER_ID.toString(), null, 0L)
        )

        assertEquals(Result.retry(), runWorker())

        verify(manager, never()).startAutoUpload(any(), any())
        verifyNoInteractions(uploadDao, operations)
    }

    @Test
    fun `a different destination cannot take ownership of a failed upload`() = runTest {
        upload.remotePath = "/manual/photo.jpg"

        assertEquals(Result.retry(), runWorker())

        verify(manager, never()).startAutoUpload(any(), any())
        verifyNoInteractions(uploadDao, operations)
    }

    @Test
    fun `an explicit ordinary upload is not handed to auto upload`() = runTest {
        assertEquals(Result.retry(), runWorker(skipAutoUploadCheck = true))

        verify(manager, never()).startAutoUpload(any(), any())
        verifyNoInteractions(uploadDao, operations)
    }

    @Test
    fun `pausing uploads leaves the record without scheduling auto upload`() = runTest {
        whenever(preferences.isGlobalUploadPaused).thenReturn(true)

        assertEquals(Result.success(), runWorker())

        verify(manager, never()).startAutoUpload(any(), any())
        verifyNoInteractions(uploadDao, operations)
    }

    @Test
    fun `a failed precheck returns retry and a later worker can upload after connectivity recovers`() = runTest {
        whenever(folders.syncedFolders).thenReturn(mutableListOf())
        whenever(connectivity.isConnected).thenReturn(true)
        whenever(connectivity.isInternetWalled()).thenReturn(true, false)

        assertEquals(Result.retry(), runWorker())
        verifyNoInteractions(operations)

        val operationUser = accounts.getUser(ACCOUNT).get()
        val operationStorage: FileDataStorageManager = mock()
        val operation: UploadFileOperation = mock {
            on { user } doReturn operationUser
            on { storageManager } doReturn operationStorage
            on { originalStoragePath } doReturn upload.localPath
            on { file } doReturn OCFile(REMOTE_PATH)
        }
        whenever(operation.execute(any<OwnCloudClient>())).thenReturn(RemoteOperationResult<Any?>(ResultCode.OK))
        whenever(operations.create(any(), any(), any(), any(), any(), any())).thenReturn(operation)

        assertEquals(Result.success(), runWorker())
        verify(operation).execute(any<OwnCloudClient>())
        verify(uploadDao, never()).deleteByRemotePathAndAccountName(any(), any())
    }

    private suspend fun runWorker(skipAutoUploadCheck: Boolean = false): Result = withContext(Dispatchers.IO) {
        val data = Data.Builder()
            .putString(FileUploadWorker.ACCOUNT, ACCOUNT)
            .putLongArray(FileUploadWorker.UPLOAD_IDS, longArrayOf(UPLOAD_ID))
            .putInt(FileUploadWorker.CURRENT_BATCH_INDEX, 0)
            .putInt(FileUploadWorker.TOTAL_UPLOAD_SIZE, 1)
            .putBoolean(FileUploadWorker.SKIP_AUTO_UPLOAD_CHECK, skipAutoUploadCheck)
            .build()
        val params: WorkerParameters = mock { on { inputData } doReturn data }
        val clientManager: OwnCloudClientManager = mock()
        whenever(clientManager.getClientFor(any(), any())).thenReturn(mock())
        mockStatic(OwnCloudClientManagerFactory::class.java).use { factory ->
            factory.`when`<OwnCloudClientManager> { OwnCloudClientManagerFactory.getDefaultSingleton() }
                .thenReturn(clientManager)
            mockConstruction(OwnCloudAccount::class.java).use {
                mockConstruction(UploadNotificationManager::class.java).use {
                    mockConstruction(FileUploaderIntents::class.java).use {
                        mockConstruction(SyncFolderHelper::class.java) { helper, _ ->
                            whenever(helper.getAutoUploadRemotePath(any(), any())).thenReturn(REMOTE_PATH)
                        }.use {
                            mockConstruction(ThumbnailsCacheManager.ThumbnailGenerationTask::class.java).use {
                                mockConstruction(FileDataStorageManager::class.java).use {
                                    FileUploadWorker(
                                        storage, connectivity, mock(), accounts, mock(), mock(), manager, preferences,
                                        FileSystemRepository(
                                            filesystem,
                                            storage,
                                            context
                                        ),
                                        folders, context, operations, params
                                    ).doWork()
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    companion object {
        private const val ACCOUNT = "test@nextcloud.localhost"
        private const val REMOTE_PATH = "/photos/photo.jpg"
        private const val FOLDER_ID = 3L
        private const val UPLOAD_ID = 655L
    }
}
