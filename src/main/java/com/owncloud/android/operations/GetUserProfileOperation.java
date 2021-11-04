/**
 *   ownCloud Android client application
 *
 *   @author David A. Velasco
 *   Copyright (C) 2016 ownCloud Inc.
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

        // get display name
        RemoteOperationResult<UserInfo> result = new GetUserInfoRemoteOperation().execute(client);

        if (result.isSuccess()) {
            // store display name with account data
            AccountManager accountManager = AccountManager.get(MainApp.getAppContext());
            UserInfo userInfo = result.getResultData();
            Account storedAccount = getStorageManager().getAccount();
            accountManager.setUserData(storedAccount, AccountUtils.Constants.KEY_DISPLAY_NAME, userInfo.getDisplayName());
        }
        return result;
    }

}
