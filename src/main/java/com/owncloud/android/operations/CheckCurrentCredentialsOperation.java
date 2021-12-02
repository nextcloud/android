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

import com.nextcloud.client.account.User;
import com.owncloud.android.datamodel.FileDataStorageManager;
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

    private final User user;

    public CheckCurrentCredentialsOperation(User user, FileDataStorageManager storageManager) {
        super(storageManager);
        this.user = user;
    }

    @Override
    protected RemoteOperationResult run(OwnCloudClient client) {
        RemoteOperationResult result;
        boolean validAccount = user.nameEquals(getStorageManager().getAccount().name);
        if (!validAccount) {
            result = new RemoteOperationResult(new IllegalStateException(
                "Account to validate is not the account connected to!")
            );
        } else {
            RemoteOperation check = new ExistenceCheckRemoteOperation(OCFile.ROOT_PATH, false);
            result = check.execute(client);
            ArrayList<Object> data = new ArrayList<>();
            data.add(user.toPlatformAccount());
            result.setData(data);
        }

        return result;
    }

}
