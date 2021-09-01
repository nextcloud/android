/*
 * Nextcloud Android client application
 *
 * @author Chris Narkiewicz
 * Copyright (C) 2019 Chris Narkiewicz, EZ Aquarii
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package com.nextcloud.client.preferences;

import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.utils.FileSortOrder;

import androidx.annotation.Nullable;

/**
 * This interface provides single point of entry for access to all application
 * preferences and allows clients to subscribe for specific configuration
 * changes.
 */
public interface AppPreferences {

    /**
     * Preferences listener. Callbacks should be invoked on main thread.
     *
     * Maintainers should extend this interface with callbacks for specific
     * events.
     */
    interface Listener {
        default void onDarkThemeModeChanged(DarkMode mode) {
            /* default empty implementation */
        };
    }

    /**
     * Registers preferences listener. It no-ops if listener
     * is already registered.
     *
     * @param listener application preferences listener
     */
    void addListener(@Nullable Listener listener);

    /**
     * Unregister listener. It no-ops if listener is not registered.
     *
     * @param listener application preferences listener
     */
    void removeListener(@Nullable Listener listener);

    void setKeysReInitEnabled();
    boolean isKeysReInitEnabled();

    void setPushToken(String pushToken);
    String getPushToken();

    boolean instantPictureUploadEnabled();
    boolean instantVideoUploadEnabled();

    boolean isShowHiddenFilesEnabled();
    void setShowHiddenFilesEnabled(boolean enabled);

    /**
     * Gets the selected file extension position the user selected to do the
     * last upload of a url file shared from other app.
     *
     * @return selectedPos     the selected file extension position.
     */
    int getUploadUrlFileExtensionUrlSelectedPos();

    /**
     * Saves the selected file extension position the user selected to do the
     * last upload of a url file shared from other app.
     *
     * @param selectedPos the selected file extension position.
     */
    void setUploadUrlFileExtensionUrlSelectedPos(int selectedPos);

    /**
     * Gets the selected map file extension position the user selected to
     * do the last upload of a url file shared from other app.
     *
     * @return selectedPos     the selected file extension position.
     */
    int getUploadMapFileExtensionUrlSelectedPos();

    /**
     * Saves the selected map file extension position the user selected to
     * do the last upload of a url file shared from other app.
     *
     * @param selectedPos the selected file extension position.
     */
    void setUploadMapFileExtensionUrlSelectedPos(int selectedPos);

    /**
     * Gets the last local path where the user selected to do an upload from.
     *
     * @return path     Absolute path to a folder, as previously stored by
     * {@link #setUploadFromLocalLastPath(String)}, or empty String if never saved before.
     */
    String getUploadFromLocalLastPath();

    /**
     * Saves the path where the user selected to do the last local upload of a file from.
     *
     * @param path    Absolute path to a folder.
     */
    void setUploadFromLocalLastPath(String path);

    /**
     * Gets the path where the user selected to do the last upload of a file shared from other app.
     *
     * @return path     Absolute path to a folder, as previously stored by {@link #setLastUploadPath(String)},
     * or empty String if never saved before.
     */
    String getLastUploadPath();

    /**
     * Get preferred folder display type.
     *
     * @param folder Folder
     * @return preference value, default is
     * {@link com.owncloud.android.ui.fragment.OCFileListFragment#FOLDER_LAYOUT_LIST}
     */
    String getFolderLayout(OCFile folder);

    /**
     * Set preferred folder display type.
     *
     * @param folder Folder which layout is being set or null for root folder
     * @param layoutName preference value
     */
    void setFolderLayout(@Nullable OCFile folder, String layoutName);

    /**
     * Saves the path where the user selected to do the last upload of a file shared from other app.
     *
     * @param path    Absolute path to a folder.
     */
    void setLastUploadPath(String path);

    String getLockPreference();
    void setLockPreference(String lockPreference);

    /**
     * Set pass code composed of 4 digits (as strings).
     *
     * @todo This must be refactored further to use a passcode stype
     * @param d1 1st digit
     * @param d2 2nd digit
     * @param d3 3rd digit
     * @param d4 4th digit
     */
    void setPassCode(String d1, String d2, String d3, String d4);

    /**
     * Get 4-digit passcode as array of strings. Strings may be null.
     *
     * @return 4 strings with digits or nulls
     */
    String[] getPassCode();

    /**
     * Gets the unlock via fingerprint preference configured by the user.
     *
     * @implNote  this is always false
     * @return useFingerprint     is unlock with fingerprint enabled
     */
    boolean isFingerprintUnlockEnabled();

    /**
     * Gets the auto upload paths flag last set.
     *
     * @return ascending order     the legacy cleaning flag, default is false
     */
    boolean isAutoUploadPathsUpdateEnabled();

