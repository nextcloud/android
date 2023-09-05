/*
 * Nextcloud Android client application
 *
 * author Chris Narkiewicz
 * author TSI-mc
 * Copyright (C) 2019 Chris Narkiewicz, EZ Aquarii
 * Copyright (C) 2023 TSI-mc
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
package com.nextcloud.client.preferences

import com.nextcloud.appReview.AppReviewShownModel
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.utils.FileSortOrder

/**
 * This interface provides single point of entry for access to all application
 * preferences and allows clients to subscribe for specific configuration
 * changes.
 */
interface AppPreferences {
    /**
     * Preferences listener. Callbacks should be invoked on main thread.
     *
     * Maintainers should extend this interface with callbacks for specific
     * events.
     */
    interface Listener {
        fun onDarkThemeModeChanged(mode: DarkMode?) {
            /* default empty implementation */
        }
    }

    /**
     * Registers preferences listener. It no-ops if listener
     * is already registered.
     *
     * param listener application preferences listener
     */
    fun addListener(listener: Listener?)

    /**
     * Unregister listener. It no-ops if listener is not registered.
     *
     * param listener application preferences listener
     */
    fun removeListener(listener: Listener?)
    fun setKeysReInitEnabled()
    
    val isKeysReInitEnabled: Boolean
    
    var pushToken: String?
    fun instantPictureUploadEnabled(): Boolean
    fun instantVideoUploadEnabled(): Boolean
    
    var isShowHiddenFilesEnabled: Boolean
    
    var isShowEcosystemApps: Boolean
    /**
     * Gets the selected file extension position the user selected to do the
     * last upload of a url file shared from other app.
     *
     * return selectedPos     the selected file extension position.
     */
    /**
     * Saves the selected file extension position the user selected to do the
     * last upload of a url file shared from other app.
     *
     * param selectedPos the selected file extension position.
     */
    
    var uploadUrlFileExtensionUrlSelectedPos: Int
    /**
     * Gets the selected map file extension position the user selected to
     * do the last upload of a url file shared from other app.
     *
     * return selectedPos     the selected file extension position.
     */
    /**
     * Saves the selected map file extension position the user selected to
     * do the last upload of a url file shared from other app.
     *
     * param selectedPos the selected file extension position.
     */
    
    var uploadMapFileExtensionUrlSelectedPos: Int
    /**
     * Gets the last local path where the user selected to do an upload from.
     *
     * return path     Absolute path to a folder, as previously stored by
     * [.setUploadFromLocalLastPath], or empty String if never saved before.
     */
    /**
     * Saves the path where the user selected to do the last local upload of a file from.
     *
     * param path    Absolute path to a folder.
     */
    
    var uploadFromLocalLastPath: String?
    /**
     * Gets the path where the user selected to do the last upload of a file shared from other app.
     *
     * return path     Absolute path to a folder, as previously stored by [.setLastUploadPath],
     * or empty String if never saved before.
     */
    /**
     * Saves the path where the user selected to do the last upload of a file shared from other app.
     *
     * param path    Absolute path to a folder.
     */
    
    var lastUploadPath: String?

    /**
     * Get preferred folder display type.
     *
     * param folder Folder
     * return preference value, default is
     * [com.owncloud.android.ui.fragment.OCFileListFragment.FOLDER_LAYOUT_LIST]
     */
    fun getFolderLayout(folder: OCFile?): String

    /**
     * Set preferred folder display type.
     *
     * param folder Folder which layout is being set or null for root folder
     * param layoutName preference value
     */
    fun setFolderLayout(folder: OCFile?, layoutName: String)
    
    var lockPreference: String?

    /**
     * Set pass code composed of 4 digits (as strings).
     *
     * todo This must be refactored further to use a passcode stype
     * param d1 1st digit
     * param d2 2nd digit
     * param d3 3rd digit
     * param d4 4th digit
     */
    fun setPassCode(d1: String?, d2: String?, d3: String?, d4: String?)

    /**
     * Get 4-digit passcode as array of strings. Strings may be null.
     *
     * return 4 strings with digits or nulls
     */
    
    val passCode: Array<String?>

    /**
     * Gets the unlock via fingerprint preference configured by the user.
     *
     * implNote  this is always false
     * return useFingerprint     is unlock with fingerprint enabled
     */
    val isFingerprintUnlockEnabled: Boolean
    /**
     * Gets the auto upload paths flag last set.
     *
     * return ascending order     the legacy cleaning flag, default is false
     */
    /**
     * Saves the legacy cleaning flag which the user has set last.
     *
     * param pathUpdate flag if it is a auto upload path update
     */
    
