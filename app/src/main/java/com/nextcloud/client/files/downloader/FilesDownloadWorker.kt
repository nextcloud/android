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
import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Pair
import androidx.core.app.NotificationCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.google.gson.Gson
import com.nextcloud.client.account.User
import com.nextcloud.client.account.UserAccountManager
import com.nextcloud.java.util.Optional
import com.owncloud.android.R
import com.owncloud.android.authentication.AuthenticatorActivity
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
import com.owncloud.android.lib.resources.files.FileUtils
import com.owncloud.android.operations.DownloadFileOperation
import com.owncloud.android.operations.DownloadType
import com.owncloud.android.ui.activity.FileActivity
import com.owncloud.android.ui.activity.FileDisplayActivity
import com.owncloud.android.ui.dialog.SendShareDialog
import com.owncloud.android.ui.fragment.OCFileListFragment
import com.owncloud.android.ui.notifications.NotificationUtils
import com.owncloud.android.ui.preview.PreviewImageActivity
import com.owncloud.android.ui.preview.PreviewImageFragment
import com.owncloud.android.utils.ErrorMessageAdapter
import com.owncloud.android.utils.MimeTypeUtil
import com.owncloud.android.utils.theme.ViewThemeUtils
import java.io.File
import java.security.SecureRandom
import java.util.AbstractList
import java.util.Vector

