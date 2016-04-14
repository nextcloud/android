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

import com.owncloud.android.utils.FileStorageUtils;

/**
 * Helper to simplify reading of Preferences all around the app
 */
public abstract class PreferenceManager {
    /**
     * Constant to access value of last path selected by the user to upload a file shared from other app.
     * Value handled by the app without direct access in the UI.
     */
    private static final String AUTO_PREF__LAST_UPLOAD_PATH = "last_upload_path";
    private static final String AUTO_PREF__SORT_ORDER = "sortOrder";
    private static final String AUTO_PREF__SORT_ASCENDING = "sortAscending";
    private static final String PREF__INSTANT_UPLOADING = "instant_uploading";
    private static final String PREF__INSTANT_VIDEO_UPLOADING = "instant_video_uploading";
    private static final String PREF__INSTANT_UPLOAD_ON_WIFI = "instant_upload_on_wifi";
    private static final String PREF__INSTANT_VIDEO_UPLOAD_ON_WIFI = "instant_video_upload_on_wifi";

    public static boolean instantPictureUploadEnabled(Context context) {
        return android.preference.PreferenceManager.getDefaultSharedPreferences(context).getBoolean(
                PREF__INSTANT_UPLOADING,
                false
        );
    }

    public static boolean instantVideoUploadEnabled(Context context) {
        return android.preference.PreferenceManager.getDefaultSharedPreferences(context).getBoolean(
                PREF__INSTANT_VIDEO_UPLOADING,
                false
        );
    }

    public static boolean instantPictureUploadViaWiFiOnly(Context context) {
        return android.preference.PreferenceManager.getDefaultSharedPreferences(context).getBoolean(
                PREF__INSTANT_UPLOAD_ON_WIFI,
                false
        );
    }

    public static boolean instantVideoUploadViaWiFiOnly(Context context) {
        return android.preference.PreferenceManager.getDefaultSharedPreferences(context).getBoolean(
                PREF__INSTANT_VIDEO_UPLOAD_ON_WIFI,
                false
        );
    }

    /**
     * Gets the path where the user selected to do the last upload of a file shared from other app.
     *
     * @param context Caller {@link Context}, used to access to shared preferences manager.
     * @return path     Absolute path to a folder, as previously stored by {@link #setLastUploadPath(String, Context)},
     * or empty String if never saved before.
     */
    public static String getLastUploadPath(Context context) {
        SharedPreferences appPreferences = android.preference.PreferenceManager
                .getDefaultSharedPreferences(context.getApplicationContext());
        return appPreferences.getString(AUTO_PREF__LAST_UPLOAD_PATH, "");
    }

    /**
     * Saves the path where the user selected to do the last upload of a file shared from other app.
     *
     * @param path    Absolute path to a folder.
     * @param context Caller {@link Context}, used to access to shared preferences manager.
     */
    public static void setLastUploadPath(String path, Context context) {
        SharedPreferences.Editor appPrefs = android.preference.PreferenceManager
                .getDefaultSharedPreferences(context.getApplicationContext()).edit();
        appPrefs.putString(AUTO_PREF__LAST_UPLOAD_PATH, path);
        appPrefs.apply();
    }

    /**
     * Gets the sort order which the user has set last.
     *
     * @param context Caller {@link Context}, used to access to shared preferences manager.
     * @return sort order     the sort order, default is {@link FileStorageUtils#SORT_NAME} (sort by name)
     */
    public static int getSortOrder(Context context) {
        SharedPreferences appPreferences = android.preference.PreferenceManager
                .getDefaultSharedPreferences(context.getApplicationContext());
        return appPreferences.getInt(AUTO_PREF__SORT_ORDER, FileStorageUtils.SORT_NAME);
    }

    /**
     * Save the sort order which the user has set last.
     *
     * @param order   the sort order
     * @param context Caller {@link Context}, used to access to shared preferences manager.
     */
    public static void setSortOrder(int order, Context context) {
        SharedPreferences.Editor appPreferences = android.preference.PreferenceManager
                .getDefaultSharedPreferences(context.getApplicationContext()).edit();
        appPreferences.putInt(AUTO_PREF__SORT_ORDER, order);
        appPreferences.apply();
    }

    /**
     * Gets the ascending order flag which the user has set last.
     *
     * @param context Caller {@link Context}, used to access to shared preferences manager.
     * @return ascending order     the ascending order, default is true
     */
    public static boolean getSortAscending(Context context) {
        SharedPreferences appPreferences = android.preference.PreferenceManager
                .getDefaultSharedPreferences(context.getApplicationContext());
        return appPreferences.getBoolean(AUTO_PREF__SORT_ASCENDING, true);
    }

    /**
     * Saves the ascending order flag which the user has set last.
     *
     * @param ascending flag if sorting is ascending or descending
     * @param context   Caller {@link Context}, used to access to shared preferences manager.
     */
    public static void setSortAscending(boolean ascending, Context context) {
        SharedPreferences.Editor appPreferences = android.preference.PreferenceManager
                .getDefaultSharedPreferences(context.getApplicationContext()).edit();
        appPreferences.putBoolean(AUTO_PREF__SORT_ASCENDING, true);
        appPreferences.apply();
    }
}
