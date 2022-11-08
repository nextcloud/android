/*
 * Nextcloud Android client application
 *
 *  @author Álvaro Brey
 *  Copyright (C) 2022 Álvaro Brey
 *  Copyright (C) 2022 Nextcloud GmbH
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU AFFERO GENERAL PUBLIC LICENSE
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU AFFERO GENERAL PUBLIC LICENSE for more details.
 *
 * You should have received a copy of the GNU Affero General Public
 * License along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package com.nextcloud.ui.fileactions

import androidx.annotation.DrawableRes
import androidx.annotation.IdRes
import androidx.annotation.StringRes
import com.owncloud.android.R

// TODO get rid of id, use enum value directly
enum class FileAction(@IdRes val id: Int, @StringRes val title: Int, @DrawableRes val icon: Int? = null) {
    UNLOCK_FILE(R.id.action_unlock_file, R.string.unlock_file),
    EDIT(R.id.action_edit, R.string.action_edit),
    FAVORITE(R.id.action_favorite, R.string.favorite),
    UNSET_FAVORITE(R.id.action_unset_favorite, R.string.unset_favorite),
    SEE_DETAILS(R.id.action_see_details, R.string.actionbar_see_details),
    LOCK_FILE(R.id.action_lock_file, R.string.lock_file),
    RENAME_FILE(R.id.action_rename_file, R.string.common_rename),
    MOVE(R.id.action_move, R.string.actionbar_move),
    COPY(R.id.action_copy, R.string.actionbar_copy),
    DOWNLOAD_FILE(R.id.action_download_file, R.string.filedetails_download),
    EXPORT_FILE(R.id.action_export_file, R.string.filedetails_export),
    STREAM_MEDIA(R.id.action_stream_media, R.string.stream),
    SEND_SHARE_FILE(R.id.action_send_share_file, R.string.action_send_share),
    SEND_FILE(R.id.action_send_file, R.string.common_send),
    OPEN_FILE_WITH(R.id.action_open_file_with, R.string.actionbar_open_with),
    SYNC_FILE(R.id.action_sync_file, R.string.filedetails_sync_file),
    CANCEL_SYNC(R.id.action_cancel_sync, R.string.common_cancel_sync),
    SELECT_ALL_ACTION_MENU(R.id.action_select_all_action_menu, R.string.select_all),
    DESELECT_ALL_ACTION_MENU(R.id.action_deselect_all_action_menu, R.string.deselect_all),
    ENCRYPTED(R.id.action_encrypted, R.string.encrypted),
    UNSET_ENCRYPTED(R.id.action_unset_encrypted, R.string.unset_encrypted),
    SET_AS_WALLPAPER(R.id.action_set_as_wallpaper, R.string.set_picture_as),
    REMOVE_FILE(R.id.action_remove_file, R.string.common_remove)
}
