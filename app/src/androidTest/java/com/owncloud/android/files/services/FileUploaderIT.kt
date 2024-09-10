/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2020 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2020 Chris Narkiewicz <hello@ezaquarii.com>
 * SPDX-FileCopyrightText: 2020 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.files.services

import com.nextcloud.client.account.UserAccountManager
import com.nextcloud.client.account.UserAccountManagerImpl
import com.nextcloud.client.device.BatteryStatus
import com.nextcloud.client.device.PowerManagementService
import com.nextcloud.client.jobs.upload.FileUploadHelper
import com.nextcloud.client.jobs.upload.FileUploadWorker
import com.nextcloud.client.network.Connectivity
import com.nextcloud.client.network.ConnectivityService
import com.owncloud.android.AbstractOnServerIT
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.datamodel.UploadsStorageManager
import com.owncloud.android.db.OCUpload
import com.owncloud.android.lib.common.operations.OperationCancelledException
import com.owncloud.android.lib.resources.files.ReadFileRemoteOperation
import com.owncloud.android.lib.resources.files.model.RemoteFile
import com.owncloud.android.operations.UploadFileOperation
import junit.framework.Assert.assertEquals
import junit.framework.Assert.assertFalse
import junit.framework.Assert.assertTrue
import org.junit.Before
import org.junit.Test

abstract class FileUploaderIT : AbstractOnServerIT() {
    private var uploadsStorageManager: UploadsStorageManager? = null

    private val connectivityServiceMock: ConnectivityService = object : ConnectivityService {
        override fun isNetworkAndServerAvailable(): Boolean {
            return false
        }

        override fun isConnected(): Boolean {
            return false
        }

        override fun isInternetWalled(): Boolean = false
        override fun getConnectivity(): Connectivity = Connectivity.CONNECTED_WIFI
    }

    private val powerManagementServiceMock: PowerManagementService = object : PowerManagementService {
        override val isPowerSavingEnabled: Boolean
            get() = false

        override val isPowerSavingExclusionAvailable: Boolean
            get() = false

        override val battery: BatteryStatus
            get() = BatteryStatus()
    }

    @Before
    fun setUp() {
        val contentResolver = targetContext.contentResolver
        val accountManager: UserAccountManager = UserAccountManagerImpl.fromContext(targetContext)
        uploadsStorageManager = UploadsStorageManager(accountManager, contentResolver)
    }

    /**
     * uploads a file, overwrites it with an empty one, check if overwritten
     */
    // disabled, flaky test
    // @Test
    // fun testKeepLocalAndOverwriteRemote() {
    //     val file = getDummyFile("chunkedFile.txt")
    //     val ocUpload = OCUpload(file.absolutePath, "/testFile.txt", account.name)
    //
    //     assertTrue(
    //         UploadFileOperation(
    //             uploadsStorageManager,
    //             connectivityServiceMock,
    //             powerManagementServiceMock,
    //             user,
    //             null,
    //             ocUpload,
    //             FileUploader.NameCollisionPolicy.DEFAULT,
    //             FileUploader.LOCAL_BEHAVIOUR_COPY,
    //             targetContext,
    //             false,
    //             false
    //         )
    //             .setRemoteFolderToBeCreated()
    //             .execute(client, storageManager)
    //             .isSuccess
    //     )
    //
    //     val result = ReadFileRemoteOperation("/testFile.txt").execute(client)
    //     assertTrue(result.isSuccess)
    //
    //     assertEquals(file.length(), (result.data[0] as RemoteFile).length)
    //
    //     val ocUpload2 = OCUpload(getDummyFile("empty.txt").absolutePath, "/testFile.txt", account.name)
    //
    //     assertTrue(
    //         UploadFileOperation(
    //             uploadsStorageManager,
    //             connectivityServiceMock,
    //             powerManagementServiceMock,
    //             user,
    //             null,
    //             ocUpload2,
    //             FileUploader.NameCollisionPolicy.OVERWRITE,
    //             FileUploader.LOCAL_BEHAVIOUR_COPY,
    //             targetContext,
    //             false,
    //             false
    //         )
    //             .execute(client, storageManager)
    //             .isSuccess
    //     )
    //
    //     val result2 = ReadFileRemoteOperation("/testFile.txt").execute(client)
    //     assertTrue(result2.isSuccess)
    //
    //     assertEquals(0, (result2.data[0] as RemoteFile).length)
    // }

