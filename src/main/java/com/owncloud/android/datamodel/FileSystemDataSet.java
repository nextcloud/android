/**
 * Nextcloud Android client application
 *
 * @author Mario Danic
 * Copyright (C) 2017 Mario Danic
 * Copyright (C) 2017 Nextcloud
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU AFFERO GENERAL PUBLIC LICENSE
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU AFFERO GENERAL PUBLIC LICENSE for more details.
 *
 * You should have received a copy of the GNU Affero General Public
 * License along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.owncloud.android.datamodel;

import android.support.annotation.Nullable;

/*
    Model for filesystem data from the database
 */
public class FileSystemDataSet {

    private int id;
    private String localPath;
    private long modifiedAt;
    private boolean isFolder;
    private boolean isSentForUpload;
    private long foundAt;
    private long syncedFolderId;
    @Nullable private String crc32;

    public FileSystemDataSet() {
    }

    public FileSystemDataSet(int id, String localPath, long modifiedAt, boolean isFolder,
                             boolean isSentForUpload, long foundAt, long syncedFolderId, @Nullable String crc32) {
        this.id = id;
        this.localPath = localPath;
        this.modifiedAt = modifiedAt;
        this.isFolder = isFolder;
        this.isSentForUpload = isSentForUpload;
        this.foundAt = foundAt;
        this.syncedFolderId = syncedFolderId;
        this.crc32 = crc32;
    }

    public String getCrc32() {
        return crc32;
    }

    public void setCrc32(String crc32) {
        this.crc32 = crc32;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getLocalPath() {
        return localPath;
    }

    public void setLocalPath(String localPath) {
        this.localPath = localPath;
    }

    public long getModifiedAt() {
        return modifiedAt;
    }

    public void setModifiedAt(long modifiedAt) {
        this.modifiedAt = modifiedAt;
    }

    public boolean isFolder() {
        return isFolder;
    }

    public void setFolder(boolean folder) {
        isFolder = folder;
    }

    public long getFoundAt() {
        return foundAt;
    }

    public void setFoundAt(long foundAt) {
        this.foundAt = foundAt;
    }

    public boolean isSentForUpload() {
        return isSentForUpload;
    }

    public void setSentForUpload(boolean sentForUpload) {
        isSentForUpload = sentForUpload;
    }

    public long getSyncedFolderId() {
        return syncedFolderId;
    }

    public void setSyncedFolderId(long syncedFolderId) {
        this.syncedFolderId = syncedFolderId;
    }
}
