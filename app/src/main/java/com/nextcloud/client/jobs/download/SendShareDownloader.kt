/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.nextcloud.client.jobs.download

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.nextcloud.client.utils.IntentUtil.createSendIntent
import com.nextcloud.utils.extensions.getParcelableArgument
import com.owncloud.android.R
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.ui.activity.FileActivity
import com.owncloud.android.ui.dialog.SendShareDialog
import com.owncloud.android.ui.fragment.OCFileListFragment

/**
 * Downloads a file that is not stored on the device yet and sends it to the app the user picked in
 * [SendShareDialog], as soon as the download finishes.
 *
 * Register it as a lifecycle observer, so that it listens for finished downloads only while the host activity is
 * started:
 *
 * ```
 * private val sendShareDownloader by lazy { SendShareDownloader(this) }
 *
 * override fun onCreate(savedInstanceState: Bundle?) {
 *     lifecycle.addObserver(sendShareDownloader)
 * }
 * ```
 */
class SendShareDownloader(
    private val activity: FileActivity,
    private val localBroadcastManager: LocalBroadcastManager = LocalBroadcastManager.getInstance(activity)
) : DefaultLifecycleObserver,
    SendShareDialog.SendShareDialogDownloader {

    private var fileWaitingToSend: OCFile? = null

    private val downloadCompletedReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            onDownloadCompleted(intent)
        }
    }

    override fun onStart(owner: LifecycleOwner) {
        val filter = IntentFilter(FileDownloadEventBroadcaster.ACTION_DOWNLOAD_COMPLETED)
        localBroadcastManager.registerReceiver(downloadCompletedReceiver, filter)
    }

    override fun onStop(owner: LifecycleOwner) {
        localBroadcastManager.unregisterReceiver(downloadCompletedReceiver)
    }

    override fun downloadFile(file: OCFile, packageName: String, activityName: String) {
        val user = activity.user.orElse(null) ?: return
        fileWaitingToSend = file

        val fileDownloadHelper = FileDownloadHelper.instance()
        if (fileDownloadHelper.isDownloading(user, file)) {
            return
        }

        fileDownloadHelper.downloadFile(
            user = user,
            ocFile = file,
            behaviour = OCFileListFragment.DOWNLOAD_SEND,
            activityName = activityName,
            packageName = packageName
        )
    }

    fun saveState(outState: Bundle) {
        outState.putParcelable(KEY_FILE_WAITING_TO_SEND, fileWaitingToSend)
    }

    fun restoreState(savedInstanceState: Bundle?) {
        fileWaitingToSend = savedInstanceState?.getParcelableArgument(KEY_FILE_WAITING_TO_SEND, OCFile::class.java)
    }

    @Suppress("ReturnCount")
    private fun onDownloadCompleted(intent: Intent) {
        val waitingToSend = fileWaitingToSend ?: return

        if (OCFileListFragment.DOWNLOAD_SEND !=
            intent.getStringExtra(FileDownloadEventBroadcaster.EXTRA_DOWNLOAD_BEHAVIOUR)
        ) {
            return
        }

        if (activity.account?.name != intent.getStringExtra(FileDownloadEventBroadcaster.EXTRA_ACCOUNT_NAME)) {
            return
        }

        val remotePath = intent.getStringExtra(FileDownloadEventBroadcaster.EXTRA_REMOTE_PATH) ?: return
        val downloadedFile = activity.storageManager?.getFileByEncryptedRemotePath(remotePath) ?: return
        if (downloadedFile.fileId != waitingToSend.fileId) {
            return
        }

        fileWaitingToSend = null

        if (!downloadedFile.isDown) {
            Log_OC.e(TAG, "File cannot be sent, download failed: " + downloadedFile.remotePath)
            return
        }

        val packageName = intent.getStringExtra(FileDownloadEventBroadcaster.EXTRA_PACKAGE_NAME) ?: return
        val activityName = intent.getStringExtra(FileDownloadEventBroadcaster.EXTRA_ACTIVITY_NAME) ?: return
        send(downloadedFile, packageName, activityName)
    }

    private fun send(file: OCFile, packageName: String, activityName: String) {
        val sendIntent = createSendIntent(activity, file).apply {
            component = ComponentName(packageName, activityName)
        }

        val title = activity.getString(R.string.activity_chooser_send_file_title)
        activity.startActivity(Intent.createChooser(sendIntent, title))
    }

    companion object {
        private val TAG = SendShareDownloader::class.java.simpleName
        private const val KEY_FILE_WAITING_TO_SEND = "KEY_FILE_WAITING_TO_SEND"
    }
}
