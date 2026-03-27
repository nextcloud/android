/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.owncloud.android.ui.adapter.uploadList

import com.owncloud.android.R
import com.owncloud.android.datamodel.UploadsStorageManager
import com.owncloud.android.db.OCUpload
import com.owncloud.android.files.services.NameCollisionPolicy

data class UploadListSection(
    val type: UploadListType?,
    val titleRes: Int,
    val status: UploadsStorageManager.UploadStatus?,
    val collisionPolicy: NameCollisionPolicy?,
    val items: List<OCUpload>
) {
    fun withItems(newItems: List<OCUpload>) = copy(items = newItems)

    companion object {
        fun sections(): MutableList<UploadListSection> = mutableListOf(
            UploadListSection(
                UploadListType.CURRENT,
                R.string.uploads_view_group_current_uploads,
                UploadsStorageManager.UploadStatus.UPLOAD_IN_PROGRESS,
                null,
                listOf()
            ),
            UploadListSection(
                UploadListType.FAILED,
                R.string.uploads_view_group_failed_uploads,
                UploadsStorageManager.UploadStatus.UPLOAD_FAILED,
                null,
                listOf()
            ),
            UploadListSection(
                UploadListType.CANCELLED,
                R.string.uploads_view_group_manually_cancelled_uploads,
                UploadsStorageManager.UploadStatus.UPLOAD_CANCELLED,
                null,
                listOf()
            ),
            UploadListSection(
                UploadListType.COMPLETED,
                R.string.uploads_view_group_completed_uploads,
                UploadsStorageManager.UploadStatus.UPLOAD_SUCCEEDED,
                NameCollisionPolicy.ASK_USER,
                listOf()
            ),
            UploadListSection(
                UploadListType.SKIPPED,
                R.string.uploads_view_upload_status_skip,
                UploadsStorageManager.UploadStatus.UPLOAD_SUCCEEDED,
                NameCollisionPolicy.SKIP,
                listOf()
            )
        )
    }
}
