/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2022 Álvaro Brey <alvaro@alvarobrey.com>
 * SPDX-FileCopyrightText: 2022 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.nextcloud.utils

import android.view.Menu
import android.view.MenuItem
import androidx.core.view.children

object MenuUtils {

    @JvmStatic
    fun showMenuItem(item: MenuItem?) {
        item?.apply {
            isVisible = true
            isEnabled = true
        }
    }

    @JvmStatic
    fun hideMenuItem(item: MenuItem?) {
        item?.apply {
            isVisible = false
            isEnabled = false
        }
    }

    @JvmStatic
    fun hideAll(menu: Menu?) {
        menu?.children?.forEach(::hideMenuItem)
    }

    @JvmStatic
    fun hideMenuItems(vararg items: MenuItem?) {
        items.filterNotNull().forEach(::hideMenuItem)
    }
}