    /**
     * uploads a file, overwrites it with an empty one, check if overwritten
     */
    @Test
    fun testKeepLocalAndOverwriteRemoteStatic() {
        val file = getDummyFile("chunkedFile.txt")

        FileUploadHelper().uploadNewFiles(
            user,
            arrayOf(file.absolutePath),
            arrayOf("/testFile.txt"),
            FileUploadWorker.LOCAL_BEHAVIOUR_COPY,
            true,
            UploadFileOperation.CREATED_BY_USER,
            false,
            false,
            NameCollisionPolicy.DEFAULT
        )

        longSleep()

        val result = ReadFileRemoteOperation("/testFile.txt").execute(client)
        assertTrue(result.isSuccess)

        assertEquals(file.length(), (result.data[0] as RemoteFile).length)

        val ocFile2 = OCFile("/testFile.txt")
        ocFile2.storagePath = getDummyFile("empty.txt").absolutePath

        FileUploadHelper().uploadUpdatedFile(
            user,
            arrayOf(ocFile2),
            FileUploadWorker.LOCAL_BEHAVIOUR_COPY,
            NameCollisionPolicy.OVERWRITE
        )

        shortSleep()

        val result2 = ReadFileRemoteOperation("/testFile.txt").execute(client)
        assertTrue(result2.isSuccess)

        assertEquals(0, (result2.data[0] as RemoteFile).length)
    }

    /**
     * uploads a file, uploads another one with automatically (2) added, check
     */
    @Test
    fun testKeepBoth() {
        var renameListenerWasTriggered = false

        val file = getDummyFile("chunkedFile.txt")
        val ocUpload = OCUpload(file.absolutePath, "/testFile.txt", account.name)

        assertTrue(
            UploadFileOperation(
                uploadsStorageManager,
                connectivityServiceMock,
                powerManagementServiceMock,
                user,
                null,
                ocUpload,
                NameCollisionPolicy.DEFAULT,
                FileUploadWorker.LOCAL_BEHAVIOUR_COPY,
                targetContext,
                false,
                false,
                storageManager
            )
                .setRemoteFolderToBeCreated()
                .execute(client)
                .isSuccess
        )

        val result = ReadFileRemoteOperation("/testFile.txt").execute(client)
        assertTrue(result.isSuccess)

        assertEquals(file.length(), (result.data[0] as RemoteFile).length)

        val file2 = getDummyFile("empty.txt")
        val ocUpload2 = OCUpload(file2.absolutePath, "/testFile.txt", account.name)

        assertTrue(
            UploadFileOperation(
                uploadsStorageManager,
                connectivityServiceMock,
                powerManagementServiceMock,
                user,
                null,
                ocUpload2,
                NameCollisionPolicy.RENAME,
                FileUploadWorker.LOCAL_BEHAVIOUR_COPY,
                targetContext,
                false,
                false,
                storageManager
            )
                .addRenameUploadListener {
                    renameListenerWasTriggered = true
                }
                .execute(client)
                .isSuccess
        )

        val result2 = ReadFileRemoteOperation("/testFile.txt").execute(client)
        assertTrue(result2.isSuccess)

        assertEquals(file.length(), (result2.data[0] as RemoteFile).length)

        val result3 = ReadFileRemoteOperation("/testFile (2).txt").execute(client)
        assertTrue(result3.isSuccess)

        assertEquals(file2.length(), (result3.data[0] as RemoteFile).length)
        assertTrue(renameListenerWasTriggered)
    }

