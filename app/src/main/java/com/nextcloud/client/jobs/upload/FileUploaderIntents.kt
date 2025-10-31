/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2023 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-FileCopyrightText: 2023 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.nextcloud.client.jobs.upload

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.owncloud.android.operations.UploadFileOperation
import com.owncloud.android.ui.activity.UploadListActivity
import java.security.SecureRandom

class FileUploaderIntents(private val context: Context) {

    private val secureRandomGenerator = SecureRandom()

    fun startIntent(operation: UploadFileOperation): PendingIntent {
        val intent = Intent(
            context,
            FileUploadHelper.UploadNotificationActionReceiver::class.java
        ).apply {
            putExtra(FileUploadWorker.EXTRA_ACCOUNT_NAME, operation.user.accountName)
            putExtra(FileUploadWorker.EXTRA_REMOTE_PATH, operation.remotePath)
            action = FileUploadWorker.ACTION_CANCEL_BROADCAST
        }

        return PendingIntent.getBroadcast(
            context,
            secureRandomGenerator.nextInt(),
            intent,
            PendingIntent.FLAG_IMMUTABLE
        )
    }

    fun notificationStartIntent(operation: UploadFileOperation?): PendingIntent {
        val intent = UploadListActivity.createIntent(
            operation?.file,
            operation?.user,
            Intent.FLAG_ACTIVITY_CLEAR_TOP,
            context
        )

        return PendingIntent.getActivity(
            context,
            System.currentTimeMillis().toInt(),
            intent,
            PendingIntent.FLAG_IMMUTABLE
        )
    }
}
