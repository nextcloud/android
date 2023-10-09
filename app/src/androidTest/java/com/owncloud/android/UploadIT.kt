/*
 * Nextcloud Android client application
 *
 * @author Tobias Kaminsky
 * Copyright (C) 2020 Tobias Kaminsky
 * Copyright (C) 2020 Nextcloud GmbH
 * Copyright (C) 2020 Chris Narkiewicz <hello@ezaquarii.com>
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
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package com.owncloud.android

import com.nextcloud.client.account.UserAccountManagerImpl
import com.nextcloud.client.device.BatteryStatus
import com.nextcloud.client.device.PowerManagementService
import com.nextcloud.client.network.Connectivity
import com.nextcloud.client.network.ConnectivityService
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.datamodel.UploadsStorageManager
import com.owncloud.android.db.OCUpload
import com.owncloud.android.files.services.FileUploader
import com.owncloud.android.files.services.NameCollisionPolicy
import com.owncloud.android.lib.common.operations.RemoteOperationResult
import com.owncloud.android.lib.resources.files.model.GeoLocation
import com.owncloud.android.lib.resources.files.model.ImageDimension
import com.owncloud.android.operations.RefreshFolderOperation
import com.owncloud.android.operations.RemoveFileOperation
import com.owncloud.android.operations.UploadFileOperation
import com.owncloud.android.utils.FileStorageUtils
import junit.framework.TestCase
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.attribute.BasicFileAttributes
import java.util.concurrent.TimeUnit

/**
 * Tests related to file uploads
 */
class UploadIT : AbstractOnServerIT() {
    private val uploadsStorageManager = UploadsStorageManager(
        UserAccountManagerImpl.fromContext(targetContext),
        targetContext.contentResolver
    )
    private val connectivityServiceMock: ConnectivityService = object : ConnectivityService {
        override fun isInternetWalled(): Boolean {
            return false
        }

        override fun getConnectivity(): Connectivity {
            return Connectivity.CONNECTED_WIFI
        }
    }
    private val powerManagementServiceMock: PowerManagementService = object : PowerManagementService {
        override val isPowerSavingEnabled: Boolean
            get() = false
        override val isPowerSavingExclusionAvailable: Boolean
            get() = false
        override val battery: BatteryStatus
            get() = BatteryStatus(false, 0)
    }

    @Before
    @Throws(IOException::class)
    fun before() {
        // make sure that every file is available, even after tests that remove source file
        createDummyFiles()
    }

    @After
    override fun after() {
        val result = RefreshFolderOperation(
            storageManager.getFileByPath("/"),
            System.currentTimeMillis() / 1000L,
            false,
            true,
            storageManager,
            user,
            targetContext
        )
            .execute(client)

        // cleanup only if folder exists
        if (result.isSuccess && storageManager.getFileByDecryptedRemotePath(FOLDER) != null) {
            RemoveFileOperation(
                storageManager.getFileByDecryptedRemotePath(FOLDER),
                false,
                user,
                false,
                targetContext,
                storageManager
            )
                .execute(client)
        }
    }

    @Test
    fun testEmptyUpload() {
        val ocUpload = OCUpload(
            FileStorageUtils.getTemporalPath(account.name) + "/empty.txt",
            FOLDER + "empty.txt",
            account.name
        )
        uploadOCUpload(ocUpload)
    }

    @Test
    fun testNonEmptyUpload() {
        val ocUpload = OCUpload(
            FileStorageUtils.getTemporalPath(account.name) + "/nonEmpty.txt",
            FOLDER + "nonEmpty.txt",
            account.name
        )
        uploadOCUpload(ocUpload)
    }

    @Test
    fun testUploadWithCopy() {
        val ocUpload = OCUpload(
            FileStorageUtils.getTemporalPath(account.name) + "/nonEmpty.txt",
            FOLDER + "nonEmpty.txt",
            account.name
        )
        uploadOCUpload(ocUpload, FileUploader.LOCAL_BEHAVIOUR_COPY)
        val originalFile = File(FileStorageUtils.getTemporalPath(account.name) + "/nonEmpty.txt")
        val uploadedFile = fileDataStorageManager.getFileByDecryptedRemotePath(FOLDER + "nonEmpty.txt")
        TestCase.assertTrue(originalFile.exists())
        TestCase.assertTrue(File(uploadedFile!!.storagePath).exists())
        verifyStoragePath(uploadedFile)
    }

