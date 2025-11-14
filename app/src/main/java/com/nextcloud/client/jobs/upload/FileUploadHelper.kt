/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2023 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-FileCopyrightText: 2023 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.nextcloud.client.jobs.upload

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.nextcloud.client.account.User
import com.nextcloud.client.account.UserAccountManager
import com.nextcloud.client.database.entity.toOCUpload
import com.nextcloud.client.database.entity.toUploadEntity
import com.nextcloud.client.device.BatteryStatus
import com.nextcloud.client.device.PowerManagementService
import com.nextcloud.client.jobs.BackgroundJobManager
import com.nextcloud.client.jobs.upload.FileUploadWorker.Companion.currentUploadFileOperation
import com.nextcloud.client.network.Connectivity
import com.nextcloud.client.network.ConnectivityService
import com.nextcloud.utils.extensions.getUploadIds
import com.owncloud.android.MainApp
import com.owncloud.android.R
import com.owncloud.android.datamodel.FileDataStorageManager
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.datamodel.UploadsStorageManager
import com.owncloud.android.datamodel.UploadsStorageManager.UploadStatus
import com.owncloud.android.db.OCUpload
import com.owncloud.android.db.UploadResult
import com.owncloud.android.files.services.NameCollisionPolicy
import com.owncloud.android.lib.common.OwnCloudClient
import com.owncloud.android.lib.common.network.OnDatatransferProgressListener
import com.owncloud.android.lib.common.operations.RemoteOperationResult
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.lib.resources.files.ReadFileRemoteOperation
import com.owncloud.android.lib.resources.files.model.RemoteFile
import com.owncloud.android.operations.RemoveFileOperation
import com.owncloud.android.operations.UploadFileOperation
import com.owncloud.android.utils.DisplayUtils
import com.owncloud.android.utils.FileUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.util.concurrent.Semaphore
import javax.inject.Inject

@Suppress("TooManyFunctions")
class FileUploadHelper {

    @Inject
    lateinit var backgroundJobManager: BackgroundJobManager

    @Inject
    lateinit var accountManager: UserAccountManager

    @Inject
    lateinit var uploadsStorageManager: UploadsStorageManager

    @Inject
    lateinit var fileStorageManager: FileDataStorageManager

    private val ioScope = CoroutineScope(Dispatchers.IO)

    init {
        MainApp.getAppComponent().inject(this)
    }

    companion object {
        private val TAG = FileUploadWorker::class.java.simpleName

        @Suppress("MagicNumber")
        const val MAX_FILE_COUNT = 500

        val mBoundListeners = HashMap<String, OnDatatransferProgressListener>()

        private var instance: FileUploadHelper? = null

        private val retryFailedUploadsSemaphore = Semaphore(1)

        fun instance(): FileUploadHelper = instance ?: synchronized(this) {
            instance ?: FileUploadHelper().also { instance = it }
        }

        fun buildRemoteName(accountName: String, remotePath: String): String = accountName + remotePath
    }

    /**
     * Retries all failed uploads across all user accounts.
     *
     * This function retrieves all uploads with the status [UploadStatus.UPLOAD_FAILED], including both
     * manual uploads and auto uploads. It runs in a background thread (Dispatcher.IO) and ensures
     * that only one retry operation runs at a time by using a semaphore to prevent concurrent execution.
     *
     * Once the failed uploads are retrieved, it calls [retryUploads], which triggers the corresponding
     * upload workers for each failed upload.
     *
     * The function returns `true` if there were any failed uploads to retry and the retry process was
     * started, or `false` if no uploads were retried.
     *
     * @param uploadsStorageManager Provides access to upload data and persistence.
     * @param connectivityService Checks the current network connectivity state.
     * @param accountManager Handles user account authentication and selection.
     * @param powerManagementService Ensures uploads respect power constraints.
     * @return `true` if any failed uploads were found and retried; `false` otherwise.
     */
    fun retryFailedUploads(
        uploadsStorageManager: UploadsStorageManager,
        connectivityService: ConnectivityService,
        accountManager: UserAccountManager,
        powerManagementService: PowerManagementService
    ): Boolean {
        if (!retryFailedUploadsSemaphore.tryAcquire()) {
            Log_OC.d(TAG, "skipping retryFailedUploads, already running")
            return true
        }

        var isUploadStarted = false

        try {
            getUploadsByStatus(null, UploadStatus.UPLOAD_FAILED) {
                if (it.isNotEmpty()) {
                    isUploadStarted = true
                }

                retryUploads(
                    uploadsStorageManager,
                    connectivityService,
                    accountManager,
                    powerManagementService,
                    uploads = it
                )
            }
        } finally {
            retryFailedUploadsSemaphore.release()
        }

        return isUploadStarted
    }