    /**
     * uploads a file, uploads another one with automatically (2) added, check
     */
    @Test
    fun testKeepBothStatic() {
        val file = getDummyFile("nonEmpty.txt")

        FileUploadHelper().uploadNewFiles(
            user,
            arrayOf(file.absolutePath),
            arrayOf("/testFile.txt"),
            FileUploadWorker.LOCAL_BEHAVIOUR_COPY,
            true,
            UploadFileOperation.CREATED_BY_USER,
            false,
            false,
            NameCollisionPolicy.DEFAULT
        )

        longSleep()

        val result = ReadFileRemoteOperation("/testFile.txt").execute(client)
        assertTrue(result.isSuccess)

        assertEquals(file.length(), (result.data[0] as RemoteFile).length)

        val ocFile2 = OCFile("/testFile.txt")
        ocFile2.storagePath = getDummyFile("empty.txt").absolutePath

        FileUploadHelper().uploadUpdatedFile(
            user,
            arrayOf(ocFile2),
            FileUploadWorker.LOCAL_BEHAVIOUR_COPY,
            NameCollisionPolicy.RENAME
        )

        shortSleep()

        val result2 = ReadFileRemoteOperation("/testFile.txt").execute(client)
        assertTrue(result2.isSuccess)

        assertEquals(file.length(), (result2.data[0] as RemoteFile).length)

        val result3 = ReadFileRemoteOperation("/testFile (2).txt").execute(client)
        assertTrue(result3.isSuccess)

        assertEquals(ocFile2.fileLength, (result3.data[0] as RemoteFile).length)
    }

    /**
     * uploads a file with "keep server" option set, so do nothing
     */
    @Test
    fun testKeepServer() {
        val file = getDummyFile("chunkedFile.txt")
        val ocUpload = OCUpload(file.absolutePath, "/testFile.txt", account.name)

        assertTrue(
            UploadFileOperation(
                uploadsStorageManager,
                connectivityServiceMock,
                powerManagementServiceMock,
                user,
                null,
                ocUpload,
                NameCollisionPolicy.DEFAULT,
                FileUploadWorker.LOCAL_BEHAVIOUR_COPY,
                targetContext,
                false,
                false,
                storageManager
            )
                .setRemoteFolderToBeCreated()
                .execute(client)
                .isSuccess
        )

        val result = ReadFileRemoteOperation("/testFile.txt").execute(client)
        assertTrue(result.isSuccess)

        assertEquals(file.length(), (result.data[0] as RemoteFile).length)

        val ocUpload2 = OCUpload(getDummyFile("empty.txt").absolutePath, "/testFile.txt", account.name)

        assertFalse(
            UploadFileOperation(
                uploadsStorageManager,
                connectivityServiceMock,
                powerManagementServiceMock,
                user,
                null,
                ocUpload2,
                NameCollisionPolicy.CANCEL,
                FileUploadWorker.LOCAL_BEHAVIOUR_COPY,
                targetContext,
                false,
                false,
                storageManager
            )
                .execute(client).isSuccess
        )

        val result2 = ReadFileRemoteOperation("/testFile.txt").execute(client)
        assertTrue(result2.isSuccess)

        assertEquals(file.length(), (result2.data[0] as RemoteFile).length)
    }

