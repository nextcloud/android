/*
 *   ownCloud Android client application
 *
 *   @author masensio
 *   @author Andy Scherzinger
 *   Copyright (C) 2015 ownCloud Inc.
 *   Copyright (C) 2018 Andy Scherzinger
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
 */

package com.owncloud.android.operations;

import android.content.Context;

import com.nextcloud.client.account.User;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.lib.common.OwnCloudClient;
import com.owncloud.android.lib.common.operations.RemoteOperation;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.common.operations.RemoteOperationResult.ResultCode;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.lib.resources.files.ExistenceCheckRemoteOperation;
import com.owncloud.android.lib.resources.shares.OCShare;
import com.owncloud.android.lib.resources.shares.RemoveShareRemoteOperation;
import com.owncloud.android.lib.resources.shares.ShareType;
import com.owncloud.android.operations.common.SyncOperation;

import java.util.List;

/**
 * create folder only if it doesn't exist in remote
 */
public class CreateFolderIfNotExistOperation extends SyncOperation {

    private static final String TAG = CreateFolderIfNotExistOperation.class.getSimpleName();

    private final String mRemotePath;
    private final User user;
    private final Context context;

    public CreateFolderIfNotExistOperation(String remotePath, User user, Context context) {
        mRemotePath = remotePath;
        this.user = user;
        this.context = context;
    }

    @Override
    protected RemoteOperationResult run(OwnCloudClient client) {
        RemoteOperation operation = new ExistenceCheckRemoteOperation(mRemotePath, false);
        RemoteOperationResult result = operation.execute(client);

        //if remote folder doesn't exist then create it else ignore it
        if (!result.isSuccess() && result.getCode() == ResultCode.FILE_NOT_FOUND) {
            SyncOperation syncOp = new CreateFolderOperation(mRemotePath, user, context);
            result = syncOp.execute(client, getStorageManager());
        }

        return result;
    }
}
