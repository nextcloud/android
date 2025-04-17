/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.owncloud.android.datamodel.quickPermission

import com.owncloud.android.R

enum class QuickPermissionType {
    NONE, VIEW_ONLY, CAN_EDIT, FILE_REQUEST, CUSTOM_PERMISSIONS;

    companion object {
        fun getPermissions() = listOf(
            VIEW_ONLY.getPermission(),
            CAN_EDIT.getPermission(),
            FILE_REQUEST.getPermission(),
            CUSTOM_PERMISSIONS.getPermission()
        )
    }

    fun getPermission(): QuickPermission {
        return when(this) {
            VIEW_ONLY -> {
                QuickPermission(
                    type = this,
                    iconId = R.drawable.ic_eye,
                    textId = R.string.link_share_view_only,
                    isSelected = false
                )
            }
            CAN_EDIT -> {
                QuickPermission(
                    type = this,
                    iconId = R.drawable.ic_edit,
                    textId = R.string.share_permission_can_edit,
                    isSelected = false
                )
            }
            FILE_REQUEST -> {
                QuickPermission(
                    type = this,
                    iconId = R.drawable.ic_file_request,
                    textId = R.string.link_share_file_request,
                    isSelected = false
                )
            }
            CUSTOM_PERMISSIONS -> {
                QuickPermission(
                    type = this,
                    iconId = R.drawable.ic_custom_permissions,
                    textId = R.string.share_custom_permission,
                    isSelected = false
                )
            }
            else -> {
                QuickPermission(
                    type = this,
                    iconId = R.drawable.ic_unknown,
                    textId = R.string.unknown,
                    isSelected = false
                )
            }
        }
    }
}
