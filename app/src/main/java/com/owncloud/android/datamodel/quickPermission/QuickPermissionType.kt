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

enum class QuickPermissionType(
    val iconId: Int,
    val textId: Int
) {
    //TODO: R.string.unknown resource referenced from osmdroid library. Ideally it should be part of this app resource.
    NONE(R.drawable.ic_unknown, org.osmdroid.library.R.string.unknown),
    VIEW_ONLY(R.drawable.ic_eye, R.string.share_permission_view_only),
    CAN_EDIT(R.drawable.ic_edit, R.string.share_permission_can_edit),
    FILE_REQUEST(R.drawable.ic_file_request, R.string.share_permission_file_request),
    SECURE_FILE_DROP(R.drawable.ic_file_request, R.string.share_permission_secure_file_drop),
    CUSTOM_PERMISSIONS(R.drawable.ic_custom_permissions, R.string.share_custom_permission);

    fun getText(context: Context): String = context.getString(textId)

    fun getIcon(context: Context): Drawable? = ContextCompat.getDrawable(context, iconId)

    companion object {
        fun getAvailablePermissions(
            hasFileRequestPermission: Boolean,
            selectedType: QuickPermissionType
        ): List<QuickPermission> {
            val permissions = listOf(VIEW_ONLY, CAN_EDIT, FILE_REQUEST, CUSTOM_PERMISSIONS)
            val result = if (hasFileRequestPermission) permissions else permissions.filter { it != FILE_REQUEST }

            return result.map { type ->
                QuickPermission(
                    type = type,
                    isSelected = (type == selectedType)
                )
            }
        }
    }
}
