/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.utils.extensions

import android.content.Context
import com.owncloud.android.R
import com.owncloud.android.db.UploadResult

fun UploadResult.isNonRetryable(): Boolean = when (this) {
    UploadResult.FILE_NOT_FOUND,
    UploadResult.FILE_ERROR,
    UploadResult.FOLDER_ERROR,
    UploadResult.CANNOT_CREATE_FILE,
    UploadResult.SYNC_CONFLICT,
    UploadResult.CONFLICT_ERROR,
    UploadResult.SAME_FILE_CONFLICT,
    UploadResult.LOCAL_STORAGE_NOT_COPIED,
    UploadResult.VIRUS_DETECTED,
    UploadResult.QUOTA_EXCEEDED,
    UploadResult.PRIVILEGES_ERROR,
    UploadResult.CREDENTIAL_ERROR,

    // most cases covered and mapped from RemoteOperationResult. Most likely UploadResult.UNKNOWN this error will
    // occur again
    UploadResult.UNKNOWN,

    // user's choice
    UploadResult.CANCELLED -> true

    // everything else may succeed after retry
    else -> false
}

fun UploadResult.getFailedStatusText(context: Context): String = when (this) {
    UploadResult.CREDENTIAL_ERROR ->
        context.getString(R.string.uploads_view_upload_status_failed_credentials_error)

    UploadResult.FOLDER_ERROR ->
        context.getString(R.string.uploads_view_upload_status_failed_folder_error)

    UploadResult.FILE_NOT_FOUND ->
        context.getString(R.string.uploads_view_upload_status_failed_localfile_error)

    UploadResult.FILE_ERROR -> context.getString(R.string.uploads_view_upload_status_failed_file_error)

    UploadResult.PRIVILEGES_ERROR -> context.getString(
        R.string.uploads_view_upload_status_failed_permission_error
    )

    UploadResult.NETWORK_CONNECTION ->
        context.getString(R.string.uploads_view_upload_status_failed_connection_error)

    UploadResult.DELAYED_FOR_WIFI -> context.getString(
        R.string.uploads_view_upload_status_waiting_for_wifi
    )

    UploadResult.DELAYED_FOR_CHARGING ->
        context.getString(R.string.uploads_view_upload_status_waiting_for_charging)

    UploadResult.CONFLICT_ERROR -> context.getString(R.string.uploads_view_upload_status_conflict)

    UploadResult.SERVICE_INTERRUPTED -> context.getString(
        R.string.uploads_view_upload_status_service_interrupted
    )

    UploadResult.CANCELLED -> // should not get here ; cancelled uploads should be wiped out
        context.getString(R.string.uploads_view_upload_status_cancelled)

    UploadResult.UPLOADED -> // should not get here ; status should be UPLOAD_SUCCESS
        context.getString(R.string.uploads_view_upload_status_succeeded)

    UploadResult.MAINTENANCE_MODE -> context.getString(R.string.maintenance_mode)

    UploadResult.SSL_RECOVERABLE_PEER_UNVERIFIED -> context.getString(
        R.string.uploads_view_upload_status_failed_ssl_certificate_not_trusted
    )

    UploadResult.UNKNOWN -> context.getString(R.string.uploads_view_upload_status_unknown_fail)

    UploadResult.LOCK_FAILED -> context.getString(R.string.upload_lock_failed)

    UploadResult.DELAYED_IN_POWER_SAVE_MODE -> context.getString(
        R.string.uploads_view_upload_status_waiting_exit_power_save_mode
    )

    UploadResult.VIRUS_DETECTED -> context.getString(R.string.uploads_view_upload_status_virus_detected)

    UploadResult.LOCAL_STORAGE_FULL -> context.getString(R.string.upload_local_storage_full)

    UploadResult.OLD_ANDROID_API -> context.getString(R.string.upload_old_android)

    UploadResult.SYNC_CONFLICT -> context.getString(R.string.upload_sync_conflict_check)

    UploadResult.CANNOT_CREATE_FILE -> context.getString(R.string.upload_cannot_create_file)

    UploadResult.LOCAL_STORAGE_NOT_COPIED -> context.getString(R.string.upload_local_storage_not_copied)

    UploadResult.QUOTA_EXCEEDED -> context.getString(R.string.upload_quota_exceeded)

    else -> context.getString(R.string.upload_unknown_error)
}
