/*
 * Nextcloud Android client application
 *
 * @author TSI-mc
 * Copyright (C) 2022 TSI-mc
 * Copyright (C) 2022 Nextcloud GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package com.owncloud.android.utils

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun Long.isCurrentYear(yearToCompare: String?): Boolean {
    val simpleDateFormat = SimpleDateFormat("yyyy", Locale.getDefault())
    val currentYear = simpleDateFormat.format(Date(this))
    return currentYear == yearToCompare
}

fun Long.getFormattedStringDate(format: String): String {
    val simpleDateFormat = SimpleDateFormat(format, Locale.getDefault())
    return simpleDateFormat.format(Date(this))
}
