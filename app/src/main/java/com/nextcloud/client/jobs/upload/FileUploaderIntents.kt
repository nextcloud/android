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
import android.os.Build
import com.owncloud.android.authentication.AuthenticatorActivity
import com.owncloud.android.operations.UploadFileOperation
import com.owncloud.android.ui.activity.ConflictsResolveActivity.Companion.createIntent
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

    fun credentialIntent(operation: UploadFileOperation): PendingIntent {
        val intent = Intent(context, AuthenticatorActivity::class.java).apply {
            putExtra(AuthenticatorActivity.EXTRA_ACCOUNT, operation.user.toPlatformAccount())
            putExtra(AuthenticatorActivity.EXTRA_ACTION, AuthenticatorActivity.ACTION_UPDATE_EXPIRED_TOKEN)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
            addFlags(Intent.FLAG_FROM_BACKGROUND)
        }

        return PendingIntent.getActivity(
            context,
            System.currentTimeMillis().toInt(),
            intent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
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

    fun conflictResolveActionIntents(context: Context, uploadFileOperation: UploadFileOperation): PendingIntent {
        val intent = createIntent(
            uploadFileOperation.file,
            uploadFileOperation.user,
            uploadFileOperation.ocUploadId,
            Intent.FLAG_ACTIVITY_CLEAR_TOP,
            context
        )

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.getActivity(context, SecureRandom().nextInt(), intent, PendingIntent.FLAG_MUTABLE)
        } else {
            PendingIntent.getActivity(
                context,
                SecureRandom().nextInt(),
                intent,
                PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
            )
        }
    }

    fun removeUploadActionIntent(uploadFileOperation: UploadFileOperation): PendingIntent {
        val intent = Intent(context, FileUploadBroadcastReceiver::class.java).apply {
            putExtra(FileUploadBroadcastReceiver.UPLOAD_ID, uploadFileOperation.ocUploadId)
            putExtra(FileUploadBroadcastReceiver.REMOTE_PATH, uploadFileOperation.file.remotePath)
            putExtra(FileUploadBroadcastReceiver.STORAGE_PATH, uploadFileOperation.file.storagePath)
        }

        return PendingIntent.getBroadcast(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
