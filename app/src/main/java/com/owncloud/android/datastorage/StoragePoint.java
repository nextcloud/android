/*
 *   Nextcloud Android client application
 *
 *   @author Bartosz Przybylski
 *   Copyright (C) 2016 Nextcloud
 *   Copyright (C) 2016 Bartosz Przybylski
 *
 * SPDX-License-Identifier: GPL-2.0-only AND (AGPL-3.0-or-later OR GPL-2.0-only)
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
