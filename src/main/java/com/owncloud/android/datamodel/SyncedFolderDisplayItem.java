/*
 *   Nextcloud Android client application
 *
 *   @author Andy Scherzinger
 *   Copyright (C) 2016 Andy Scherzinger
 *   Copyright (C) 2016 Nextcloud
 *
 *   This program is free software; you can redistribute it and/or
 *   modify it under the terms of the GNU AFFERO GENERAL PUBLIC LICENSE
 *   License as published by the Free Software Foundation; either
 *   version 3 of the License, or any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU AFFERO GENERAL PUBLIC LICENSE for more details.
 *
 *   You should have received a copy of the GNU Affero General Public
 *   License along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.owncloud.android.datamodel;

import java.util.List;

/**
 * Display item specialization for synced folder objects to be displayed in a list/grid view adding further
 * information to be displayed in the UI but not part of the persisted underlying {@link SyncedFolder} object.
 */
public class SyncedFolderDisplayItem extends SyncedFolder {
    private List<String> filePaths;
    private String folderName;
    private long numberOfFiles;

    /**
     * constructor for the display item specialization for a synced folder object.
     *
     * @param id              id
     * @param localPath       local path
     * @param remotePath      remote path
     * @param wifiOnly        upload on wifi only flag
     * @param chargingOnly    upload on charging only
     * @param existing        also upload existing
     * @param subfolderByDate create sub-folders by date (month)
     * @param account         the account owning the synced folder
     * @param uploadAction    the action to be done after the upload
     * @param enabled         flag if synced folder config is active
     * @param filePaths       the UI info for the file path
     * @param folderName      the UI info for the folder's name
     * @param numberOfFiles   the UI info for number of files within the folder
     * @param type            the type of the folder
     * @param hidden          hide item flag
     */
    public SyncedFolderDisplayItem(long id,
                                   String localPath,
                                   String remotePath,
                                   boolean wifiOnly,
                                   boolean chargingOnly,
                                   boolean existing,
                                   boolean subfolderByDate,
                                   String account,
                                   int uploadAction,
                                   int nameCollisionPolicy,
                                   boolean enabled,
                                   long timestampMs,
                                   List<String> filePaths,
                                   String folderName,
                                   long numberOfFiles,
                                   MediaFolderType type,
                                   boolean hidden) {
        super(id,
              localPath,
              remotePath,
              wifiOnly,
              chargingOnly,
              existing,
              subfolderByDate,
              account,
              uploadAction,
              nameCollisionPolicy,
              enabled,
              timestampMs,
              type,
              hidden);
        this.filePaths = filePaths;
        this.folderName = folderName;
        this.numberOfFiles = numberOfFiles;
    }

    public SyncedFolderDisplayItem(long id,
                                   String localPath,
                                   String remotePath,
                                   boolean wifiOnly,
                                   boolean chargingOnly,
                                   boolean existing,
                                   boolean subfolderByDate,
                                   String account,
                                   int uploadAction,
                                   int nameCollisionPolicy,
                                   boolean enabled,
                                   long timestampMs,
                                   String folderName,
                                   MediaFolderType type,
                                   boolean hidden) {
        super(id,
              localPath,
              remotePath,
              wifiOnly,
              chargingOnly,
              existing,
              subfolderByDate,
              account,
              uploadAction,
              nameCollisionPolicy,
              enabled,
              timestampMs,
              type,
              hidden);
        this.folderName = folderName;
    }

    public List<String> getFilePaths() {
        return this.filePaths;
    }

    public String getFolderName() {
        return this.folderName;
    }

    public long getNumberOfFiles() {
        return this.numberOfFiles;
    }

    public void setFilePaths(List<String> filePaths) {
        this.filePaths = filePaths;
    }

    public void setFolderName(String folderName) {
        this.folderName = folderName;
    }

    public void setNumberOfFiles(long numberOfFiles) {
        this.numberOfFiles = numberOfFiles;
    }
}
