/*
 * ownCloud Android client application
 *
 * @author David A. Velasco
 * @author Chris Narkiewicz Chris Narkiewicz
 * @author TSI-mc
 * Copyright (C) 2016 ownCloud Inc.
 * Copyright (C) 2019 Chris Narkiewicz <hello@ezaquarii.com>
 * Copyright (C) 2023 TSI-mc
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.nextcloud.client.preferences

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.preference.PreferenceManager
import androidx.annotation.VisibleForTesting
import com.google.gson.Gson
import com.nextcloud.appReview.AppReviewShownModel
import com.nextcloud.client.account.User
import com.nextcloud.client.account.UserAccountManager
import com.nextcloud.client.account.UserAccountManagerImpl
import com.owncloud.android.datamodel.ArbitraryDataProvider
import com.owncloud.android.datamodel.ArbitraryDataProviderImpl
import com.owncloud.android.datamodel.FileDataStorageManager
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.ui.activity.PassCodeActivity
import com.owncloud.android.ui.activity.SettingsActivity
import com.owncloud.android.ui.fragment.OCFileListFragment
import com.owncloud.android.utils.FileSortOrder
import java.util.concurrent.CopyOnWriteArraySet

/**
 * Implementation of application-wide preferences using [SharedPreferences].
 *
 *
 * Users should not use this class directly. Please use [AppPreferences] interface instead.
 */
