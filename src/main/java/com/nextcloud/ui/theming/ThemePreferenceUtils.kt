/*
 * Nextcloud Android client application
 *
 * @author Andy Scherzinger
 * Copyright (C) 2020 Andy Scherzinger
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
import android.preference.PreferenceCategory
import androidx.annotation.StringRes
import com.owncloud.android.utils.ThemeUtils

/**
 * Utility class with methods for client side preference theming.
 */
object ThemePreferenceUtils {
    @JvmStatic
    fun colorPreferenceCategory(context: Context?, preferenceCategory: PreferenceCategory?,
                                @StringRes title: Int) {
        if (context != null && preferenceCategory != null) {
            preferenceCategory.title = ThemeUtils.getColoredTitle(context.getString(title),
                ThemeUtils.primaryAccentColor(context))
        }
    }
}
