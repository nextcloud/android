/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 TSI-mc <surinder.kumar@t-systems.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.ui.albumItemActions

import com.owncloud.android.R

enum class AlbumItemAction(val id: Int, val titleId: Int, val iconId: Int) {
    UPLOAD_FROM_CAMERA_ROLL(
        R.id.action_upload_from_camera_roll,
        R.string.upload_direct_camera_upload,
        R.drawable.ic_camera
    ),
    SELECT_IMAGES_FROM_ACCOUNT(
        R.id.action_select_images_from_account,
        R.string.album_upload_from_account,
        R.drawable.file_image
    ),
    RENAME_ALBUM(R.id.action_rename_file, R.string.album_rename, R.drawable.ic_edit),
    DELETE_ALBUM(R.id.action_delete, R.string.album_delete, R.drawable.ic_delete);

    companion object {
        @JvmField
        val SORTED_VALUES = listOf(
            UPLOAD_FROM_CAMERA_ROLL,
            SELECT_IMAGES_FROM_ACCOUNT,
            RENAME_ALBUM,
            DELETE_ALBUM
        )
    }
}
