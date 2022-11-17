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
