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

package com.nextcloud.client.files.downloader

import android.accounts.Account
import android.accounts.AccountManager
import android.accounts.OnAccountsUpdateListener
import android.app.PendingIntent
import android.content.Context
import android.os.Binder
import android.os.IBinder
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.google.gson.Gson
import com.nextcloud.client.account.User
import com.nextcloud.client.account.UserAccountManager
import com.nextcloud.client.jobs.BackgroundJobManagerImpl.Companion.JOB_FILES_DOWNLOAD
import com.nextcloud.client.notifications.download.DownloadNotificationManager
import com.nextcloud.java.util.Optional
import com.nextcloud.utils.extensions.isWorkScheduled
import com.owncloud.android.datamodel.FileDataStorageManager
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.datamodel.UploadsStorageManager
import com.owncloud.android.files.services.IndexedForest
import com.owncloud.android.lib.common.OwnCloudAccount
import com.owncloud.android.lib.common.OwnCloudClient
import com.owncloud.android.lib.common.OwnCloudClientManagerFactory
import com.owncloud.android.lib.common.network.OnDatatransferProgressListener
import com.owncloud.android.lib.common.operations.RemoteOperationResult
import com.owncloud.android.lib.common.operations.RemoteOperationResult.ResultCode
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.operations.DownloadFileOperation
import com.owncloud.android.operations.DownloadType
import com.owncloud.android.utils.theme.ViewThemeUtils
import java.util.AbstractList
import java.util.Vector

