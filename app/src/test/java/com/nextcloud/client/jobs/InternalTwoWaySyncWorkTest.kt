/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.nextcloud.client.jobs

import android.app.Notification
import android.content.Context
import androidx.work.ForegroundInfo
import androidx.work.ListenableWorker
import androidx.work.WorkerParameters
import com.google.common.util.concurrent.ListenableFuture
import com.nextcloud.client.account.User
import com.nextcloud.client.account.UserAccountManager
import com.nextcloud.client.device.PowerManagementService
import com.nextcloud.client.jobs.notification.WorkerNotificationManager
import com.nextcloud.client.network.Connectivity
import com.nextcloud.client.network.ConnectivityService
import com.nextcloud.client.preferences.AppPreferences
import com.owncloud.android.MainApp
import com.owncloud.android.datamodel.FileDataStorageManager
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.lib.common.operations.RemoteOperationResult
import com.owncloud.android.lib.common.operations.RemoteOperationResult.ResultCode
import com.owncloud.android.operations.SynchronizeFolderOperation
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.MockedConstruction
import org.mockito.Mockito
import org.mockito.kotlin.same
import org.mockito.kotlin.never
import org.mockito.kotlin.verify

class InternalTwoWaySyncWorkTest {
    private val context = mockk<Context>(relaxed = true)
    private val params = mockk<WorkerParameters>(relaxed = true)
    private val accounts = mockk<UserAccountManager>(relaxed = true)
    private val power = mockk<PowerManagementService>(relaxed = true)
    private val connectivity = mockk<ConnectivityService>(relaxed = true)
    private val preferences = mockk<AppPreferences>(relaxed = true)
    private val user = mockk<User>(relaxed = true)
    private val selectedFolder = OCFile("/Root/")
    private var currentFolder: OCFile? = OCFile("/Root/")
    private var operationResult = RemoteOperationResult<Unit>(ResultCode.OK)
    private lateinit var storage: MockedConstruction<FileDataStorageManager>
    private lateinit var operations: MockedConstruction<SynchronizeFolderOperation>
    private lateinit var notifications: MockedConstruction<WorkerNotificationManager>

    @Before
    fun setUp() {
        every { accounts.allUsers } returns listOf(user)
        every { preferences.isTwoWaySyncEnabled } returns true
        every { power.isPowerSavingEnabled } returns false
        every { connectivity.isConnected } returns true
        every { connectivity.connectivity } returns Connectivity(isWifi = true)
        every { connectivity.isInternetWalled() } returns false
        val foregroundFuture = mockk<ListenableFuture<Void>>(relaxed = true)
        every { params.foregroundUpdater.setForegroundAsync(any(), any(), any()) } returns foregroundFuture
        mockkStatic(MainApp::class)
        every { MainApp.getStoragePath() } returns "/nonexistent-nextcloud-worker-test"
        storage = Mockito.mockConstruction(FileDataStorageManager::class.java) { manager, _ ->
            Mockito.`when`(manager.getInternalTwoWaySyncFolders(user)).thenReturn(listOf(selectedFolder))
            Mockito.`when`(manager.getFileByEncryptedRemotePath("/Root/")).thenAnswer { currentFolder }
        }
        operations = Mockito.mockConstruction(SynchronizeFolderOperation::class.java) { operation, _ ->
            Mockito.`when`(operation.execute(context)).thenAnswer { operationResult }
        }
        notifications = Mockito.mockConstruction(WorkerNotificationManager::class.java) { manager, _ ->
            val notification = Mockito.mock(Notification::class.java)
            Mockito.`when`(manager.createSilentNotification(Mockito.anyString(), Mockito.anyInt()))
                .thenReturn(notification)
            Mockito.`when`(manager.getForegroundInfo(notification)).thenReturn(ForegroundInfo(392, notification))
        }
    }

    @After
    fun tearDown() {
        notifications.close()
        operations.close()
        storage.close()
        unmockkAll()
    }

    @Test
    fun `completion updates freshly loaded folder metadata`() {
        val refreshed = currentFolder!!
        refreshed.modificationTimestamp = 1234L
        assertEquals(ListenableWorker.Result.success(), worker().doWork())
        verify(storage.constructed().single()).saveFile(same(refreshed))
        verify(storage.constructed().single(), never()).saveFile(same(selectedFolder))
        assertEquals(1234L, refreshed.modificationTimestamp)
        assertEquals("OK", refreshed.internalFolderSyncResult)
    }

    @Test
    fun `a folder removed during synchronization is not recreated`() {
        currentFolder = null
        assertEquals(ListenableWorker.Result.success(), worker().doWork())
        verify(storage.constructed().single(), never()).saveFile(org.mockito.kotlin.any())
    }

    @Test
    fun `download failure is recorded and the worker reports failure`() {
        operationResult = RemoteOperationResult(ResultCode.UNAUTHORIZED)
        assertEquals(ListenableWorker.Result.failure(), worker().doWork())
        assertEquals("UNAUTHORIZED", currentFolder!!.internalFolderSyncResult)
    }

    @Test
    fun `disabled ongoing sync does not inspect selected folders`() {
        every { preferences.isTwoWaySyncEnabled } returns false
        assertEquals(ListenableWorker.Result.success(), worker().doWork())
        assertEquals(0, storage.constructed().size)
        assertEquals(0, operations.constructed().size)
    }

    private fun worker() = InternalTwoWaySyncWork(context, params, accounts, power, connectivity, preferences)
}
