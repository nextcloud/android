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
    private static final String AUTO_PREF__SORT_ORDER = "sort_order";
    private static final String AUTO_PREF__SORT_ASCENDING = "sort_ascending";
    private static final String AUTO_PREF__UPLOAD_FILE_EXTENSION_MAP_URL = "prefs_upload_file_extension_map_url";
    private static final String AUTO_PREF__UPLOAD_FILE_EXTENSION_URL = "prefs_upload_file_extension_url";
    private static final String AUTO_PREF__UPLOADER_BEHAVIOR = "prefs_uploader_behaviour";
    private static final String AUTO_PREF__GRID_COLUMNS = "grid_columns";
    private static final String PREF__INSTANT_UPLOADING = "instant_uploading";
    private static final String PREF__INSTANT_VIDEO_UPLOADING = "instant_video_uploading";
    private static final String PREF__INSTANT_UPLOAD_PATH_USE_SUBFOLDERS = "instant_upload_path_use_subfolders";
    private static final String PREF__INSTANT_UPLOAD_ON_WIFI = "instant_upload_on_wifi";
    private static final String PREF__INSTANT_VIDEO_UPLOAD_ON_WIFI = "instant_video_upload_on_wifi";
    private static final String PREF__INSTANT_VIDEO_UPLOAD_PATH_USE_SUBFOLDERS = "instant_video_upload_path_use_subfolders";
    private static final String PREF__LEGACY_CLEAN = "legacyClean";
    private static final String PREF__AUTO_UPLOAD_UPDATE_PATH = "autoUploadPathUpdate";
    private static final String PREF__PUSH_TOKEN = "pushToken";
    private static final String PREF__AUTO_UPLOAD_SPLIT_OUT = "autoUploadEntriesSplitOut";
    private static final String PREF__AUTO_UPLOAD_INIT = "autoUploadInit";

    public static void setPushToken(Context context, String pushToken) {
        saveStringPreferenceNow(context, PREF__PUSH_TOKEN, pushToken);
    }

    public static String getPushToken(Context context) {
        return getDefaultSharedPreferences(context).getString(PREF__PUSH_TOKEN, "");
    }

    public static boolean instantPictureUploadEnabled(Context context) {
        return getDefaultSharedPreferences(context).getBoolean(PREF__INSTANT_UPLOADING, false);
    }

    public static boolean instantVideoUploadEnabled(Context context) {
        return getDefaultSharedPreferences(context).getBoolean(PREF__INSTANT_VIDEO_UPLOADING, false);
    }

    public static boolean instantPictureUploadPathUseSubfolders(Context context) {
        return getDefaultSharedPreferences(context).getBoolean(PREF__INSTANT_UPLOAD_PATH_USE_SUBFOLDERS, false);
    }

    public static boolean instantPictureUploadViaWiFiOnly(Context context) {
        return getDefaultSharedPreferences(context).getBoolean(PREF__INSTANT_UPLOAD_ON_WIFI, false);
    }

    public static boolean instantVideoUploadPathUseSubfolders(Context context) {
        return getDefaultSharedPreferences(context).getBoolean(PREF__INSTANT_VIDEO_UPLOAD_PATH_USE_SUBFOLDERS, false);
    }

    public static boolean instantVideoUploadViaWiFiOnly(Context context) {
        return getDefaultSharedPreferences(context).getBoolean(PREF__INSTANT_VIDEO_UPLOAD_ON_WIFI, false);
    }

    public static boolean instantPictureUploadWhenChargingOnly(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean("instant_upload_on_charging", false);
    }

    public static boolean instantVideoUploadWhenChargingOnly(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean("instant_video_upload_on_charging", false);
    }

    public static boolean showHiddenFilesEnabled(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean("show_hidden_files_pref", false);
    }

    /**
     * Gets the selected file extension position the user selected to do the last upload of a url file shared from other
     * app.
     *
     * @param context Caller {@link Context}, used to access to shared preferences manager.
     * @return selectedPos     the selected file extension position.
     */
    public static int getUploadUrlFileExtensionUrlSelectedPos(Context context) {
        return getDefaultSharedPreferences(context).getInt(AUTO_PREF__UPLOAD_FILE_EXTENSION_URL, 0);
    }

    /**
     * Saves the selected file extension position the user selected to do the last upload of a url file shared from
     * other app.
     *
     * @param context     Caller {@link Context}, used to access to shared preferences manager.
     * @param selectedPos the selected file extension position.
     */
    public static void setUploadUrlFileExtensionUrlSelectedPos(Context context, int selectedPos) {
        saveIntPreference(context, AUTO_PREF__UPLOAD_FILE_EXTENSION_URL, selectedPos);
    }

    /**
     * Gets the selected map file extension position the user selected to do the last upload of a url file shared
     * from other app.
     *
     * @param context Caller {@link Context}, used to access to shared preferences manager.
     * @return selectedPos     the selected file extension position.
     */
    public static int getUploadMapFileExtensionUrlSelectedPos(Context context) {
        return getDefaultSharedPreferences(context).getInt(AUTO_PREF__UPLOAD_FILE_EXTENSION_MAP_URL, 0);
    }

    /**
     * Saves the selected map file extension position the user selected to do the last upload of a url file shared from
     * other app.
     *
     * @param context     Caller {@link Context}, used to access to shared preferences manager.
     * @param selectedPos the selected file extension position.
     */
    public static void setUploadMapFileExtensionUrlSelectedPos(Context context, int selectedPos) {
        saveIntPreference(context, AUTO_PREF__UPLOAD_FILE_EXTENSION_MAP_URL, selectedPos);
    }

    /**
     * Gets the path where the user selected to do the last upload of a file shared from other app.
     *
     * @param context Caller {@link Context}, used to access to shared preferences manager.
     * @return path     Absolute path to a folder, as previously stored by {@link #setLastUploadPath(Context, String)},
     * or empty String if never saved before.
     */
    public static String getLastUploadPath(Context context) {
        return getDefaultSharedPreferences(context).getString(AUTO_PREF__LAST_UPLOAD_PATH, "");
    }

    /**
     * Saves the path where the user selected to do the last upload of a file shared from other app.
     *
     * @param context Caller {@link Context}, used to access to shared preferences manager.
     * @param path    Absolute path to a folder.
     */
    public static void setLastUploadPath(Context context, String path) {
        saveStringPreference(context, AUTO_PREF__LAST_UPLOAD_PATH, path);
    }

    /**
     * Gets the sort order which the user has set last.
     *
     * @param context Caller {@link Context}, used to access to shared preferences manager.
     * @return sort order     the sort order, default is {@link FileStorageUtils#SORT_NAME} (sort by name)
     */
    public static int getSortOrder(Context context) {
        return getDefaultSharedPreferences(context).getInt(AUTO_PREF__SORT_ORDER, FileStorageUtils.SORT_NAME);
    }

    /**
     * Save the sort order which the user has set last.
     *
     * @param context Caller {@link Context}, used to access to shared preferences manager.
     * @param order   the sort order
     */
    public static void setSortOrder(Context context, int order) {
        saveIntPreference(context, AUTO_PREF__SORT_ORDER, order);
    }

    /**
     * Gets the ascending order flag which the user has set last.
     *
     * @param context Caller {@link Context}, used to access to shared preferences manager.
     * @return ascending order     the ascending order, default is true
     */
    public static boolean getSortAscending(Context context) {
        return getDefaultSharedPreferences(context).getBoolean(AUTO_PREF__SORT_ASCENDING, true);
    }

    /**
     * Saves the ascending order flag which the user has set last.
     *
     * @param context   Caller {@link Context}, used to access to shared preferences manager.
     * @param ascending flag if sorting is ascending or descending
     */
    public static void setSortAscending(Context context, boolean ascending) {
        saveBooleanPreference(context, AUTO_PREF__SORT_ASCENDING, ascending);
    }

    public static boolean getAutoUploadInit(Context context) {
        return getDefaultSharedPreferences(context).getBoolean(PREF__AUTO_UPLOAD_INIT, false);
    }

    /**
     * Gets the legacy cleaning flag last set.
     *
     * @param context Caller {@link Context}, used to access to shared preferences manager.
     * @return ascending order     the legacy cleaning flag, default is false
     */
    public static boolean getLegacyClean(Context context) {
        return getDefaultSharedPreferences(context).getBoolean(PREF__LEGACY_CLEAN, false);
    }

    /**
     * Gets the auto upload paths flag last set.
     *
     * @param context Caller {@link Context}, used to access to shared preferences manager.
     * @return ascending order     the legacy cleaning flag, default is false
     */
    public static boolean getAutoUploadPathsUpdate(Context context) {
        return getDefaultSharedPreferences(context).getBoolean(PREF__AUTO_UPLOAD_UPDATE_PATH, false);
    }

    /**
     * Gets the auto upload split out flag last set.
     *
     * @param context Caller {@link Context}, used to access to shared preferences manager.
     * @return ascending order     the legacy cleaning flag, default is false
     */
    public static boolean getAutoUploadSplitEntries(Context context) {
        return getDefaultSharedPreferences(context).getBoolean(PREF__AUTO_UPLOAD_SPLIT_OUT, false);
    }

    /**
     * Saves the legacy cleaning flag which the user has set last.
     *
     * @param context     Caller {@link Context}, used to access to shared preferences manager.
     * @param legacyClean flag if it is a legacy cleaning
     */
    public static void setLegacyClean(Context context, boolean legacyClean) {
        saveBooleanPreference(context, PREF__LEGACY_CLEAN, legacyClean);
    }

    public static void setAutoUploadInit(Context context, boolean autoUploadInit) {
        saveBooleanPreference(context, PREF__AUTO_UPLOAD_INIT, autoUploadInit);
    }

    /**
     * Saves the legacy cleaning flag which the user has set last.
     *
     * @param context    Caller {@link Context}, used to access to shared preferences manager.
     * @param pathUpdate flag if it is a auto upload path update
     */
    public static void setAutoUploadPathsUpdate(Context context, boolean pathUpdate) {
        saveBooleanPreference(context, PREF__AUTO_UPLOAD_UPDATE_PATH, pathUpdate);
    }

    /**
     * Saves the flag for split entries magic
     *
     * @param context    Caller {@link Context}, used to access to shared preferences manager.
     * @param splitOut flag if it is a auto upload path update
     */
    public static void setAutoUploadSplitEntries(Context context, boolean splitOut) {
        saveBooleanPreference(context, PREF__AUTO_UPLOAD_SPLIT_OUT, splitOut);
    }

    /**
     * Gets the uploader behavior which the user has set last.
     *
     * @param context Caller {@link Context}, used to access to shared preferences manager.
     * @return uploader behavior     the uploader behavior
     */
    public static int getUploaderBehaviour(Context context) {
        return getDefaultSharedPreferences(context).getInt(AUTO_PREF__UPLOADER_BEHAVIOR, 1);
    }

    /**
     * Saves the uploader behavior which the user has set last.
     *
     * @param context           Caller {@link Context}, used to access to shared preferences manager.
     * @param uploaderBehaviour the uploader behavior
     */
    public static void setUploaderBehaviour(Context context, int uploaderBehaviour) {
        saveIntPreference(context, AUTO_PREF__UPLOADER_BEHAVIOR, uploaderBehaviour);
    }

    /**
     * Gets the grid columns which the user has set last.
     *
     * @param context Caller {@link Context}, used to access to shared preferences manager.
     * @return grid columns     grid columns
     */
    public static float getGridColumns(Context context) {
        return getDefaultSharedPreferences(context).getFloat(AUTO_PREF__GRID_COLUMNS, 4.0f);
    }

    /**
     * Saves the grid columns which the user has set last.
     *
     * @param context   Caller {@link Context}, used to access to shared preferences manager.
     * @param gridColumns the uploader behavior
     */
    public static void setGridColumns(Context context, float gridColumns) {
        saveFloatPreference(context, AUTO_PREF__GRID_COLUMNS, gridColumns);
    }

    private static void saveBooleanPreference(Context context, String key, boolean value) {
        SharedPreferences.Editor appPreferences = getDefaultSharedPreferences(context.getApplicationContext()).edit();
        appPreferences.putBoolean(key, value).apply();
    }

    private static void saveStringPreference(Context context, String key, String value) {
        SharedPreferences.Editor appPreferences = getDefaultSharedPreferences(context.getApplicationContext()).edit();
        appPreferences.putString(key, value).apply();
    }

    private static void saveStringPreferenceNow(Context context, String key, String value) {
        SharedPreferences.Editor appPreferences = getDefaultSharedPreferences(context.getApplicationContext()).edit();
        appPreferences.putString(key, value);
        appPreferences.apply();
    }

    private static void saveIntPreference(Context context, String key, int value) {
        SharedPreferences.Editor appPreferences = getDefaultSharedPreferences(context.getApplicationContext()).edit();
        appPreferences.putInt(key, value).apply();
    }

    private static void saveFloatPreference(Context context, String key, float value) {
        SharedPreferences.Editor appPreferences = getDefaultSharedPreferences(context.getApplicationContext()).edit();
        appPreferences.putFloat(key, value).apply();
    }

    public static SharedPreferences getDefaultSharedPreferences(Context context) {
        return android.preference.PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext());
    }
}
