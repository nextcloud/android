/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.utils.extensions

import android.view.Menu
import androidx.core.view.forEach
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.navigation.NavigationView
import com.owncloud.android.R

fun NavigationView.getSelectedMenuItemId(): Int {
    menu.forEach {
        if (it.isChecked) {
            return it.itemId
        }
    }
    return Menu.NONE
}

fun highlightNavigationView(
    drawerNavigationView: NavigationView?,
    bottomNavigationView: BottomNavigationView?,
    menuItemId: Int
) {
    drawerNavigationView?.setCheckedItem(menuItemId)

    bottomNavigationView?.let { bottomNav ->
        val bottomNavItems = setOf(R.id.nav_assistant, R.id.nav_all_files, R.id.nav_favorites, R.id.nav_gallery)
        val menuItem = bottomNav.menu.findItem(menuItemId)

        // Uncheck previous item that exists in drawer if this ID doesn't belong to bottom nav
        if (menuItemId !in bottomNavItems) {
            bottomNav.menu.findItem(bottomNav.selectedItemId)?.isChecked = false
        }

        // Highlight new item, skip assistant because Assistant screen doesn't have same bottom navigation bar
        if (menuItem != null && menuItem.itemId != R.id.nav_assistant) {
            menuItem.isChecked = true
        }
    }
}