@Suppress("LongParameterList")
class FileDownloadWorker(
    private val viewThemeUtils: ViewThemeUtils,
    private val accountManager: UserAccountManager,
    private val uploadsStorageManager: UploadsStorageManager,
    private var localBroadcastManager: LocalBroadcastManager,
    private val context: Context,
    params: WorkerParameters
) : Worker(context, params), OnAccountsUpdateListener, OnDatatransferProgressListener {

    companion object {
        private val TAG = FileDownloadWorker::class.java.simpleName

        const val USER_NAME = "USER"
        const val FILE = "FILE"
        const val BEHAVIOUR = "BEHAVIOUR"
        const val DOWNLOAD_TYPE = "DOWNLOAD_TYPE"
        const val ACTIVITY_NAME = "ACTIVITY_NAME"
        const val PACKAGE_NAME = "PACKAGE_NAME"
        const val CONFLICT_UPLOAD_ID = "CONFLICT_UPLOAD_ID"
        const val EXTRA_USER = "USER"
        const val EXTRA_FILE = "FILE"
        const val EXTRA_DOWNLOAD_RESULT = "RESULT"
        const val EXTRA_REMOTE_PATH = "REMOTE_PATH"
        const val EXTRA_LINKED_TO_PATH = "LINKED_TO"
        const val ACCOUNT_NAME = "ACCOUNT_NAME"

        fun getDownloadAddedMessage(): String {
            return FileDownloadWorker::class.java.name + "DOWNLOAD_ADDED"
        }

        fun getDownloadFinishMessage(): String {
            return FileDownloadWorker::class.java.name + "DOWNLOAD_FINISH"
        }
    }

    private var currentDownload: DownloadFileOperation? = null
    private var conflictUploadId: Long? = null
    private var lastPercent = 0
    private val intents = FileDownloadIntents(context)
    private val notificationManager = DownloadNotificationManager(context, viewThemeUtils)
    private val pendingDownloads = IndexedForest<DownloadFileOperation>()
    private var downloadBinder: IBinder = FileDownloaderBinder()
    private var currentUser = Optional.empty<User>()
    private val helper = FileDownloadHelper()
    private var startedDownload = false
    private var storageManager: FileDataStorageManager? = null
    private var downloadClient: OwnCloudClient? = null
    private val gson = Gson()

    @Suppress("TooGenericExceptionCaught")
    override fun doWork(): Result {
        return try {
            val requestDownloads = getRequestDownloads()

            notificationManager.init()
            addAccountUpdateListener()
            startDownloadForEachRequest(requestDownloads)

            Log_OC.e(TAG, "FilesDownloadWorker successfully completed")
            Result.success()
        } catch (t: Throwable) {
            Log_OC.e(TAG, "Error caught at FilesDownloadWorker(): " + t.localizedMessage)
            Result.failure()
        }
    }

    private fun getRequestDownloads(): AbstractList<String> {
        conflictUploadId = inputData.keyValueMap[CONFLICT_UPLOAD_ID] as Long?
        val file = gson.fromJson(inputData.keyValueMap[FILE] as String, OCFile::class.java)
        val accountName = inputData.keyValueMap[USER_NAME] as String
        val user = accountManager.getUser(accountName).get()
        val downloadTypeAsString = inputData.keyValueMap[DOWNLOAD_TYPE] as String?
        val downloadType = if (downloadTypeAsString != null) {
            if (downloadTypeAsString == DownloadType.DOWNLOAD.toString()) {
                DownloadType.DOWNLOAD
            } else {
                DownloadType.EXPORT
            }
        } else {
            null
        }
        val behaviour = inputData.keyValueMap[BEHAVIOUR] as String
        val activityName = inputData.keyValueMap[ACTIVITY_NAME] as String
        val packageName = inputData.keyValueMap[PACKAGE_NAME] as String

        val requestedDownloads: AbstractList<String> = Vector()

        try {
            val operation = DownloadFileOperation(
                user,
                file,
                behaviour,
                activityName,
                packageName,
                context,
                downloadType
            )

            operation.addDownloadDataTransferProgressListener(this)
            operation.addDownloadDataTransferProgressListener(downloadBinder as FileDownloaderBinder)
            val putResult = pendingDownloads.putIfAbsent(
                user?.accountName,
                file.remotePath,
                operation
            )

            if (putResult != null) {
                val downloadKey = putResult.first
                requestedDownloads.add(downloadKey)
                localBroadcastManager.sendBroadcast(intents.newDownloadIntent(operation, putResult.second))
            }
        } catch (e: IllegalArgumentException) {
            Log_OC.e(TAG, "Not enough information provided in intent: " + e.message)
        }

        return requestedDownloads
    }

    private fun addAccountUpdateListener() {
        val am = AccountManager.get(context)
        am.addOnAccountsUpdatedListener(this, null, false)
    }

    private fun startDownloadForEachRequest(requestDownloads: AbstractList<String>) {
        val it: Iterator<String> = requestDownloads.iterator()

        while (it.hasNext()) {
            val next = it.next()
            Log_OC.e(TAG, "Download Key: $next")
            downloadFile(next)
        }

        startedDownload = false
    }

    @Suppress("TooGenericExceptionCaught")
    private fun downloadFile(downloadKey: String) {
        startedDownload = true
        currentDownload = pendingDownloads.get(downloadKey)

        if (currentDownload == null) {
            return
        }

        val isAccountExist = accountManager.exists(currentDownload?.user?.toPlatformAccount())
        if (!isAccountExist) {
            removePendingDownload(currentDownload?.user?.accountName)
            return
        }

        notifyDownloadStart(currentDownload!!)
        var downloadResult: RemoteOperationResult<*>? = null
        try {
            val ocAccount = getOCAccountForDownload()
            downloadClient =
                OwnCloudClientManagerFactory.getDefaultSingleton().getClientFor(ocAccount, context)

            downloadResult = currentDownload?.execute(downloadClient)
            if (downloadResult?.isSuccess == true && currentDownload?.downloadType === DownloadType.DOWNLOAD) {
                getCurrentFile()?.let {
                    helper.saveFile(it, currentDownload, storageManager)
                }
            }
        } catch (e: Exception) {
            Log_OC.e(TAG, "Error downloading", e)
            downloadResult = RemoteOperationResult<Any?>(e)
        } finally {
            cleanupDownloadProcess(downloadResult)
        }
    }

    private fun removePendingDownload(accountName: String?) {
        pendingDownloads.remove(accountName)
    }

    private fun notifyDownloadStart(download: DownloadFileOperation) {
        lastPercent = 0

        notificationManager.run {
            notifyForStart(download)
            setContentIntent(intents.detailsIntent(download), PendingIntent.FLAG_IMMUTABLE)
            showDownloadInProgressNotification()
        }
    }

    private fun getOCAccountForDownload(): OwnCloudAccount {
        val currentDownloadAccount = currentDownload?.user?.toPlatformAccount()
        val currentDownloadUser = accountManager.getUser(currentDownloadAccount?.name)
        if (currentUser != currentDownloadUser) {
            currentUser = currentDownloadUser
            storageManager = FileDataStorageManager(currentUser.get(), context.contentResolver)
        }
        return currentDownloadUser.get().toOwnCloudAccount()
    }

    private fun getCurrentFile(): OCFile? {
        var file: OCFile? = currentDownload?.file?.fileId?.let { storageManager?.getFileById(it) }

        if (file == null) {
            file = storageManager?.getFileByDecryptedRemotePath(currentDownload?.file?.remotePath)
        }

        if (file == null) {
            Log_OC.e(this, "Could not save " + currentDownload?.file?.remotePath)
            return null
        }

        return file
    }

    private fun cleanupDownloadProcess(result: RemoteOperationResult<*>?) {
        val removeResult = pendingDownloads.removePayload(
            currentDownload?.user?.accountName,
            currentDownload?.remotePath
        )

        val downloadResult = result ?: RemoteOperationResult<Any?>(RuntimeException("Error downloading…"))

        currentDownload?.run {
            notifyDownloadResult(this, downloadResult)
            val downloadFinishedIntent = intents.downloadFinishedIntent(
                this,
                downloadResult,
                removeResult.second
            )
            localBroadcastManager.sendBroadcast(downloadFinishedIntent)
        }
    }

    private fun notifyDownloadResult(
        download: DownloadFileOperation,
        downloadResult: RemoteOperationResult<*>
    ) {
        if (downloadResult.isCancelled) {
            return
        }

        // TODO Check why we calling only for success?
        if (downloadResult.isSuccess) {
            dismissDownloadInProgressNotification()
        }

        val needsToUpdateCredentials = (ResultCode.UNAUTHORIZED == downloadResult.code)
        notificationManager.run {
            prepareForResult(downloadResult, needsToUpdateCredentials)

            if (needsToUpdateCredentials) {
                setCredentialContentIntent(download.user)
            } else {
                setContentIntent(intents.detailsIntent(null), PendingIntent.FLAG_IMMUTABLE)
            }

            notifyForResult(downloadResult, download)
        }
    }

    private fun dismissDownloadInProgressNotification() {
        // TODO Check necessity of this function call
        conflictUploadId?.let {
            if (it > 0) {
                uploadsStorageManager.removeUpload(it)
            }
        }

        notificationManager.dismissDownloadInProgressNotification()
    }

    override fun onAccountsUpdated(accounts: Array<out Account>?) {
        if (!accountManager.exists(currentDownload?.user?.toPlatformAccount())) {
            currentDownload?.cancel()
        }
    }

    @Suppress("MagicNumber")
    override fun onTransferProgress(
        progressRate: Long,
        totalTransferredSoFar: Long,
        totalToTransfer: Long,
        filePath: String
    ) {
        val percent: Int = (100.0 * totalTransferredSoFar.toDouble() / totalToTransfer.toDouble()).toInt()

        if (percent != lastPercent) {
            notificationManager.run {
                updateDownloadProgressNotification(filePath, percent, totalToTransfer)
                showDownloadInProgressNotification()
            }
        }

        lastPercent = percent
    }

    inner class FileDownloaderBinder : Binder(), OnDatatransferProgressListener {
        private val boundListeners: MutableMap<Long, OnDatatransferProgressListener> = HashMap()

        fun cancelPendingOrCurrentDownloads() {
            helper.backgroundJobManager.cancelFilesDownloadJob(currentUser.get())
        }

        fun cancelAllDownloadsForAccount(accountName: String?) {
            currentDownload?.user?.let {
                if (it.nameEquals(accountName)) {
                    helper.backgroundJobManager.cancelFilesDownloadJob(it)
                    currentDownload?.cancel()
                }
            }

            removePendingDownload(accountName)
        }

        fun isDownloading(): Boolean {
            return context.isWorkScheduled(JOB_FILES_DOWNLOAD)
        }

        fun addDataTransferProgressListener(listener: OnDatatransferProgressListener?, file: OCFile?) {
            if (file == null || listener == null) {
                return
            }

            boundListeners[file.fileId] = listener
        }

        fun removeDataTransferProgressListener(listener: OnDatatransferProgressListener?, file: OCFile?) {
            if (file == null || listener == null) {
                return
            }

            val fileId = file.fileId
            if (boundListeners[fileId] === listener) {
                boundListeners.remove(fileId)
            }
        }

        override fun onTransferProgress(
            progressRate: Long,
            totalTransferredSoFar: Long,
            totalToTransfer: Long,
            fileName: String
        ) {
            val listener = boundListeners[currentDownload?.file?.fileId]
            listener?.onTransferProgress(
                progressRate,
                totalTransferredSoFar,
                totalToTransfer,
                fileName
            )
        }
    }
}
