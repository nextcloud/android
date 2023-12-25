/*
 *
 * Nextcloud Android client application
 *
 * @author Tobias Kaminsky
 * Copyright (C) 2022 Tobias Kaminsky
 * Copyright (C) 2022 Nextcloud GmbH
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

package com.owncloud.android.utils

import android.accounts.Account
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.google.common.util.concurrent.ListenableFuture
import com.nextcloud.client.account.User
import com.nextcloud.client.account.UserAccountManager
import com.nextcloud.client.jobs.BackgroundJobManager
import com.nextcloud.client.jobs.BackgroundJobManagerImpl
import com.nextcloud.client.jobs.FilesUploadWorker
import com.nextcloud.client.jobs.FilesUploadWorker.Companion.buildRemoteName
import com.nextcloud.client.jobs.FilesUploadWorker.Companion.currentUploadFileOperation
import com.owncloud.android.MainApp
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.datamodel.UploadsStorageManager
import com.owncloud.android.datamodel.UploadsStorageManager.UploadStatus
import com.owncloud.android.db.OCUpload
import com.owncloud.android.files.services.NameCollisionPolicy
import com.owncloud.android.lib.common.network.OnDatatransferProgressListener
import com.owncloud.android.lib.common.operations.RemoteOperationResult.ResultCode
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.lib.resources.files.model.ServerFileInterface
import java.util.concurrent.ExecutionException
import javax.inject.Inject

@Suppress("TooManyFunctions")
class FilesUploadHelper {
    @Inject
    lateinit var backgroundJobManager: BackgroundJobManager

    @Inject
    lateinit var accountManager: UserAccountManager

    @Inject
    lateinit var uploadsStorageManager: UploadsStorageManager

    init {
        MainApp.getAppComponent().inject(this)
    }

    companion object {
        private val TAG = FilesUploadWorker::class.java.simpleName
        val mBoundListeners = HashMap<String, OnDatatransferProgressListener>()

        fun onTransferProgress(
            accountName: String?,
            remotePath: String?,
            progressRate: Long,
            totalTransferredSoFar: Long,
            totalToTransfer: Long,
            fileName: String?
        ) {
            if (accountName == null || remotePath == null) return

            val key: String =
                buildRemoteName(accountName, remotePath)
            val boundListener = mBoundListeners[key]

            boundListener?.onTransferProgress(progressRate, totalTransferredSoFar, totalToTransfer, fileName)
        }

        fun isWorkScheduled(tag: String): Boolean {
            val instance = WorkManager.getInstance(MainApp.getAppContext())
            val statuses: ListenableFuture<List<WorkInfo>> = instance.getWorkInfosByTag(tag)
            var running = false
            var workInfoList: List<WorkInfo> = emptyList()

            try {
                workInfoList = statuses.get()
            } catch (e: ExecutionException) {
                Log_OC.d(TAG, "ExecutionException in isWorkScheduled: $e")
            } catch (e: InterruptedException) {
                Log_OC.d(TAG, "InterruptedException in isWorkScheduled: $e")
            }

            for (workInfo in workInfoList) {
                val state = workInfo.state
                running = running || (state == WorkInfo.State.RUNNING || state == WorkInfo.State.ENQUEUED)
            }

            return running
        }
    }

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
        nameCollisionPolicy: NameCollisionPolicy
    ) {
        val uploads = localPaths.mapIndexed { index, localPath ->
            OCUpload(localPath, remotePaths[index], user.accountName).apply {
                this.nameCollisionPolicy = nameCollisionPolicy
                isUseWifiOnly = requiresWifi
                isWhileChargingOnly = requiresCharging
                uploadStatus = UploadStatus.UPLOAD_IN_PROGRESS
                this.createdBy = createdBy
                isCreateRemoteFolder = createRemoteFolder
                localAction = localBehavior
            }
        }
        uploadsStorageManager.storeUploads(uploads)
        backgroundJobManager.startFilesUploadJob(user)
    }

    fun cancelFileUpload(remotePath: String, user: User) {
        // need to update now table in mUploadsStorageManager,
        // since the operation will not get to be run by FileUploader#uploadFile
        uploadsStorageManager.removeUpload(user.accountName, remotePath)

        restartUploadJob(user)
    }

    fun restartUploadJob(user: User) {
        backgroundJobManager.cancelFilesUploadJob(user)
        backgroundJobManager.startFilesUploadJob(user)
    }

    @Suppress("ReturnCount")
    fun isUploading(user: User?, file: OCFile?): Boolean {
        if (user == null || file == null || !isWorkScheduled(BackgroundJobManagerImpl.JOB_FILES_UPLOAD)) {
            return false
        }

        val upload: OCUpload = uploadsStorageManager.getUploadByRemotePath(file.remotePath) ?: return false
        return upload.uploadStatus == UploadStatus.UPLOAD_IN_PROGRESS
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
                OCUpload(file, user).apply {
                    fileSize = file.fileLength
                    this.nameCollisionPolicy = nameCollisionPolicy
                    isCreateRemoteFolder = true
                    this.localAction = behaviour
                    isUseWifiOnly = false
                    isWhileChargingOnly = false
                    uploadStatus = UploadStatus.UPLOAD_IN_PROGRESS
                }
            }
        }
        uploadsStorageManager.storeUploads(uploads)
        backgroundJobManager.startFilesUploadJob(user)
    }

    fun retryUpload(upload: OCUpload, user: User) {
        Log_OC.d(this, "retry upload")

        upload.uploadStatus = UploadStatus.UPLOAD_IN_PROGRESS
        uploadsStorageManager.updateUpload(upload)

        backgroundJobManager.startFilesUploadJob(user)
    }

    fun cancel(storedUpload: OCUpload) {
        cancel(storedUpload.accountName, storedUpload.remotePath, null)
    }

    fun cancel(account: Account, file: ServerFileInterface) {
        cancel(account.name, file.remotePath, null)
    }

    fun cancel(accountName: String?) {
        // cancelPendingUploads(accountName)
        FilesUploadHelper().restartUploadJob(accountManager.getUser(accountName).get())
    }

    fun cancel(user: User) {
        cancel(user.accountName)
    }

    fun cancel(accountName: String?, remotePath: String?, resultCode: ResultCode?) {
        try {
            cancelFileUpload(remotePath!!, accountManager.getUser(accountName).get())
        } catch (e: NoSuchElementException) {
            Log_OC.e(TAG, "Error cancelling current upload because user does not exist!")
        }
    }

    fun addDatatransferProgressListener(
        listener: OnDatatransferProgressListener?,
        ocUpload: OCUpload?
    ) {
        if (ocUpload == null || listener == null) {
            return
        }
        val targetKey = buildRemoteName(ocUpload.accountName, ocUpload.remotePath)
        addDatatransferProgressListener(listener, targetKey)
    }

    fun addDatatransferProgressListener(
        listener: OnDatatransferProgressListener?,
        user: User?,
        file: ServerFileInterface?
    ) {
        if (user == null || file == null || listener == null) {
            return
        }
        val targetKey = buildRemoteName(user.accountName, file.remotePath)
        addDatatransferProgressListener(listener, targetKey)
    }

    fun addDatatransferProgressListener(
        listener: OnDatatransferProgressListener,
        targetKey: String
    ) {
        mBoundListeners[targetKey] = listener
    }

    fun removeDatatransferProgressListener(
        listener: OnDatatransferProgressListener?,
        user: User?,
        file: ServerFileInterface?
    ) {
        if (user == null || file == null || listener == null) {
            return
        }
        val targetKey = buildRemoteName(user.accountName, file.remotePath)
        removeDatatransferProgressListener(listener, targetKey)
    }

    fun removeDatatransferProgressListener(
        listener: OnDatatransferProgressListener?,
        ocUpload: OCUpload?
    ) {
        if (ocUpload == null || listener == null) {
            return
        }
        val targetKey = buildRemoteName(ocUpload.accountName, ocUpload.remotePath)
        removeDatatransferProgressListener(listener, targetKey)
    }

    fun removeDatatransferProgressListener(
        listener: OnDatatransferProgressListener,
        targetKey: String
    ) {
        if (mBoundListeners[targetKey] === listener) {
            mBoundListeners.remove(targetKey)
        }
    }
}