class FilesDownloadWorker(
    private val viewThemeUtils: ViewThemeUtils,
    private val accountManager: UserAccountManager,
    private val uploadsStorageManager: UploadsStorageManager,
    private var localBroadcastManager: LocalBroadcastManager,
    private val context: Context,
    params: WorkerParameters,
) : Worker(context, params), OnAccountsUpdateListener, OnDatatransferProgressListener {

    companion object {
        private val TAG = FilesDownloadWorker::class.java.simpleName

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
            return FilesDownloadWorker::class.java.name + "DOWNLOAD_ADDED"
        }

        fun getDownloadFinishMessage(): String {
            return FilesDownloadWorker::class.java.name + "DOWNLOAD_FINISH"
        }
    }

    private var notification: Notification? = null
    private var currentDownload: DownloadFileOperation? = null
    private var conflictUploadId: Long? = null
    private var lastPercent = 0
    private var notificationBuilder: NotificationCompat.Builder? = null
    private var notificationManager: NotificationManager? = null
    private val pendingDownloads = IndexedForest<DownloadFileOperation>()
    private var downloadBinder: IBinder? = null
    private var currentUser = Optional.empty<User>()
    private var startedDownload = false
    private var storageManager: FileDataStorageManager? = null
    private var downloadClient: OwnCloudClient? = null
    private val gson = Gson()

    override fun doWork(): Result {
        return try {
            val requestDownloads = getRequestDownloads()

            showDownloadingFilesNotification()
            addAccountUpdateListener()

            val it: Iterator<String> = requestDownloads.iterator()
            while (it.hasNext()) {
                val next = it.next()
                downloadFile(next)
            }

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
            operation.addDatatransferProgressListener(this)
            operation.addDatatransferProgressListener(downloadBinder as FileDownloaderBinder?)
            val putResult: Pair<String, String> = pendingDownloads.putIfAbsent(
                user?.accountName,
                file.remotePath,
                operation
            )

            val downloadKey = putResult.first
            requestedDownloads.add(downloadKey)
            sendBroadcastNewDownload(operation, putResult.second)
        } catch (e: IllegalArgumentException) {
            Log_OC.e(TAG, "Not enough information provided in intent: " + e.message)
        }

        return requestedDownloads
    }

    private fun downloadFile(downloadKey: String) {
        startedDownload = true
        currentDownload = pendingDownloads.get(downloadKey)

        if (currentDownload == null) {
            return
        }

        val isAccountExist = accountManager.exists(currentDownload?.user?.toPlatformAccount())
        if (!isAccountExist) {
            cancelPendingDownloads(currentDownload?.user?.accountName)
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
                saveDownloadedFile()
            }
        } catch (e: Exception) {
            Log_OC.e(TAG, "Error downloading", e)
            downloadResult = RemoteOperationResult<Any?>(e)
        } finally {
            cleanupDownloadProcess(downloadResult)
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

    private fun cleanupDownloadProcess(result: RemoteOperationResult<*>?) {
        var downloadResult = result
        val removeResult: Pair<DownloadFileOperation, String> = pendingDownloads.removePayload(
            currentDownload?.user?.accountName, currentDownload?.remotePath
        )

        if (downloadResult == null) {
            downloadResult = RemoteOperationResult<Any?>(RuntimeException("Error downloading…"))
        }

        currentDownload?.let {
            notifyDownloadResult(it, downloadResult)
            sendBroadcastDownloadFinished(it, downloadResult, removeResult.second)
        }
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

    private fun saveDownloadedFile() {
        val file = getCurrentFile() ?: return

        val syncDate = System.currentTimeMillis()
        file.lastSyncDateForProperties = syncDate
        file.lastSyncDateForData = syncDate
        file.isUpdateThumbnailNeeded = true
        file.modificationTimestamp = currentDownload?.modificationTimestamp ?: 0L
        file.modificationTimestampAtLastSyncForData = currentDownload?.modificationTimestamp ?: 0L
        file.etag = currentDownload?.etag
        file.mimeType = currentDownload?.mimeType
        file.storagePath = currentDownload?.savePath
        file.fileLength = File(currentDownload?.getSavePath()).length()
        file.remoteId = currentDownload?.file?.remoteId
        storageManager?.saveFile(file)

        if (MimeTypeUtil.isMedia(currentDownload?.mimeType)) {
            FileDataStorageManager.triggerMediaScan(file.storagePath, file)
        }

        storageManager?.saveConflict(file, null)
    }

    private fun sendBroadcastDownloadFinished(
        download: DownloadFileOperation,
        downloadResult: RemoteOperationResult<*>,
        unlinkedFromRemotePath: String?
    ) {
        val end = Intent(getDownloadFinishMessage())
        end.putExtra(EXTRA_DOWNLOAD_RESULT, downloadResult.isSuccess)
        end.putExtra(ACCOUNT_NAME, download.user.accountName)
        end.putExtra(EXTRA_REMOTE_PATH, download.remotePath)
        end.putExtra(OCFileListFragment.DOWNLOAD_BEHAVIOUR, download.behaviour)
        end.putExtra(SendShareDialog.ACTIVITY_NAME, download.activityName)
        end.putExtra(SendShareDialog.PACKAGE_NAME, download.packageName)
        if (unlinkedFromRemotePath != null) {
            end.putExtra(EXTRA_LINKED_TO_PATH, unlinkedFromRemotePath)
        }
        end.setPackage(context.packageName)
        localBroadcastManager.sendBroadcast(end)
    }

    private fun notifyDownloadResult(
        download: DownloadFileOperation,
        downloadResult: RemoteOperationResult<*>
    ) {
        if (notificationManager == null) {
            notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        }

        if (!downloadResult.isCancelled) {
            if (downloadResult.isSuccess) {
                conflictUploadId?.let {
                    if (it > 0) {
                        uploadsStorageManager.removeUpload(it)
                    }
                }

                return
            }
            var tickerId =
                if (downloadResult.isSuccess) R.string.downloader_download_succeeded_ticker else R.string.downloader_download_failed_ticker
            val needsToUpdateCredentials = ResultCode.UNAUTHORIZED == downloadResult.code
            tickerId = if (needsToUpdateCredentials) R.string.downloader_download_failed_credentials_error else tickerId

            notificationBuilder
                ?.setTicker(context.getString(tickerId))
                ?.setContentTitle(context.getString(tickerId))
                ?.setAutoCancel(true)
                ?.setOngoing(false)
                ?.setProgress(0, 0, false)

            if (needsToUpdateCredentials) {
                configureUpdateCredentialsNotification(download.user)
            } else {
                // TODO put something smart in showDetailsIntent
                val showDetailsIntent = Intent()
                notificationBuilder?.setContentIntent(
                    PendingIntent.getActivity(
                        context, System.currentTimeMillis().toInt(),
                        showDetailsIntent, PendingIntent.FLAG_IMMUTABLE
                    )
                )
            }

            notificationBuilder?.setContentText(
                ErrorMessageAdapter.getErrorCauseMessage(
                    downloadResult,
                    download, context.resources
                )
            )

            notificationManager?.notify(SecureRandom().nextInt(), notificationBuilder?.build())
            if (downloadResult.isSuccess) {
                NotificationUtils.cancelWithDelay(
                    notificationManager,
                    R.string.downloader_download_succeeded_ticker, 2000
                )
            }
        }
    }

    private fun configureUpdateCredentialsNotification(user: User) {
        val updateAccountCredentials = Intent(context, AuthenticatorActivity::class.java)
        updateAccountCredentials.putExtra(AuthenticatorActivity.EXTRA_ACCOUNT, user.toPlatformAccount())
        updateAccountCredentials.putExtra(
            AuthenticatorActivity.EXTRA_ACTION,
            AuthenticatorActivity.ACTION_UPDATE_EXPIRED_TOKEN
        )
        updateAccountCredentials.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        updateAccountCredentials.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
        updateAccountCredentials.addFlags(Intent.FLAG_FROM_BACKGROUND)
        notificationBuilder?.setContentIntent(
            PendingIntent.getActivity(
                context, System.currentTimeMillis().toInt(),
                updateAccountCredentials,
                PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
            )
        )
    }

    private fun notifyDownloadStart(download: DownloadFileOperation) {
        lastPercent = 0
        notificationBuilder = NotificationUtils.newNotificationBuilder(context, viewThemeUtils)
            .setSmallIcon(R.drawable.notification_icon)
            .setTicker(context.getString(R.string.downloader_download_in_progress_ticker))
            .setContentTitle(context.getString(R.string.downloader_download_in_progress_ticker))
            .setOngoing(true)
            .setProgress(100, 0, download.size < 0)
            .setContentText(
                String.format(
                    context.getString(R.string.downloader_download_in_progress_content), 0,
                    File(download.savePath).name
                )
            )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationBuilder?.setChannelId(NotificationUtils.NOTIFICATION_CHANNEL_DOWNLOAD)
        }

        /// includes a pending intent in the notification showing the details view of the file
        var showDetailsIntent: Intent? = null
        showDetailsIntent = if (PreviewImageFragment.canBePreviewed(download.file)) {
            Intent(context, PreviewImageActivity::class.java)
        } else {
            Intent(context, FileDisplayActivity::class.java)
        }
        showDetailsIntent.putExtra(FileActivity.EXTRA_FILE, download.file)
        showDetailsIntent.putExtra(FileActivity.EXTRA_USER, download.user)
        showDetailsIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        notificationBuilder?.setContentIntent(
            PendingIntent.getActivity(
                context, System.currentTimeMillis().toInt(),
                showDetailsIntent, PendingIntent.FLAG_IMMUTABLE
            )
        )

        if (notificationManager == null) {
            notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        }

        notificationManager?.notify(R.string.downloader_download_in_progress_ticker, notificationBuilder?.build())
    }

    private fun showDownloadingFilesNotification() {
        val builder = NotificationUtils.newNotificationBuilder(context, viewThemeUtils)
            .setContentTitle(context.resources.getString(R.string.app_name))
            .setContentText(context.resources.getString(R.string.foreground_service_download))
            .setSmallIcon(R.drawable.notification_icon)
            .setLargeIcon(BitmapFactory.decodeResource(context.resources, R.drawable.notification_icon))


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder.setChannelId(NotificationUtils.NOTIFICATION_CHANNEL_DOWNLOAD)
        }

        notification = builder.build()
    }

    private fun addAccountUpdateListener() {
        val am = AccountManager.get(context)
        am.addOnAccountsUpdatedListener(this, null, false)
    }

    private fun cancelPendingDownloads(accountName: String?) {
        pendingDownloads.remove(accountName)
    }

    private fun sendBroadcastNewDownload(
        download: DownloadFileOperation,
        linkedToRemotePath: String
    ) {
        val added = Intent(getDownloadAddedMessage())
        added.putExtra(ACCOUNT_NAME, download.user.accountName)
        added.putExtra(EXTRA_REMOTE_PATH, download.remotePath)
        added.putExtra(EXTRA_LINKED_TO_PATH, linkedToRemotePath)
        added.setPackage(context.packageName)
        localBroadcastManager.sendBroadcast(added)
    }

    override fun onAccountsUpdated(accounts: Array<out Account>?) {
        if (!accountManager.exists(currentDownload?.user?.toPlatformAccount())) {
            currentDownload?.cancel()
        }
    }

    override fun onTransferProgress(
        progressRate: Long,
        totalTransferredSoFar: Long,
        totalToTransfer: Long,
        filePath: String
    ) {
        val percent: Int = (100.0 * totalTransferredSoFar.toDouble() / totalToTransfer.toDouble()).toInt()
        if (percent != lastPercent) {
            notificationBuilder?.setProgress(100, percent, totalToTransfer < 0)
            val fileName: String = filePath.substring(filePath.lastIndexOf(FileUtils.PATH_SEPARATOR) + 1)
            val text =
                String.format(context.getString(R.string.downloader_download_in_progress_content), percent, fileName)
            notificationBuilder?.setContentText(text)

            if (notificationManager == null) {
                notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            }
            notificationManager?.notify(
                R.string.downloader_download_in_progress_ticker,
                notificationBuilder?.build()
            )
        }
        lastPercent = percent
    }

    inner class FileDownloaderBinder : Binder(), OnDatatransferProgressListener {
        private val boundListeners: MutableMap<Long, OnDatatransferProgressListener> = HashMap()

        fun cancelPendingOrCurrentDownloads(account: Account, file: OCFile) {
            val removeResult: Pair<DownloadFileOperation, String> =
                pendingDownloads.remove(account.name, file.remotePath)
            val download = removeResult.first
            if (download != null) {
                download.cancel()
            } else {
                if (currentDownload != null && currentUser?.isPresent == true &&
                    currentDownload?.remotePath?.startsWith(file.remotePath) == true && account.name == currentUser.get()?.accountName
                ) {
                    currentDownload?.cancel()
                }
            }
        }

        fun cancelAllDownloadsForAccount(accountName: String?) {
            if (currentDownload != null && currentDownload?.user?.nameEquals(accountName) == true) {
                currentDownload?.cancel()
            }

            cancelPendingDownloads(accountName)
        }

        fun isDownloading(user: User?, file: OCFile?): Boolean {
            return user != null && file != null && pendingDownloads.contains(user.accountName, file.remotePath)
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
            progressRate: Long, totalTransferredSoFar: Long,
            totalToTransfer: Long, fileName: String
        ) {
            val boundListener = boundListeners[currentDownload?.file?.fileId]
            boundListener?.onTransferProgress(
                progressRate, totalTransferredSoFar,
                totalToTransfer, fileName
            )
        }
    }
}