    fun retryCancelledUploads(
        uploadsStorageManager: UploadsStorageManager,
        connectivityService: ConnectivityService,
        accountManager: UserAccountManager,
        powerManagementService: PowerManagementService
    ): Boolean {
        var result = false
        getUploadsByStatus(accountManager.user.accountName, UploadStatus.UPLOAD_CANCELLED) {
            result = retryUploads(
                uploadsStorageManager,
                connectivityService,
                accountManager,
                powerManagementService,
                it
            )
        }

        return result
    }

    @Suppress("ComplexCondition")
    private fun retryUploads(
        uploadsStorageManager: UploadsStorageManager,
        connectivityService: ConnectivityService,
        accountManager: UserAccountManager,
        powerManagementService: PowerManagementService,
        uploads: Array<OCUpload>
    ): Boolean {
        var showNotExistMessage = false
        val isOnline = checkConnectivity(connectivityService)
        val connectivity = connectivityService.connectivity
        val batteryStatus = powerManagementService.battery

        val uploadsToRetry = mutableListOf<Long>()

        for (upload in uploads) {
            val uploadResult = checkUploadConditions(
                upload,
                connectivity,
                batteryStatus,
                powerManagementService,
                isOnline
            )

            if (uploadResult != UploadResult.UPLOADED) {
                if (upload.lastResult != uploadResult) {
                    // Setting Upload status else cancelled uploads will behave wrong, when retrying
                    // Needs to happen first since lastResult wil be overwritten by setter
                    upload.uploadStatus = UploadStatus.UPLOAD_FAILED

                    upload.lastResult = uploadResult
                    uploadsStorageManager.updateUpload(upload)
                }
                if (uploadResult == UploadResult.FILE_NOT_FOUND) {
                    showNotExistMessage = true
                }
                continue
            }

            // Only uploads that passed checks get marked in progress and are collected for scheduling
            upload.uploadStatus = UploadStatus.UPLOAD_IN_PROGRESS
            uploadsStorageManager.updateUpload(upload)
            uploadsToRetry.add(upload.uploadId)
        }

        if (uploadsToRetry.isNotEmpty()) {
            backgroundJobManager.startFilesUploadJob(
                accountManager.user,
                uploadsToRetry.toLongArray(),
                false
            )
        }

        return showNotExistMessage
    }

    @JvmOverloads
    @Suppress("LongParameterList")
    fun uploadNewFiles(
        user: User,
        localPaths: Array<String>,
        remotePaths: Array<String>,
        localBehavior: Int,
        createRemoteFolder: Boolean,
        createdBy: Int,
        requiresWifi: Boolean,
        requiresCharging: Boolean,
        nameCollisionPolicy: NameCollisionPolicy,
        showSameFileAlreadyExistsNotification: Boolean = true
    ) {
        val uploads = localPaths.mapIndexed { index, localPath ->
            val result = OCUpload(localPath, remotePaths[index], user.accountName).apply {
                this.nameCollisionPolicy = nameCollisionPolicy
                isUseWifiOnly = requiresWifi
                isWhileChargingOnly = requiresCharging
                uploadStatus = UploadStatus.UPLOAD_IN_PROGRESS
                this.createdBy = createdBy
                isCreateRemoteFolder = createRemoteFolder
                localAction = localBehavior
            }

            val id = uploadsStorageManager.uploadDao.insertOrReplace(result.toUploadEntity())
            result.uploadId = id
            result
        }
        backgroundJobManager.startFilesUploadJob(user, uploads.getUploadIds(), showSameFileAlreadyExistsNotification)
    }

    fun removeFileUpload(remotePath: String, accountName: String) {
        uploadsStorageManager.uploadDao.deleteByAccountAndRemotePath(accountName, remotePath)
    }

    fun updateUploadStatus(remotePath: String, accountName: String, status: UploadStatus) {
        ioScope.launch {
            uploadsStorageManager.uploadDao.updateStatus(remotePath, accountName, status.value)
        }
    }

