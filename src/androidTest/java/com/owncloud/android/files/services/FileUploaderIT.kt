/*
 *
 * Nextcloud Android client application
 *
 * @author Tobias Kaminsky
 * Copyright (C) 2020 Tobias Kaminsky
 * Copyright (C) 2020 Nextcloud GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */
package com.owncloud.android.files.services

import com.evernote.android.job.JobRequest
import com.nextcloud.client.account.UserAccountManager
import com.nextcloud.client.account.UserAccountManagerImpl
import com.nextcloud.client.device.PowerManagementService
import com.nextcloud.client.network.ConnectivityService
import com.owncloud.android.AbstractIT
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.datamodel.UploadsStorageManager
import com.owncloud.android.db.OCUpload
import com.owncloud.android.lib.resources.files.ReadFileRemoteOperation
import com.owncloud.android.lib.resources.files.model.RemoteFile
import com.owncloud.android.operations.UploadFileOperation
import com.owncloud.android.utils.FileStorageUtils.getSavePath
import junit.framework.Assert.assertEquals
import junit.framework.Assert.assertFalse
import junit.framework.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File

class FileUploaderIT : AbstractIT() {
    var uploadsStorageManager: UploadsStorageManager? = null

    val connectivityServiceMock: ConnectivityService = object : ConnectivityService {
        override fun isInternetWalled(): Boolean {
            return false
        }

        override fun isOnlineWithWifi(): Boolean {
            return true
        }

        override fun getActiveNetworkType(): JobRequest.NetworkType {
            return JobRequest.NetworkType.ANY
        }
    }