    /**
     * Saves the legacy cleaning flag which the user has set last.
     *
     * @param pathUpdate flag if it is a auto upload path update
     */
    void setAutoUploadPathsUpdateEnabled(boolean pathUpdate);

    /**
     * Gets the auto upload split out flag last set.
     *
     * @return ascending order     the legacy cleaning flag, default is false
     */
    boolean isAutoUploadSplitEntriesEnabled();

    /**
     * Saves the flag for split entries magic
     *
     * @param splitOut flag if it is a auto upload path update
     */
    void setAutoUploadSplitEntriesEnabled(boolean splitOut);

    boolean isAutoUploadInitialized();
    void setAutoUploadInit(boolean autoUploadInit);

    /**
     * Get preferred folder sort order.
     *
     * @param folder Folder whoch order is being retrieved or null for root folder
     * @return sort order     the sort order, default is {@link FileSortOrder#sort_a_to_z} (sort by name)
     */
    FileSortOrder getSortOrderByFolder(@Nullable OCFile folder);

    /**
     * Set preferred folder sort order.
     *
     * @param folder Folder which sort order is changed; if null, root folder is assumed
     * @param sortOrder the sort order of a folder
     */
    void setSortOrder(@Nullable OCFile folder, FileSortOrder sortOrder);

    /**
     * Set preferred folder sort order.
     *
     * @param sortOrder the sort order
     */
    void setSortOrder(FileSortOrder.Type type, FileSortOrder sortOrder);

    /**
     * Get preferred folder sort order.
     *
     * @return sort order     the sort order, default is {@link FileSortOrder#sort_a_to_z} (sort by name)
     */
    FileSortOrder getSortOrderByType(FileSortOrder.Type type, FileSortOrder defaultOrder);
    FileSortOrder getSortOrderByType(FileSortOrder.Type type);


    /**
     * Gets the legacy cleaning flag last set.
     *
     * @return ascending order     the legacy cleaning flag, default is false
     */
    boolean isLegacyClean();

    /**
     * Saves the legacy cleaning flag which the user has set last.
     *
     * @param legacyClean flag if it is a legacy cleaning
     */
    void setLegacyClean(boolean legacyClean);

    boolean isKeysMigrationEnabled();
    void setKeysMigrationEnabled(boolean enabled);

    boolean isStoragePathFixEnabled();
    void setStoragePathFixEnabled(boolean enabled);

    boolean isShowDetailedTimestampEnabled();
    void setShowDetailedTimestampEnabled(boolean showDetailedTimestamp);

    boolean isShowMediaScanNotifications();
    void setShowMediaScanNotifications(boolean showMediaScanNotification);

    /**
     * Gets the uploader behavior which the user has set last.
     *
     * @return uploader behavior     the uploader behavior
     */
    int getUploaderBehaviour();

    /**
     * Changes dark theme mode
     *
     * This is reactive property. Listeners will be invoked if registered.
     *
     * @param mode dark mode setting: on, off, system
     */
    void setDarkThemeMode(DarkMode mode);

    /**
     * @return dark mode setting: on, off, system
     */
    DarkMode getDarkThemeMode();

    /**
     * Saves the uploader behavior which the user has set last.
     *
     * @param uploaderBehaviour the uploader behavior
     */
    void setUploaderBehaviour(int uploaderBehaviour);

    float getGridColumns();
    void setGridColumns(float gridColumns);

    long getLockTimestamp();
    void setLockTimestamp(long timestamp);

    /**
     * Gets the last seen version code right before updating.
     *
     * @return grid columns     grid columns
     */
    int getLastSeenVersionCode();

    /**
     * Saves the version code as the last seen version code.
     *
     * @param versionCode the app's version code
     */
    void setLastSeenVersionCode(int versionCode);

    void removeLegacyPreferences();

    /**
     * Clears all user preferences.
     *
     * @implNote this clears only shared preferences, not preferences kept in account manager
     */
    void clear();

    String getStoragePath(String defaultPath);

    void setStoragePath(String path);

    void setStoragePathValid();

    boolean isStoragePathValid();

    void removeKeysMigrationPreference();

    String getCurrentAccountName();

    void setCurrentAccountName(String accountName);

    /**
     * Gets status of migration to user id, default false
     *
     * @return true: migration done: every account has userId, false: pending accounts without userId
     */
    boolean isUserIdMigrated();

    void setMigratedUserId(boolean value);

    void setPhotoSearchTimestamp(long timestamp);

    long getPhotoSearchTimestamp();

    boolean isPowerCheckDisabled();

    void setPowerCheckDisabled(boolean value);

    void increasePinWrongAttempts();

    void resetPinWrongAttempts();

    int pinBruteForceDelay();
}
