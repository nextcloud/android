/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2016 Andy Scherzinger
 * SPDX-FileCopyrightText: 2016 Nextcloud
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.datamodel;

import com.nextcloud.client.preferences.SubFolderRule;

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
     * @param subFolderRule   whether to filter subFolder by year/month/day
     * @param excludeHidden   exclude hidden file or folder, for {@link MediaFolderType#CUSTOM} only
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
                                   long uploadDelayTimeMs,
                                   boolean enabled,
                                   long timestampMs,
                                   List<String> filePaths,
                                   String folderName,
                                   long numberOfFiles,
                                   MediaFolderType type,
                                   boolean hidden,
                                   SubFolderRule subFolderRule,
                                   boolean excludeHidden,
                                   long lastScanTimestampMs) {
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
              uploadDelayTimeMs,
              enabled,
              timestampMs,
              type,
              hidden,
              subFolderRule,
              excludeHidden,
              lastScanTimestampMs);
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
                                   long uploadDelayTimeMs,
                                   boolean enabled,
                                   long timestampMs,
                                   String folderName,
                                   MediaFolderType type,
                                   boolean hidden,
                                   SubFolderRule subFolderRule,
                                   boolean excludeHidden,
                                   long lastScanTimestampMs) {
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
              uploadDelayTimeMs,
              enabled,
              timestampMs,
              type,
              hidden,
              subFolderRule,
              excludeHidden,
              lastScanTimestampMs);
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