    @Test
    fun testUploadWithMove() {
        val ocUpload = OCUpload(
            FileStorageUtils.getTemporalPath(account.name) + "/nonEmpty.txt",
            FOLDER + "nonEmpty.txt",
            account.name
        )
        uploadOCUpload(ocUpload, FileUploader.LOCAL_BEHAVIOUR_MOVE)
        val originalFile = File(FileStorageUtils.getTemporalPath(account.name) + "/nonEmpty.txt")
        val uploadedFile = fileDataStorageManager.getFileByDecryptedRemotePath(FOLDER + "nonEmpty.txt")
        TestCase.assertFalse(originalFile.exists())
        TestCase.assertTrue(File(uploadedFile!!.storagePath).exists())
        verifyStoragePath(uploadedFile)
    }

    @Test
    fun testUploadWithForget() {
        val ocUpload = OCUpload(
            FileStorageUtils.getTemporalPath(account.name) + "/nonEmpty.txt",
            FOLDER + "nonEmpty.txt",
            account.name
        )
        uploadOCUpload(ocUpload, FileUploader.LOCAL_BEHAVIOUR_FORGET)
        val originalFile = File(FileStorageUtils.getTemporalPath(account.name) + "/nonEmpty.txt")
        val uploadedFile = fileDataStorageManager.getFileByDecryptedRemotePath(FOLDER + "nonEmpty.txt")
        TestCase.assertTrue(originalFile.exists())
        TestCase.assertFalse(File(uploadedFile!!.storagePath).exists())
        TestCase.assertTrue(uploadedFile.storagePath.isEmpty())
    }

    @Test
    fun testUploadWithDelete() {
        val ocUpload = OCUpload(
            FileStorageUtils.getTemporalPath(account.name) + "/nonEmpty.txt",
            FOLDER + "nonEmpty.txt",
            account.name
        )
        uploadOCUpload(ocUpload, FileUploader.LOCAL_BEHAVIOUR_DELETE)
        val originalFile = File(FileStorageUtils.getTemporalPath(account.name) + "/nonEmpty.txt")
        val uploadedFile = fileDataStorageManager.getFileByDecryptedRemotePath(FOLDER + "nonEmpty.txt")
        TestCase.assertFalse(originalFile.exists())
        TestCase.assertFalse(File(uploadedFile!!.storagePath).exists())
        TestCase.assertTrue(uploadedFile.storagePath.isEmpty())
    }

    @Test
    fun testChunkedUpload() {
        val ocUpload = OCUpload(
            FileStorageUtils.getTemporalPath(account.name) + "/chunkedFile.txt",
            FOLDER + "chunkedFile.txt", account.name
        )
        uploadOCUpload(ocUpload)
    }

    @Test
    fun testUploadInNonExistingFolder() {
        val ocUpload = OCUpload(
            FileStorageUtils.getTemporalPath(account.name) + "/empty.txt",
            FOLDER + "2/3/4/1.txt", account.name
        )
        uploadOCUpload(ocUpload)
    }

    @Test
    fun testUploadOnChargingOnlyButNotCharging() {
        val ocUpload = OCUpload(
            FileStorageUtils.getTemporalPath(account.name) + "/empty.txt",
            FOLDER + "notCharging.txt", account.name
        )
        ocUpload.isWhileChargingOnly = true
        val newUpload = UploadFileOperation(
            uploadsStorageManager,
            connectivityServiceMock,
            powerManagementServiceMock,
            user,
            null,
            ocUpload,
            NameCollisionPolicy.DEFAULT,
            FileUploader.LOCAL_BEHAVIOUR_COPY,
            targetContext,
            false,
            true,
            storageManager
        )
        newUpload.setRemoteFolderToBeCreated()
        newUpload.addRenameUploadListener {}
        val result = newUpload.execute(client)
        TestCase.assertFalse(result.toString(), result.isSuccess)
        TestCase.assertEquals(RemoteOperationResult.ResultCode.DELAYED_FOR_CHARGING, result.code)
    }

