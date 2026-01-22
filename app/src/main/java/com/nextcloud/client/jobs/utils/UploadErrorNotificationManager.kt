/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.client.jobs.utils

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.nextcloud.client.jobs.notification.WorkerNotificationManager
import com.nextcloud.client.jobs.upload.FileUploadHelper
import com.nextcloud.client.jobs.upload.UploadBroadcastAction
import com.nextcloud.utils.extensions.isFileSpecificError
import com.owncloud.android.R
import com.owncloud.android.authentication.AuthenticatorActivity
import com.owncloud.android.lib.common.operations.RemoteOperationResult
import com.owncloud.android.lib.common.operations.RemoteOperationResult.ResultCode
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.operations.UploadFileOperation
import com.owncloud.android.ui.activity.ConflictsResolveActivity
import com.owncloud.android.utils.ErrorMessageAdapter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

object UploadErrorNotificationManager {
    private const val TAG = "UploadErrorNotificationManager"

    /**
     * Processes the result of an upload operation and manages error notifications.
     * * It filters out successful or silent results and handles [ResultCode.SYNC_CONFLICT]
     * by checking if the remote file is identical. If it's a "real" conflict or error,
     * it displays a notification with relevant actions (e.g., Resolve Conflict, Pause, Cancel).
     *
     * @param onSameFileConflict Triggered only if a 409 Conflict occurs but files are identical.
     */
    @Suppress("ReturnCount")
    suspend fun handleResult(
        context: Context,
        notificationManager: WorkerNotificationManager,
        operation: UploadFileOperation,
        result: RemoteOperationResult<Any?>,
        onSameFileConflict: suspend () -> Unit = {}
    ) {
        Log_OC.d(TAG, "handle upload result with result code: " + result.code)

        if (result.isSuccess || result.isCancelled || operation.isMissingPermissionThrown) {
            Log_OC.d(TAG, "operation is successful, cancelled or lack of storage permission, notification skipped")
            return
        }

        val silentCodes = setOf(
            ResultCode.DELAYED_FOR_WIFI,
            ResultCode.DELAYED_FOR_CHARGING,
            ResultCode.DELAYED_IN_POWER_SAVE_MODE,
            ResultCode.LOCAL_FILE_NOT_FOUND,
            ResultCode.LOCK_FAILED
        )

        if (result.code in silentCodes) {
            Log_OC.d(TAG, "silent error code, notification skipped")
            return
        }

        // Do not show an error notification when uploading the same file again (manual uploads only).
        if (result.code == ResultCode.SYNC_CONFLICT) {
            val isSameFile = withContext(Dispatchers.IO) {
                FileUploadHelper.instance().isSameFileOnRemote(
                    operation.user,
                    File(operation.storagePath),
                    operation.remotePath,
                    context
                )
            }

            if (isSameFile) {
                Log_OC.w(TAG, "exact same file already exists on remote, error notification skipped")
                onSameFileConflict()
                return
            }
        }

        // now we can show error notification
        val notification = getNotification(
            context,
            notificationManager.notificationBuilder,
            operation,
            result
        )

        Log_OC.d(TAG, "🔔" + "notification created")

        withContext(Dispatchers.Main) {
            if (result.code.isFileSpecificError()) {
                notificationManager.showNotification(operation.ocUploadId.toInt(), notification)
            } else {
                notificationManager.showNotification(notification)
            }
        }
    }

    private fun getNotification(
        context: Context,
        builder: NotificationCompat.Builder,
        operation: UploadFileOperation,
        result: RemoteOperationResult<Any?>
    ): Notification {
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

            // actions for all error types
            addAction(UploadBroadcastAction.PauseAndCancel(operation).pauseAction(context))
            addAction(UploadBroadcastAction.PauseAndCancel(operation).cancelAction(context))

            if (result.code == ResultCode.SYNC_CONFLICT) {
                addAction(
                    R.drawable.ic_cloud_upload,
                    context.getString(R.string.upload_list_resolve_conflict),
                    conflictResolvePendingIntent(context, operation)
                )
            }

            if (result.code == ResultCode.UNAUTHORIZED) {
                setContentIntent(credentialPendingIntent(context, operation))
            }
        }.build()
    }

    private fun ResultCode.toFailedResultTitleId(): Int = when (this) {
        ResultCode.UNAUTHORIZED -> R.string.uploader_upload_failed_credentials_error
        ResultCode.SYNC_CONFLICT -> R.string.uploader_upload_failed_sync_conflict_error
        else -> R.string.uploader_upload_failed_ticker
    }

    @Suppress("DEPRECATION")
    private fun credentialPendingIntent(context: Context, operation: UploadFileOperation): PendingIntent {
        val intent = Intent(context, AuthenticatorActivity::class.java).apply {
            putExtra(AuthenticatorActivity.EXTRA_ACCOUNT, operation.user.toPlatformAccount())
            putExtra(AuthenticatorActivity.EXTRA_ACTION, AuthenticatorActivity.ACTION_UPDATE_EXPIRED_TOKEN)
            addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS or
                    Intent.FLAG_FROM_BACKGROUND
            )
            setClass(context, AuthenticatorActivity::class.java)
            setPackage(context.packageName)
        }

        return PendingIntent.getActivity(
            context,
            System.currentTimeMillis().toInt(),
            intent,
            PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun conflictResolvePendingIntent(context: Context, operation: UploadFileOperation): PendingIntent {
        val intent = ConflictsResolveActivity.createIntent(
            operation.file,
            operation.user,
            conflictUploadId = operation.ocUploadId,
            Intent.FLAG_ACTIVITY_CLEAR_TOP,
            context
        ).apply {
            setClass(context, ConflictsResolveActivity::class.java)
            setPackage(context.packageName)
        }

        return PendingIntent.getActivity(
            context,
            operation.ocUploadId.toInt(),
            intent,
            PendingIntent.FLAG_IMMUTABLE
        )
    }
}
