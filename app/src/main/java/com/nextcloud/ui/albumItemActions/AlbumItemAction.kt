/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 TSI-mc <surinder.kumar@t-systems.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.ui.albumItemActions

import androidx.annotation.DrawableRes
import androidx.annotation.IdRes
import androidx.annotation.StringRes
import com.owncloud.android.R

enum class AlbumItemAction(@IdRes val id: Int, @StringRes val title: Int, @DrawableRes val icon: Int? = null) {
    RENAME_ALBUM(R.id.action_rename_file, R.string.album_rename, R.drawable.ic_edit),
    DELETE_ALBUM(R.id.action_delete, R.string.album_delete, R.drawable.ic_delete);

    companion object {
        /**
         * All file actions, in the order they should be displayed
         */
        @JvmField
        val SORTED_VALUES = listOf(
            RENAME_ALBUM,
            DELETE_ALBUM
        )
    }
}
