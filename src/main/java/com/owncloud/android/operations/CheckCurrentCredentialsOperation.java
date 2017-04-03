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

import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.lib.common.OwnCloudClient;
import com.owncloud.android.lib.common.operations.RemoteOperation;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.resources.files.ExistenceCheckRemoteOperation;
import com.owncloud.android.operations.common.SyncOperation;

import java.util.ArrayList;

/**
 * Checks validity of currently stored credentials for a given OC account
 */
public class CheckCurrentCredentialsOperation extends SyncOperation {

    private Account mAccount = null;

    public CheckCurrentCredentialsOperation(Account account) {
        if (account == null) {
            throw new IllegalArgumentException("NULL account");
        }
        mAccount = account;
    }

    @Override
    protected RemoteOperationResult run(OwnCloudClient client) {
        RemoteOperationResult result = null;
        if (!getStorageManager().getAccount().name.equals(mAccount.name)) {
            result = new RemoteOperationResult(new IllegalStateException(
                "Account to validate is not the account connected to!")
            );
        } else {
            RemoteOperation check = new ExistenceCheckRemoteOperation(OCFile.ROOT_PATH, false);
            result = check.execute(client);
            ArrayList<Object> data = new ArrayList<Object>();
            data.add(mAccount);
            result.setData(data);
        }
        return result;
    }

}
