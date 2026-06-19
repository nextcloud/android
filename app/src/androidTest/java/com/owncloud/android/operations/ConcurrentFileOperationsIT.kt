/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.owncloud.android.operations

import com.nextcloud.client.account.UserAccountManagerImpl
import com.nextcloud.client.device.BatteryStatus
import com.nextcloud.client.device.PowerManagementService
import com.nextcloud.client.jobs.upload.FileUploadWorker
import com.nextcloud.client.network.Connectivity
import com.nextcloud.client.network.ConnectivityService
import com.nextcloud.client.network.NetworkChangeListener
import com.owncloud.android.AbstractOnServerIT
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.datamodel.UploadsStorageManager
import com.owncloud.android.db.OCUpload
import com.owncloud.android.files.services.NameCollisionPolicy
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class ConcurrentFileOperationsIT : AbstractOnServerIT() {

    companion object {
        private const val TIMEOUT_SECONDS = 120L
        private const val FILE_SIZE_ITERATIONS = 500
        private const val FILE_COUNT = 3
    }

    private val connectivityServiceMock: ConnectivityService = object : ConnectivityService {
        override fun addListener(listener: NetworkChangeListener) = Unit
        override fun removeListener(listener: NetworkChangeListener) = Unit
        override fun isNetworkAndServerAvailable(callback: ConnectivityService.GenericCallback<Boolean>) = Unit
        override fun isConnected(): Boolean = true
        override fun isInternetWalled(): Boolean = false
        override fun getConnectivity(): Connectivity = Connectivity.CONNECTED_WIFI
    }

    private val powerManagementServiceMock: PowerManagementService = object : PowerManagementService {
        override val isIgnoringOptimization: Boolean get() = true
        override val isPowerSavingEnabled: Boolean get() = false
        override val battery: BatteryStatus get() = BatteryStatus()
    }

    private val uploadsStorageManager: UploadsStorageManager by lazy {
        UploadsStorageManager(
            UserAccountManagerImpl.fromContext(targetContext),
            targetContext.contentResolver
        )
    }

    @Test
    fun uploadAndDownloadConcurrently() {
        val remoteFolder = "/concurrentUploadDownload/"
        val fileData = prepareFilesOnServer(remoteFolder, prefix = "ud")
        val ocFiles = resolveOCFiles(remoteFolder, fileData)

        val errors = CopyOnWriteArrayList<String>()
        val latch = CountDownLatch(2)

        startUploadThread(fileData, errors, latch)
        startDownloadThread(ocFiles, errors, latch)

        assertTrue("Concurrent upload+download timed out", latch.await(TIMEOUT_SECONDS, TimeUnit.SECONDS))
        assertFalse("Unexpected errors during concurrent upload+download: $errors", errors.isNotEmpty())
    }

    @Test
    fun uploadAndDeleteLocalOnlyConcurrently() {
        val remoteFolder = "/concurrentUploadDelete/"
        val fileData = prepareFilesOnServer(remoteFolder, prefix = "udel")
        val ocFiles = resolveOCFiles(remoteFolder, fileData)

        val errors = CopyOnWriteArrayList<String>()
        val latch = CountDownLatch(2)

        startUploadThread(fileData, errors, latch)
        startDeleteLocalThread(ocFiles, errors, latch, delayMs = 500L)

        assertTrue("Concurrent upload+delete timed out", latch.await(TIMEOUT_SECONDS, TimeUnit.SECONDS))
        assertFalse("Unexpected errors during concurrent upload+delete: $errors", errors.isNotEmpty())
    }

    @Test
    fun uploadDownloadAndDeleteLocalConcurrently() {
        val remoteFolder = "/concurrentAll/"
        val fileData = prepareFilesOnServer(remoteFolder, prefix = "all")
        val ocFiles = resolveOCFiles(remoteFolder, fileData)

        val errors = CopyOnWriteArrayList<String>()
        val latch = CountDownLatch(3)

        startUploadThread(fileData, errors, latch)
        startDownloadThread(ocFiles, errors, latch)
        startDeleteLocalThread(ocFiles, errors, latch, delayMs = 1000L)

        assertTrue("Concurrent upload+download+delete timed out", latch.await(TIMEOUT_SECONDS, TimeUnit.SECONDS))
        assertFalse(
            "Unexpected errors during concurrent upload+download+delete: $errors",
            errors.isNotEmpty()
        )
    }

    private fun prepareFilesOnServer(remoteFolder: String, prefix: String): List<Pair<File, String>> =
        (0 until FILE_COUNT).map { index ->
            val localFile = createFile("${prefix}_$index.txt", FILE_SIZE_ITERATIONS)
            val remotePath = "$remoteFolder${prefix}_$index.txt"
            uploadOCUpload(OCUpload(localFile.absolutePath, remotePath, account.name))
            localFile to remotePath
        }.also { refreshFolder(remoteFolder) }

    private fun resolveOCFiles(remoteFolder: String, fileData: List<Pair<File, String>>): List<OCFile> {
        val ocFiles = fileData.mapNotNull { (_, remotePath) ->
            storageManager.getFileByDecryptedRemotePath(remotePath)
        }
        assertTrue(
            "Expected ${fileData.size} files in storage under $remoteFolder, got ${ocFiles.size}",
            ocFiles.size == fileData.size
        )
        return ocFiles
    }

    private fun startUploadThread(
        fileData: List<Pair<File, String>>,
        errors: CopyOnWriteArrayList<String>,
        latch: CountDownLatch
    ) {
        Thread {
            try {
                fileData.forEach { (localFile, remotePath) ->
                    val upload = OCUpload(localFile.absolutePath, remotePath, account.name)
                    val result = UploadFileOperation(
                        uploadsStorageManager,
                        connectivityServiceMock,
                        powerManagementServiceMock,
                        user,
                        null,
                        upload,
                        NameCollisionPolicy.OVERWRITE,
                        FileUploadWorker.LOCAL_BEHAVIOUR_COPY,
                        targetContext,
                        false,
                        false,
                        storageManager
                    ).execute(client)

                    if (!result.isSuccess) {
                        errors.add("Upload INSTANCE_NOT_CONFIGURED for $remotePath")
                    }
                }
            } finally {
                latch.countDown()
            }
        }.start()
    }

    private fun startDownloadThread(
        ocFiles: List<OCFile>,
        errors: CopyOnWriteArrayList<String>,
        latch: CountDownLatch
    ) {
        Thread {
            try {
                ocFiles.forEach { ocFile ->
                    val result = DownloadFileOperation(user, ocFile, targetContext).execute(client)
                    if (!result.isSuccess) {
                        errors.add("Download INSTANCE_NOT_CONFIGURED for ${ocFile.remotePath}")
                    }
                }
            } finally {
                latch.countDown()
            }
        }.start()
    }

    private fun startDeleteLocalThread(
        ocFiles: List<OCFile>,
        errors: CopyOnWriteArrayList<String>,
        latch: CountDownLatch,
        delayMs: Long
    ) {
        Thread {
            try {
                Thread.sleep(delayMs)
                ocFiles.forEach { ocFile ->
                    val result = RemoveFileOperation(
                        ocFile,
                        true,
                        user,
                        true,
                        targetContext,
                        storageManager
                    ).execute(client)
                    if (!result.isSuccess) {
                        errors.add("DeleteLocalOnly INSTANCE_NOT_CONFIGURED for ${ocFile.remotePath}")
                    }
                }
            } finally {
                latch.countDown()
            }
        }.start()
    }
}