    private val powerManagementServiceMock: PowerManagementService = object : PowerManagementService {
        override val isPowerSavingEnabled: Boolean
            get() = false

        override val isPowerSavingExclusionAvailable: Boolean
            get() = false

        override val isBatteryCharging: Boolean
            get() = false
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
    @Test
    fun testKeepLocalAndOverwriteRemote() {
        val file = File(getSavePath(account.name) + "/chunkedFile.txt")
        val ocUpload = OCUpload(file.absolutePath, "/testFile.txt", account.name)

        assertTrue(UploadFileOperation(
            uploadsStorageManager,
            connectivityServiceMock,
            powerManagementServiceMock,
            account,
            null,
            ocUpload,
            FileUploader.NameCollisionPolicy.DEFAULT,
            FileUploader.LOCAL_BEHAVIOUR_COPY,
            targetContext,
            false,
            false)
            .setRemoteFolderToBeCreated()
            .execute(client, storageManager).isSuccess)

        val result = ReadFileRemoteOperation("/testFile.txt").execute(client)
        assertTrue(result.isSuccess)

        assertEquals(file.length(), (result.data[0] as RemoteFile).length)

        val ocUpload2 = OCUpload(getSavePath(account.name) + "/empty.txt", "/testFile.txt", account.name)

        assertTrue(UploadFileOperation(
            uploadsStorageManager,
            connectivityServiceMock,
            powerManagementServiceMock,
            account,
            null,
            ocUpload2,
            FileUploader.NameCollisionPolicy.OVERWRITE,
            FileUploader.LOCAL_BEHAVIOUR_COPY,
            targetContext,
            false,
            false)
            .execute(client, storageManager).isSuccess)

        val result2 = ReadFileRemoteOperation("/testFile.txt").execute(client)
        assertTrue(result2.isSuccess)

        assertEquals(0, (result2.data[0] as RemoteFile).length)
    }

    /**
     * uploads a file, overwrites it with an empty one, check if overwritten
     */
    @Test
    fun testKeepLocalAndOverwriteRemoteStatic() {
        val file = File(getSavePath(account.name) + "/chunkedFile.txt")

        FileUploader.uploadNewFile(
            targetContext,
            account,
            file.absolutePath,
            "/testFile.txt",
            FileUploader.LOCAL_BEHAVIOUR_COPY,
            null,
            true,
            UploadFileOperation.CREATED_BY_USER,
            false,
            false,
            FileUploader.NameCollisionPolicy.DEFAULT)

        longSleep()

        val result = ReadFileRemoteOperation("/testFile.txt").execute(client)
        assertTrue(result.isSuccess)

        assertEquals(file.length(), (result.data[0] as RemoteFile).length)

        val ocFile2 = OCFile("/testFile.txt")
        ocFile2.setStoragePath(getSavePath(account.name) + "/empty.txt")

        FileUploader.uploadUpdateFile(
            targetContext,
            account,
            ocFile2,
            FileUploader.LOCAL_BEHAVIOUR_COPY,
            FileUploader.NameCollisionPolicy.OVERWRITE)

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

        val file = File(getSavePath(account.name) + "/chunkedFile.txt")
        val ocUpload = OCUpload(file.absolutePath, "/testFile.txt", account.name)

        assertTrue(UploadFileOperation(
            uploadsStorageManager,
            connectivityServiceMock,
            powerManagementServiceMock,
            account,
            null,
            ocUpload,
            FileUploader.NameCollisionPolicy.DEFAULT,
            FileUploader.LOCAL_BEHAVIOUR_COPY,
            targetContext,
            false,
            false)
            .setRemoteFolderToBeCreated()
            .execute(client, storageManager).isSuccess)

        val result = ReadFileRemoteOperation("/testFile.txt").execute(client)
        assertTrue(result.isSuccess)

        assertEquals(file.length(), (result.data[0] as RemoteFile).length)

        val file2 = File(getSavePath(account.name) + "/empty.txt")
        val ocUpload2 = OCUpload(file2.absolutePath, "/testFile.txt", account.name)

        assertTrue(UploadFileOperation(
            uploadsStorageManager,
            connectivityServiceMock,
            powerManagementServiceMock,
            account,
            null,
            ocUpload2,
            FileUploader.NameCollisionPolicy.RENAME,
            FileUploader.LOCAL_BEHAVIOUR_COPY,
            targetContext,
            false,
            false)
            .addRenameUploadListener {
                renameListenerWasTriggered = true
            }
            .execute(client, storageManager).isSuccess)

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
        val file = File(getSavePath(account.name) + "/chunkedFile.txt")

        FileUploader.uploadNewFile(
            targetContext,
            account,
            file.absolutePath,
            "/testFile.txt",
            FileUploader.LOCAL_BEHAVIOUR_COPY,
            null,
            true,
            UploadFileOperation.CREATED_BY_USER,
            false,
            false,
            FileUploader.NameCollisionPolicy.DEFAULT)

        longSleep()

        val result = ReadFileRemoteOperation("/testFile.txt").execute(client)
        assertTrue(result.isSuccess)

        assertEquals(file.length(), (result.data[0] as RemoteFile).length)

        val ocFile2 = OCFile("/testFile.txt")
        ocFile2.setStoragePath(getSavePath(account.name) + "/empty.txt")

        FileUploader.uploadUpdateFile(
            targetContext,
            account,
            ocFile2,
            FileUploader.LOCAL_BEHAVIOUR_COPY,
            FileUploader.NameCollisionPolicy.RENAME)

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
        val file = File(getSavePath(account.name) + "/chunkedFile.txt")
        val ocUpload = OCUpload(file.absolutePath, "/testFile.txt", account.name)

        assertTrue(UploadFileOperation(
            uploadsStorageManager,
            connectivityServiceMock,
            powerManagementServiceMock,
            account,
            null,
            ocUpload,
            FileUploader.NameCollisionPolicy.DEFAULT,
            FileUploader.LOCAL_BEHAVIOUR_COPY,
            targetContext,
            false,
            false)
            .setRemoteFolderToBeCreated()
            .execute(client, storageManager).isSuccess)

        val result = ReadFileRemoteOperation("/testFile.txt").execute(client)
        assertTrue(result.isSuccess)

        assertEquals(file.length(), (result.data[0] as RemoteFile).length)

        val ocUpload2 = OCUpload(getSavePath(account.name) + "/empty.txt", "/testFile.txt", account.name)

        assertFalse(UploadFileOperation(
            uploadsStorageManager,
            connectivityServiceMock,
            powerManagementServiceMock,
            account,
            null,
            ocUpload2,
            FileUploader.NameCollisionPolicy.CANCEL,
            FileUploader.LOCAL_BEHAVIOUR_COPY,
            targetContext,
            false,
            false)
            .execute(client, storageManager).isSuccess)

        val result2 = ReadFileRemoteOperation("/testFile.txt").execute(client)
        assertTrue(result2.isSuccess)

        assertEquals(file.length(), (result2.data[0] as RemoteFile).length)
    }

    /**
     * uploads a file with "keep server" option set, so do nothing
     */
    @Test
    fun testKeepServerStatic() {
        val file = File(getSavePath(account.name) + "/chunkedFile.txt")

        FileUploader.uploadNewFile(
            targetContext,
            account,
            file.absolutePath,
            "/testFile.txt",
            FileUploader.LOCAL_BEHAVIOUR_COPY,
            null,
            true,
            UploadFileOperation.CREATED_BY_USER,
            false,
            false,
            FileUploader.NameCollisionPolicy.DEFAULT)

        longSleep()

        val result = ReadFileRemoteOperation("/testFile.txt").execute(client)
        assertTrue(result.isSuccess)

        assertEquals(file.length(), (result.data[0] as RemoteFile).length)

        val ocFile2 = OCFile("/testFile.txt")
        ocFile2.setStoragePath(getSavePath(account.name) + "/empty.txt")

        FileUploader.uploadUpdateFile(
            targetContext,
            account,
            ocFile2,
            FileUploader.LOCAL_BEHAVIOUR_COPY,
            FileUploader.NameCollisionPolicy.CANCEL)

        shortSleep()

        val result2 = ReadFileRemoteOperation("/testFile.txt").execute(client)
        assertTrue(result2.isSuccess)

        assertEquals(file.length(), (result2.data[0] as RemoteFile).length)
    }
}
