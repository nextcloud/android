/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.owncloud.android.ui.fragment.util

import com.owncloud.android.datamodel.quickPermission.QuickPermissionType
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.lib.resources.shares.OCShare
import com.owncloud.android.lib.resources.shares.attributes.ShareAttributes
import com.owncloud.android.lib.resources.shares.attributes.ShareAttributesJsonHandler
import com.owncloud.android.ui.fragment.FileDetailsSharingProcessFragment.Companion.TAG
import org.json.JSONArray
import org.json.JSONObject

object SharePermissionManager {

    // region Permission change
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
    // endregion

    // region Permission check
    fun hasPermission(permission: Int, permissionFlag: Int): Boolean {
        return permission != OCShare.NO_PERMISSION && (permission and permissionFlag) == permissionFlag
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
    // endregion

    // region DownloadAttribute
    fun toggleAllowDownloadAndSync(
        isChecked: Boolean,
        useV2DownloadAttributes: Boolean,
        share: OCShare?
    ): String? {
        val jsonArray = if (share?.attributes.isNullOrEmpty()) {
            JSONArray()
        } else {
            JSONArray(share.attributes)
        }

        val downloadAttr = getDownloadPermissionAttribute(jsonArray)

        if (downloadAttr != null) {
            if (useV2DownloadAttributes) {
                downloadAttr.put("value", isChecked)
            } else {
                downloadAttr.put("enabled", isChecked)
            }
        } else {
            val newAttr = JSONObject().apply {
                put("key", "download")
                put("scope", "permissions")
                if (useV2DownloadAttributes) {
                    put("value", isChecked)
                } else {
                    put("enabled", isChecked)
                }
            }
            jsonArray.put(newAttr)
        }

        return jsonArray.toString()
    }


    private fun getDownloadPermissionAttribute(jsonArray: JSONArray): JSONObject? {
        for (i in 0 until jsonArray.length()) {
            val obj = jsonArray.getJSONObject(i)
            if (obj.optString("key") == "download" && obj.optString("scope") == "permissions") {
                return obj
            }
        }
        return null
    }

    fun isAllowDownloadAndSyncEnabled(attributes: String?, useV2DownloadAttributes: Boolean): Boolean {
        if (attributes.isNullOrEmpty()) return false

        val jsonArray = JSONArray(attributes)
        val downloadAttr = getDownloadPermissionAttribute(jsonArray)

        return if (useV2DownloadAttributes) {
            downloadAttr?.optBoolean("value", false) ?: false
        } else {
            downloadAttr?.optBoolean("enabled", false) ?: false
        }
    }

    private fun getShareDownloadAttributes(share: OCShare?): List<ShareAttributes>? {
        return share?.attributes?.let { ShareAttributesJsonHandler.toList(it) }
    }
    // endregion

    // region Helper Methods
    fun canEdit(share: OCShare?): Boolean {
        if (share == null) {
            return false
        }

        return hasPermission(share.permissions, getMaximumPermission(share.isFolder))
    }

    fun isViewOnly(share: OCShare?): Boolean {
        return share?.permissions != OCShare.NO_PERMISSION && share?.permissions == OCShare.READ_PERMISSION_FLAG
    }

    fun isFileRequest(share: OCShare?): Boolean {
        if (share?.isFolder == false) {
            return false
        }

        return share?.permissions != OCShare.NO_PERMISSION && share?.permissions == OCShare.CREATE_PERMISSION_FLAG
    }

    fun isSecureFileDrop(share: OCShare?): Boolean {
        if (share == null) {
            return false
        }

        return hasPermission(share.permissions, OCShare.CREATE_PERMISSION_FLAG + OCShare.READ_PERMISSION_FLAG)
    }

    fun canReshare(share: OCShare?): Boolean {
        if (share == null) {
            return false
        }

        return (share.permissions and OCShare.Companion.SHARE_PERMISSION_FLAG) > 0
    }

    fun getSelectedType(share: OCShare?, encrypted: Boolean): QuickPermissionType {
        return if (canEdit(share)) {
            QuickPermissionType.CAN_EDIT
        } else if (encrypted && isSecureFileDrop(share)) {
            QuickPermissionType.SECURE_FILE_DROP
        } else if (isFileRequest(share)) {
            QuickPermissionType.FILE_REQUEST
        } else if (isViewOnly(share)) {
            QuickPermissionType.VIEW_ONLY
        } else if (isCustomPermission(share)) {
            QuickPermissionType.CUSTOM_PERMISSIONS
        } else {
            QuickPermissionType.NONE
        }
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
    // endregion
}
