/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Raphael Vieira raphaelecv.projects@gmail.com
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */

package com.nextcloud.client.jobs.upload

import android.app.NotificationManager
import android.content.Context
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.work.ListenableWorker
import androidx.work.WorkerParameters
import com.nextcloud.android.common.ui.theme.MaterialSchemes
import com.nextcloud.client.account.User
import com.nextcloud.client.account.UserAccountManager
import com.nextcloud.client.device.PowerManagementService
import com.nextcloud.client.jobs.BackgroundJobManager
import com.nextcloud.client.network.ClientFactory
import com.nextcloud.client.network.Connectivity
import com.nextcloud.client.network.ConnectivityService
import com.nextcloud.client.preferences.AppPreferences
import com.owncloud.android.datamodel.UploadsStorageManager
import com.owncloud.android.lib.common.operations.RemoteOperationResult.ResultCode
import com.owncloud.android.operations.UploadFileOperation
import com.owncloud.android.utils.theme.ViewThemeUtils
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.Optional

class FileUploadWorkerTest {

    private lateinit var worker: FileUploadWorker
    private val uploadsStorageManager: UploadsStorageManager = mockk(relaxed = true)
    private val connectivityService: ConnectivityService = mockk(relaxed = true)
    private val powerManagementService: PowerManagementService = mockk(relaxed = true)
    private val userAccountManager: UserAccountManager = mockk(relaxed = true)
    private val localBroadcastManager: LocalBroadcastManager = mockk(relaxed = true)
    private val backgroundJobManager: BackgroundJobManager = mockk(relaxed = true)
    private val preferences: AppPreferences = mockk(relaxed = true)
    private val clientFactory: ClientFactory = mockk(relaxed = true)
    private val uploadFileOperationFactory: FileUploadOperationFactory = mockk(relaxed = true)
    private val context: Context = mockk(relaxed = true)
    private val params: WorkerParameters = mockk(relaxed = true)
    private val systemNotificationManager: NotificationManager = mockk(relaxed = true)
    private val uploadNotificationManager: UploadNotificationManager = mockk(relaxed = true)

    @Before
    fun setUp() {
        every { context.getSystemService(Context.NOTIFICATION_SERVICE) } returns systemNotificationManager

        val materialSchemes = mockk<MaterialSchemes>(relaxed = true)
        val viewThemeUtils = ViewThemeUtils(materialSchemes, mockk(relaxed = true))

        val connectivity = mockk<Connectivity>()
        every { connectivity.isConnected } returns true
        every { connectivityService.getConnectivity() } returns connectivity
        every { connectivityService.isConnected } returns true
        every { connectivityService.isInternetWalled } returns false

        worker = FileUploadWorker(
            uploadsStorageManager,
            connectivityService,
            powerManagementService,
            userAccountManager,
            viewThemeUtils,
            localBroadcastManager,
            backgroundJobManager,
            preferences,
            clientFactory,
            uploadFileOperationFactory,
            context,
            uploadNotificationManager,
            params
        )
    }

    @After
    fun tearDown() {
        unmockkAll()
        FileUploadWorker.activeUploadFileOperations.clear()
    }

    @Test
    fun `doWork returns failure when account name is missing`() = runBlocking {
        // GIVEN
        every { params.inputData.getString(FileUploadWorker.ACCOUNT) } returns null

        // WHEN
        val result = worker.doWork()

        // THEN
        assertEquals(ListenableWorker.Result.failure(), result)
    }

    @Test
    fun `doWork returns failure when upload ids are missing`() = runBlocking {
        // GIVEN
        every { params.inputData.getString(FileUploadWorker.ACCOUNT) } returns "account"
        every { params.inputData.getLongArray(FileUploadWorker.UPLOAD_IDS) } returns null

        // WHEN
        val result = worker.doWork()

        // THEN
        assertEquals(ListenableWorker.Result.failure(), result)
    }

    @Test
    fun `doWork returns failure when batch index is missing`() = runBlocking {
        // GIVEN
        every { params.inputData.getString(FileUploadWorker.ACCOUNT) } returns "account"
        every { params.inputData.getLongArray(FileUploadWorker.UPLOAD_IDS) } returns longArrayOf(1L)
        every { params.inputData.getInt(FileUploadWorker.CURRENT_BATCH_INDEX, -1) } returns -1

        // WHEN
        val result = worker.doWork()

        // THEN
        assertEquals(ListenableWorker.Result.failure(), result)
    }

