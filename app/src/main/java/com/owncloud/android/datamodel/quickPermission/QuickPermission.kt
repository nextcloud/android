/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.owncloud.android.datamodel.quickPermission

data class QuickPermission(val type: QuickPermissionType, var isSelected: Boolean) {
    companion object {
        fun getPermissions(isFolder: Boolean, selectedType: QuickPermissionType): List<QuickPermission> {
            return QuickPermissionType.getAvailablePermissions(isFolder).map { type ->
                QuickPermission(
                    type = type,
                    isSelected = type == selectedType
                )
            }
        }
    }
}
