/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.utils.extensions

import android.content.Context
import com.nextcloud.client.preferences.AppPreferences
import com.owncloud.android.R
import com.owncloud.android.datamodel.UploadsStorageManager
import com.owncloud.android.db.OCUpload
import com.owncloud.android.db.UploadResult
import com.owncloud.android.files.services.NameCollisionPolicy

fun List<OCUpload>.getUploadIds(): LongArray = map { it.uploadId }.toLongArray()

fun Array<OCUpload>.getUploadIds(): LongArray = map { it.uploadId }.toLongArray()

fun List<OCUpload>.sortedByUploadOrder(): List<OCUpload> = sortedWith(
    compareBy<OCUpload> { it.fixedUploadStatus }
        .thenByDescending { it.isFixedUploadingNow }
        .thenByDescending { it.fixedUploadEndTimeStamp }
        .thenBy { it.fixedUploadId }
)

fun OCUpload.getStatusText(activity: Context, isGlobalUploadPaused: Boolean, isUploading: Boolean): String {
    val status: String
    val res = activity.resources
    when (val uploadStatus = uploadStatus) {
        UploadsStorageManager.UploadStatus.UPLOAD_IN_PROGRESS -> {
            status = if (isGlobalUploadPaused) {
                res.getString(R.string.upload_global_pause_title)
            } else if (isUploading) {
                res.getString(R.string.uploader_upload_in_progress_ticker)
            } else {
                res.getString(R.string.uploads_view_later_waiting_to_upload)
            }
        }

        UploadsStorageManager.UploadStatus.UPLOAD_SUCCEEDED -> {
            status = if (lastResult == UploadResult.SAME_FILE_CONFLICT) {
                res.getString(R.string.uploads_view_upload_status_succeeded_same_file)
            } else if (lastResult == UploadResult.FILE_NOT_FOUND) {
                lastResult.getFailedStatusText(activity)
            } else if (nameCollisionPolicy == NameCollisionPolicy.SKIP) {
                res.getString(R.string.uploads_view_upload_status_skip_reason)
            } else {
                res.getString(R.string.uploads_view_upload_status_succeeded)
            }
        }

        UploadsStorageManager.UploadStatus.UPLOAD_FAILED ->
            status =
                lastResult.getFailedStatusText(activity)

        UploadsStorageManager.UploadStatus.UPLOAD_CANCELLED ->
            status =
                res.getString(R.string.upload_manually_cancelled)

        else -> status = "Uncontrolled status: $uploadStatus"
    }

    return status
}
