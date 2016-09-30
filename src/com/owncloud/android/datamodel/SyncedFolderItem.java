/**
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
 * TODO javadoc
 */
public class SyncedFolderItem extends SyncedFolder {
    public static final long UNPERSISTED_ID = Long.MIN_VALUE;
    private List<String> filePaths;
    private String folderName;
    private long numberOfFiles;

    public SyncedFolderItem(long id, String localPath, String remotePath, Boolean wifiOnly, Boolean chargingOnly,
                            Boolean subfolderByDate, String account, Integer uploadAction, Boolean enabled,
                            List<String> filePaths, String folderName, long numberOfFiles) {
        super(id, localPath, remotePath, wifiOnly, chargingOnly, subfolderByDate, account, uploadAction, enabled);
        this.filePaths = filePaths;
        this.folderName = folderName;
        this.numberOfFiles = numberOfFiles;
    }

    public List<String> getFilePaths() {
        return filePaths;
    }

    public void setFilePaths(List<String> filePaths) {
        this.filePaths = filePaths;
    }

    public String getFolderName() {
        return folderName;
    }

    public void setFolderName(String folderName) {
        this.folderName = folderName;
    }

    public long getNumberOfFiles() {
        return numberOfFiles;
    }

    public void setNumberOfFiles(long numberOfFiles) {
        this.numberOfFiles = numberOfFiles;
    }
}
