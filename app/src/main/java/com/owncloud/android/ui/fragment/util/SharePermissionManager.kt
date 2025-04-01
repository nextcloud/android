/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.owncloud.android.ui.fragment.util

import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.lib.resources.shares.OCShare
import com.owncloud.android.ui.fragment.FileDetailsSharingProcessFragment.Companion.TAG

class SharePermissionManager {

    fun togglePermission(permission: Int, permissionFlag: Int): Int {
        Log_OC.d(TAG, "togglePermission before: $permission")

        if (!isPermissionValid(permission)) {
            Log_OC.d(TAG, "permission is not valid, togglePermission cancelled")
            return permission
        }

        val result = if (hasPermission(permission, permissionFlag)) {
            permission and permissionFlag.inv()
        } else {
            permission or permissionFlag
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
}
