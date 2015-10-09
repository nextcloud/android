/**
 *   ownCloud Android client application
 *
 *   @author masensio
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
package com.owncloud.android.operations;

import com.owncloud.android.lib.common.OwnCloudClient;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.resources.users.GetRemoteUsersOperation;
import com.owncloud.android.operations.common.SyncOperation;

/**
 * Get the users from the server
 */
public class GetUsersOperation  extends SyncOperation{

    private static final String TAG = GetUsersOperation.class.getSimpleName();

    private String mSearchString;
    private int mLimit;
    private int mOffset;

    /**
     * Constructor
     *
     * @param searchString  	string for searching users, optional
     * @param limit 			limit, optional
     * @param offset			offset, optional
     */
    public GetUsersOperation(String searchString, int limit, int offset) {
        mSearchString = searchString;
        mLimit = limit;
        mOffset = offset;
    }

    @Override
    protected RemoteOperationResult run(OwnCloudClient client) {
        GetRemoteUsersOperation operation = new GetRemoteUsersOperation(mSearchString,
                mLimit, mOffset);
        RemoteOperationResult result = operation.execute(client);

        return result;
    }
}
