/*
 *   ownCloud Android client application
 *
 *   Copyright (C) 2012 Bartek Przybylski
 *   Copyright (C) 2012-2016 ownCloud Inc.
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License version 2,
 *   as published by the Free Software Foundation.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package com.owncloud.android.files.services

import android.accounts.Account
import android.accounts.AccountManager
import android.accounts.OnAccountsUpdateListener
import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.os.Looper
import android.os.Message
import android.os.Process
import androidx.core.app.NotificationCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.nextcloud.client.account.User
import com.nextcloud.client.account.UserAccountManager
import com.nextcloud.java.util.Optional
import com.owncloud.android.R
import com.owncloud.android.authentication.AuthenticatorActivity
import com.owncloud.android.datamodel.FileDataStorageManager
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.datamodel.UploadsStorageManager
import com.owncloud.android.lib.common.OwnCloudClient
import com.owncloud.android.lib.common.OwnCloudClientManagerFactory
import com.owncloud.android.lib.common.network.OnDatatransferProgressListener
import com.owncloud.android.lib.common.operations.RemoteOperationResult
import com.owncloud.android.lib.common.operations.RemoteOperationResult.ResultCode
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.operations.DownloadFileOperation
import com.owncloud.android.operations.DownloadType
import com.owncloud.android.ui.activity.ConflictsResolveActivity
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
import dagger.android.AndroidInjection
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings
import java.io.File
import java.security.SecureRandom
import java.util.AbstractList
import java.util.Vector
import javax.inject.Inject

class FileDownloader : Service(), OnDatatransferProgressListener, OnAccountsUpdateListener {
    private var mServiceLooper: Looper? = null
    private var mServiceHandler: ServiceHandler? = null
    private var mBinder: IBinder? = null
    private var mDownloadClient: OwnCloudClient? = null
    private var currentUser = Optional.empty<User>()
    private var mStorageManager: FileDataStorageManager? = null
    private val mPendingDownloads = IndexedForest<DownloadFileOperation>()
    private var mCurrentDownload: DownloadFileOperation? = null
    private var notificationManager: NotificationManager? = null
    private var notification: Notification? = null
    private var notificationBuilder: NotificationCompat.Builder? = null
    private var mLastPercent = 0
    private var conflictUploadId: Long = 0
    var mStartedDownload = false

    @JvmField
    @Inject
    var accountManager: UserAccountManager? = null

    @JvmField
    @Inject
    var uploadsStorageManager: UploadsStorageManager? = null

    @JvmField
    @Inject
    var localBroadcastManager: LocalBroadcastManager? = null

    @JvmField
    @Inject
    var viewThemeUtils: ViewThemeUtils? = null

    /**
     * Service initialization
     */
    override fun onCreate() {
        super.onCreate()

        AndroidInjection.inject(this)
        Log_OC.d(TAG, "Creating service")
        initNotificationManager()
        val thread = HandlerThread("FileDownloaderThread", Process.THREAD_PRIORITY_BACKGROUND)
        thread.start()
        mServiceLooper = thread.looper
        mServiceHandler = ServiceHandler(mServiceLooper, this)
        mBinder = FileDownloaderBinder()
        initNotificationBuilder()

        // add AccountsUpdatedListener
        val am = AccountManager.get(applicationContext)
        am.addOnAccountsUpdatedListener(this, null, false)
    }

    private fun initNotificationManager() {
        if (notificationManager == null) {
            notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        }
    }

    private fun initNotificationBuilder() {
        val resources = applicationContext.resources
        val title = resources.getString(R.string.foreground_service_download)

        notificationBuilder = NotificationUtils.newNotificationBuilder(this, viewThemeUtils)
            .setSmallIcon(R.drawable.notification_icon)
            .setOngoing(true)
            .setContentTitle(title)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationBuilder?.setChannelId(NotificationUtils.NOTIFICATION_CHANNEL_DOWNLOAD)
        }
        notification = notificationBuilder?.build()
    }

    private fun notifyNotificationManager() {
        notificationManager?.notify(R.string.downloader_download_in_progress_ticker, notificationBuilder?.build())
    }

    /**
     * Service clean up
     */
    override fun onDestroy() {
        Log_OC.v(TAG, "Destroying service")
        mBinder = null
        mServiceHandler = null
        mServiceLooper!!.quit()
        mServiceLooper = null
        notificationManager = null
        notification = null
        notificationBuilder = null

        // remove AccountsUpdatedListener
        val am = AccountManager.get(applicationContext)
        am.removeOnAccountsUpdatedListener(this)
        super.onDestroy()
    }

    /**
     * Entry point to add one or several files to the queue of downloads.
     *
     * New downloads are added calling to startService(), resulting in a call to this method.
     * This ensures the service will keep on working although the caller activity goes away.
     */
    @Suppress("LongParameterList")
    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        Log_OC.d(TAG, "Starting command with id $startId")

        startForeground(FOREGROUND_SERVICE_ID, notification)

        if (!intent.hasExtra(EXTRA_USER) || !intent.hasExtra(EXTRA_FILE)) {
            Log_OC.e(TAG, "Not enough information provided in intent")
            return START_NOT_STICKY
        } else {
            val user = intent.getParcelableExtra<User>(EXTRA_USER)
            val file = intent.getParcelableExtra<OCFile>(EXTRA_FILE)
            val behaviour = intent.getStringExtra(OCFileListFragment.DOWNLOAD_BEHAVIOUR)
            var downloadType: DownloadType? = DownloadType.DOWNLOAD
            if (intent.hasExtra(DOWNLOAD_TYPE)) {
                downloadType = intent.getSerializableExtra(DOWNLOAD_TYPE) as DownloadType?
            }
            val activityName = intent.getStringExtra(SendShareDialog.ACTIVITY_NAME)
            val packageName = intent.getStringExtra(SendShareDialog.PACKAGE_NAME)
            conflictUploadId = intent.getLongExtra(ConflictsResolveActivity.EXTRA_CONFLICT_UPLOAD_ID, -1)
            val requestedDownloads: AbstractList<String> = Vector()
            try {
                val newDownload = DownloadFileOperation(
                    user,
                    file,
                    behaviour,
                    activityName,
                    packageName,
                    baseContext,
                    downloadType
                )
                newDownload.addDatatransferProgressListener(this)
                newDownload.addDatatransferProgressListener(mBinder as FileDownloaderBinder?)
                val putResult = mPendingDownloads.putIfAbsent(
                    user!!.accountName,
                    file!!.remotePath,
                    newDownload
                )
                if (putResult != null) {
                    val downloadKey = putResult.first
                    requestedDownloads.add(downloadKey)
                    sendBroadcastNewDownload(newDownload, putResult.second)
                } // else, file already in the queue of downloads; don't repeat the request
            } catch (e: IllegalArgumentException) {
                Log_OC.e(TAG, "Not enough information provided in intent: " + e.message)
                return START_NOT_STICKY
            }
            if (requestedDownloads.size > 0) {
                val msg = mServiceHandler!!.obtainMessage()
                msg.arg1 = startId
                msg.obj = requestedDownloads
                mServiceHandler!!.sendMessage(msg)
            }
        }
        return START_NOT_STICKY
    }

    /**
     * Provides a binder object that clients can use to perform operations on the queue of downloads,
     * excepting the addition of new files.
     *
     * Implemented to perform cancellation, pause and resume of existing downloads.
     */
    override fun onBind(intent: Intent): IBinder? {
        return mBinder
    }

    /**
     * Called when ALL the bound clients were onbound.
     */
    override fun onUnbind(intent: Intent): Boolean {
        (mBinder as FileDownloaderBinder?)!!.clearListeners()
        return false // not accepting rebinding (default behaviour)
    }

    override fun onAccountsUpdated(accounts: Array<Account>) {
        // review the current download and cancel it if its account doesn't exist
        if (mCurrentDownload != null && !accountManager!!.exists(mCurrentDownload!!.user.toPlatformAccount())) {
            mCurrentDownload!!.cancel()
        }
        // The rest of downloads are cancelled when they try to start
    }

    /**
     * Binder to let client components to perform operations on the queue of downloads.
     *
     *
     * It provides by itself the available operations.
     */
    inner class FileDownloaderBinder : Binder(), OnDatatransferProgressListener {
        /**
         * Map of listeners that will be reported about progress of downloads from a
         * [FileDownloaderBinder]
         * instance.
         */
        private val mBoundListeners: MutableMap<Long, OnDatatransferProgressListener> = HashMap()

        /**
         * Cancels a pending or current download of a remote file.
         *
         * @param account ownCloud account where the remote file is stored.
         * @param file    A file in the queue of pending downloads
         */
        @Suppress("ComplexMethod")
        fun cancel(account: Account, file: OCFile) {
            val removeResult = mPendingDownloads.remove(account.name, file.remotePath)
            val download = removeResult.first

            if (download != null) {
                download.cancel()
            } else {
                if (mCurrentDownload != null && currentUser.isPresent &&
                    mCurrentDownload!!
                        .remotePath
                        .startsWith(file.remotePath) && account.name == currentUser.get().accountName
                ) {
                    mCurrentDownload!!.cancel()
                }
            }
        }

        /**
         * Cancels all the downloads for an account
         */
        fun cancel(accountName: String?) {
            if (mCurrentDownload != null && mCurrentDownload!!.user.nameEquals(accountName)) {
                mCurrentDownload!!.cancel()
            }
            // Cancel pending downloads
            cancelPendingDownloads(accountName)
        }

        fun clearListeners() {
            mBoundListeners.clear()
        }

        /**
         * Returns True when the file described by 'file' in the ownCloud account 'account'
         * is downloading or waiting to download.
         *
         * If 'file' is a directory, returns 'true' if any of its descendant files is downloading or
         * waiting to download.
         *
         * @param user    user where the remote file is stored.
         * @param file    A file that could be in the queue of downloads.
         */
        fun isDownloading(user: User?, file: OCFile?): Boolean {
            return user != null && file != null && mPendingDownloads.contains(user.accountName, file.remotePath)
        }

        /**
         * Adds a listener interested in the progress of the download for a concrete file.
         *
         * @param listener Object to notify about progress of transfer.
         * @param file     [OCFile] of interest for listener.
         */
        fun addDataTransferProgressListener(listener: OnDatatransferProgressListener?, file: OCFile?) {
            if (file == null || listener == null) {
                return
            }
            mBoundListeners[file.fileId] = listener
        }

        /**
         * Removes a listener interested in the progress of the download for a concrete file.
         *
         * @param listener      Object to notify about progress of transfer.
         * @param file          [OCFile] of interest for listener.
         */
        fun removeDataTransferProgressListener(listener: OnDatatransferProgressListener?, file: OCFile?) {
            if (file == null || listener == null) {
                return
            }
            val fileId = file.fileId
            if (mBoundListeners[fileId] === listener) {
                mBoundListeners.remove(fileId)
            }
        }

        override fun onTransferProgress(
            progressRate: Long,
            totalTransferredSoFar: Long,
            totalToTransfer: Long,
            fileName: String
        ) {
            val boundListener = mBoundListeners[mCurrentDownload!!.file.fileId]
            boundListener?.onTransferProgress(
                progressRate,
                totalTransferredSoFar,
                totalToTransfer,
                fileName
            )
        }
    }

    /**
     * Download worker. Performs the pending downloads in the order they were requested.
     *
     * Created with the Looper of a new thread, started in [FileUploader.onCreate].
     */
    private class ServiceHandler(looper: Looper?, service: FileDownloader?) : Handler(looper!!) {
        // don't make it a final class, and don't remove the static ; lint will warn about a
        // possible memory leak
        var mService: FileDownloader

        init {
            requireNotNull(service) { "Received invalid NULL in parameter 'service'" }
            mService = service
        }

        @Suppress("MagicNumber")
        override fun handleMessage(msg: Message) {
            val requestedDownloads = msg.obj as AbstractList<String>
            if (msg.obj != null) {
                val it: Iterator<String> = requestedDownloads.iterator()
                while (it.hasNext()) {
                    val next = it.next()
                    mService.downloadFile(next)
                }
            }
            mService.mStartedDownload = false

            Handler(Looper.getMainLooper()).postDelayed({
                if (!mService.mStartedDownload) {
                    mService.notificationManager!!.cancel(R.string.downloader_download_in_progress_ticker)
                }
                Log_OC.d(TAG, "Stopping after command with id " + msg.arg1)
                mService.notificationManager!!.cancel(FOREGROUND_SERVICE_ID)
                mService.stopForeground(true)
                mService.stopSelf(msg.arg1)
            }, 2000)
        }
    }

    /**
     * Core download method: requests a file to download and stores it.
     *
     * @param downloadKey Key to access the download to perform, contained in mPendingDownloads
     */
    @Suppress("NestedBlockDepth", "TooGenericExceptionCaught")
    private fun downloadFile(downloadKey: String) {
        mStartedDownload = true
        mCurrentDownload = mPendingDownloads[downloadKey]

        if (mCurrentDownload != null) {
            val isAccountExist = accountManager?.exists(mCurrentDownload!!.user.toPlatformAccount())

            if (isAccountExist == true) {
                notifyDownloadStart(mCurrentDownload!!)
                var downloadResult: RemoteOperationResult<*>? = null
                try {
                    // / prepare client object to send the request to the ownCloud server
                    val currentDownloadAccount = mCurrentDownload!!.user.toPlatformAccount()
                    val currentDownloadUser = accountManager!!.getUser(currentDownloadAccount.name)
                    if (currentUser != currentDownloadUser) {
                        currentUser = currentDownloadUser
                        mStorageManager = FileDataStorageManager(currentUser.get(), contentResolver)
                    } // else, reuse storage manager from previous operation

                    // always get client from client manager, to get fresh credentials in case
                    // of update
                    val ocAccount = currentDownloadUser.get().toOwnCloudAccount()
                    mDownloadClient = OwnCloudClientManagerFactory.getDefaultSingleton().getClientFor(ocAccount, this)

                    // / perform the download
                    downloadResult = mCurrentDownload!!.execute(mDownloadClient)
                    if (downloadResult.isSuccess && mCurrentDownload!!.downloadType === DownloadType.DOWNLOAD) {
                        saveDownloadedFile()
                    }
                } catch (e: Exception) {
                    Log_OC.e(TAG, "Error downloading", e)
                    downloadResult = RemoteOperationResult<Any?>(e)
                } finally {
                    val removeResult = mPendingDownloads.removePayload(
                        mCurrentDownload!!.user.accountName,
                        mCurrentDownload!!.remotePath
                    )
                    if (downloadResult == null) {
                        downloadResult = RemoteOperationResult<Any?>(RuntimeException("Error downloading…"))
                    }

                    // / notify result
                    notifyDownloadResult(mCurrentDownload!!, downloadResult)
                    sendBroadcastDownloadFinished(mCurrentDownload!!, downloadResult, removeResult.second)
                }
            } else {
                cancelPendingDownloads(mCurrentDownload!!.user.accountName)
            }
        }
    }

    /**
     * Updates the OC File after a successful download.
     *
     * TODO move to DownloadFileOperation
     * unify with code from [DocumentsStorageProvider] and [DownloadTask].
     */
    private fun saveDownloadedFile() {
        var file = mStorageManager?.getFileById(mCurrentDownload!!.file.fileId)
        if (file == null) {
            // try to get file via path, needed for overwriting existing files on conflict dialog
            file = mStorageManager?.getFileByDecryptedRemotePath(mCurrentDownload!!.file.remotePath)
        }
        if (file == null) {
            Log_OC.e(this, "Could not save " + mCurrentDownload!!.file.remotePath)
            return
        }

        val syncDate = System.currentTimeMillis()
        file.lastSyncDateForProperties = syncDate
        file.lastSyncDateForData = syncDate
        file.isUpdateThumbnailNeeded = true
        file.modificationTimestamp = mCurrentDownload!!.modificationTimestamp
        file.modificationTimestampAtLastSyncForData = mCurrentDownload!!.modificationTimestamp
        file.etag = mCurrentDownload!!.etag
        file.mimeType = mCurrentDownload!!.mimeType
        file.storagePath = mCurrentDownload!!.savePath
        file.fileLength = File(mCurrentDownload!!.savePath).length()
        file.remoteId = mCurrentDownload!!.file.remoteId
        mStorageManager!!.saveFile(file)
        if (MimeTypeUtil.isMedia(mCurrentDownload!!.mimeType)) {
            FileDataStorageManager.triggerMediaScan(file.storagePath, file)
        }
        mStorageManager!!.saveConflict(file, null)
    }

    /**
     * Creates a status notification to show the download progress
     *
     * @param download Download operation starting.
     */
    @Suppress("MagicNumber")
    private fun notifyDownloadStart(download: DownloadFileOperation) {
        val fileName = download.file.getFileNameWithExtension(10)
        val titlePrefix = getString(R.string.file_downloader_notification_title_prefix)
        val title = titlePrefix + fileName

        // / update status notification with a progress bar
        mLastPercent = 0
        notificationBuilder
            ?.setContentTitle(title)
            ?.setTicker(title)
            ?.setProgress(100, 0, download.size < 0)

        // / includes a pending intent in the notification showing the details view of the file
        val showDetailsIntent: Intent = if (PreviewImageFragment.canBePreviewed(download.file)) {
            Intent(this, PreviewImageActivity::class.java)
        } else {
            Intent(this, FileDisplayActivity::class.java)
        }

        showDetailsIntent.putExtra(FileActivity.EXTRA_FILE, download.file)
        showDetailsIntent.putExtra(FileActivity.EXTRA_USER, download.user)
        showDetailsIntent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
        notificationBuilder?.setContentIntent(
            PendingIntent.getActivity(
                this,
                System.currentTimeMillis().toInt(),
                showDetailsIntent,
                PendingIntent.FLAG_IMMUTABLE
            )
        )
        initNotificationManager()
        notifyNotificationManager()
    }

    /**
     * Callback method to update the progress bar in the status notification.
     */
    @Suppress("MagicNumber")
    override fun onTransferProgress(
        progressRate: Long,
        totalTransferredSoFar: Long,
        totalToTransfer: Long,
        filePath: String
    ) {
        val percent = (100.0 * totalTransferredSoFar.toDouble() / totalToTransfer.toDouble()).toInt()
        if (percent != mLastPercent) {
            notificationBuilder?.setProgress(100, percent, totalToTransfer < 0)
            initNotificationManager()
            notifyNotificationManager()
        }
        mLastPercent = percent
    }

    /**
     * Updates the status notification with the result of a download operation.
     *
     * @param downloadResult Result of the download operation.
     * @param download       Finished download operation
     */
    @SuppressFBWarnings("DMI")
    @Suppress("MagicNumber")
    private fun notifyDownloadResult(
        download: DownloadFileOperation,
        downloadResult: RemoteOperationResult<*>
    ) {
        initNotificationManager()
        if (!downloadResult.isCancelled) {
            if (downloadResult.isSuccess) {
                if (conflictUploadId > 0) {
                    uploadsStorageManager!!.removeUpload(conflictUploadId)
                }
                // Don't show notification except an error has occurred.
                return
            }

            var tickerId = if (downloadResult.isSuccess) {
                R.string.downloader_download_succeeded_ticker
            } else {
                R.string.downloader_download_failed_ticker
            }

            val needsToUpdateCredentials = ResultCode.UNAUTHORIZED == downloadResult.code

            tickerId = if (needsToUpdateCredentials) {
                R.string.downloader_download_failed_credentials_error
            } else {
                tickerId
            }

            notificationBuilder
                ?.setSmallIcon(R.drawable.notification_icon)
                ?.setTicker(getString(tickerId))
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
                        this,
                        System.currentTimeMillis().toInt(),
                        showDetailsIntent,
                        PendingIntent.FLAG_IMMUTABLE
                    )
                )
            }
            notificationBuilder?.setContentText(
                ErrorMessageAdapter.getErrorCauseMessage(
                    downloadResult,
                    download,
                    resources
                )
            )
            if (notificationManager != null) {
                notificationManager?.notify(SecureRandom().nextInt(), notificationBuilder?.build())

                // Remove success notification
                if (downloadResult.isSuccess) {
                    // Sleep 2 seconds, so show the notification before remove it
                    NotificationUtils.cancelWithDelay(
                        notificationManager,
                        R.string.downloader_download_succeeded_ticker,
                        2000
                    )
                }
            }
        }
    }

    private fun configureUpdateCredentialsNotification(user: User) {
        // let the user update credentials with one click
        val updateAccountCredentials = Intent(this, AuthenticatorActivity::class.java)
        updateAccountCredentials.putExtra(AuthenticatorActivity.EXTRA_ACCOUNT, user.toPlatformAccount())
        updateAccountCredentials.putExtra(
            AuthenticatorActivity.EXTRA_ACTION,
            AuthenticatorActivity.ACTION_UPDATE_EXPIRED_TOKEN
        )
        updateAccountCredentials.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        updateAccountCredentials.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
        updateAccountCredentials.addFlags(Intent.FLAG_FROM_BACKGROUND)
        notificationBuilder!!.setContentIntent(
            PendingIntent.getActivity(
                this,
                System.currentTimeMillis().toInt(),
                updateAccountCredentials,
                PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
            )
        )
    }

    /**
     * Sends a broadcast when a download finishes in order to the interested activities can
     * update their view
     *
     * @param download               Finished download operation
     * @param downloadResult         Result of the download operation
     * @param unlinkedFromRemotePath Path in the downloads tree where the download was unlinked from
     */
    private fun sendBroadcastDownloadFinished(
        download: DownloadFileOperation,
        downloadResult: RemoteOperationResult<*>,
        unlinkedFromRemotePath: String?
    ) {
        val end = Intent(downloadFinishMessage)
        end.putExtra(EXTRA_DOWNLOAD_RESULT, downloadResult.isSuccess)
        end.putExtra(ACCOUNT_NAME, download.user.accountName)
        end.putExtra(EXTRA_REMOTE_PATH, download.remotePath)
        end.putExtra(OCFileListFragment.DOWNLOAD_BEHAVIOUR, download.behaviour)
        end.putExtra(SendShareDialog.ACTIVITY_NAME, download.activityName)
        end.putExtra(SendShareDialog.PACKAGE_NAME, download.packageName)
        if (unlinkedFromRemotePath != null) {
            end.putExtra(EXTRA_LINKED_TO_PATH, unlinkedFromRemotePath)
        }
        end.setPackage(packageName)
        localBroadcastManager!!.sendBroadcast(end)
    }

    /**
     * Sends a broadcast when a new download is added to the queue.
     *
     * @param download           Added download operation
     * @param linkedToRemotePath Path in the downloads tree where the download was linked to
     */
    private fun sendBroadcastNewDownload(
        download: DownloadFileOperation,
        linkedToRemotePath: String
    ) {
        val added = Intent(downloadAddedMessage)
        added.putExtra(ACCOUNT_NAME, download.user.accountName)
        added.putExtra(EXTRA_REMOTE_PATH, download.remotePath)
        added.putExtra(EXTRA_LINKED_TO_PATH, linkedToRemotePath)
        added.setPackage(packageName)
        localBroadcastManager!!.sendBroadcast(added)
    }

    private fun cancelPendingDownloads(accountName: String?) {
        mPendingDownloads.remove(accountName)
    }

    companion object {
        const val EXTRA_USER = "USER"
        const val EXTRA_FILE = "FILE"
        private const val DOWNLOAD_ADDED_MESSAGE = "DOWNLOAD_ADDED"
        private const val DOWNLOAD_FINISH_MESSAGE = "DOWNLOAD_FINISH"
        const val EXTRA_DOWNLOAD_RESULT = "RESULT"
        const val EXTRA_REMOTE_PATH = "REMOTE_PATH"
        const val EXTRA_LINKED_TO_PATH = "LINKED_TO"
        const val ACCOUNT_NAME = "ACCOUNT_NAME"
        const val DOWNLOAD_TYPE = "DOWNLOAD_TYPE"
        private const val FOREGROUND_SERVICE_ID = 412
        private val TAG = FileDownloader::class.java.simpleName

        @JvmStatic
        val downloadAddedMessage: String
            get() = FileDownloader::class.java.name + DOWNLOAD_ADDED_MESSAGE

        @JvmStatic
        val downloadFinishMessage: String
            get() = FileDownloader::class.java.name + DOWNLOAD_FINISH_MESSAGE
    }
}
