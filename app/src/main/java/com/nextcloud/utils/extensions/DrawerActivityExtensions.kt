/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.utils.extensions

import android.content.Intent
import android.view.Menu
import androidx.core.view.forEach
import com.owncloud.android.MainApp
import com.owncloud.android.R
import com.owncloud.android.ui.activity.DrawerActivity
import com.owncloud.android.ui.activity.FileDisplayActivity
import androidx.core.view.get
import androidx.core.view.size

fun DrawerActivity.navigateToAllFiles() {
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
}

fun DrawerActivity.unsetAllNavigationItems() {
    fun uncheckMenu(menu: Menu) {
        menu.forEach { item ->
            item.isChecked = false

            // recursively uncheck submenu items
            item.subMenu?.let { uncheckMenu(it) }
        }
    }

    drawerNavigationView?.menu?.let { uncheckMenu(it) }
    bottomNavigationView?.menu?.let { uncheckMenu(it) }
}
