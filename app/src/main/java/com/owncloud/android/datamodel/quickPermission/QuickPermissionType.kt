/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.owncloud.android.datamodel.quickPermission

import com.owncloud.android.R

enum class QuickPermissionType(
    val iconId: Int,
    val textId: Int
) {
    NONE(R.drawable.ic_unknown, R.string.unknown),
    VIEW_ONLY(R.drawable.ic_eye, R.string.link_share_view_only),
    CAN_EDIT(R.drawable.ic_edit, R.string.share_permission_can_edit),
    FILE_REQUEST(R.drawable.ic_file_request, R.string.link_share_file_request),
    CUSTOM_PERMISSIONS(R.drawable.ic_custom_permissions, R.string.share_custom_permission);

    companion object {
        fun getAvailablePermissions(isFolder: Boolean): List<QuickPermissionType> {
            val permissions = listOf(VIEW_ONLY, CAN_EDIT, FILE_REQUEST, CUSTOM_PERMISSIONS)
            return if (isFolder) permissions else permissions.filter { it != FILE_REQUEST }
        }
    }
}
