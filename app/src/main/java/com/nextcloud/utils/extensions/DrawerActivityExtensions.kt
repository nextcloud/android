/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2024 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.utils.extensions

import android.content.Intent
import com.owncloud.android.MainApp
import com.owncloud.android.R
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.ui.activity.DrawerActivity
import com.owncloud.android.ui.activity.FileDisplayActivity

/**
 * Determines the appropriate menu item ID based on the current ActionBar title.
 *
 * This function serves as a workaround solution because not all drawer menu item
 * navigations extend from DrawerActivity and back button changes content but not the drawer menu item.
 * As a result, the content and highlighted
 * menu item may not always match. This function helps maintain consistency between
 * the displayed content and the highlighted menu item.
 *
 * @return The menu item ID corresponding to the current ActionBar title, or null if
 *         the ActionBar is not available.
 */
fun DrawerActivity.getMenuItemIdFromTitle(): Int? {
    val actionBar = supportActionBar ?: return null

    return when (actionBar.title.toString()) {
        getString(R.string.drawer_item_all_files) -> R.id.nav_all_files
        getString(R.string.drawer_item_personal_files) -> R.id.nav_personal_files
        getString(R.string.drawer_item_activities) -> R.id.nav_activity
        getString(R.string.drawer_item_favorites) -> R.id.nav_favorites
        getString(R.string.drawer_item_gallery) -> R.id.nav_gallery
        getString(R.string.drawer_item_shared) -> R.id.nav_shared
        getString(R.string.drawer_item_groupfolders) -> R.id.nav_groupfolders
        getString(R.string.drawer_item_on_device) -> R.id.nav_on_device
        getString(R.string.drawer_item_recently_modified) -> R.id.nav_recently_modified
        getString(R.string.drawer_item_assistant) -> R.id.nav_assistant
        getString(R.string.drawer_item_uploads_list) -> R.id.nav_uploads
        getString(R.string.drawer_item_trashbin) -> R.id.nav_trashbin
        else -> {
            if (MainApp.isOnlyPersonFiles()) {
                R.id.nav_personal_files
            } else if (MainApp.isOnlyOnDevice()) {
                R.id.nav_on_device
            } else {
                DrawerActivity.menuItemId
            }
        }
    }
}

fun DrawerActivity.handleBackButtonEvent(currentDir: OCFile): Boolean {
    if (DrawerActivity.menuItemId == R.id.nav_all_files && currentDir.isRootDirectory) {
        moveTaskToBack(true)
        return true
    }

    val isParentDirExists = (storageManager.getFileById(currentDir.parentId) != null)
    if (isParentDirExists) {
        return false
    }

    DrawerActivity.menuItemId = R.id.nav_all_files
    setNavigationViewItemChecked()

    MainApp.showOnlyFilesOnDevice(false)
    MainApp.showOnlyPersonalFiles(false)

    Intent(applicationContext, FileDisplayActivity::class.java).apply {
        addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        action = FileDisplayActivity.ALL_FILES
    }.run {
        startActivity(this)
    }

    return true
}
