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

package com.owncloud.android.utils;

import android.text.TextUtils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import androidx.annotation.ColorInt;

/**
 * Helper class for handling and manipulating strings.
 */
public final class StringUtils {

    private StringUtils() {
        // prevent class from being constructed
    }

    public static String searchAndColor(String text, String searchText, @ColorInt int color) {

        if (TextUtils.isEmpty(text) || TextUtils.isEmpty(searchText)) {
            return text;
        }

        Matcher matcher = Pattern.compile(searchText, Pattern.CASE_INSENSITIVE | Pattern.LITERAL).matcher(text);

        StringBuffer stringBuffer = new StringBuffer();

        while (matcher.find()) {
            String replacement = matcher.group().replace(
                matcher.group(),
                "<font color='" + color + "'><b>" + matcher.group() + "</b></font>"
            );
            matcher.appendReplacement(stringBuffer, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(stringBuffer);

        return stringBuffer.toString();
    }
}
