/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.owncloud.android.datamodel.quickPermission

import android.content.Context
import android.graphics.drawable.Drawable
import androidx.core.content.ContextCompat
import com.owncloud.android.R
import com.owncloud.android.lib.resources.shares.OCShare

enum class QuickPermissionType(val iconId: Int, val textId: Int) {
    NONE(R.drawable.ic_unknown, R.string.unknown),
    VIEW_ONLY(R.drawable.ic_eye, R.string.share_permission_view_only),
    CAN_EDIT(R.drawable.ic_edit, R.string.share_permission_can_edit),
    FILE_REQUEST(R.drawable.ic_file_request, R.string.share_permission_file_request),
    SECURE_FILE_DROP(R.drawable.ic_file_request, R.string.share_permission_secure_file_drop),
    CUSTOM_PERMISSIONS(R.drawable.ic_custom_permissions, R.string.share_custom_permission);

    fun getText(context: Context): String = context.getString(textId)

    fun getIcon(context: Context): Drawable? = ContextCompat.getDrawable(context, iconId)

    fun getPermissionFlag(isFolder: Boolean): Int = when (this) {
        NONE -> OCShare.NO_PERMISSION
        VIEW_ONLY -> OCShare.READ_PERMISSION_FLAG
        CAN_EDIT -> if (isFolder) OCShare.MAXIMUM_PERMISSIONS_FOR_FOLDER else OCShare.MAXIMUM_PERMISSIONS_FOR_FILE
        FILE_REQUEST -> OCShare.CREATE_PERMISSION_FLAG
        SECURE_FILE_DROP -> OCShare.CREATE_PERMISSION_FLAG + OCShare.READ_PERMISSION_FLAG
        else -> {
            // Custom permission's flag can't be determined
            OCShare.NO_PERMISSION
        }
    }

    fun getAvailablePermissions(hasFileRequestPermission: Boolean): List<QuickPermission> {
        val permissions = listOf(VIEW_ONLY, CAN_EDIT, FILE_REQUEST, CUSTOM_PERMISSIONS)
        val result = if (hasFileRequestPermission) permissions else permissions.filter { it != FILE_REQUEST }

        return result.map { type ->
            QuickPermission(
                type = type,
                isSelected = (type == this)
            )
        }
    }
}
