/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-FileCopyrightText: 2020 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2020 Chris Narkiewicz <hello@ezaquarii.com>
 * SPDX-FileCopyrightText: 2020 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
@file:Suppress("DEPRECATION")

package com.owncloud.android

import com.nextcloud.client.account.UserAccountManagerImpl
import com.nextcloud.client.device.PowerManagementService
import com.nextcloud.client.jobs.upload.FileUploadWorker
import com.nextcloud.client.network.ConnectivityManagerFactory
import com.nextcloud.client.network.ConnectivityService
import com.nextcloud.test.SinceServer
import com.nextcloud.utils.PowerManagementFactory
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.datamodel.UploadsStorageManager
import com.owncloud.android.db.OCUpload
import com.owncloud.android.files.services.NameCollisionPolicy
import com.owncloud.android.lib.common.operations.RemoteOperationResult.ResultCode
import com.owncloud.android.lib.resources.files.model.GeoLocation
import com.owncloud.android.lib.resources.files.model.ImageDimension
import com.owncloud.android.lib.resources.status.NextcloudVersion
import com.owncloud.android.operations.RefreshFolderOperation
import com.owncloud.android.operations.RemoveFileOperation
import com.owncloud.android.operations.UploadFileOperation
import com.owncloud.android.utils.FileStorageUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import java.nio.file.Files
import java.nio.file.attribute.BasicFileAttributes
import java.util.concurrent.TimeUnit
import kotlin.math.abs

class UploadIT : AbstractOnServerIT() {
    private val uploadsStorageManager = UploadsStorageManager(
        UserAccountManagerImpl.fromContext(targetContext),
        targetContext.contentResolver
    )

    private val powerManagementServiceMock = PowerManagementFactory.mock

    @Before
    fun before() {
        createDummyFiles()
    }

    @Test
    fun testEmptyUpload() {
        uploadOCUpload(ocUpload(EMPTY_FILE, "$FOLDER$EMPTY_FILE"))
    }

    @Test
    fun testNonEmptyUpload() {
        uploadOCUpload(ocUpload(NON_EMPTY_FILE, "$FOLDER$NON_EMPTY_FILE"))
    }

    @Test
    fun testUploadWithCopy() {
        uploadOCUpload(nonEmptyUpload(), FileUploadWorker.LOCAL_BEHAVIOUR_COPY)

        val uploadedFile = uploadedNonEmptyFile()
        assertTrue(temporalFile(NON_EMPTY_FILE).exists())
        assertTrue(File(uploadedFile.storagePath).exists())
        verifyStoragePath(uploadedFile)
    }

    @Test
    fun testUploadWithMove() {
        uploadOCUpload(nonEmptyUpload(), FileUploadWorker.LOCAL_BEHAVIOUR_MOVE)

        val uploadedFile = uploadedNonEmptyFile()
        assertFalse(temporalFile(NON_EMPTY_FILE).exists())
        assertTrue(File(uploadedFile.storagePath).exists())
        verifyStoragePath(uploadedFile)
    }

    @Test
    fun testUploadWithForget() {
        uploadOCUpload(nonEmptyUpload(), FileUploadWorker.LOCAL_BEHAVIOUR_FORGET)

        val uploadedFile = uploadedNonEmptyFile()
        assertTrue(temporalFile(NON_EMPTY_FILE).exists())
        assertFalse(File(uploadedFile.storagePath).exists())
        assertTrue(uploadedFile.storagePath.isEmpty())
    }

    @Test
    fun testUploadWithDelete() {
        uploadOCUpload(nonEmptyUpload(), FileUploadWorker.LOCAL_BEHAVIOUR_DELETE)

        val uploadedFile = uploadedNonEmptyFile()
        assertFalse(temporalFile(NON_EMPTY_FILE).exists())
        assertFalse(File(uploadedFile.storagePath).exists())
        assertTrue(uploadedFile.storagePath.isEmpty())
    }

    @Test
    fun testChunkedUpload() {
        uploadOCUpload(ocUpload(CHUNKED_FILE, "$FOLDER$CHUNKED_FILE"))
    }

    @Test
    fun testUploadInNonExistingFolder() {
        uploadOCUpload(ocUpload(EMPTY_FILE, "${FOLDER}2/3/4/1.txt"))
    }

    @Test
    fun testUploadOnChargingOnlyButNotCharging() {
        val upload = ocUpload(EMPTY_FILE, "${FOLDER}notCharging.txt").apply { isWhileChargingOnly = true }

        val result = uploadOperation(upload, whileChargingOnly = true).execute(client)

        assertFalse(result.toString(), result.isSuccess)
        assertEquals(ResultCode.DELAYED_FOR_CHARGING, result.code)
    }

    @Test
    fun testUploadOnChargingOnlyAndCharging() {
        val upload = ocUpload(EMPTY_FILE, "${FOLDER}charging.txt").apply { isWhileChargingOnly = true }

        val result = uploadOperation(
            upload,
            powerManagementService = PowerManagementFactory.mockCharging,
            whileChargingOnly = true
        ).execute(client)

        assertTrue(result.toString(), result.isSuccess)
    }

    @Test
    fun testUploadOnWifiOnlyButNoWifi() {
        val upload = ocUpload(EMPTY_FILE, "${FOLDER}noWifi.txt").apply { isUseWifiOnly = true }

        val result = uploadOperation(
            upload,
            connectivityService = ConnectivityManagerFactory.wifi,
            onWifiOnly = true
        ).execute(client)

        assertFalse(result.toString(), result.isSuccess)
        assertEquals(ResultCode.DELAYED_FOR_WIFI, result.code)
    }