    var isAutoUploadPathsUpdateEnabled: Boolean
    /**
     * Gets the auto upload split out flag last set.
     *
     * return ascending order     the legacy cleaning flag, default is false
     */
    /**
     * Saves the flag for split entries magic
     *
     * param splitOut flag if it is a auto upload path update
     */
    
    var isAutoUploadSplitEntriesEnabled: Boolean
    
    val isAutoUploadInitialized: Boolean
    fun setAutoUploadInit(autoUploadInit: Boolean)

    /**
     * Get preferred folder sort order.
     *
     * param folder Folder whoch order is being retrieved or null for root folder
     * return sort order     the sort order, default is [FileSortOrder.sort_a_to_z] (sort by name)
     */
    fun getSortOrderByFolder(folder: OCFile?): FileSortOrder?

    /**
     * Set preferred folder sort order.
     *
     * param folder Folder which sort order is changed; if null, root folder is assumed
     * param sortOrder the sort order of a folder
     */
    fun setSortOrder(folder: OCFile?, sortOrder: FileSortOrder)

    /**
     * Set preferred folder sort order.
     *
     * param sortOrder the sort order
     */
    fun setSortOrder(type: FileSortOrder.Type, sortOrder: FileSortOrder)

    /**
     * Get preferred folder sort order.
     *
     * return sort order     the sort order, default is [FileSortOrder.sort_a_to_z] (sort by name)
     */
    fun getSortOrderByType(type: FileSortOrder.Type, defaultOrder: FileSortOrder): FileSortOrder
    fun getSortOrderByType(type: FileSortOrder.Type): FileSortOrder
    /**
     * Gets the legacy cleaning flag last set.
     *
     * return ascending order     the legacy cleaning flag, default is false
     */
    /**
     * Saves the legacy cleaning flag which the user has set last.
     *
     * param legacyClean flag if it is a legacy cleaning
     */
    
    var isLegacyClean: Boolean
    var isKeysMigrationEnabled: Boolean
    
    var isStoragePathFixEnabled: Boolean
    
    var isShowDetailedTimestampEnabled: Boolean
    
    var isShowMediaScanNotifications: Boolean
    /**
     * Gets the uploader behavior which the user has set last.
     *
     * return uploader behavior     the uploader behavior
     */
    /**
     * Saves the uploader behavior which the user has set last.
     *
     * param uploaderBehaviour the uploader behavior
     */
    
    var uploaderBehaviour: Int
    /**
     * return dark mode setting: on, off, system
     */
    /**
     * Changes dark theme mode
     *
     * This is reactive property. Listeners will be invoked if registered.
     *
     * param mode dark mode setting: on, off, system
     */
    
    var darkThemeMode: DarkMode
    
    var gridColumns: Float
    
    var lockTimestamp: Long
    /**
     * Gets the last seen version code right before updating.
     *
     * return grid columns     grid columns
     */
    /**
     * Saves the version code as the last seen version code.
     *
     * param versionCode the app's version code
     */
    
    var lastSeenVersionCode: Int
    fun removeLegacyPreferences()

    /**
     * Clears all user preferences.
     *
     * implNote this clears only shared preferences, not preferences kept in account manager
     */
    fun clear()
    fun getStoragePath(defaultPath: String?): String?
    fun setStoragePath(path: String?)
    fun setStoragePathValid()
    
    val isStoragePathValid: Boolean
    fun removeKeysMigrationPreference()
    var currentAccountName: String?

    /**
     * Gets status of migration to user id, default false
     *
     * return true: migration done: every account has userId, false: pending accounts without userId
     */
    
    val isUserIdMigrated: Boolean
    fun setMigratedUserId(value: Boolean)
    
    var photoSearchTimestamp: Long
    var isPowerCheckDisabled: Boolean
    fun increasePinWrongAttempts()
    fun resetPinWrongAttempts()
    fun pinBruteForceDelay(): Int
    
    var uidPid: String?
    var calendarLastBackup: Long
    var pdfZoomTipShownCount: Int
    var isStoragePermissionRequested: Boolean
    fun setInAppReviewData(appReviewShownModel: AppReviewShownModel)
    fun getInAppReviewData(): AppReviewShownModel?
    
    var lastSelectedMediaFolder: String
}