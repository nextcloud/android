/*
 * Nextcloud Android client application
 *
 * @author Mario Danic
 * @author Andy Scherzinger
 * Copyright (C) 2017 Mario Danic
 * Copyright (C) 2017 Nextcloud
 * Copyright (C) 2018 Andy Scherzinger
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

import androidx.annotation.Nullable;

/**
 * Model for filesystem data from the database.
 */
public class FileSystemDataSet {
    private int id;
    private String localPath;
    private long modifiedAt;
    private boolean folder;
    private boolean sentForUpload;
    private long foundAt;
    private long syncedFolderId;
    @Nullable private String crc32;

    public FileSystemDataSet(int id, String localPath, long modifiedAt, boolean folder, boolean sentForUpload, long foundAt, long syncedFolderId, String crc32) {
        this.id = id;
        this.localPath = localPath;
        this.modifiedAt = modifiedAt;
        this.folder = folder;
        this.sentForUpload = sentForUpload;
        this.foundAt = foundAt;
        this.syncedFolderId = syncedFolderId;
        this.crc32 = crc32;
    }

    public FileSystemDataSet() {
        // empty constructor
    }

    public int getId() {
        return this.id;
    }

    public String getLocalPath() {
        return this.localPath;
    }

    public long getModifiedAt() {
        return this.modifiedAt;
    }

    public boolean isFolder() {
        return this.folder;
    }

    public boolean isSentForUpload() {
        return this.sentForUpload;
    }

    public long getFoundAt() {
        return this.foundAt;
    }

    public long getSyncedFolderId() {
        return this.syncedFolderId;
    }

    @Nullable
    public String getCrc32() {
        return this.crc32;
    }

    public void setId(int id) {
        this.id = id;
    }

    public void setLocalPath(String localPath) {
        this.localPath = localPath;
    }

    public void setModifiedAt(long modifiedAt) {
        this.modifiedAt = modifiedAt;
    }

    public void setFolder(boolean folder) {
        this.folder = folder;
    }

    public void setSentForUpload(boolean sentForUpload) {
        this.sentForUpload = sentForUpload;
    }

    public void setFoundAt(long foundAt) {
        this.foundAt = foundAt;
    }

    public void setSyncedFolderId(long syncedFolderId) {
        this.syncedFolderId = syncedFolderId;
    }

    public void setCrc32(@Nullable String crc32) {
        this.crc32 = crc32;
    }
}
