/* ownCloud Android client application
 *   Copyright (C) 2011  Bartek Przybylski
 *   Copyright (C) 2012-2013 ownCloud Inc.
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License version 2,
 *   as published by the Free Software Foundation.
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

package com.owncloud.android;

import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

/**
 * A helper class for some string operations.
 * 
 * @author Bartek Przybylski
 * @author David A. Velasco
 */
public class DisplayUtils {
    
    private static String TAG = DisplayUtils.class.getSimpleName(); 
    
    private static final String[] sizeSuffixes = { "B", "KB", "MB", "GB", "TB", "PB", "EB", "ZB", "YB" };

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

    private static final String TYPE_APPLICATION = "application";
    private static final String TYPE_AUDIO = "audio";
    private static final String TYPE_IMAGE = "image";
    private static final String TYPE_TXT = "text";
    private static final String TYPE_VIDEO = "video";
    
    private static final String SUBTYPE_PDF = "pdf";
    private static final String[] SUBTYPES_DOCUMENT = { "msword", "mspowerpoint", "msexcel", 
                                                        "vnd.oasis.opendocument.presentation",
                                                        "vnd.oasis.opendocument.spreadsheet",
                                                        "vnd.oasis.opendocument.text"
                                                        };
    private static Set<String> SUBTYPES_DOCUMENT_SET = new HashSet<String>(Arrays.asList(SUBTYPES_DOCUMENT));
    private static final String[] SUBTYPES_COMPRESSED = {"x-tar", "x-gzip", "zip"};
    private static final Set<String> SUBTYPES_COMPRESSED_SET = new HashSet<String>(Arrays.asList(SUBTYPES_COMPRESSED));
    
    /**
     * Converts the file size in bytes to human readable output.
     * 
     * @param bytes Input file size
     * @return Like something readable like "12 MB"
     */
    public static String bytesToHumanReadable(long bytes) {
        double result = bytes;
        int attachedsuff = 0;
        while (result > 1024 && attachedsuff < sizeSuffixes.length) {
            result /= 1024.;
            attachedsuff++;
        }
        result = ((int) (result * 100)) / 100.;
        return result + " " + sizeSuffixes[attachedsuff];
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
        if (mimetype.split("/").length >= 2)
            return mimetype.split("/")[1].toUpperCase() + " file";
        return "Unknown type";
    }
    
    
    /**
     * Returns the resource identifier of an image resource to use as icon associated to a 
     * known MIME type.
     * 
     * @param mimetype      MIME type string.
     * @return              Resource identifier of an image resource.
     */
    public static int getResourceId(String mimetype) {

        if (mimetype == null || "DIR".equals(mimetype)) {
            return R.drawable.ic_menu_archive;
            
        } else {
            String [] parts = mimetype.split("/");
            String type = parts[0];
            String subtype = (parts.length > 1) ? parts[1] : "";
            
            if(TYPE_TXT.equals(type)) {
                return R.drawable.file_doc;
    
            } else if(TYPE_IMAGE.equals(type)) {
                return R.drawable.file_image;
                
            } else if(TYPE_VIDEO.equals(type)) {
                return R.drawable.file_movie;
                
            } else if(TYPE_AUDIO.equals(type)) {  
                return R.drawable.file_sound;
                
            } else if(TYPE_APPLICATION.equals(type)) {
                
                if (SUBTYPE_PDF.equals(subtype)) {
                    return R.drawable.file_pdf;
                    
                } else if (SUBTYPES_DOCUMENT_SET.contains(subtype)) {
                    return R.drawable.file_doc;

                } else if (SUBTYPES_COMPRESSED_SET.contains(subtype)) {
                    return R.drawable.file_zip;
                }
    
            }
            // problems: RAR, RTF, 3GP are send as application/octet-stream from the server ; extension in the filename should be explicitly reviewed
        }

        // default icon
        return R.drawable.file;
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
