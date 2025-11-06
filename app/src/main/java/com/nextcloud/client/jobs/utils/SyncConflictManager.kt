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
import com.owncloud.android.ui.activity.ConflictsResolveActivity.Companion.createIntent
import com.owncloud.android.utils.ErrorMessageAdapter
import java.io.File
import java.security.SecureRandom

object SyncConflictManager {
    fun getNotification(
        context: Context,
        notificationBuilder: NotificationCompat.Builder,
        operation: UploadFileOperation,
        result: RemoteOperationResult<Any?>,
        notifyOnSameFileExists: () -> Unit
    ): Notification? {
        if (!handle(context, operation, result, notifyOnSameFileExists)) {
            return null
        }

        val errorMessage = ErrorMessageAdapter.getErrorCauseMessage(
            result,
            operation,
            context.resources
        )

        val conflictsResolveIntent = if (result.code == ResultCode.SYNC_CONFLICT) {
            conflictResolveActionIntents(context, operation)
        } else {
            null
        }

        val credentialIntent: PendingIntent? = if (result.code == ResultCode.UNAUTHORIZED) {
            credentialIntent(context, operation)
        } else {
            null
        }

        val cancelUploadActionIntent = if (conflictsResolveIntent != null) {
            cancelUploadActionIntent(context, operation)
        } else {
            null
        }

        val textId = getFailedResultTitleId(result.code)

        return notificationBuilder.run {
            setTicker(context.getString(textId))
            setContentTitle(context.getString(textId))
            setAutoCancel(false)
            setOngoing(false)
            setProgress(0, 0, false)
            clearActions()

            conflictsResolveIntent?.let {
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
        }.build()
    }

    private fun getFailedResultTitleId(resultCode: ResultCode): Int {
        val needsToUpdateCredentials = (resultCode == ResultCode.UNAUTHORIZED)

        return if (needsToUpdateCredentials) {
            R.string.uploader_upload_failed_credentials_error
        } else if (resultCode == ResultCode.SYNC_CONFLICT) {
            R.string.uploader_upload_failed_sync_conflict_error
        } else {
            R.string.uploader_upload_failed_ticker
        }
    }

    private fun credentialIntent(context: Context, operation: UploadFileOperation): PendingIntent {
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

    private fun conflictResolveActionIntents(
        context: Context,
        uploadFileOperation: UploadFileOperation
    ): PendingIntent {
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

    private fun cancelUploadActionIntent(context: Context, operation: UploadFileOperation): PendingIntent {
        val intent = Intent(context, FileUploadBroadcastReceiver::class.java).apply {
            putExtra(FileUploadBroadcastReceiver.UPLOAD_ID, operation.ocUploadId)
            putExtra(FileUploadBroadcastReceiver.REMOTE_PATH, operation.file.remotePath)
            putExtra(FileUploadBroadcastReceiver.STORAGE_PATH, operation.file.storagePath)
        }

        return PendingIntent.getBroadcast(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun handle(
        context: Context,
        operation: UploadFileOperation,
        result: RemoteOperationResult<Any?>,
        notifyOnSameFileExists: () -> Unit
    ): Boolean {
        if (result.isSuccess || result.isCancelled || operation.isMissingPermissionThrown) {
            return false
        }

        if (result.code == ResultCode.SYNC_CONFLICT &&
            FileUploadHelper.instance().isSameFileOnRemote(
                operation.user,
                File(operation.storagePath),
                operation.remotePath,
                context
            )
        ) {
            notifyOnSameFileExists()
            return false
        }

        val notDelayed = result.code !in setOf(
            ResultCode.DELAYED_FOR_WIFI,
            ResultCode.DELAYED_FOR_CHARGING,
            ResultCode.DELAYED_IN_POWER_SAVE_MODE
        )

        val isValidFile = result.code !in setOf(
            ResultCode.LOCAL_FILE_NOT_FOUND,
            ResultCode.LOCK_FAILED
        )

        return !(!notDelayed || !isValidFile || result.code == ResultCode.USER_CANCELLED)
    }
}
