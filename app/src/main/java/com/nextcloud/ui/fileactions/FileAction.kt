/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2022 Álvaro Brey <alvaro@alvarobrey.com>
 * SPDX-FileCopyrightText: 2022 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.nextcloud.ui.fileactions

import androidx.annotation.DrawableRes
import androidx.annotation.IdRes
import androidx.annotation.StringRes
import com.owncloud.android.R

enum class FileAction(@IdRes val id: Int, @StringRes val title: Int, @DrawableRes val icon: Int? = null) {
    // selection
    SELECT_ALL(R.id.action_select_all_action_menu, R.string.select_all, R.drawable.ic_select_all),
    SELECT_NONE(R.id.action_deselect_all_action_menu, R.string.deselect_all, R.drawable.ic_select_none),

    // generic file actions
    EDIT(R.id.action_edit, R.string.action_edit, R.drawable.ic_edit),
    SEE_DETAILS(R.id.action_see_details, R.string.actionbar_see_details, R.drawable.ic_information_outline),
    REMOVE_FILE(R.id.action_remove_file, R.string.common_remove, R.drawable.ic_delete),

    // File moving
    RENAME_FILE(R.id.action_rename_file, R.string.common_rename, R.drawable.ic_rename),
    MOVE_OR_COPY(R.id.action_move_or_copy, R.string.actionbar_move_or_copy, R.drawable.ic_external),

    // favorites
    FAVORITE(R.id.action_favorite, R.string.favorite, R.drawable.ic_star),
    UNSET_FAVORITE(R.id.action_unset_favorite, R.string.unset_favorite, R.drawable.ic_star_outline),

    // Uploads and downloads
    DOWNLOAD_FILE(R.id.action_download_file, R.string.filedetails_download, R.drawable.ic_cloud_download),
    SYNC_FILE(R.id.action_sync_file, R.string.filedetails_sync_file, R.drawable.ic_cloud_sync_on),
    CANCEL_SYNC(R.id.action_cancel_sync, R.string.common_cancel_sync, R.drawable.ic_cloud_sync_off),

    // File sharing
    EXPORT_FILE(R.id.action_export_file, R.string.filedetails_export, R.drawable.ic_export),
    SEND_SHARE_FILE(R.id.action_send_share_file, R.string.action_send_share, R.drawable.ic_share),
    SEND_FILE(R.id.action_send_file, R.string.common_send, R.drawable.ic_share),
    OPEN_FILE_WITH(R.id.action_open_file_with, R.string.actionbar_open_with, R.drawable.ic_external),
    STREAM_MEDIA(R.id.action_stream_media, R.string.stream, R.drawable.ic_play_arrow),
    SET_AS_WALLPAPER(R.id.action_set_as_wallpaper, R.string.set_picture_as, R.drawable.ic_wallpaper),

    // Encryption
    SET_ENCRYPTED(R.id.action_encrypted, R.string.encrypted, R.drawable.ic_encrypt),
    UNSET_ENCRYPTED(R.id.action_unset_encrypted, R.string.unset_encrypted, R.drawable.ic_decrypt),

    // locks
    UNLOCK_FILE(R.id.action_unlock_file, R.string.unlock_file, R.drawable.ic_lock_open_white),
    LOCK_FILE(R.id.action_lock_file, R.string.lock_file, R.drawable.ic_lock),

    // Shortcuts
    PIN_TO_HOMESCREEN(R.id.action_pin_to_homescreen, R.string.pin_home, R.drawable.add_to_home_screen),

    // Retry for offline operation
    RETRY(R.id.action_retry, R.string.retry, R.drawable.ic_retry);

    companion object {
        /**
         * All file actions, in the order they should be displayed
         */
        @JvmField
        val SORTED_VALUES = listOf(
            UNLOCK_FILE,
            EDIT,
            FAVORITE,
            UNSET_FAVORITE,
            SEE_DETAILS,
            LOCK_FILE,
            RENAME_FILE,
            MOVE_OR_COPY,
            DOWNLOAD_FILE,
            EXPORT_FILE,
            STREAM_MEDIA,
            SEND_SHARE_FILE,
            SEND_FILE,
            OPEN_FILE_WITH,
            SYNC_FILE,
            CANCEL_SYNC,
            SELECT_ALL,
            SELECT_NONE,
            SET_ENCRYPTED,
            UNSET_ENCRYPTED,
            SET_AS_WALLPAPER,
            REMOVE_FILE,
            PIN_TO_HOMESCREEN,
            RETRY
        )
    }
}