    /**
     * Retrieves uploads filtered by their status, optionally for a specific account.
     *
     * This function queries the uploads database asynchronously to obtain a list of uploads
     * that match the specified [status]. If an [accountName] is provided, only uploads
     * belonging to that account are retrieved. If [accountName] is `null`, uploads with the
     * given [status] from **all user accounts** are returned.
     *
     * Once the uploads are fetched, the [onCompleted] callback is invoked with the resulting array.
     *
     * @param accountName The name of the account to filter uploads by.
     * If `null`, uploads matching the given [status] from all accounts are returned.
     * @param status The [UploadStatus] to filter uploads by (e.g., `UPLOAD_FAILED`).
     * @param nameCollisionPolicy The [NameCollisionPolicy] to filter uploads by (e.g., `SKIP`).
     * @param onCompleted A callback invoked with the resulting array of [OCUpload] objects.
     */
    fun getUploadsByStatus(
        accountName: String?,
        status: UploadStatus,
        nameCollisionPolicy: NameCollisionPolicy? = null,
        onCompleted: (Array<OCUpload>) -> Unit
    ) {
        ioScope.launch {
            val dao = uploadsStorageManager.uploadDao
            val result = if (accountName != null) {
                dao.getUploadsByAccountNameAndStatus(accountName, status.value, nameCollisionPolicy?.serialize())
            } else {
                dao.getUploadsByStatus(status.value, nameCollisionPolicy?.serialize())
            }.map { it.toOCUpload(null) }.toTypedArray()
            onCompleted(result)
        }
    }

    fun cancelAndRestartUploadJob(user: User, uploadIds: LongArray) {
        backgroundJobManager.run {
            cancelFilesUploadJob(user)
            startFilesUploadJob(user, uploadIds, false)
        }
    }

    @Suppress("ReturnCount")
    fun isUploading(remotePath: String?, accountName: String?): Boolean {
        accountName ?: return false
        if (!backgroundJobManager.isStartFileUploadJobScheduled(accountName)) {
            return false
        }

        remotePath ?: return false
        val upload = uploadsStorageManager.uploadDao.getByRemotePath(remotePath)
        return upload?.status == UploadStatus.UPLOAD_IN_PROGRESS.value ||
            FileUploadWorker.isUploading(remotePath, accountName)
    }

    private fun checkConnectivity(connectivityService: ConnectivityService): Boolean {
        // check that connection isn't walled off and that the server is reachable
        return connectivityService.getConnectivity().isConnected && !connectivityService.isInternetWalled()
    }

    /**
     * Dupe of [UploadFileOperation.checkConditions], needed to check if the upload should even be scheduled
     * @return [UploadResult.UPLOADED] if the upload should be scheduled, otherwise the reason why it shouldn't
     */
    private fun checkUploadConditions(
        upload: OCUpload,
        connectivity: Connectivity,
        battery: BatteryStatus,
        powerManagementService: PowerManagementService,
        hasGeneralConnection: Boolean
    ): UploadResult {
        var conditions = UploadResult.UPLOADED

        // check that internet is available
        if (!hasGeneralConnection) {
            conditions = UploadResult.NETWORK_CONNECTION
        }

        // check that local file exists; skip the upload otherwise
        if (!File(upload.localPath).exists()) {
            conditions = UploadResult.FILE_NOT_FOUND
        }

        // check that connectivity conditions are met; delay upload otherwise
        if (upload.isUseWifiOnly && (!connectivity.isWifi || connectivity.isMetered)) {
            conditions = UploadResult.DELAYED_FOR_WIFI
        }

        // check if charging conditions are met; delay upload otherwise
        if (upload.isWhileChargingOnly && !battery.isCharging && !battery.isFull) {
            conditions = UploadResult.DELAYED_FOR_CHARGING
        }

        // check that device is not in power save mode; delay upload otherwise
        if (powerManagementService.isPowerSavingEnabled) {
            conditions = UploadResult.DELAYED_IN_POWER_SAVE_MODE
        }

        return conditions
    }

    @Suppress("ReturnCount")
    fun isUploadingNow(upload: OCUpload?): Boolean {
        val currentUploadFileOperation = currentUploadFileOperation
        if (currentUploadFileOperation == null || currentUploadFileOperation.user == null) return false
        if (upload == null || upload.accountName != currentUploadFileOperation.user.accountName) return false

        return if (currentUploadFileOperation.oldFile != null) {
            // For file conflicts check old file remote path
            upload.remotePath == currentUploadFileOperation.remotePath ||
                upload.remotePath == currentUploadFileOperation.oldFile!!
                    .remotePath
        } else {
            upload.remotePath == currentUploadFileOperation.remotePath
        }
    }

