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
import android.content.SharedPreferences;

/**
 * Helper to simplify reading of Preferences all around the app
 */

public class PreferenceManager {

    /**
     * Constant to access value of last path selected by the user to upload a file shared from other app.
     * Value handled by the app without direct access in the UI.
     */
    private static final String AUTO_PREF__LAST_UPLOAD_PATH = "last_upload_path";

    public static boolean instantPictureUploadEnabled(Context context) {
        return android.preference.PreferenceManager.getDefaultSharedPreferences(context).getBoolean(
                "instant_uploading",
                false
        );
    }

    public static boolean instantVideoUploadEnabled(Context context) {
        return android.preference.PreferenceManager.getDefaultSharedPreferences(context).getBoolean(
                "instant_video_uploading",
                false
        );
    }

    public static boolean instantPictureUploadViaWiFiOnly(Context context) {
        return android.preference.PreferenceManager.getDefaultSharedPreferences(context).getBoolean(
                "instant_upload_on_wifi",
                false
        );
    }

    public static boolean instantVideoUploadViaWiFiOnly(Context context) {
        return android.preference.PreferenceManager.getDefaultSharedPreferences(context).getBoolean(
                "instant_video_upload_on_wifi",
                false
        );
    }

    /**
     * Gets the path where the user selected to do the last upload of a file shared from other app.
     *
     * @param context   Caller {@link Context}, used to access to shared preferences manager.
     * @return path     Absolute path to a folder, as previously stored by {@link #setLastUploadPath(String, Context)},
     *                  or empty String if never saved before.
     */
    public static String getLastUploadPath(Context context) {
        SharedPreferences appPreferences = android.preference.PreferenceManager
            .getDefaultSharedPreferences(context.getApplicationContext());
        return appPreferences.getString(AUTO_PREF__LAST_UPLOAD_PATH, "");
    }

    /**
     * Saves the path where the user selected to do the last upload of a file shared from other app.
     *
     * @param path      Absolute path to a folder.
     * @param context   Caller {@link Context}, used to access to shared preferences manager.
     */
    public static void setLastUploadPath(String path, Context context) {
        SharedPreferences.Editor appPrefs = android.preference.PreferenceManager
            .getDefaultSharedPreferences(context.getApplicationContext()).edit();
        appPrefs.putString(AUTO_PREF__LAST_UPLOAD_PATH, path);
        appPrefs.apply();
    }

}
