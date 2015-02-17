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

package com.owncloud.android.utils;

import java.net.IDN;
import java.text.DateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.Vector;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.text.format.DateUtils;
import android.webkit.MimeTypeMap;

import com.owncloud.android.MainApp;
import com.owncloud.android.R;
import com.owncloud.android.datamodel.OCFile;

/**
 * A helper class for some string operations.
 * 
 * @author Bartek Przybylski
 * @author David A. Velasco
 */
public class DisplayUtils {
    
    private static final String OWNCLOUD_APP_NAME = "ownCloud";

    //private static String TAG = DisplayUtils.class.getSimpleName(); 
    
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
    private static final String SUBTYPE_XML = "xml";
    private static final String[] SUBTYPES_DOCUMENT = { 
        "msword",
        "vnd.openxmlformats-officedocument.wordprocessingml.document",
        "vnd.oasis.opendocument.text",
        "rtf",
        "javascript"
    };
    private static Set<String> SUBTYPES_DOCUMENT_SET = new HashSet<String>(Arrays.asList(SUBTYPES_DOCUMENT));
    private static final String[] SUBTYPES_SPREADSHEET = {
        "msexcel",
        "vnd.ms-excel",
        "vnd.openxmlformats-officedocument.spreadsheetml.sheet",
        "vnd.oasis.opendocument.spreadsheet"
    };
    private static Set<String> SUBTYPES_SPREADSHEET_SET = new HashSet<String>(Arrays.asList(SUBTYPES_SPREADSHEET));
    private static final String[] SUBTYPES_PRESENTATION = { 
        "mspowerpoint",
        "vnd.ms-powerpoint",
        "vnd.openxmlformats-officedocument.presentationml.presentation",
        "vnd.oasis.opendocument.presentation"
    };
    private static Set<String> SUBTYPES_PRESENTATION_SET = new HashSet<String>(Arrays.asList(SUBTYPES_PRESENTATION));
    private static final String[] SUBTYPES_COMPRESSED = {"x-tar", "x-gzip", "zip"};
    private static final Set<String> SUBTYPES_COMPRESSED_SET = new HashSet<String>(Arrays.asList(SUBTYPES_COMPRESSED));
    private static final String SUBTYPE_OCTET_STREAM = "octet-stream";
    private static final String EXTENSION_RAR = "rar";
    private static final String EXTENSION_RTF = "rtf";
    private static final String EXTENSION_3GP = "3gp";
    private static final String EXTENSION_PY = "py";
    private static final String EXTENSION_JS = "js";
    
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
     * Returns the resource identifier of an image to use as icon associated to a known MIME type.
     * 
     * @param mimetype      MIME type string; if NULL, the method tries to guess it from the extension in filename
     * @param filename      Name, with extension.
     * @return              Identifier of an image resource.
     */
    public static int getFileTypeIconId(String mimetype, String filename) {

        if (mimetype == null) {
            String fileExtension = getExtension(filename);
            mimetype = MimeTypeMap.getSingleton().getMimeTypeFromExtension(fileExtension);
            if (mimetype == null) {
                mimetype = TYPE_APPLICATION + "/" + SUBTYPE_OCTET_STREAM;
            }
        } 
            
        if ("DIR".equals(mimetype)) {
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
                    
                } else if (SUBTYPE_XML.equals(subtype)) {
                    return R.drawable.file_doc;

                } else if (SUBTYPES_DOCUMENT_SET.contains(subtype)) {
                    return R.drawable.file_doc;

                } else if (SUBTYPES_SPREADSHEET_SET.contains(subtype)) {
                    return R.drawable.file_xls;

                } else if (SUBTYPES_PRESENTATION_SET.contains(subtype)) {
                    return R.drawable.file_ppt;

                } else if (SUBTYPES_COMPRESSED_SET.contains(subtype)) {
                    return R.drawable.file_zip;

                } else if (SUBTYPE_OCTET_STREAM.equals(subtype) ) {
                    if (getExtension(filename).equalsIgnoreCase(EXTENSION_RAR)) {
                        return R.drawable.file_zip;
                        
                    } else if (getExtension(filename).equalsIgnoreCase(EXTENSION_RTF)) {
                        return R.drawable.file_doc;
                        
                    } else if (getExtension(filename).equalsIgnoreCase(EXTENSION_3GP)) {
                        return R.drawable.file_movie;
                     
                    } else if ( getExtension(filename).equalsIgnoreCase(EXTENSION_PY) ||
                                getExtension(filename).equalsIgnoreCase(EXTENSION_JS)) {
                        return R.drawable.file_doc;
                    } 
                } 
            }
        }

        // default icon
        return R.drawable.file;
    }

    
    private static String getExtension(String filename) {
        String extension = filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
        return extension;
    }
    
    /**
     * Converts Unix time to human readable format
     * @param milliseconds that have passed since 01/01/1970
     * @return The human readable time for the users locale
     */
    public static String unixTimeToHumanReadable(long milliseconds) {
        Date date = new Date(milliseconds);
        DateFormat df = DateFormat.getDateTimeInstance();
        return df.format(date);
    }
    
    
    public static int getSeasonalIconId() {
        if (Calendar.getInstance().get(Calendar.DAY_OF_YEAR) >= 354 &&
                MainApp.getAppContext().getString(R.string.app_name).equals(OWNCLOUD_APP_NAME)) {
            return R.drawable.winter_holidays_icon;
        } else {
            return R.drawable.icon;
        }
    }
    
    /**
     * Converts an internationalized domain name (IDN) in an URL to and from ASCII/Unicode.
     * @param url the URL where the domain name should be converted
     * @param toASCII if true converts from Unicode to ASCII, if false converts from ASCII to Unicode
     * @return the URL containing the converted domain name
     */
    @TargetApi(Build.VERSION_CODES.GINGERBREAD)
    public static String convertIdn(String url, boolean toASCII) {
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
            // Find host name after '//' or '@'
            int hostStart = 0;
            if  (url.indexOf("//") != -1) {
                hostStart = url.indexOf("//") + "//".length();
            } else if (url.indexOf("@") != -1) {
                hostStart = url.indexOf("@") + "@".length();
            }
            
            int hostEnd = url.substring(hostStart).indexOf("/");
            // Handle URL which doesn't have a path (path is implicitly '/')
            hostEnd = (hostEnd == -1 ? url.length() : hostStart + hostEnd);
            
            String host = url.substring(hostStart, hostEnd);
            host = (toASCII ? IDN.toASCII(host) : IDN.toUnicode(host));
            
            return url.substring(0, hostStart) + host + url.substring(hostEnd);
        } else {
            return url;
        }
    }

    /**
     * Get the file extension if it is on path as type "content://.../DocInfo.doc"
     * @param filepath: Content Uri converted to string format
     * @return String: fileExtension (type '.pdf'). Empty if no extension
     */
    public static String getComposedFileExtension(String filepath) {
        String fileExtension = "";
        String fileNameInContentUri = filepath.substring(filepath.lastIndexOf("/"));

        // Check if extension is included in uri
        int pos = fileNameInContentUri.lastIndexOf('.');
        if (pos >= 0) {
            fileExtension = fileNameInContentUri.substring(pos);
        }
        return fileExtension;
    }

    @SuppressWarnings("deprecation")
    public static CharSequence getRelativeDateTimeString (
            Context c, long time, long minResolution, long transitionResolution, int flags
            ){
        
        CharSequence dateString = "";
        
        // in Future
        if (time > System.currentTimeMillis()){
            return DisplayUtils.unixTimeToHumanReadable(time);
        } 
        // < 60 seconds -> seconds ago
        else if ((System.currentTimeMillis() - time) < 60 * 1000) {
            return c.getString(R.string.file_list_seconds_ago);
        } else {
            // Workaround 2.x bug (see https://github.com/owncloud/android/issues/716)
            if (    Build.VERSION.SDK_INT <= Build.VERSION_CODES.HONEYCOMB && 
                    (System.currentTimeMillis() - time) > 24 * 60 * 60 * 1000   ) {
                Date date = new Date(time);
                date.setHours(0);
                date.setMinutes(0);
                date.setSeconds(0);
                dateString = DateUtils.getRelativeDateTimeString(
                        c, date.getTime(), minResolution, transitionResolution, flags
                );
            } else {
                dateString = DateUtils.getRelativeDateTimeString(c, time, minResolution, transitionResolution, flags);
            }
        }
        
        return dateString.toString().split(",")[0];
    }

    /**
     * Update the passed path removing the last "/" if it is not the root folder
     * @param path
     */
    public static String getPathWithoutLastSlash(String path) {

        // Remove last slash from path
        if (path.length() > 1 && path.charAt(path.length()-1) == OCFile.PATH_SEPARATOR.charAt(0)) {
            path = path.substring(0, path.length()-1);
        }
        return path;
    }

}
