/**
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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by mdjanic on 19/01/2017.
 */

public class StringUtils {

    public static String searchAndColor(String text, String searchText) {

        if (text == null) {
            return null;
        }

        if (text.isEmpty() || searchText == null || searchText.isEmpty() || searchText.equals("\n")) {
            return text;
        }

        Matcher m = Pattern.compile(searchText, Pattern.CASE_INSENSITIVE | Pattern.LITERAL)
                .matcher(text);

        StringBuffer sb = new StringBuffer();

        while (m.find()) {
            String replacement = m.group().replace(m.group(), "<font color='red'>" + m.group() + "</font>");
            m.appendReplacement(sb, Matcher.quoteReplacement(replacement));
        }
        m.appendTail(sb);

        return sb.toString();

    }
}
