/*
 *   ownCloud Android client application
 *
 *   @author David A. Velasco
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

import com.owncloud.android.lib.common.OwnCloudClient;
import com.owncloud.android.lib.common.operations.RemoteOperation;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.resources.files.FileUtils;
import com.owncloud.android.lib.resources.shares.GetRemoteShareOperation;
import com.owncloud.android.lib.resources.shares.OCShare;
import com.owncloud.android.lib.resources.shares.UpdateRemoteShareOperation;
import com.owncloud.android.operations.common.SyncOperation;


/**
 * Updates an existing private share for a given file.
 */
public class UpdateSharePermissionsOperation extends SyncOperation {

    private long mShareId;
    private int mPermissions;
    private long mExpirationDateInMillis;
    private String mPath;

    /**
     * Constructor
     *
     * @param shareId Private {@link OCShare} to update. Mandatory argument
     */
    public UpdateSharePermissionsOperation(long shareId) {
        mShareId = shareId;
        mPermissions = -1;
        mExpirationDateInMillis = 0L;
    }

    /**
     * Set permissions to update in private share.
     *
     * @param permissions   Permissions to set to the private share.
     *                      Values <= 0 result in no update applied to the permissions.
     */
    public void setPermissions(int permissions) {
        mPermissions = permissions;
    }

    /**
     * Set expiration date to update private share.
     *
     * @param expirationDateInMillis    Expiration date to set to the public link.
     *                                  A negative value clears the current expiration date.
     *                                  Zero value (start-of-epoch) results in no update done on
     *                                  the expiration date.
     */
    public void setExpirationDate(long expirationDateInMillis) {
        mExpirationDateInMillis = expirationDateInMillis;
    }

    @Override
    protected RemoteOperationResult run(OwnCloudClient client) {

        OCShare share = getStorageManager().getShareById(mShareId); // ShareType.USER | ShareType.GROUP

        if (share == null) {
            // TODO try to get remote share before failing?
            return new RemoteOperationResult(RemoteOperationResult.ResultCode.SHARE_NOT_FOUND);
        }

        mPath = share.getPath();

        // Update remote share with password
        UpdateRemoteShareOperation updateOp = new UpdateRemoteShareOperation(share.getRemoteId());
        updateOp.setPermissions(mPermissions);
        updateOp.setExpirationDate(mExpirationDateInMillis);
        RemoteOperationResult result = updateOp.execute(client);

        if (result.isSuccess()) {
            RemoteOperation getShareOp = new GetRemoteShareOperation(share.getRemoteId());
            result = getShareOp.execute(client);
            if (result.isSuccess()) {
                share = (OCShare) result.getData().get(0);
                // TODO check permissions are being saved
                updateData(share);
            }
        }

        return result;
    }

    public String getPath() {
        return mPath;
    }

    private void updateData(OCShare share) {
        // Update DB with the response
        share.setPath(mPath);   // TODO - check if may be moved to UpdateRemoteShareOperation
        if (mPath.endsWith(FileUtils.PATH_SEPARATOR)) {
            share.setIsFolder(true);
        } else {
            share.setIsFolder(false);
        }
        getStorageManager().saveShare(share);
    }

}

