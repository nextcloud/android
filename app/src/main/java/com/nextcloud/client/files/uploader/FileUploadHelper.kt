/*
 * Nextcloud Android client application
 *
 * @author Alper Ozturk
 * Copyright (C) 2023 Alper Ozturk
 * Copyright (C) 2023 Nextcloud GmbH
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

package com.nextcloud.client.files.uploader

import com.nextcloud.client.account.User
import com.nextcloud.client.account.UserAccountManager
import com.nextcloud.client.files.uploader.FileUploadWorker.Companion.currentUploadFileOperation
import com.nextcloud.client.jobs.BackgroundJobManager
import com.nextcloud.client.jobs.BackgroundJobManagerImpl
import com.nextcloud.utils.extensions.isWorkScheduled
import com.owncloud.android.MainApp
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.datamodel.UploadsStorageManager
import com.owncloud.android.datamodel.UploadsStorageManager.UploadStatus
import com.owncloud.android.db.OCUpload
import com.owncloud.android.files.services.NameCollisionPolicy
import com.owncloud.android.lib.common.network.OnDatatransferProgressListener
import com.owncloud.android.lib.common.utils.Log_OC
import javax.inject.Inject

@Suppress("TooManyFunctions")
class FileUploadHelper {

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
        private val TAG = FileUploadWorker::class.java.simpleName

        val mBoundListeners = HashMap<String, OnDatatransferProgressListener>()

        private var instance: FileUploadHelper? = null

        fun instance(): FileUploadHelper {
            return instance ?: synchronized(this) {
                instance ?: FileUploadHelper().also { instance = it }
            }
        }

        fun buildRemoteName(accountName: String, remotePath: String): String {
            return accountName + remotePath
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

    fun cancelFileUpload(remotePath: String, accountName: String) {
        try {
            val user = accountManager.getUser(accountName).get()

            // need to update now table in mUploadsStorageManager,
            // since the operation will not get to be run by FileUploader#uploadFile
            uploadsStorageManager.removeUpload(accountName, remotePath)

            cancelAndRestartUploadJob(user)
        } catch (e: NoSuchElementException) {
            Log_OC.e(TAG, "Error cancelling current upload because user does not exist!")
        }
    }

    private fun cancelAndRestartUploadJob(user: User) {
        backgroundJobManager.run {
            cancelFilesUploadJob(user)
            startFilesUploadJob(user)
        }
    }

    @Suppress("ReturnCount")
    fun isUploading(user: User?, file: OCFile?): Boolean {
        if (user == null || file == null || !backgroundJobManager.isStartFileUploadJobScheduled(user)) {
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

    fun cancel(accountName: String) {
        uploadsStorageManager.removeUploads(accountName)
        cancelAndRestartUploadJob(accountManager.getUser(accountName).get())
    }

    fun addUploadTransferProgressListener(
        listener: OnDatatransferProgressListener,
        targetKey: String
    ) {
        mBoundListeners[targetKey] = listener
    }

    fun removeUploadTransferProgressListener(
        listener: OnDatatransferProgressListener,
        targetKey: String
    ) {
        if (mBoundListeners[targetKey] === listener) {
            mBoundListeners.remove(targetKey)
        }
    }
}
