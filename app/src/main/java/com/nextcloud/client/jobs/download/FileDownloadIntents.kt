/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2023 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-FileCopyrightText: 2023 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.nextcloud.client.jobs.download

import android.content.Context
import android.content.Intent
import com.nextcloud.client.account.User
import com.owncloud.android.authentication.AuthenticatorActivity
import com.owncloud.android.lib.common.operations.RemoteOperationResult
import com.owncloud.android.operations.DownloadFileOperation
import com.owncloud.android.ui.activity.FileActivity
import com.owncloud.android.ui.activity.FileDisplayActivity
import com.owncloud.android.ui.dialog.SendShareDialog
import com.owncloud.android.ui.fragment.OCFileListFragment
import com.owncloud.android.ui.preview.PreviewImageActivity
import com.owncloud.android.ui.preview.PreviewImageFragment

class FileDownloadIntents(private val context: Context) {

    fun newDownloadIntent(download: DownloadFileOperation, linkedToRemotePath: String): Intent {
        return Intent(FileDownloadWorker.getDownloadAddedMessage()).apply {
            putExtra(FileDownloadWorker.EXTRA_ACCOUNT_NAME, download.user.accountName)
            putExtra(FileDownloadWorker.EXTRA_REMOTE_PATH, download.remotePath)
            putExtra(FileDownloadWorker.EXTRA_LINKED_TO_PATH, linkedToRemotePath)
            setPackage(context.packageName)
        }
    }

    fun downloadFinishedIntent(
        download: DownloadFileOperation,
        downloadResult: RemoteOperationResult<*>,
        unlinkedFromRemotePath: String?
    ): Intent {
        return Intent(FileDownloadWorker.getDownloadFinishMessage()).apply {
            putExtra(FileDownloadWorker.EXTRA_DOWNLOAD_RESULT, downloadResult.isSuccess)
            putExtra(FileDownloadWorker.EXTRA_ACCOUNT_NAME, download.user.accountName)
            putExtra(FileDownloadWorker.EXTRA_REMOTE_PATH, download.remotePath)
            putExtra(OCFileListFragment.DOWNLOAD_BEHAVIOUR, download.behaviour)
            putExtra(SendShareDialog.ACTIVITY_NAME, download.activityName)
            putExtra(SendShareDialog.PACKAGE_NAME, download.packageName)
            if (unlinkedFromRemotePath != null) {
                putExtra(FileDownloadWorker.EXTRA_LINKED_TO_PATH, unlinkedFromRemotePath)
            }
            setPackage(context.packageName)
        }
    }

    fun credentialContentIntent(user: User): Intent {
        return Intent(context, AuthenticatorActivity::class.java).apply {
            putExtra(AuthenticatorActivity.EXTRA_ACCOUNT, user.toPlatformAccount())
            putExtra(
                AuthenticatorActivity.EXTRA_ACTION,
                AuthenticatorActivity.ACTION_UPDATE_EXPIRED_TOKEN
            )
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
            addFlags(Intent.FLAG_FROM_BACKGROUND)
        }
    }

    fun detailsIntent(operation: DownloadFileOperation?): Intent {
        return if (operation != null) {
            if (PreviewImageFragment.canBePreviewed(operation.file)) {
                Intent(context, PreviewImageActivity::class.java)
            } else {
                Intent(context, FileDisplayActivity::class.java)
            }.apply {
                putExtra(FileActivity.EXTRA_FILE, operation.file)
                putExtra(FileActivity.EXTRA_USER, operation.user)
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
        } else {
            Intent()
        }
    }
}
