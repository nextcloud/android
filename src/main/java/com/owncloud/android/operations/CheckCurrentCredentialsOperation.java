/*
 * ownCloud Android client application
 *
 * @author David A. Velasco Copyright (C) 2016 ownCloud Inc.
 * <p>
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public
 * License version 2, as published by the Free Software Foundation.
 * <p>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more
 * details.
 * <p>
 * You should have received a copy of the GNU General Public License along with this program.  If not, see
 * <http://www.gnu.org/licenses/>.
 */

package com.owncloud.android.operations;

import android.accounts.Account;

import com.nextcloud.client.account.User;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.lib.common.OwnCloudClient;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.resources.files.ExistenceCheckRemoteOperation;
import com.owncloud.android.operations.common.SyncOperation;

/**
 * Checks validity of currently stored credentials for a given OC account
 */
public class CheckCurrentCredentialsOperation extends SyncOperation<Account> {

    private final User user;

    public CheckCurrentCredentialsOperation(User user) {
        this.user = user;
    }

    @Override
    protected RemoteOperationResult<Account> run(OwnCloudClient client) {
        RemoteOperationResult<Account> result;
        boolean validAccount = user.nameEquals(getStorageManager().getAccount().name);
        if (!validAccount) {
            result = new RemoteOperationResult<>(new IllegalStateException(
                "Account to validate is not the account connected to!")
            );
        } else {
            RemoteOperationResult<Void> existenceResult = new ExistenceCheckRemoteOperation(OCFile.ROOT_PATH, false)
                .execute(client);

            result = new RemoteOperationResult<>(existenceResult.getCode());
            result.setResultData(user.toPlatformAccount());
        }

        return result;
    }
}
