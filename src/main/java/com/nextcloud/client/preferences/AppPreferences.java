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

public interface AppPreferences {
    boolean instantPictureUploadEnabled();
    boolean instantVideoUploadEnabled();

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
     * Saves the path where the user selected to do the last upload of a file shared from other app.
     *
     * @param path    Absolute path to a folder.
     */
    void setLastUploadPath(String path);

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
     * Saves the uploader behavior which the user has set last.
     *
     * @param uploaderBehaviour the uploader behavior
     */
    void setUploaderBehaviour(int uploaderBehaviour);

    float getGridColumns();
    void setGridColumns(float gridColumns);

    void removeLegacyPreferences();
}
