/**
 * ownCloud Android client application
 *
 * @author David A. Velasco
 * Copyright (C) 2016 ownCloud Inc.
 * <p/>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2,
 * as published by the Free Software Foundation.
 * <p/>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p/>
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.owncloud.android.db;

import android.content.Context;
import android.preference.PreferenceManager;

/**
 * Helper to simplify reading of Preferences all around the app
 */

public class PreferenceReader {

    public static boolean instantPictureUploadEnabled(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(
                "instant_uploading",
                false
        );
    }

    public static boolean instantVideoUploadEnabled(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(
                "instant_video_uploading",
                false
        );
    }

    public static boolean instantPictureUploadViaWiFiOnly(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(
                "instant_upload_on_wifi",
                false
        );
    }

    public static boolean instantVideoUploadViaWiFiOnly(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(
                "instant_video_upload_on_wifi",
                false
        );
    }

}