    fun uploadUpdatedFile(
        user: User,
        existingFiles: Array<OCFile?>?,
        behaviour: Int,
        nameCollisionPolicy: NameCollisionPolicy
    ) {
        if (existingFiles == null) {
            return
        }

        Log_OC.d(this, "upload updated file")

        val uploads = existingFiles.map { file ->
            file?.let {
                val result = OCUpload(file, user).apply {
                    fileSize = file.fileLength
                    this.nameCollisionPolicy = nameCollisionPolicy
                    isCreateRemoteFolder = true
                    this.localAction = behaviour
                    isUseWifiOnly = false
                    isWhileChargingOnly = false
                    uploadStatus = UploadStatus.UPLOAD_IN_PROGRESS
                }

                val id = uploadsStorageManager.uploadDao.insertOrReplace(result.toUploadEntity())
                result.uploadId = id
                result
            }
        }
        val uploadIds: LongArray = uploads.filterNotNull().map { it.uploadId }.toLongArray()
        backgroundJobManager.startFilesUploadJob(user, uploadIds, true)
    }

    /**
     * Removes any existing file in the same directory that has the same name as the provided new file.
     *
     * This function checks the parent directory of the given `newFile` for any file with the same name.
     * If such a file is found, it is removed using the `RemoveFileOperation`.
     *
     * @param duplicatedFile File to be deleted
     * @param client Needed for executing RemoveFileOperation
     * @param user Needed for creating client
     */
    fun removeDuplicatedFile(duplicatedFile: OCFile, client: OwnCloudClient, user: User, onCompleted: () -> Unit) {
        val job = CoroutineScope(Dispatchers.IO)

        job.launch {
            val removeFileOperation = RemoveFileOperation(
                duplicatedFile,
                false,
                user,
                true,
                MainApp.getAppContext(),
                fileStorageManager
            )

            val result = removeFileOperation.execute(client)

            if (result.isSuccess) {
                Log_OC.d(TAG, "Replaced file successfully removed")

                launch(Dispatchers.Main) {
                    onCompleted()
                }
            }
        }
    }

    fun retryUpload(upload: OCUpload, user: User) {
        Log_OC.d(this, "retry upload")

        upload.uploadStatus = UploadStatus.UPLOAD_IN_PROGRESS
        uploadsStorageManager.updateUpload(upload)

        backgroundJobManager.startFilesUploadJob(user, longArrayOf(upload.uploadId), false)
    }

    fun cancel(accountName: String) {
        uploadsStorageManager.removeUploads(accountName)
        val uploadIds = uploadsStorageManager.getCurrentUploadIds(accountName)
        cancelAndRestartUploadJob(accountManager.getUser(accountName).get(), uploadIds)
    }

    fun addUploadTransferProgressListener(listener: OnDatatransferProgressListener, targetKey: String) {
        mBoundListeners[targetKey] = listener
    }

    fun removeUploadTransferProgressListener(listener: OnDatatransferProgressListener, targetKey: String) {
        if (mBoundListeners[targetKey] === listener) {
            mBoundListeners.remove(targetKey)
        }
    }

    @Suppress("MagicNumber")
    fun isSameFileOnRemote(user: User, localFile: File, remotePath: String, context: Context): Boolean {
        // Compare remote file to local file
        val localLastModifiedTimestamp = localFile.lastModified() / 1000 // remote file timestamp in milli not micro sec
        val localCreationTimestamp = FileUtil.getCreationTimestamp(localFile)
        val localSize: Long = localFile.length()

        val operation = ReadFileRemoteOperation(remotePath)
        val result: RemoteOperationResult<*> = operation.execute(user, context)
        if (result.isSuccess) {
            val remoteFile = result.data[0] as RemoteFile
            return remoteFile.size == localSize &&
                localCreationTimestamp != null &&
                localCreationTimestamp == remoteFile.creationTimestamp &&
                remoteFile.modifiedTimestamp == localLastModifiedTimestamp * 1000
        }
        return false
    }

    fun showFileUploadLimitMessage(activity: Activity) {
        val message = activity.resources.getQuantityString(
            R.plurals.file_upload_limit_message,
            MAX_FILE_COUNT
        )
        DisplayUtils.showSnackMessage(activity, message)
    }

    class UploadNotificationActionReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val accountName = intent.getStringExtra(FileUploadWorker.EXTRA_ACCOUNT_NAME)
            val remotePath = intent.getStringExtra(FileUploadWorker.EXTRA_REMOTE_PATH)
            val action = intent.action

            if (FileUploadWorker.ACTION_CANCEL_BROADCAST == action) {
                Log_OC.d(
                    FileUploadWorker.TAG,
                    "Cancel broadcast received for file " + remotePath + " at " + System.currentTimeMillis()
                )
                if (accountName == null || remotePath == null) {
                    return
                }

                FileUploadWorker.cancelCurrentUpload(remotePath, accountName, onCompleted = {
                    instance().updateUploadStatus(remotePath, accountName, UploadStatus.UPLOAD_CANCELLED)
                })
            }
        }
    }
}
