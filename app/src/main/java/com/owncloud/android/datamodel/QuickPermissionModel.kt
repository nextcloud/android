/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.owncloud.android.datamodel

import android.content.Context
import com.owncloud.android.R

data class QuickPermissionModel(val iconId: Int, val textId: Int, var isSelected: Boolean) {
    companion object {
        fun getPermissionModel(isFolder: Boolean): List<QuickPermissionModel> =
            if (isFolder) folderSharePermissions else fileSharePermissions

        fun getPermissionArray(permissions: List<QuickPermissionModel>, context: Context): Array<String> =
            permissions.map { context.getString(it.textId) }.toTypedArray()

        private val folderSharePermissions = listOf(
            QuickPermissionModel(
                iconId = R.drawable.ic_eye,
                textId = R.string.link_share_view_only,
                isSelected = false
            ),
            QuickPermissionModel(
                iconId = R.drawable.ic_edit,
                textId = R.string.share_permission_can_edit,
                isSelected = false
            ),
            QuickPermissionModel(
                iconId = R.drawable.ic_file_request,
                textId = R.string.link_share_file_request,
                isSelected = false
            ),
            QuickPermissionModel(
                iconId = R.drawable.ic_custom_permissions,
                textId = R.string.share_custom_permission,
                isSelected = false
            )
        )

        private const val FILE_REQUEST_INDEX = 2

        private val fileSharePermissions =
            folderSharePermissions.filterIndexed { index, _ -> index != FILE_REQUEST_INDEX }
    }
}
