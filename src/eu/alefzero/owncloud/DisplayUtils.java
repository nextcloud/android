/* ownCloud Android client application
 *   Copyright (C) 2011  Bartek Przybylski
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package eu.alefzero.owncloud;

import java.util.Date;
import java.util.HashMap;

/**
 * A helper class for some string operations.
 * 
 * @author Bartek Przybylski
 * 
 */
public class DisplayUtils {

    private static final String[] suffixes = { "B", "KB", "MB", "GB", "TB", "PB", "EB", "ZB", "YB" };

    private static HashMap<String, String> mimeType2HUmanReadable;
    static {
        mimeType2HUmanReadable = new HashMap<String, String>();
        // images
        mimeType2HUmanReadable.put("image/jpeg", "JPEG image");
        mimeType2HUmanReadable.put("image/jpg", "JPEG image");
        mimeType2HUmanReadable.put("image/png", "PNG image");
        mimeType2HUmanReadable.put("image/bmp", "Bitmap image");
        mimeType2HUmanReadable.put("image/gif", "GIF image");
        mimeType2HUmanReadable.put("image/svg+xml", "JPEG image");
        mimeType2HUmanReadable.put("image/tiff", "TIFF image");
        // music
        mimeType2HUmanReadable.put("audio/mpeg", "MP3 music file");
        mimeType2HUmanReadable.put("application/ogg", "OGG music file");

    }

    /**
     * Converts the file size in bytes to human readable output.
     * 
     * @param bytes Input file size
     * @return Like something readable like "12 MB"
     */
    public static String bytesToHumanReadable(long bytes) {
        double result = bytes;
        int attachedsuff = 0;
        while (result > 1024 && attachedsuff < suffixes.length) {
            result /= 1024.;
            attachedsuff++;
        }
        result = ((int) (result * 100)) / 100.;
        return result + " " + suffixes[attachedsuff];
    }

    /**
     * Removes special HTML entities from a string
     * 
     * @param s Input string
     * @return A cleaned version of the string
     */
    public static String HtmlDecode(String s) {
        /*
         * TODO: Perhaps we should use something more proven like:
         * http://commons.apache.org/lang/api-2.6/org/apache/commons/lang/StringEscapeUtils.html#unescapeHtml%28java.lang.String%29
         */

        String ret = "";
        for (int i = 0; i < s.length(); ++i) {
            if (s.charAt(i) == '%') {
                ret += (char) Integer.parseInt(s.substring(i + 1, i + 3), 16);
                i += 2;
            } else {
                ret += s.charAt(i);
            }
        }
        return ret;
    }

    /**
     * Converts MIME types like "image/jpg" to more end user friendly output
     * like "JPG image".
     * 
     * @param mimetype MIME type to convert
     * @return A human friendly version of the MIME type
     */
    public static String convertMIMEtoPrettyPrint(String mimetype) {
        if (mimeType2HUmanReadable.containsKey(mimetype)) {
            return mimeType2HUmanReadable.get(mimetype);
        }
        return mimetype.split("/")[1].toUpperCase() + " file";
    }

    /**
     * Converts Unix time to human readable format
     * @param miliseconds that have passed since 01/01/1970
     * @return The human readable time for the users locale
     */
    public static String unixTimeToHumanReadable(long milliseconds) {
        Date date = new Date(milliseconds);
        return date.toLocaleString();
    }
}