    @Test
    fun testUploadOnChargingOnlyAndCharging() {
        val powerManagementServiceMock: PowerManagementService = object : PowerManagementService {
            override val isPowerSavingEnabled: Boolean
                get() = false
            override val isPowerSavingExclusionAvailable: Boolean
                get() = false
            override val battery: BatteryStatus
                get() = BatteryStatus(true, 100)
        }
        val ocUpload = OCUpload(
            FileStorageUtils.getTemporalPath(account.name) + "/empty.txt",
            FOLDER + "charging.txt", account.name
        )
        ocUpload.isWhileChargingOnly = true
        val newUpload = UploadFileOperation(
            uploadsStorageManager,
            connectivityServiceMock,
            powerManagementServiceMock,
            user,
            null,
            ocUpload,
            NameCollisionPolicy.DEFAULT,
            FileUploader.LOCAL_BEHAVIOUR_COPY,
            targetContext,
            false,
            true,
            storageManager
        )
        newUpload.setRemoteFolderToBeCreated()
        newUpload.addRenameUploadListener {}
        val result = newUpload.execute(client)
        TestCase.assertTrue(result.toString(), result.isSuccess)
    }

    @Test
    fun testUploadOnWifiOnlyButNoWifi() {
        val connectivityServiceMock: ConnectivityService = object : ConnectivityService {
            override fun isInternetWalled(): Boolean {
                return false
            }

            override fun getConnectivity(): Connectivity {
                return Connectivity(true, false, false, true)
            }
        }
        val ocUpload = OCUpload(
            FileStorageUtils.getTemporalPath(account.name) + "/empty.txt",
            FOLDER + "noWifi.txt", account.name
        )
        ocUpload.isUseWifiOnly = true
        val newUpload = UploadFileOperation(
            uploadsStorageManager,
            connectivityServiceMock,
            powerManagementServiceMock,
            user,
            null,
            ocUpload,
            NameCollisionPolicy.DEFAULT,
            FileUploader.LOCAL_BEHAVIOUR_COPY,
            targetContext,
            true,
            false,
            storageManager
        )
        newUpload.setRemoteFolderToBeCreated()
        newUpload.addRenameUploadListener {}
        val result = newUpload.execute(client)
        TestCase.assertFalse(result.toString(), result.isSuccess)
        TestCase.assertEquals(RemoteOperationResult.ResultCode.DELAYED_FOR_WIFI, result.code)
    }

    @Test
    fun testUploadOnWifiOnlyAndWifi() {
        val ocUpload = OCUpload(
            FileStorageUtils.getTemporalPath(account.name) + "/empty.txt",
            FOLDER + "wifi.txt", account.name
        )
        ocUpload.isWhileChargingOnly = true
        val newUpload = UploadFileOperation(
            uploadsStorageManager,
            connectivityServiceMock,
            powerManagementServiceMock,
            user,
            null,
            ocUpload,
            NameCollisionPolicy.DEFAULT,
            FileUploader.LOCAL_BEHAVIOUR_COPY,
            targetContext,
            true,
            false,
            storageManager
        )
        newUpload.setRemoteFolderToBeCreated()
        newUpload.addRenameUploadListener {}
        val result = newUpload.execute(client)
        TestCase.assertTrue(result.toString(), result.isSuccess)

        // cleanup
        RemoveFileOperation(
            storageManager.getFileByDecryptedRemotePath(FOLDER),
            false,
            user,
            false,
            targetContext,
            storageManager
        )
            .execute(client)
    }

    @Test
    fun testUploadOnWifiOnlyButMeteredWifi() {
        val connectivityServiceMock: ConnectivityService = object : ConnectivityService {
            override fun isInternetWalled(): Boolean {
                return false
            }

            override fun getConnectivity(): Connectivity {
                return Connectivity(true, true, true, true)
            }
        }
        val ocUpload = OCUpload(
            FileStorageUtils.getTemporalPath(account.name) + "/empty.txt",
            FOLDER + "noWifi.txt",
            account.name
        )
        ocUpload.isUseWifiOnly = true
        val newUpload = UploadFileOperation(
            uploadsStorageManager,
            connectivityServiceMock,
            powerManagementServiceMock,
            user,
            null,
            ocUpload,
            NameCollisionPolicy.DEFAULT,
            FileUploader.LOCAL_BEHAVIOUR_COPY,
            targetContext,
            true,
            false,
            storageManager
        )
        newUpload.setRemoteFolderToBeCreated()
        newUpload.addRenameUploadListener {}
        val result = newUpload.execute(client)
        TestCase.assertFalse(result.toString(), result.isSuccess)
        TestCase.assertEquals(RemoteOperationResult.ResultCode.DELAYED_FOR_WIFI, result.code)
    }