class AppPreferencesImpl internal constructor(
    private val context: Context,
    private val preferences: SharedPreferences,
    private val userAccountManager: UserAccountManager
) : AppPreferences {
    private val listeners: ListenerRegistry

    /**
     * Adapter delegating raw [SharedPreferences.OnSharedPreferenceChangeListener] calls with key-value pairs to
     * respective [com.nextcloud.client.preferences.AppPreferences.Listener] method.
     */
    internal class ListenerRegistry(private val preferences: AppPreferences) : OnSharedPreferenceChangeListener {
        private val listeners: MutableSet<AppPreferences.Listener>

        init {
            listeners = CopyOnWriteArraySet()
        }

        fun add(listener: AppPreferences.Listener?) {
            if (listener != null) {
                listeners.add(listener)
            }
        }

        fun remove(listener: AppPreferences.Listener?) {
            if (listener != null) {
                listeners.remove(listener)
            }
        }

        override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String) {
            if (PREF__DARK_THEME == key) {
                val mode = preferences.darkThemeMode
                for (l in listeners) {
                    l.onDarkThemeModeChanged(mode)
                }
            }
        }
    }

    init {
        listeners = ListenerRegistry(this)
        preferences.registerOnSharedPreferenceChangeListener(listeners)
    }

    override fun addListener(listener: AppPreferences.Listener?) {
        listeners.add(listener)
    }

    override fun removeListener(listener: AppPreferences.Listener?) {
        listeners.remove(listener)
    }

    override fun setKeysReInitEnabled() {
        preferences.edit().putBoolean(PREF__KEYS_REINIT, true).apply()
    }

    override val isKeysReInitEnabled: Boolean
        get() = preferences.getBoolean(PREF__KEYS_REINIT, false)
    override var pushToken: String?
        get() = preferences.getString(PREF__PUSH_TOKEN, "")
        set(pushToken) {
            preferences.edit().putString(PREF__PUSH_TOKEN, pushToken).apply()
        }

    override fun instantPictureUploadEnabled(): Boolean {
        return preferences.getBoolean(PREF__INSTANT_UPLOADING, false)
    }

    override fun instantVideoUploadEnabled(): Boolean {
        return preferences.getBoolean(PREF__INSTANT_VIDEO_UPLOADING, false)
    }

    override var isShowHiddenFilesEnabled: Boolean
        get() = preferences.getBoolean(PREF__SHOW_HIDDEN_FILES, false)
        set(enabled) {
            preferences.edit().putBoolean(PREF__SHOW_HIDDEN_FILES, enabled).apply()
        }
    override var isShowEcosystemApps: Boolean
        get() = preferences.getBoolean(PREF__SHOW_ECOSYSTEM_APPS, true)
        set(enabled) {
            preferences.edit().putBoolean(PREF__SHOW_ECOSYSTEM_APPS, enabled).apply()
        }
    override var uploadUrlFileExtensionUrlSelectedPos: Int
        get() = preferences.getInt(AUTO_PREF__UPLOAD_FILE_EXTENSION_URL, 0)
        set(selectedPos) {
            preferences.edit().putInt(AUTO_PREF__UPLOAD_FILE_EXTENSION_URL, selectedPos).apply()
        }
    override var uploadMapFileExtensionUrlSelectedPos: Int
        get() = preferences.getInt(AUTO_PREF__UPLOAD_FILE_EXTENSION_MAP_URL, 0)
        set(selectedPos) {
            preferences.edit().putInt(AUTO_PREF__UPLOAD_FILE_EXTENSION_MAP_URL, selectedPos).apply()
        }
    override var uploadFromLocalLastPath: String?
        get() = preferences.getString(AUTO_PREF__UPLOAD_FROM_LOCAL_LAST_PATH, "")
        set(path) {
            preferences.edit().putString(AUTO_PREF__UPLOAD_FROM_LOCAL_LAST_PATH, path).apply()
        }
    override var lastUploadPath: String?
        get() = preferences.getString(AUTO_PREF__LAST_UPLOAD_PATH, "")
        set(path) {
            preferences.edit().putString(AUTO_PREF__LAST_UPLOAD_PATH, path).apply()
        }
    override var lockPreference: String?
        get() = preferences.getString(PREF__LOCK, SettingsActivity.LOCK_NONE)
        set(lockPreference) {
            preferences.edit().putString(PREF__LOCK, lockPreference).apply()
        }

    override fun setPassCode(d1: String?, d2: String?, d3: String?, d4: String?) {
        preferences
            .edit()
            .putString(PassCodeActivity.PREFERENCE_PASSCODE_D1, d1)
            .putString(PassCodeActivity.PREFERENCE_PASSCODE_D2, d2)
            .putString(PassCodeActivity.PREFERENCE_PASSCODE_D3, d3)
            .putString(PassCodeActivity.PREFERENCE_PASSCODE_D4, d4)
            .apply()
    }

    override val passCode: Array<String?>
        get() = arrayOf(
            preferences.getString(PassCodeActivity.PREFERENCE_PASSCODE_D1, null),
            preferences.getString(PassCodeActivity.PREFERENCE_PASSCODE_D2, null),
            preferences.getString(PassCodeActivity.PREFERENCE_PASSCODE_D3, null),
            preferences.getString(PassCodeActivity.PREFERENCE_PASSCODE_D4, null)
        )
    override val isFingerprintUnlockEnabled: Boolean
        get() = preferences.getBoolean(SettingsActivity.PREFERENCE_USE_FINGERPRINT, false)

    override fun getFolderLayout(folder: OCFile?): String {
        return getFolderPreference(
            context,
            userAccountManager.user,
            PREF__FOLDER_LAYOUT,
            folder,
            OCFileListFragment.FOLDER_LAYOUT_LIST
        )
    }

    override fun setFolderLayout(folder: OCFile?, layoutName: String) {
        setFolderPreference(
            context,
            userAccountManager.user,
            PREF__FOLDER_LAYOUT,
            folder,
            layoutName
        )
    }

    override fun getSortOrderByFolder(folder: OCFile?): FileSortOrder? {
        return FileSortOrder.sortOrders[getFolderPreference(
            context,
            userAccountManager.user,
            PREF__FOLDER_SORT_ORDER,
            folder,
            FileSortOrder.sort_a_to_z.name
        )]
    }

    override fun setSortOrder(folder: OCFile?, sortOrder: FileSortOrder) {
        setFolderPreference(
            context,
            userAccountManager.user,
            PREF__FOLDER_SORT_ORDER,
            folder,
            sortOrder.name
        )
    }

    override fun getSortOrderByType(type: FileSortOrder.Type): FileSortOrder {
        return getSortOrderByType(type, FileSortOrder.sort_a_to_z)
    }

    override fun getSortOrderByType(type: FileSortOrder.Type, defaultOrder: FileSortOrder): FileSortOrder {
        val user = userAccountManager.user
        if (user.isAnonymous) {
            return defaultOrder
        }
        val dataProvider: ArbitraryDataProvider = ArbitraryDataProviderImpl(context)
        val value = dataProvider.getValue(user.accountName, PREF__FOLDER_SORT_ORDER + "_" + type)
        return if (value.isEmpty()) defaultOrder else FileSortOrder.sortOrders[value]!!
    }

    override fun setSortOrder(type: FileSortOrder.Type, sortOrder: FileSortOrder) {
        val user = userAccountManager.user
        val dataProvider: ArbitraryDataProvider = ArbitraryDataProviderImpl(context)
        dataProvider.storeOrUpdateKeyValue(user.accountName, PREF__FOLDER_SORT_ORDER + "_" + type, sortOrder.name)
    }

    override var isLegacyClean: Boolean
        get() = preferences.getBoolean(PREF__LEGACY_CLEAN, false)
        set(isLegacyClean) {
            preferences.edit().putBoolean(PREF__LEGACY_CLEAN, isLegacyClean).apply()
        }
    override var isKeysMigrationEnabled: Boolean
        get() = preferences.getBoolean(PREF__KEYS_MIGRATION, false)
        set(keysMigration) {
            preferences.edit().putBoolean(PREF__KEYS_MIGRATION, keysMigration).apply()
        }
    override var isStoragePathFixEnabled: Boolean
        get() = preferences.getBoolean(PREF__FIX_STORAGE_PATH, false)
        set(storagePathFixEnabled) {
            preferences.edit().putBoolean(PREF__FIX_STORAGE_PATH, storagePathFixEnabled).apply()
        }
    override var isAutoUploadPathsUpdateEnabled: Boolean
        get() = preferences.getBoolean(PREF__AUTO_UPLOAD_UPDATE_PATH, false)
        set(pathUpdate) {
            preferences.edit().putBoolean(PREF__AUTO_UPLOAD_UPDATE_PATH, pathUpdate).apply()
        }
    override var isAutoUploadSplitEntriesEnabled: Boolean
        get() = preferences.getBoolean(PREF__AUTO_UPLOAD_SPLIT_OUT, false)
        set(splitOut) {
            preferences.edit().putBoolean(PREF__AUTO_UPLOAD_SPLIT_OUT, splitOut).apply()
        }
    override val isAutoUploadInitialized: Boolean
        get() = preferences.getBoolean(PREF__AUTO_UPLOAD_INIT, false)

    override fun setAutoUploadInit(autoUploadInit: Boolean) {
        preferences.edit().putBoolean(PREF__AUTO_UPLOAD_INIT, autoUploadInit).apply()
    }

    override var uploaderBehaviour: Int
        get() = preferences.getInt(AUTO_PREF__UPLOADER_BEHAVIOR, 1)
        set(uploaderBehaviour) {
            preferences.edit().putInt(AUTO_PREF__UPLOADER_BEHAVIOR, uploaderBehaviour).apply()
        }
    override var darkThemeMode: DarkMode
        get() = try {
            DarkMode.valueOf(preferences.getString(PREF__DARK_THEME, DarkMode.SYSTEM.name)!!)
        } catch (e: ClassCastException) {
            preferences.edit().putString(PREF__DARK_THEME, DarkMode.SYSTEM.name).apply()
            DarkMode.SYSTEM
        }
        set(mode) {
            preferences.edit().putString(PREF__DARK_THEME, mode.name).apply()
        }
    override var gridColumns: Float
        /**
         * Gets the grid columns which the user has set last.
         *
         * @return grid columns     grid columns
         */
        get() {
            val columns = preferences.getFloat(AUTO_PREF__GRID_COLUMNS, DEFAULT_GRID_COLUMN)
            return if (columns < 0) {
                DEFAULT_GRID_COLUMN
            } else {
                columns
            }
        }
        /**
         * Saves the grid columns which the user has set last.
         *
         * @param gridColumns the uploader behavior
         */
        set(gridColumns) {
            preferences.edit().putFloat(AUTO_PREF__GRID_COLUMNS, gridColumns).apply()
        }
    override var lastSeenVersionCode: Int
        get() = preferences.getInt(AUTO_PREF__LAST_SEEN_VERSION_CODE, 0)
        set(versionCode) {
            preferences.edit().putInt(AUTO_PREF__LAST_SEEN_VERSION_CODE, versionCode).apply()
        }
    override var lockTimestamp: Long
        get() = preferences.getLong(PREF__LOCK_TIMESTAMP, 0)
        set(timestamp) {
            preferences.edit().putLong(PREF__LOCK_TIMESTAMP, timestamp).apply()
        }
    override var isShowDetailedTimestampEnabled: Boolean
        get() = preferences.getBoolean(AUTO_PREF__SHOW_DETAILED_TIMESTAMP, false)
        set(showDetailedTimestamp) {
            preferences.edit().putBoolean(AUTO_PREF__SHOW_DETAILED_TIMESTAMP, showDetailedTimestamp).apply()
        }
    override var isShowMediaScanNotifications: Boolean
        get() = preferences.getBoolean(PREF__SHOW_MEDIA_SCAN_NOTIFICATIONS, true)
        set(value) {
            preferences.edit().putBoolean(PREF__SHOW_MEDIA_SCAN_NOTIFICATIONS, value).apply()
        }

    override fun removeLegacyPreferences() {
        preferences.edit()
            .remove("instant_uploading")
            .remove("instant_video_uploading")
            .remove("instant_upload_path")
            .remove("instant_upload_path_use_subfolders")
            .remove("instant_upload_on_wifi")
            .remove("instant_upload_on_charging")
            .remove("instant_video_upload_path")
            .remove("instant_video_upload_path_use_subfolders")
            .remove("instant_video_upload_on_wifi")
            .remove("instant_video_uploading")
            .remove("instant_video_upload_on_charging")
            .remove("prefs_instant_behaviour")
            .apply()
    }

    override fun clear() {
        preferences.edit().clear().apply()
    }

    override fun getStoragePath(defaultPath: String?): String? {
        return preferences.getString(STORAGE_PATH, defaultPath)
    }

    @SuppressLint("ApplySharedPref")
    override fun setStoragePath(path: String?) {
        preferences.edit().putString(STORAGE_PATH, path).commit() // commit synchronously
    }

    @SuppressLint("ApplySharedPref")
    override fun setStoragePathValid() {
        preferences.edit().putBoolean(STORAGE_PATH_VALID, true).commit()
    }

    override val isStoragePathValid: Boolean
        get() = preferences.getBoolean(STORAGE_PATH_VALID, false)

    /**
     * Removes keys migration key from shared preferences.
     */
    @SuppressLint("ApplySharedPref")
    override fun removeKeysMigrationPreference() {
        preferences.edit().remove(PREF__KEYS_MIGRATION).commit() // commit synchronously
    }

    override var currentAccountName: String?
        get() = preferences.getString(PREF__SELECTED_ACCOUNT_NAME, null)
        set(accountName) {
            preferences.edit().putString(PREF__SELECTED_ACCOUNT_NAME, accountName).apply()
        }
    override val isUserIdMigrated: Boolean
        get() = preferences.getBoolean(PREF__MIGRATED_USER_ID, false)

    override fun setMigratedUserId(value: Boolean) {
        preferences.edit().putBoolean(PREF__MIGRATED_USER_ID, value).apply()
    }

    override var photoSearchTimestamp: Long
        get() = preferences.getLong(PREF__PHOTO_SEARCH_TIMESTAMP, 0)
        set(timestamp) {
            preferences.edit().putLong(PREF__PHOTO_SEARCH_TIMESTAMP, timestamp).apply()
        }
    override var isPowerCheckDisabled: Boolean
        get() = preferences.getBoolean(PREF__POWER_CHECK_DISABLED, false)
        set(value) {
            preferences.edit().putBoolean(PREF__POWER_CHECK_DISABLED, value).apply()
        }

    override fun increasePinWrongAttempts() {
        val count = preferences.getInt(PREF__PIN_BRUTE_FORCE_COUNT, 0)
        preferences.edit().putInt(PREF__PIN_BRUTE_FORCE_COUNT, count + 1).apply()
    }

    override fun resetPinWrongAttempts() {
        preferences.edit().putInt(PREF__PIN_BRUTE_FORCE_COUNT, 0).apply()
    }

    override fun pinBruteForceDelay(): Int {
        val count = preferences.getInt(PREF__PIN_BRUTE_FORCE_COUNT, 0)
        return computeBruteForceDelay(count)
    }

    override var uidPid: String?
        get() = preferences.getString(PREF__UID_PID, "")
        set(uidPid) {
            preferences.edit().putString(PREF__UID_PID, uidPid).apply()
        }
    override var calendarLastBackup: Long
        get() = preferences.getLong(PREF__CALENDAR_LAST_BACKUP, 0)
        set(timestamp) {
            preferences.edit().putLong(PREF__CALENDAR_LAST_BACKUP, timestamp).apply()
        }
    override var pdfZoomTipShownCount: Int
        get() = preferences.getInt(PREF__PDF_ZOOM_TIP_SHOWN, 0)
        set(count) {
            preferences.edit().putInt(PREF__PDF_ZOOM_TIP_SHOWN, count).apply()
        }
    override var isStoragePermissionRequested: Boolean
        get() = preferences.getBoolean(PREF__STORAGE_PERMISSION_REQUESTED, false)
        set(value) {
            preferences.edit().putBoolean(PREF__STORAGE_PERMISSION_REQUESTED, value).apply()
        }

    @VisibleForTesting
    fun computeBruteForceDelay(count: Int): Int {
        return Math.min(count / 3.0, 10.0).toInt()
    }

    override fun setInAppReviewData(appReviewShownModel: AppReviewShownModel) {
        val gson = Gson()
        val json = gson.toJson(appReviewShownModel)
        preferences.edit().putString(PREF__IN_APP_REVIEW_DATA, json).apply()
    }

    override fun getInAppReviewData(): AppReviewShownModel? {
        val gson = Gson()
        val json = preferences.getString(PREF__IN_APP_REVIEW_DATA, "")
        return gson.fromJson(json, AppReviewShownModel::class.java)
    }

    override var lastSelectedMediaFolder: String
        get() = preferences.getString(PREF__MEDIA_FOLDER_LAST_PATH, OCFile.ROOT_PATH)!!
        set(path) {
            preferences.edit().putString(PREF__MEDIA_FOLDER_LAST_PATH, path).apply()
        }

    companion object {
        /**
         * Constant to access value of last path selected by the user to upload a file shared from other app. Value handled
         * by the app without direct access in the UI.
         */
        const val AUTO_PREF__LAST_SEEN_VERSION_CODE = "lastSeenVersionCode"
        const val STORAGE_PATH = "storage_path"
        const val STORAGE_PATH_VALID = "storage_path_valid"
        const val PREF__DARK_THEME = "dark_theme_mode"
        const val DEFAULT_GRID_COLUMN = 3f
        private const val AUTO_PREF__LAST_UPLOAD_PATH = "last_upload_path"
        private const val AUTO_PREF__UPLOAD_FROM_LOCAL_LAST_PATH = "upload_from_local_last_path"
        private const val AUTO_PREF__UPLOAD_FILE_EXTENSION_MAP_URL = "prefs_upload_file_extension_map_url"
        private const val AUTO_PREF__UPLOAD_FILE_EXTENSION_URL = "prefs_upload_file_extension_url"
        private const val AUTO_PREF__UPLOADER_BEHAVIOR = "prefs_uploader_behaviour"
        private const val AUTO_PREF__GRID_COLUMNS = "grid_columns"
        private const val AUTO_PREF__SHOW_DETAILED_TIMESTAMP = "detailed_timestamp"
        private const val PREF__INSTANT_UPLOADING = "instant_uploading"
        private const val PREF__INSTANT_VIDEO_UPLOADING = "instant_video_uploading"
        private const val PREF__SHOW_HIDDEN_FILES = "show_hidden_files_pref"
        private const val PREF__SHOW_ECOSYSTEM_APPS = "show_ecosystem_apps"
        private const val PREF__LEGACY_CLEAN = "legacyClean"
        private const val PREF__KEYS_MIGRATION = "keysMigration"
        private const val PREF__FIX_STORAGE_PATH = "storagePathFix"
        private const val PREF__KEYS_REINIT = "keysReinit"
        private const val PREF__AUTO_UPLOAD_UPDATE_PATH = "autoUploadPathUpdate"
        private const val PREF__PUSH_TOKEN = "pushToken"
        private const val PREF__AUTO_UPLOAD_SPLIT_OUT = "autoUploadEntriesSplitOut"
        private const val PREF__AUTO_UPLOAD_INIT = "autoUploadInit"
        private const val PREF__FOLDER_SORT_ORDER = "folder_sort_order"
        private const val PREF__FOLDER_LAYOUT = "folder_layout"
        private const val PREF__LOCK_TIMESTAMP = "lock_timestamp"
        private const val PREF__SHOW_MEDIA_SCAN_NOTIFICATIONS = "show_media_scan_notifications"
        private const val PREF__LOCK = SettingsActivity.PREFERENCE_LOCK
        private const val PREF__SELECTED_ACCOUNT_NAME = "select_oc_account"
        private const val PREF__MIGRATED_USER_ID = "migrated_user_id"
        private const val PREF__PHOTO_SEARCH_TIMESTAMP = "photo_search_timestamp"
        private const val PREF__POWER_CHECK_DISABLED = "power_check_disabled"
        private const val PREF__PIN_BRUTE_FORCE_COUNT = "pin_brute_force_count"
        private const val PREF__UID_PID = "uid_pid"
        private const val PREF__CALENDAR_AUTOMATIC_BACKUP = "calendar_automatic_backup"
        private const val PREF__CALENDAR_LAST_BACKUP = "calendar_last_backup"
        private const val PREF__PDF_ZOOM_TIP_SHOWN = "pdf_zoom_tip_shown"
        private const val PREF__MEDIA_FOLDER_LAST_PATH = "media_folder_last_path"
        private const val PREF__STORAGE_PERMISSION_REQUESTED = "storage_permission_requested"
        private const val PREF__IN_APP_REVIEW_DATA = "in_app_review_data"

        /**
         * This is a temporary workaround to access app preferences in places that cannot use dependency injection yet. Use
         * injected component via [AppPreferences] interface.
         *
         *
         * WARNING: this creates new instance! it does not return app-wide singleton
         *
         * @param context Context used to create shared preferences
         * @return New instance of app preferences component
         */
        @JvmStatic
        @Deprecated("")
        fun fromContext(context: Context): AppPreferences {
            val userAccountManager: UserAccountManager = UserAccountManagerImpl.fromContext(context)
            val prefs = PreferenceManager.getDefaultSharedPreferences(context)
            return AppPreferencesImpl(context, prefs, userAccountManager)
        }

        /**
         * Get preference value for a folder. If folder is not set itself, it finds an ancestor that is set.
         *
         * @param context        Context object.
         * @param preferenceName Name of the preference to lookup.
         * @param folder         Folder.
         * @param defaultValue   Fallback value in case no ancestor is set.
         * @return Preference value
         */
        private fun getFolderPreference(
            context: Context,
            user: User,
            preferenceName: String,
            folder: OCFile?,
            defaultValue: String
        ): String {
            if (user.isAnonymous) {
                return defaultValue
            }
            val dataProvider: ArbitraryDataProvider = ArbitraryDataProviderImpl(context)
            val storageManager = FileDataStorageManager(user, context.contentResolver)
            var value = dataProvider.getValue(user.accountName, getKeyFromFolder(preferenceName, folder))
            var prefFolder = folder
            while (prefFolder != null && value.isEmpty()) {
                prefFolder = storageManager.getFileById(prefFolder.parentId)
                value = dataProvider.getValue(user.accountName, getKeyFromFolder(preferenceName, prefFolder))
            }
            return if (value.isEmpty()) defaultValue else value
        }

        /**
         * Set preference value for a folder.
         *
         * @param context        Context object.
         * @param preferenceName Name of the preference to set.
         * @param folder         Folder.
         * @param value          Preference value to set.
         */
        private fun setFolderPreference(
            context: Context,
            user: User,
            preferenceName: String,
            folder: OCFile?,
            value: String
        ) {
            val dataProvider: ArbitraryDataProvider = ArbitraryDataProviderImpl(context)
            dataProvider.storeOrUpdateKeyValue(user.accountName, getKeyFromFolder(preferenceName, folder), value)
        }

        private fun getKeyFromFolder(preferenceName: String, folder: OCFile?): String {
            val folderIdString = (folder?.fileId ?: FileDataStorageManager.ROOT_PARENT_ID).toString()
            return preferenceName + "_" + folderIdString
        }
    }
}