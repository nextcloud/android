/*
 *   Nextcloud Android client application
 *
 *   @author Bartosz Przybylski
 *   Copyright (C) 2016 Nextcloud
 *   Copyright (C) 2016 Bartosz Przybylski
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

package com.owncloud.android.datastorage;

/**
 * @author Bartosz Przybylski
 */
public class StoragePoint implements Comparable<StoragePoint> {
    private String description;
    private String path;
    private StorageType storageType;
    private PrivacyType privacyType;

    public StoragePoint(String description, String path, StorageType storageType, PrivacyType privacyType) {
        this.description = description;
        this.path = path;
        this.storageType = storageType;
        this.privacyType = privacyType;
    }

    public StoragePoint() {
        // empty constructor
    }

    public String getDescription() {
        return this.description;
    }

    public String getPath() {
        return this.path;
    }

    public StorageType getStorageType() {
        return this.storageType;
    }

    public PrivacyType getPrivacyType() {
        return this.privacyType;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public void setStorageType(StorageType storageType) {
        this.storageType = storageType;
    }

    public void setPrivacyType(PrivacyType privacyType) {
        this.privacyType = privacyType;
    }

    public enum StorageType {
        INTERNAL, EXTERNAL
    }

    public enum PrivacyType {
        PRIVATE, PUBLIC
    }

    @Override
    public int compareTo(StoragePoint another) {
        return path.compareTo(another.getPath());
    }
}
