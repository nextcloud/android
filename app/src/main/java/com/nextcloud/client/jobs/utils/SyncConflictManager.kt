/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.client.jobs.utils

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.nextcloud.client.jobs.upload.FileUploadBroadcastReceiver
import com.nextcloud.client.jobs.upload.FileUploadHelper
import com.owncloud.android.R
import com.owncloud.android.authentication.AuthenticatorActivity
import com.owncloud.android.lib.common.operations.RemoteOperationResult
import com.owncloud.android.lib.common.operations.RemoteOperationResult.ResultCode
import com.owncloud.android.operations.UploadFileOperation
import com.owncloud.android.ui.activity.ConflictsResolveActivity
import com.owncloud.android.utils.ErrorMessageAdapter
import java.io.File
import java.security.SecureRandom

object SyncConflictManager {
    fun getNotification(
        context: Context,
        builder: NotificationCompat.Builder,
        operation: UploadFileOperation,
        result: RemoteOperationResult<Any?>,
        notifyOnSameFileExists: () -> Unit
    ): Notification? {
        if (!shouldShowConflictDialog(context, operation, result, notifyOnSameFileExists)) return null

        val textId = result.code.toFailedResultTitleId()
        val errorMessage = ErrorMessageAdapter.getErrorCauseMessage(result, operation, context.resources)

        return builder.apply {
            setTicker(context.getString(textId))
            setContentTitle(context.getString(textId))
            setContentText(errorMessage)
            setAutoCancel(false)
            setOngoing(false)
            setProgress(0, 0, false)
            clearActions()

            result.code.takeIf { it == ResultCode.SYNC_CONFLICT }?.let {
                addAction(
                    R.drawable.ic_cloud_upload,
                    context.getString(R.string.upload_list_resolve_conflict),
                    conflictResolvePendingIntent(context, operation)
                )
                addAction(
                    R.drawable.ic_delete,
                    context.getString(R.string.upload_list_cancel_upload),
                    cancelUploadPendingIntent(context, operation)
                )
            }

            result.code.takeIf { it == ResultCode.UNAUTHORIZED }?.let {
                setContentIntent(credentialPendingIntent(context, operation))
            }
        }.build()
    }

    private fun ResultCode.toFailedResultTitleId(): Int = when (this) {
        ResultCode.UNAUTHORIZED -> R.string.uploader_upload_failed_credentials_error
        ResultCode.SYNC_CONFLICT -> R.string.uploader_upload_failed_sync_conflict_error
        else -> R.string.uploader_upload_failed_ticker
    }

    private fun credentialPendingIntent(context: Context, operation: UploadFileOperation): PendingIntent {
        val intent = Intent(context, AuthenticatorActivity::class.java).apply {
            putExtra(AuthenticatorActivity.EXTRA_ACCOUNT, operation.user.toPlatformAccount())
            putExtra(AuthenticatorActivity.EXTRA_ACTION, AuthenticatorActivity.ACTION_UPDATE_EXPIRED_TOKEN)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS or Intent.FLAG_FROM_BACKGROUND)
        }

        return PendingIntent.getActivity(
            context,
            System.currentTimeMillis().toInt(),
            intent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun conflictResolvePendingIntent(context: Context, operation: UploadFileOperation): PendingIntent {
        val intent = ConflictsResolveActivity.createIntent(
            operation.file,
            operation.user,
            operation.ocUploadId,
            Intent.FLAG_ACTIVITY_CLEAR_TOP,
            context
        )
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
            PendingIntent.FLAG_MUTABLE else PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE

        return PendingIntent.getActivity(context, SecureRandom().nextInt(), intent, flags)
    }

    private fun cancelUploadPendingIntent(context: Context, operation: UploadFileOperation): PendingIntent {
        val intent = Intent(context, FileUploadBroadcastReceiver::class.java).apply {
            putExtra(FileUploadBroadcastReceiver.UPLOAD_ID, operation.ocUploadId)
            putExtra(FileUploadBroadcastReceiver.REMOTE_PATH, operation.file.remotePath)
            putExtra(FileUploadBroadcastReceiver.STORAGE_PATH, operation.file.storagePath)
        }

        return PendingIntent.getBroadcast(
            context,
            operation.file.fileId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun shouldShowConflictDialog(
        context: Context,
        operation: UploadFileOperation,
        result: RemoteOperationResult<Any?>,
        notifyOnSameFileExists: () -> Unit
    ): Boolean {
        if (result.isSuccess ||
            result.isCancelled ||
            result.code == ResultCode.USER_CANCELLED ||
            operation.isMissingPermissionThrown
        ) {
            return false
        }

        val isSameFileOnRemote = FileUploadHelper.instance().isSameFileOnRemote(
            operation.user,
            File(operation.storagePath),
            operation.remotePath,
            context
        )

        if (result.code == ResultCode.SYNC_CONFLICT && isSameFileOnRemote) {
            notifyOnSameFileExists()
            return false
        }

        val delayedCodes =
            setOf(ResultCode.DELAYED_FOR_WIFI, ResultCode.DELAYED_FOR_CHARGING, ResultCode.DELAYED_IN_POWER_SAVE_MODE)
        val invalidCodes = setOf(ResultCode.LOCAL_FILE_NOT_FOUND, ResultCode.LOCK_FAILED)

        return result.code !in delayedCodes && result.code !in invalidCodes
    }
}
