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

import java.util.HashMap;

import android.util.Log;

/**
 * A helper class for some string operations.
 * @author Bartek Przybylski
 *
 */
public class DisplayUtils {
  public static String bitsToHumanReadable(long bitsLen) {
    double result = bitsLen;
    int attachedsuff = 0;
    while (result > 1024 && attachedsuff < suffixes.length) {
      result /= 1024.;
      attachedsuff++;
    }
    result = ((int)(result * 100))/100.;
    return result+suffixes[attachedsuff];
  }
  
  public static String HtmlDecode(String s) {
    String ret = "";
    for (int i = 0; i < s.length(); ++i) {
      if (s.charAt(i) == '%') {
        ret += (char)Integer.parseInt(s.substring(i+1, i+3), 16);
        i+=2;
      } else {
        ret += s.charAt(i);
      }
    }
    return ret;
  }

  public static String convertMIMEtoPrettyPrint(String mimetype) {
    if (mimeType2HUmanReadable.containsKey(mimetype)) {
      return mimeType2HUmanReadable.get(mimetype);
    }
    return mimetype.split("/")[1].toUpperCase() + " file";
  }
  
  private static final String[] suffixes = {"B", "KB", "MB", "GB", "TB", "PB", "EB", "ZB", "YB"};
  
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
}
