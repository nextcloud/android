/* ownCloud Android Library is available under MIT license
 *   Copyright (C) 2014 ownCloud (http://www.owncloud.org/)
 *   Copyright (C) 2012  Bartek Przybylski
 *   
 *   Permission is hereby granted, free of charge, to any person obtaining a copy
 *   of this software and associated documentation files (the "Software"), to deal
 *   in the Software without restriction, including without limitation the rights
 *   to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *   copies of the Software, and to permit persons to whom the Software is
 *   furnished to do so, subject to the following conditions:
 *   
 *   The above copyright notice and this permission notice shall be included in
 *   all copies or substantial portions of the Software.
 *   
 *   THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, 
 *   EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 *   MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND 
 *   NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS 
 *   BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN 
 *   ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN 
 *   CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 *   THE SOFTWARE.
 *
 */

package com.owncloud.android.lib.network.webdav;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import android.net.Uri;

public class WebdavUtils {
    public static final SimpleDateFormat DISPLAY_DATE_FORMAT = new SimpleDateFormat(
            "dd.MM.yyyy hh:mm");
    private static final SimpleDateFormat DATETIME_FORMATS[] = {
            new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US),
            new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.US),
            new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.sss'Z'", Locale.US),
            new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.US),
            new SimpleDateFormat("EEE MMM dd HH:mm:ss zzz yyyy", Locale.US),
            new SimpleDateFormat("EEEEEE, dd-MMM-yy HH:mm:ss zzz", Locale.US),
            new SimpleDateFormat("EEE MMMM d HH:mm:ss yyyy", Locale.US) };

    public static String prepareXmlForPropFind() {
        String ret = "<?xml version=\"1.0\" ?><D:propfind xmlns:D=\"DAV:\"><D:allprop/></D:propfind>";
        return ret;
    }

    public static String prepareXmlForPatch() {
        return "<?xml version=\"1.0\" ?><D:propertyupdate xmlns:D=\"DAV:\"></D:propertyupdate>";
    }

    public static Date parseResponseDate(String date) {
        Date returnDate = null;
        for (int i = 0; i < DATETIME_FORMATS.length; ++i) {
            try {
                returnDate = DATETIME_FORMATS[i].parse(date);
                return returnDate;
            } catch (ParseException e) {
            }
        }
        return null;
    }

    /**
     * Encodes a path according to URI RFC 2396. 
     * 
     * If the received path doesn't start with "/", the method adds it.
     * 
     * @param remoteFilePath    Path
     * @return                  Encoded path according to RFC 2396, always starting with "/"
     */
    public static String encodePath(String remoteFilePath) {
        String encodedPath = Uri.encode(remoteFilePath, "/");
        if (!encodedPath.startsWith("/"))
            encodedPath = "/" + encodedPath;
        return encodedPath;
    }
    
}
