/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.owncloud.android.ui.fragment.util

import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.lib.resources.shares.OCShare
import com.owncloud.android.lib.resources.shares.attributes.ShareAttributes
import com.owncloud.android.lib.resources.shares.attributes.ShareAttributesJsonHandler
import com.owncloud.android.lib.resources.shares.attributes.getDownloadAttribute
import com.owncloud.android.ui.fragment.FileDetailsSharingProcessFragment.Companion.TAG

class SharePermissionManager {

    fun togglePermission(isChecked: Boolean, permission: Int, permissionFlag: Int): Int {
        Log_OC.d(TAG, "togglePermission before: $permission")

        if (!isPermissionValid(permission)) {
            Log_OC.d(TAG, "permission is not valid, togglePermission cancelled")
            return permission
        }

        val result = if (isChecked) {
            permission or permissionFlag
        } else {
            permission and permissionFlag.inv()
        }

        Log_OC.d(TAG, "togglePermission after: $result")

        return result
    }

    @Suppress("ReturnCount")
    private fun isPermissionValid(permission: Int): Boolean {
        // Must have at least READ or CREATE permission.
        if (!hasPermission(permission, OCShare.READ_PERMISSION_FLAG) &&
            !hasPermission(permission, OCShare.CREATE_PERMISSION_FLAG)
        ) {
            return false
        }

        // Must have READ permission if have UPDATE or DELETE.
        if (!hasPermission(permission, OCShare.READ_PERMISSION_FLAG) &&
            (
                hasPermission(permission, OCShare.UPDATE_PERMISSION_FLAG) ||
                    hasPermission(permission, OCShare.DELETE_PERMISSION_FLAG)
                )
        ) {
            return false
        }

        return true
    }

    fun hasPermission(permission: Int, permissionFlag: Int): Boolean {
        return permission != OCShare.NO_PERMISSION && (permission and permissionFlag) == permissionFlag
    }

    @Suppress("ReturnCount")
    fun isCustomPermission(share: OCShare?): Boolean {
        if (share == null) return false
        val permissions = share.permissions
        if (permissions == OCShare.NO_PERMISSION) return false

        val hasRead = hasPermission(permissions, OCShare.READ_PERMISSION_FLAG)
        if (!hasRead) return false

        val hasCreate = hasPermission(permissions, OCShare.CREATE_PERMISSION_FLAG)
        val hasUpdate = hasPermission(permissions, OCShare.UPDATE_PERMISSION_FLAG)
        val hasDelete = hasPermission(permissions, OCShare.DELETE_PERMISSION_FLAG)
        val hasShare = hasPermission(permissions, OCShare.SHARE_PERMISSION_FLAG)

        return when {
            share.isFolder -> hasCreate || hasUpdate || hasDelete || hasShare
            else -> hasUpdate || hasShare
        }
    }

    fun getMaximumPermission(isFolder: Boolean): Int {
        return if (isFolder) {
            OCShare.MAXIMUM_PERMISSIONS_FOR_FOLDER
        } else {
            OCShare.MAXIMUM_PERMISSIONS_FOR_FILE
        }
    }

    fun toggleAllowDownloadAndSync(isChecked: Boolean, share: OCShare?): String? {
        val shareAttributes = getShareAttributes(share)?.toMutableList()
        if (shareAttributes == null) {
            val downloadAttribute = ShareAttributes.createDownloadAttributes(isChecked)
            val updatedShareAttributes = listOf(downloadAttribute)
            return ShareAttributesJsonHandler.toJson(updatedShareAttributes)
        }

        val downloadAttributeIndex = shareAttributes.indexOf(shareAttributes.getDownloadAttribute())
        if (downloadAttributeIndex >= 0) {
            val updatedAttribute = shareAttributes[downloadAttributeIndex].copy(value = isChecked)
            shareAttributes[downloadAttributeIndex] = updatedAttribute
        }

        return ShareAttributesJsonHandler.toJson(shareAttributes)
    }

    fun isAllowDownloadAndSyncEnabled(share: OCShare?): Boolean {
        return getShareAttributes(share).getDownloadAttribute()?.value == true
    }

    private fun getShareAttributes(share: OCShare?): List<ShareAttributes>? {
        return share?.attributes?.let { ShareAttributesJsonHandler.toList(it) }
    }
}
