/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.model

import android.view.Menu
import com.owncloud.android.R

enum class ToolbarItem(val navId: Int, val titleId: Int, val style: ToolbarStyle) {
    NONE(Menu.NONE, R.string.drawer_item_all_files, ToolbarStyle.SEARCH),
    ALL_FILES(R.id.nav_all_files, R.string.drawer_item_all_files, ToolbarStyle.SEARCH),
    PERSONAL_FILES(R.id.nav_personal_files, R.string.drawer_item_personal_files, ToolbarStyle.SEARCH),
    ACTIVITIES(R.id.nav_activity, R.string.drawer_item_activities, ToolbarStyle.PLAIN),
    FAVORITES(R.id.nav_favorites, R.string.drawer_item_favorites, ToolbarStyle.PLAIN),
    GALLERY(R.id.nav_gallery, R.string.drawer_item_gallery, ToolbarStyle.PLAIN),
    SHARED(R.id.nav_shared, R.string.drawer_item_shared, ToolbarStyle.PLAIN),
    GROUP_FOLDERS(R.id.nav_groupfolders, R.string.drawer_item_groupfolders, ToolbarStyle.PLAIN),
    ON_DEVICE(R.id.nav_on_device, R.string.drawer_item_on_device, ToolbarStyle.PLAIN),
    RECENTLY_MODIFIED(R.id.nav_recently_modified, R.string.drawer_item_recently_modified, ToolbarStyle.PLAIN),
    ASSISTANT(R.id.nav_assistant, R.string.drawer_item_assistant, ToolbarStyle.PLAIN),
    UPLOADS(R.id.nav_uploads, R.string.drawer_item_uploads_list, ToolbarStyle.PLAIN),
    SETTINGS(R.id.nav_settings, R.string.actionbar_settings, ToolbarStyle.PLAIN),
    COMMUNITY(R.id.nav_community, R.string.drawer_community, ToolbarStyle.PLAIN),
    TRASHBIN(R.id.nav_trashbin, R.string.drawer_item_trashbin, ToolbarStyle.PLAIN);

    companion object {
        fun fromNavId(navId: Int): ToolbarItem? = entries.find { it.navId == navId }
    }
}

enum class ToolbarStyle {
    PLAIN,
    SEARCH
}
