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

import android.content.Context;
import android.os.Handler;

import com.nextcloud.common.NextcloudClient;
import com.owncloud.android.datamodel.FileDataStorageManager;
import com.owncloud.android.lib.common.OwnCloudClient;
import com.owncloud.android.lib.common.operations.OnRemoteOperationListener;
import com.owncloud.android.lib.common.operations.RemoteOperation;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;

import androidx.annotation.NonNull;


/**
 * Operation which execution involves both interactions with an ownCloud server and with local data in the device.
 * <p>
 * Provides methods to execute the operation both synchronously or asynchronously.
 */
public abstract class SyncOperation extends RemoteOperation {
    private final FileDataStorageManager storageManager;

    public SyncOperation(@NonNull FileDataStorageManager storageManager) {
        this.storageManager = storageManager;
    }

    /**
     * Synchronously executes the operation on the received ownCloud account.
     * <p>
     * Do not call this method from the main thread.
     * <p>
     * This method should be used whenever an ownCloud account is available, instead of {@link
     * #execute(OwnCloudClient)}.
     *
     * @param context Android context for the component calling the method.
     * @return Result of the operation.
     */
    public RemoteOperationResult execute(Context context) {
        if (storageManager.getAccount() == null) {
            throw new IllegalArgumentException("Trying to execute a sync operation with a " +
                                                   "storage manager for a NULL account");
        }
        return super.execute(this.storageManager.getAccount(), context);
    }

    public RemoteOperationResult execute(@NonNull NextcloudClient client) {
        return run(client);
    }

    /**
     * Asynchronously executes the remote operation
     *
     * @param client            Client object to reach an ownCloud server during the
     *                          execution of the operation.
     * @param listener            Listener to be notified about the execution of the operation.
     * @param listenerHandler    Handler associated to the thread where the methods of
     *                          the listener objects must be called.
     * @return Thread were the remote operation is executed.
     */
    public Thread execute(OwnCloudClient client,
                          OnRemoteOperationListener listener,
                          Handler listenerHandler) {
        return super.execute(client, listener, listenerHandler);
    }

    public FileDataStorageManager getStorageManager() {
        return this.storageManager;
    }
}