    @Test
    fun testUploadOnWifiOnlyAndWifi() {
        val upload = ocUpload(EMPTY_FILE, "${FOLDER}wifi.txt").apply { isWhileChargingOnly = true }

        val result = uploadOperation(upload, onWifiOnly = true).execute(client)

        assertTrue(result.toString(), result.isSuccess)
        removeTestFolder()
    }

    @Test
    fun testUploadOnWifiOnlyButMeteredWifi() {
        val upload = ocUpload(EMPTY_FILE, "${FOLDER}noWifi.txt").apply { isUseWifiOnly = true }

        val result = uploadOperation(
            upload,
            connectivityService = ConnectivityManagerFactory.metered,
            onWifiOnly = true
        ).execute(client)

        assertFalse(result.toString(), result.isSuccess)
        assertEquals(ResultCode.DELAYED_FOR_WIFI, result.code)
    }

    @Test
    @SinceServer(majorVersion = 27)
    fun testCreationAndUploadTimestamp() {
        testOnlyOnServer(NextcloudVersion.nextcloud_27)

        val file = getDummyFile(EMPTY_FILE)
        val remotePath = "/testFile.txt"

        assertTrue(uploadOperation(OCUpload(file.absolutePath, remotePath, account.name)).execute(client).isSuccess)

        val creationTimestamp = Files.readAttributes(file.toPath(), BasicFileAttributes::class.java)
            .creationTime()
            .to(TimeUnit.SECONDS)
        val uploadTimestamp = nowInSeconds()

        val ocFile = refreshRootAndGetContent().first()

        assertEquals(remotePath, ocFile.remotePath)
        assertEquals(creationTimestamp, ocFile.creationTimestamp)
        assertTrue(abs(ocFile.uploadTimestamp - uploadTimestamp) < UPLOAD_TIMESTAMP_TOLERANCE_SECONDS)
    }

    @Test
    @SinceServer(majorVersion = 27)
    fun testMetadata() {
        testOnlyOnServer(NextcloudVersion.nextcloud_27)

        val file = getFile("gps.jpg")
        val remotePath = "/$METADATA_FILE"

        assertTrue(uploadOperation(OCUpload(file.absolutePath, remotePath, account.name)).execute(client).isSuccess)

        val ocFile = checkNotNull(refreshRootAndGetContent().find { it.fileName == METADATA_FILE }) {
            "$METADATA_FILE not found in root folder"
        }

        assertEquals(remotePath, ocFile.remotePath)
        assertEquals(GeoLocation(EXPECTED_LATITUDE, EXPECTED_LONGITUDE), ocFile.geoLocation)
        assertEquals(ImageDimension(EXPECTED_WIDTH, EXPECTED_HEIGHT), ocFile.imageDimension)
    }

    private fun uploadOperation(
        upload: OCUpload,
        connectivityService: ConnectivityService = connectivityServiceMock,
        powerManagementService: PowerManagementService = powerManagementServiceMock,
        onWifiOnly: Boolean = false,
        whileChargingOnly: Boolean = false
    ): UploadFileOperation = UploadFileOperation(
        uploadsStorageManager,
        connectivityService,
        powerManagementService,
        user,
        null,
        upload,
        NameCollisionPolicy.DEFAULT,
        FileUploadWorker.LOCAL_BEHAVIOUR_COPY,
        targetContext,
        onWifiOnly,
        whileChargingOnly,
        storageManager
    ).apply {
        setRemoteFolderToBeCreated()
        addRenameUploadListener {}
    }

    private fun refreshRootAndGetContent(): List<OCFile> {
        val refreshResult = RefreshFolderOperation(
            storageManager.getFileByDecryptedRemotePath(OCFile.ROOT_PATH),
            nowInSeconds(),
            false,
            false,
            storageManager,
            user,
            targetContext
        ).execute(client)
        assertTrue(refreshResult.isSuccess)

        return storageManager.getFolderContent(storageManager.getFileByDecryptedRemotePath(OCFile.ROOT_PATH), false)
    }

    private fun removeTestFolder() {
        val folder = checkNotNull(storageManager.getFileByDecryptedRemotePath(FOLDER)) { "$FOLDER not found" }
        RemoveFileOperation(folder, false, user, false, targetContext, storageManager).execute(client)
    }

    private fun temporalPath(name: String) = "${FileStorageUtils.getTemporalPath(account.name)}/$name"

    private fun temporalFile(name: String) = File(temporalPath(name))

    private fun ocUpload(localName: String, remotePath: String) =
        OCUpload(temporalPath(localName), remotePath, account.name)

    private fun nonEmptyUpload() = ocUpload(NON_EMPTY_FILE, "$FOLDER$NON_EMPTY_FILE")

    private fun uploadedNonEmptyFile(): OCFile =
        checkNotNull(storageManager.getFileByDecryptedRemotePath("$FOLDER$NON_EMPTY_FILE")) {
            "$FOLDER$NON_EMPTY_FILE not found"
        }

    private fun nowInSeconds() = TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis())

    private fun verifyStoragePath(file: OCFile) {
        assertEquals(
            FileStorageUtils.getSavePath(account.name) + FOLDER + file.decryptedFileName,
            file.storagePath
        )
    }

    companion object {
        private const val FOLDER = "/testUpload/"
        private const val EMPTY_FILE = "empty.txt"
        private const val NON_EMPTY_FILE = "nonEmpty.txt"
        private const val CHUNKED_FILE = "chunkedFile.txt"
        private const val METADATA_FILE = "metadata.jpg"
        private const val UPLOAD_TIMESTAMP_TOLERANCE_SECONDS = 10
        private const val EXPECTED_LATITUDE = 64.0
        private const val EXPECTED_LONGITUDE = -46.0
        private const val EXPECTED_WIDTH = 300f
        private const val EXPECTED_HEIGHT = 200f
    }
}
