/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.owncloud.android.operations.upload

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.nextcloud.client.jobs.upload.FileUploadBroadcastReceiver
import com.nextcloud.utils.extensions.failedResultTitleId
import com.owncloud.android.R
import com.owncloud.android.authentication.AuthenticatorActivity
import com.owncloud.android.lib.common.operations.RemoteOperationResult.ResultCode
import com.owncloud.android.operations.UploadFileOperation
import com.owncloud.android.operations.UploadFileOperation.MISSING_FILE_PERMISSION_NOTIFICATION_ID
import com.owncloud.android.ui.activity.ConflictsResolveActivity.Companion.createIntent
import com.owncloud.android.ui.notifications.NotificationUtils
import java.security.SecureRandom

fun UploadFileOperation.buildFailedResultNotification(
    notificationBuilder: NotificationCompat.Builder,
    resultCode: ResultCode,
    errorMessage: String
) {
    if (isMissingPermissionThrown) {
        return
    }

    val conflictResolveIntent = if (resultCode == ResultCode.SYNC_CONFLICT) {
        onConflictResolveActionIntents(context)
    } else {
        null
    }

    val credentialIntent: PendingIntent? = if (resultCode == ResultCode.UNAUTHORIZED) {
        credentialIntent()
    } else {
        null
    }

    val cancelUploadActionIntent = if (conflictResolveIntent != null) {
        cancelUploadActionIntent()
    } else {
        null
    }

    val textId = resultCode.failedResultTitleId()

    notificationBuilder.run {
        setTicker(context.getString(textId))
        setContentTitle(context.getString(textId))
        setAutoCancel(false)
        setOngoing(false)
        setProgress(0, 0, false)
        clearActions()

        conflictResolveIntent?.let {
            addAction(
                R.drawable.ic_cloud_upload,
                context.getString(R.string.upload_list_resolve_conflict),
                it
            )
        }

        cancelUploadActionIntent?.let {
            addAction(
                R.drawable.ic_delete,
                context.getString(R.string.upload_list_cancel_upload),
                cancelUploadActionIntent
            )
        }

        credentialIntent?.let {
            setContentIntent(it)
        }

        setContentText(errorMessage)
    }
}

fun UploadFileOperation.onConflictResolveActionIntents(
    context: Context
): PendingIntent {
    val intent = createIntent(
        file,
        user,
        ocUploadId,
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

fun UploadFileOperation.credentialIntent(): PendingIntent {
    val intent = Intent(context, AuthenticatorActivity::class.java).apply {
        putExtra(AuthenticatorActivity.EXTRA_ACCOUNT, user.toPlatformAccount())
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

fun UploadFileOperation.cancelUploadActionIntent(): PendingIntent {
    val intent = Intent(context, FileUploadBroadcastReceiver::class.java).apply {
        putExtra(FileUploadBroadcastReceiver.UPLOAD_ID, ocUploadId)
        putExtra(FileUploadBroadcastReceiver.REMOTE_PATH, file.remotePath)
        putExtra(FileUploadBroadcastReceiver.STORAGE_PATH, file.storagePath)
    }

    return PendingIntent.getBroadcast(
        context,
        ocUploadId.toInt(),
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
}

fun UploadFileOperation.showStoragePermissionNotification() {
    val notificationManager = ContextCompat.getSystemService(context, NotificationManager::class.java)
        ?: return
    val alreadyShown = notificationManager.activeNotifications.any {
        it.id == MISSING_FILE_PERMISSION_NOTIFICATION_ID
    }
    if (alreadyShown) {
        return
    }

    val allowAllFileAccessAction = getAllowAllFileAccessAction(context)
    val appPermissionsAction = getAppPermissionsAction(context)

    val notificationBuilder =
        NotificationCompat.Builder(context, NotificationUtils.NOTIFICATION_CHANNEL_UPLOAD)
            .setSmallIcon(android.R.drawable.stat_sys_warning)
            .setContentTitle(context.getString(R.string.upload_missing_storage_permission_title))
            .setContentText(context.getString(R.string.upload_missing_storage_permission_description))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .addAction(allowAllFileAccessAction)
            .addAction(appPermissionsAction)
            .setAutoCancel(true)

    notificationManager.notify(MISSING_FILE_PERMISSION_NOTIFICATION_ID, notificationBuilder.build())
}

private fun getActionPendingIntent(context: Context, actionType: UploadFileBroadcastReceiverActions): PendingIntent {
    val intent = Intent(context, UploadFileBroadcastReceiver::class.java).apply {
        action = "com.owncloud.android.ACTION_UPLOAD_FILE_PERMISSION"
        putExtra(UploadFileBroadcastReceiver.ACTION_TYPE, actionType)
    }

    return PendingIntent.getBroadcast(
        context,
        actionType.ordinal,
        intent,
        PendingIntent.FLAG_IMMUTABLE
    )
}

private fun getAllowAllFileAccessAction(context: Context): NotificationCompat.Action {
    val pendingIntent = getActionPendingIntent(context, UploadFileBroadcastReceiverActions.ALLOW_ALL_FILES)
    return NotificationCompat.Action(
        null,
        context.getString(R.string.upload_missing_storage_permission_allow_file_access),
        pendingIntent
    )
}

private fun getAppPermissionsAction(context: Context): NotificationCompat.Action {
    val pendingIntent = getActionPendingIntent(context, UploadFileBroadcastReceiverActions.APP_PERMISSIONS)
    return NotificationCompat.Action(
        null,
        context.getString(R.string.upload_missing_storage_permission_app_permissions),
        pendingIntent
    )
}
