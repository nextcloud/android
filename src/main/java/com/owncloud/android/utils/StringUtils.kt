/*
 * Nextcloud Android client application
 *
 * @author Mario Danic
 * Copyright (C) 2017 Nextcloud GmbH.
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
package com.owncloud.android.utils

import androidx.annotation.ColorInt
import java.util.Locale
import java.util.regex.Matcher
import java.util.regex.Pattern

/**
 * Helper class for handling and manipulating strings.
 */
object StringUtils {
    @JvmStatic
    fun searchAndColor(
        text: String?,
        searchText: String?,
        @ColorInt color: Int
    ): String {
        return if (text != null) {
            if (text.isEmpty() || searchText == null || searchText.isEmpty()) {
                return text
            }
            val matcher = Pattern.compile(
                searchText,
                Pattern.CASE_INSENSITIVE or Pattern.LITERAL
            ).matcher(text)
            val stringBuffer = StringBuffer()
            while (matcher.find()) {
                val replacement = matcher.group().replace(
                    matcher.group(),
                    String.format(
                        Locale.getDefault(), "<font color='%d'><b>%s</b></font>", color,
                        matcher.group()
                    )
                )
                matcher.appendReplacement(stringBuffer, Matcher.quoteReplacement(replacement))
            }
            matcher.appendTail(stringBuffer)
            stringBuffer.toString()
        } else {
            ""
        }
    }
}
