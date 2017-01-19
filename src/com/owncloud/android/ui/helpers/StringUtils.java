package com.owncloud.android.ui.helpers;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by mdjanic on 19/01/2017.
 */

public class StringUtils {

    public static String SearchAndColor(String text, String searchText) {

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
