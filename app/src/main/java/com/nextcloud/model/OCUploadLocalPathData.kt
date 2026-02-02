/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.model

import com.nextcloud.client.account.User
import com.nextcloud.client.jobs.upload.FileUploadWorker
import com.owncloud.android.datamodel.UploadsStorageManager.UploadStatus
import com.owncloud.android.db.OCUpload
import com.owncloud.android.files.services.NameCollisionPolicy
import com.owncloud.android.operations.UploadFileOperation

data class OCUploadLocalPathData(
    val user: User,
    val localPaths: Array<String>,
    val remotePaths: Array<String>,
    val localBehavior: Int,
    val createRemoteFolder: Boolean,
    val createdBy: Int,
    val requiresWifi: Boolean,
    val requiresCharging: Boolean,
    val nameCollisionPolicy: NameCollisionPolicy,
) {
    companion object {
        fun forDocument(
            user: User,
            localPaths: Array<String>,
            remotePaths: Array<String>
        ): OCUploadLocalPathData {
            return OCUploadLocalPathData(
                user,
                localPaths,
                remotePaths,
                FileUploadWorker.LOCAL_BEHAVIOUR_DELETE,
                createRemoteFolder = true,
                UploadFileOperation.CREATED_BY_USER,
                requiresWifi = false,
                requiresCharging = false,
                NameCollisionPolicy.ASK_USER
            )
        }

        fun forAlbum(
            user: User,
            localPaths: Array<String>,
            remotePaths: Array<String>,
            localBehavior: Int
        ): OCUploadLocalPathData {
            return OCUploadLocalPathData(
                user,
                localPaths,
                remotePaths,
                localBehavior,
                createRemoteFolder = true,
                UploadFileOperation.CREATED_BY_USER,
                requiresWifi = false,
                requiresCharging = false,
                NameCollisionPolicy.RENAME
            )
        }

        @JvmOverloads
        fun forFile(
            user: User,
            localPaths: Array<String>,
            remotePaths: Array<String>,
            localBehavior: Int,
            createRemoteFolder: Boolean = false
        ): OCUploadLocalPathData {
            return OCUploadLocalPathData(
                user,
                localPaths,
                remotePaths,
                localBehavior,
                createRemoteFolder = createRemoteFolder,
                UploadFileOperation.CREATED_BY_USER,
                requiresWifi = false,
                requiresCharging = false,
                NameCollisionPolicy.ASK_USER
            )
        }
    }

    fun toOCUpload(localPath: String, index: Int): OCUpload {
        return OCUpload(localPath, remotePaths[index], user.accountName).apply {
            this.nameCollisionPolicy = nameCollisionPolicy
            isUseWifiOnly = requiresWifi
            isWhileChargingOnly = requiresCharging
            uploadStatus = UploadStatus.UPLOAD_IN_PROGRESS
            this.createdBy = createdBy
            isCreateRemoteFolder = createRemoteFolder
            localAction = localBehavior
        }
    }
}
