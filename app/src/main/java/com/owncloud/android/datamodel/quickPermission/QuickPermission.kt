/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.owncloud.android.datamodel.quickPermission

data class QuickPermission(val type: QuickPermissionType, val iconId: Int, val textId: Int, var isSelected: Boolean) {
    companion object {
        fun getPermissionModel(isFolder: Boolean, selectedType: QuickPermissionType): List<QuickPermission> {
            val result = getFolderSharePermissions().toMutableList()
            if (!isFolder) {
                result.removeAt(FILE_REQUEST_INDEX)
            }
            result.find { it.type == selectedType }?.isSelected = true
            return result
        }

        private fun getFolderSharePermissions() = QuickPermissionType.getPermissions()

        private const val FILE_REQUEST_INDEX = 2
    }
}
