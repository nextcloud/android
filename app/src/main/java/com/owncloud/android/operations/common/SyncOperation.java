/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2022 Chris Narkiewicz <hello@ezaquarii.com>
 * SPDX-FileCopyrightText: 2021 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2015 ownCloud Inc.
 * SPDX-FileCopyrightText: 2014 David A. Velasco <dvelasco@solidgear.es>
 * SPDX-License-Identifier: GPL-2.0-only AND (AGPL-3.0-or-later OR GPL-2.0-only)
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
        if (storageManager.getUser().isAnonymous()) {
            return new RemoteOperationResult(RemoteOperationResult.ResultCode.ACCOUNT_EXCEPTION);
        }
        return super.execute(this.storageManager.getUser(), context);
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