    @Test
    fun `doWork returns failure when user is not found`() = runBlocking {
        // GIVEN
        val accountName = "account"
        every { params.inputData.getString(FileUploadWorker.ACCOUNT) } returns accountName
        every { params.inputData.getLongArray(FileUploadWorker.UPLOAD_IDS) } returns longArrayOf(1L)
        every { params.inputData.getInt(FileUploadWorker.CURRENT_BATCH_INDEX, any()) } returns 0
        every { params.inputData.getInt(FileUploadWorker.TOTAL_UPLOAD_SIZE, any()) } returns 1
        every { userAccountManager.getUser(accountName) } returns Optional.empty()

        // WHEN
        val result = worker.doWork()

        // THEN
        assertEquals(ListenableWorker.Result.failure(), result)
    }

    @Test
    fun `doWork returns success when there are no uploads`() = runBlocking {
        // GIVEN
        val accountName = "account"
        val user = mockk<User>(relaxed = true)
        every { params.inputData.getString(FileUploadWorker.ACCOUNT) } returns accountName
        every { params.inputData.getLongArray(FileUploadWorker.UPLOAD_IDS) } returns longArrayOf(1L)
        every { params.inputData.getInt(FileUploadWorker.CURRENT_BATCH_INDEX, any()) } returns 0
        every { params.inputData.getInt(FileUploadWorker.TOTAL_UPLOAD_SIZE, any()) } returns 1
        every { userAccountManager.getUser(accountName) } returns Optional.of(user)
        every { uploadsStorageManager.getUploadsByIds(any(), accountName) } returns emptyList()

        // WHEN
        val result = worker.doWork()

        // THEN
        assertEquals(ListenableWorker.Result.success(), result)
    }


    @Test
    fun `onTransferProgress updates notification manager`() {
        // GIVEN
        val fileName = "testFile"
        val operation = mockk<UploadFileOperation>(relaxed = true)
        FileUploadWorker.activeUploadFileOperations[fileName] = operation

        // WHEN
        worker.onTransferProgress(100, 50, 100, fileName)

        // THEN
        verify { uploadNotificationManager.updateUploadProgress(50, operation) }
    }

    @Test
    fun `cancelCurrentUpload cancels matching operations`() {
        // GIVEN
        val remotePath = "path"
        val accountName = "account"
        val operation = mockk<UploadFileOperation>(relaxed = true)
        every { operation.remotePath } returns remotePath
        every { operation.user.accountName } returns accountName
        FileUploadWorker.activeUploadFileOperations["key"] = operation

        // WHEN
        var completed = false
        FileUploadWorker.cancelCurrentUpload(remotePath, accountName) {
            completed = true
        }

        // THEN
        verify { operation.cancel(ResultCode.USER_CANCELLED) }
        assertTrue(completed)
    }

    @Test
    fun `isUploading returns true when operation exists`() {
        // GIVEN
        val remotePath = "path"
        val accountName = "account"
        val operation = mockk<UploadFileOperation>(relaxed = true)
        every { operation.remotePath } returns remotePath
        every { operation.user.accountName } returns accountName
        FileUploadWorker.activeUploadFileOperations["key"] = operation

        // WHEN & THEN
        assertTrue(FileUploadWorker.isUploading(remotePath, accountName))
        assertFalse(FileUploadWorker.isUploading("other", accountName))
    }

    @Test
    fun `getUploadAction returns correct values`() {
        assertEquals(FileUploadWorker.LOCAL_BEHAVIOUR_FORGET,
            FileUploadWorker.getUploadAction("LOCAL_BEHAVIOUR_FORGET"))
        assertEquals(FileUploadWorker.LOCAL_BEHAVIOUR_MOVE,
            FileUploadWorker.getUploadAction("LOCAL_BEHAVIOUR_MOVE"))
        assertEquals(FileUploadWorker.LOCAL_BEHAVIOUR_DELETE,
            FileUploadWorker.getUploadAction("LOCAL_BEHAVIOUR_DELETE"))
        assertEquals(FileUploadWorker.LOCAL_BEHAVIOUR_FORGET,
            FileUploadWorker.getUploadAction("UNKNOWN"))
    }
}
