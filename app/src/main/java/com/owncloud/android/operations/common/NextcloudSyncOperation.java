/*
 *   ownCloud Android client application
 *
 *   @author David A. Velasco
 *   Copyright (C) 2015 ownCloud Inc.
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License version 2,
 *   as published by the Free Software Foundation.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package com.owncloud.android.operations.common;

import com.owncloud.android.datamodel.FileDataStorageManager;
import com.owncloud.android.lib.common.operations.NextcloudRemoteOperation;

import androidx.annotation.NonNull;


/**
 * Operation which execution involves both interactions with an ownCloud server and with local data in the device.
 * <p>
 * Provides methods to execute the operation both synchronously or asynchronously.
 */
public abstract class NextcloudSyncOperation<T> extends NextcloudRemoteOperation<T> {
    private final FileDataStorageManager storageManager;

    public NextcloudSyncOperation(@NonNull FileDataStorageManager storageManager) {
        this.storageManager = storageManager;
    }
    
    public FileDataStorageManager getStorageManager() {
        return this.storageManager;
    }
}
