/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2024 TSI-mc <surinder.kumar@t-systems.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.nextcloud.ui.trashbinFileActions

import androidx.annotation.DrawableRes
import androidx.annotation.IdRes
import androidx.annotation.StringRes
import com.owncloud.android.R

enum class TrashbinFileAction(@IdRes val id: Int, @StringRes val title: Int, @DrawableRes val icon: Int? = null) {
    DELETE_PERMANENTLY(R.id.action_delete, R.string.trashbin_file_remove, R.drawable.ic_delete),
    RESTORE(R.id.restore, R.string.restore_item, R.drawable.ic_history),
    SELECT_ALL(R.id.action_select_all_action_menu, R.string.select_all, R.drawable.ic_select_all),
    SELECT_NONE(R.id.action_deselect_all_action_menu, R.string.deselect_all, R.drawable.ic_select_none);

    companion object {
        /**
         * All file actions, in the order they should be displayed
         */
        @JvmField
        val SORTED_VALUES = listOf(
            DELETE_PERMANENTLY,
            RESTORE,
            SELECT_ALL,
            SELECT_NONE
        )
    }
}
