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

fun NavigationView.getSelectedMenuItemId(): Int {
    menu.forEach {
        if (it.isChecked) {
            return it.itemId
        }
    }
    return Menu.NONE
}

fun NavigationView.unsetAllNavigationItems() {
    uncheckMenu(menu)
}

fun BottomNavigationView.unsetAllNavigationItems() {
    uncheckMenu(menu)
}

private fun uncheckMenu(menu: Menu) {
    menu.forEach { item ->
        item.isChecked = false

        // recursively uncheck submenu items
        item.subMenu?.let { uncheckMenu(it) }
    }
}
