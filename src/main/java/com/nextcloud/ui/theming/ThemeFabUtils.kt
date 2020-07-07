/*
 * Nextcloud Android client application
 *
 * @author Tobias Kaminsky
 * @author Andy Scherzinger
 * Copyright (C) 2017 Tobias Kaminsky
 * Copyright (C) 2017 Nextcloud GmbH
 * Copyright (C) 2018 Andy Scherzinger
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.nextcloud.ui.theming

import android.content.Context
import android.content.res.ColorStateList
import androidx.annotation.DrawableRes
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.owncloud.android.utils.ThemeUtils

/**
 * Utility class with methods for client side FAB theming.
 */
object ThemeFabUtils {
    /**
     * themes a given FAB including the FAB's icon based on the primary color.
     *
     * @param fab     the FAB to be themed
     * @param icon    the FAB's icon
     * @param context the context to load colors
     */
    @JvmStatic
    fun colorFloatingActionButton(fab: FloatingActionButton, @DrawableRes icon: Int, context: Context?) {
        colorFloatingActionButton(fab, context)
        fab.setImageDrawable(ThemeUtils.tintDrawable(icon, ColorsUtils.elementTextColor(context)))
    }

    /**
     * themes a given FAB.
     *
     * @param button          the FAB to be themed
     * @param context         the context to load colors
     * @param backgroundColor the FABs color to be used
     */
    @JvmStatic
    @JvmOverloads
    fun colorFloatingActionButton(fab: FloatingActionButton,
                                  context: Context?,
                                  backgroundColor: Int = ColorsUtils.elementColor(context)) {
        fab.backgroundTintList = ColorStateList.valueOf(backgroundColor)
        fab.rippleColor = ColorsUtils.calculateDarkColor(backgroundColor, context)
    }
}
