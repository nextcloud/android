/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2017 Mario Danic <mario@lovelyhq.com>
 * SPDX-FileCopyrightText: 2017 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
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
}
