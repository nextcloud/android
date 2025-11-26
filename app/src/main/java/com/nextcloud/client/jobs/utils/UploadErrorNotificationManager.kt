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
import com.nextcloud.client.jobs.upload.FileUploadBroadcastReceiver
import com.nextcloud.client.jobs.upload.FileUploadHelper
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

    suspend fun handleResult(
        context: Context,
        notificationManager: WorkerNotificationManager,
        operation: UploadFileOperation,
        result: RemoteOperationResult<Any?>,
        showSameFileAlreadyExistsNotification: suspend () -> Unit = {}
    ) {
        Log_OC.d(TAG, "handle upload result with result code: " + result.code)

        val notification = withContext(Dispatchers.IO) {
            val isSameFileOnRemote = FileUploadHelper.instance().isSameFileOnRemote(
                operation.user,
                File(operation.storagePath),
                operation.remotePath,
                context
            )

            getNotification(
                isSameFileOnRemote,
                context,
                notificationManager.notificationBuilder,
                operation,
                result,
                notifyOnSameFileExists = {
                    showSameFileAlreadyExistsNotification()
                    operation.handleLocalBehaviour()
                }
            )
        } ?: return

        Log_OC.d(TAG, "🔔" + "notification created")

        withContext(Dispatchers.Main) {
            if (result.code.isFileSpecificError()) {
                notificationManager.showNotification(operation.ocUploadId.toInt(), notification)
            } else {
                notificationManager.showNotification(notification)
            }
        }
    }

    private suspend fun getNotification(
        isSameFileOnRemote: Boolean,
        context: Context,
        builder: NotificationCompat.Builder,
        operation: UploadFileOperation,
        result: RemoteOperationResult<Any?>,
        notifyOnSameFileExists: suspend () -> Unit
    ): Notification? {
        if (!shouldShowConflictDialog(isSameFileOnRemote, operation, result, notifyOnSameFileExists)) return null

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
                    FileUploadBroadcastReceiver.getBroadcast(context, operation.ocUploadId)
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

    @Suppress("ReturnCount", "ComplexCondition")
    private suspend fun shouldShowConflictDialog(
        isSameFileOnRemote: Boolean,
        operation: UploadFileOperation,
        result: RemoteOperationResult<Any?>,
        notifyOnSameFileExists: suspend () -> Unit
    ): Boolean {
        if (result.isSuccess ||
            result.isCancelled ||
            result.code == ResultCode.USER_CANCELLED ||
            operation.isMissingPermissionThrown
        ) {
            Log_OC.w(TAG, "operation is successful, cancelled or lack of storage permission")
            return false
        }

        if (result.code == ResultCode.SYNC_CONFLICT && isSameFileOnRemote) {
            Log_OC.w(TAG, "same file exists on remote")
            notifyOnSameFileExists()
            return false
        }

        val delayedCodes =
            setOf(ResultCode.DELAYED_FOR_WIFI, ResultCode.DELAYED_FOR_CHARGING, ResultCode.DELAYED_IN_POWER_SAVE_MODE)
        val invalidCodes = setOf(ResultCode.LOCAL_FILE_NOT_FOUND, ResultCode.LOCK_FAILED)

        return result.code !in delayedCodes && result.code !in invalidCodes
    }
}