    /**
     * uploads a file with "keep server" option set, so do nothing
     */
    @Test
    fun testKeepServerStatic() {
        val file = getDummyFile("chunkedFile.txt")

        FileUploadHelper().uploadNewFiles(
            user,
            arrayOf(file.absolutePath),
            arrayOf("/testFile.txt"),
            FileUploadWorker.LOCAL_BEHAVIOUR_COPY,
            true,
            UploadFileOperation.CREATED_BY_USER,
            false,
            false,
            NameCollisionPolicy.DEFAULT
        )

        longSleep()

        val result = ReadFileRemoteOperation("/testFile.txt").execute(client)
        assertTrue(result.isSuccess)

        assertEquals(file.length(), (result.data[0] as RemoteFile).length)

        val ocFile2 = OCFile("/testFile.txt")
        ocFile2.storagePath = getDummyFile("empty.txt").absolutePath

        FileUploadHelper().uploadUpdatedFile(
            user,
            arrayOf(ocFile2),
            FileUploadWorker.LOCAL_BEHAVIOUR_COPY,
            NameCollisionPolicy.CANCEL
        )

        shortSleep()

        val result2 = ReadFileRemoteOperation("/testFile.txt").execute(client)
        assertTrue(result2.isSuccess)

        assertEquals(file.length(), (result2.data[0] as RemoteFile).length)
    }

    /**
     * uploads a file with "skip if exists" option set, so do nothing if file exists
     */
    @Test
    fun testCancelServer() {
        val file = getDummyFile("chunkedFile.txt")
        val ocUpload = OCUpload(file.absolutePath, "/testFile.txt", account.name)

        assertTrue(
            UploadFileOperation(
                uploadsStorageManager,
                connectivityServiceMock,
                powerManagementServiceMock,
                user,
                null,
                ocUpload,
                NameCollisionPolicy.CANCEL,
                FileUploadWorker.LOCAL_BEHAVIOUR_COPY,
                targetContext,
                false,
                false,
                storageManager
            )
                .setRemoteFolderToBeCreated()
                .execute(client)
                .isSuccess
        )

        val result = ReadFileRemoteOperation("/testFile.txt").execute(client)
        assertTrue(result.isSuccess)

        assertEquals(file.length(), (result.data[0] as RemoteFile).length)

        val ocUpload2 = OCUpload(getDummyFile("empty.txt").absolutePath, "/testFile.txt", account.name)

        val uploadResult = UploadFileOperation(
            uploadsStorageManager,
            connectivityServiceMock,
            powerManagementServiceMock,
            user,
            null,
            ocUpload2,
            NameCollisionPolicy.CANCEL,
            FileUploadWorker.LOCAL_BEHAVIOUR_COPY,
            targetContext,
            false,
            false,
            storageManager
        )
            .execute(client)

        assertFalse(uploadResult.isSuccess)
        assertTrue(uploadResult.exception is OperationCancelledException)

        val result2 = ReadFileRemoteOperation("/testFile.txt").execute(client)
        assertTrue(result2.isSuccess)

        assertEquals(file.length(), (result2.data[0] as RemoteFile).length)
    }

    /**
     * uploads a file with "skip if exists" option set, so do nothing if file exists
     */
    @Test
    fun testKeepCancelStatic() {
        val file = getDummyFile("chunkedFile.txt")

        FileUploadHelper().uploadNewFiles(
            user,
            arrayOf(file.absolutePath),
            arrayOf("/testFile.txt"),
            FileUploadWorker.LOCAL_BEHAVIOUR_COPY,
            true,
            UploadFileOperation.CREATED_BY_USER,
            false,
            false,
            NameCollisionPolicy.DEFAULT
        )

        longSleep()

        val result = ReadFileRemoteOperation("/testFile.txt").execute(client)
        assertTrue(result.isSuccess)

        assertEquals(file.length(), (result.data[0] as RemoteFile).length)

        val ocFile2 = OCFile("/testFile.txt")
        ocFile2.storagePath = getDummyFile("empty.txt").absolutePath

        FileUploadHelper().uploadUpdatedFile(
            user,
            arrayOf(ocFile2),
            FileUploadWorker.LOCAL_BEHAVIOUR_COPY,
            NameCollisionPolicy.CANCEL
        )

        shortSleep()

        val result2 = ReadFileRemoteOperation("/testFile.txt").execute(client)
        assertTrue(result2.isSuccess)

        assertEquals(file.length(), (result2.data[0] as RemoteFile).length)
    }
}
