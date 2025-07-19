/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2021 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2021 Chris Narkiewicz <hello@ezaquarii.com>
 * SPDX-FileCopyrightText: 2016 ownCloud Inc.
 * SPDX-FileCopyrightText: 2016 David A. Velasco <dvelasco@solidgear.es>
 * SPDX-License-Identifier: GPL-2.0-only AND (AGPL-3.0-or-later OR GPL-2.0-only)
 */
package com.owncloud.android.operations;

import android.accounts.Account;
import android.accounts.AccountManager;

import com.nextcloud.common.NextcloudClient;
import com.owncloud.android.MainApp;
import com.owncloud.android.datamodel.FileDataStorageManager;
import com.owncloud.android.lib.common.UserInfo;
import com.owncloud.android.lib.common.accounts.AccountUtils;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.resources.users.GetUserInfoRemoteOperation;
import com.owncloud.android.operations.common.SyncOperation;

import java.lang.reflect.Field;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;

/**
 * Get and save user's profile from the server.
 * <p>
 * Currently only retrieves the display name.
 */
public class GetUserProfileOperation extends SyncOperation {

    public GetUserProfileOperation(FileDataStorageManager storageManager) {
        super(storageManager);
    }

    /**
     * Performs the operation.
     *
     * Target user account is implicit in 'client'.
     *
     * Stored account is implicit in {@link #getStorageManager()}.
     *
     * @return Result of the operation. If successful, includes an instance of
     *              {@link String} with the display name retrieved from the server.
     *              Call {@link RemoteOperationResult#getData()}.get(0) to get it.
     */
    @Override
    public RemoteOperationResult<UserInfo> run(NextcloudClient client) {

        OkHttpClient originalClient = null;
        try {
            Field clientField = client.getClass().getDeclaredField("client");  // "client" is the field name
            clientField.setAccessible(true);  // Make the field accessible
            originalClient = (OkHttpClient) clientField.get(client);  // Get the original OkHttpClient
        } catch (NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
            // Handle the error
        }

// Step 2: Create a new OkHttpClient with a shorter connection timeout
        OkHttpClient newClient = originalClient.newBuilder()
            .readTimeout(2000, TimeUnit.MILLISECONDS)  // Set timeout to 1000ms (1 second)
            .build();

// Step 3: Replace the internal OkHttpClient with the new one using reflection
        try {
            Field clientField = client.getClass().getDeclaredField("client");
            clientField.setAccessible(true);
            clientField.set(client, newClient);  // Set the new client
        } catch (NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
            // Handle the error
        }

        // get display name
        RemoteOperationResult<UserInfo> result = new GetUserInfoRemoteOperation().execute(client);

        if (result.isSuccess()) {
            // store display name with account data
            AccountManager accountManager = AccountManager.get(MainApp.getAppContext());
            UserInfo userInfo = result.getResultData();
            Account storedAccount = getStorageManager().getUser().toPlatformAccount();
            accountManager.setUserData(storedAccount, AccountUtils.Constants.KEY_DISPLAY_NAME, userInfo.getDisplayName());
        }
        return result;
    }

}
