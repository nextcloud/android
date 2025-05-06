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

data class QuickPermission(val type: QuickPermissionType, var isSelected: Boolean) {
    companion object {
        fun getPermissions(
            hasFileRequestPermission: Boolean,
            selectedType: QuickPermissionType
        ): List<QuickPermission> {
            return QuickPermissionType.getAvailablePermissions(hasFileRequestPermission).map { type ->
                QuickPermission(
                    type = type,
                    isSelected = (type == selectedType)
                )
            }
        }
    }

    fun getText(context: Context): String = context.getString(type.textId)
    fun getIcon(context: Context): Drawable? = ContextCompat.getDrawable(context, type.iconId)
}
