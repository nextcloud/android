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
    val creationType: Int,
    val requiresWifi: Boolean,
    val requiresCharging: Boolean,
    val collisionPolicy: NameCollisionPolicy
) {
    companion object {
        fun forDocument(user: User, localPaths: Array<String>, remotePaths: Array<String>): OCUploadLocalPathData =
            OCUploadLocalPathData(
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

        fun forAlbum(
            user: User,
            localPaths: Array<String>,
            remotePaths: Array<String>,
            localBehavior: Int
        ): OCUploadLocalPathData = OCUploadLocalPathData(
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

        @JvmOverloads
        fun forFile(
            user: User,
            localPaths: Array<String>,
            remotePaths: Array<String>,
            localBehavior: Int,
            createRemoteFolder: Boolean = false
        ): OCUploadLocalPathData = OCUploadLocalPathData(
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

    fun toOCUpload(localPath: String, index: Int): OCUpload =
        OCUpload(localPath, remotePaths[index], user.accountName).apply {
            nameCollisionPolicy = collisionPolicy
            isUseWifiOnly = requiresWifi
            isWhileChargingOnly = requiresCharging
            uploadStatus = UploadStatus.UPLOAD_IN_PROGRESS
            createdBy = creationType
            isCreateRemoteFolder = createRemoteFolder
            localAction = localBehavior
        }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as OCUploadLocalPathData

        if (localBehavior != other.localBehavior) return false
        if (createRemoteFolder != other.createRemoteFolder) return false
        if (creationType != other.creationType) return false
        if (requiresWifi != other.requiresWifi) return false
        if (requiresCharging != other.requiresCharging) return false
        if (user != other.user) return false
        if (!localPaths.contentEquals(other.localPaths)) return false
        if (!remotePaths.contentEquals(other.remotePaths)) return false
        if (collisionPolicy != other.collisionPolicy) return false

        return true
    }

    override fun hashCode(): Int {
        var result = localBehavior
        result = 31 * result + createRemoteFolder.hashCode()
        result = 31 * result + creationType
        result = 31 * result + requiresWifi.hashCode()
        result = 31 * result + requiresCharging.hashCode()
        result = 31 * result + user.hashCode()
        result = 31 * result + localPaths.contentHashCode()
        result = 31 * result + remotePaths.contentHashCode()
        result = 31 * result + collisionPolicy.hashCode()
        return result
    }
}