    @Test
    @Throws(IOException::class)
    fun testCreationAndUploadTimestamp() {
        val file = getDummyFile("empty.txt")
        val remotePath = "/testFile.txt"
        val ocUpload = OCUpload(file.absolutePath, remotePath, account.name)
        TestCase.assertTrue(
            UploadFileOperation(
                uploadsStorageManager,
                connectivityServiceMock,
                powerManagementServiceMock,
                user,
                null,
                ocUpload,
                NameCollisionPolicy.DEFAULT,
                FileUploader.LOCAL_BEHAVIOUR_COPY,
                targetContext,
                false,
                false,
                storageManager
            )
                .setRemoteFolderToBeCreated()
                .execute(client)
                .isSuccess
        )
        val creationTimestamp = Files.readAttributes(file.toPath(), BasicFileAttributes::class.java)
            .creationTime()
            .to(TimeUnit.SECONDS)
        val uploadTimestamp = System.currentTimeMillis() / 1000

        // RefreshFolderOperation
        TestCase.assertTrue(
            RefreshFolderOperation(
                storageManager.getFileByDecryptedRemotePath("/"),
                System.currentTimeMillis() / 1000,
                false,
                false,
                storageManager,
                user,
                targetContext
            ).execute(client).isSuccess
        )
        val files = storageManager.getFolderContent(
            storageManager.getFileByDecryptedRemotePath("/"),
            false
        )
        val ocFile = files[0]
        TestCase.assertEquals(remotePath, ocFile.remotePath)
        TestCase.assertEquals(creationTimestamp, ocFile.creationTimestamp)
        TestCase.assertTrue(
            uploadTimestamp - 10 < ocFile.uploadTimestamp ||
                uploadTimestamp + 10 > ocFile.uploadTimestamp
        )
    }

    @Test
    @Throws(IOException::class)
    fun testMetadata() {
        val file = getFile("gps.jpg")
        val remotePath = "/gps.jpg"
        val ocUpload = OCUpload(file.absolutePath, remotePath, account.name)
        TestCase.assertTrue(
            UploadFileOperation(
                uploadsStorageManager,
                connectivityServiceMock,
                powerManagementServiceMock,
                user,
                null,
                ocUpload,
                NameCollisionPolicy.DEFAULT,
                FileUploader.LOCAL_BEHAVIOUR_COPY,
                targetContext,
                false,
                false,
                storageManager
            )
                .setRemoteFolderToBeCreated()
                .execute(client)
                .isSuccess
        )

        // RefreshFolderOperation
        TestCase.assertTrue(
            RefreshFolderOperation(
                storageManager.getFileByDecryptedRemotePath("/"),
                System.currentTimeMillis() / 1000,
                false,
                false,
                storageManager,
                user,
                targetContext
            ).execute(client).isSuccess
        )
        val files = storageManager.getFolderContent(
            storageManager.getFileByDecryptedRemotePath("/"),
            false
        )
        var ocFile: OCFile? = null
        for (f in files) {
            if (f.fileName == "gps.jpg") {
                ocFile = f
                break
            }
        }
        TestCase.assertNotNull(ocFile)
        TestCase.assertEquals(remotePath, ocFile!!.remotePath)
        TestCase.assertEquals(ImageDimension(451f, 529f), ocFile.imageDimension)
        TestCase.assertEquals(GeoLocation(49.99679166666667, 8.67198611111111), ocFile.geoLocation)
    }

    private fun verifyStoragePath(file: OCFile?) {
        TestCase.assertEquals(
            FileStorageUtils.getSavePath(account.name) + FOLDER + file!!.decryptedFileName,
            file.storagePath
        )
    }

    companion object {
        private const val FOLDER = "/testUpload/"
    }
}