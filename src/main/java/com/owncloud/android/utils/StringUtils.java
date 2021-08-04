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


import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Helper class for handling and manipulating strings.
 */
public final class StringUtils {

    private StringUtils() {
        // prevent class from being constructed
    }

    public static @NonNull
    String searchAndColor(@Nullable String text, @Nullable String searchText,
                          @ColorInt int color) {

        if (text != null) {

            if (text.isEmpty() || searchText == null || searchText.isEmpty()) {
                return text;
            }

            Matcher matcher = Pattern.compile(searchText,
                                              Pattern.CASE_INSENSITIVE | Pattern.LITERAL).matcher(text);

            StringBuffer stringBuffer = new StringBuffer();

            while (matcher.find()) {
                String replacement = matcher.group().replace(
                    matcher.group(),
                    String.format(Locale.getDefault(), "<font color='%d'><b>%s</b></font>", color,
                                  matcher.group())
                                                            );
                matcher.appendReplacement(stringBuffer, Matcher.quoteReplacement(replacement));
            }
            matcher.appendTail(stringBuffer);

            return stringBuffer.toString();
        } else {
            return "";
        }
    }

    public static
    @NonNull
    String removePrefix(@NonNull String s, @NonNull String prefix) {
        if (s.startsWith(prefix)) {
            return s.substring(prefix.length());
        }
        return s;
    }

    public static String escapeStacktrace(String string) {
        return string
            .replaceAll("\n", "%0A")
            .replaceAll(" ", "\\+")
            .replaceAll("`", "%A")
            .replaceAll("#", "%23");
    }
}
