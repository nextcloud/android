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

import android.text.TextUtils;

import com.owncloud.android.datamodel.FileDataStorageManager;
import com.owncloud.android.lib.common.OwnCloudClient;
import com.owncloud.android.lib.common.operations.RemoteOperation;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.resources.files.FileUtils;
import com.owncloud.android.lib.resources.shares.GetShareRemoteOperation;
import com.owncloud.android.lib.resources.shares.OCShare;
import com.owncloud.android.lib.resources.shares.UpdateShareRemoteOperation;
import com.owncloud.android.operations.common.SyncOperation;


/**
 * Updates an existing private share for a given file.
 */
public class UpdateSharePermissionsOperation extends SyncOperation {

    private final long shareId;
    private int permissions;
    private long expirationDateInMillis;
    private String password;
    private String path;

    /**
     * Constructor
     *
     * @param shareId Private {@link OCShare} to update. Mandatory argument
     */
    public UpdateSharePermissionsOperation(long shareId, FileDataStorageManager storageManager) {
        super(storageManager);

        this.shareId = shareId;
        permissions = -1;
        expirationDateInMillis = 0L;
        password = null;
    }

    @Override
    protected RemoteOperationResult run(OwnCloudClient client) {

        OCShare share = getStorageManager().getShareById(shareId); // ShareType.USER | ShareType.GROUP

        if (share == null) {
            // TODO try to get remote share before failing?
            return new RemoteOperationResult(RemoteOperationResult.ResultCode.SHARE_NOT_FOUND);
        }

        path = share.getPath();

        // Update remote share with password
        UpdateShareRemoteOperation updateOp = new UpdateShareRemoteOperation(share.getRemoteId());
        updateOp.setPassword(password);
        updateOp.setPermissions(permissions);
        updateOp.setExpirationDate(expirationDateInMillis);
        RemoteOperationResult result = updateOp.execute(client);

        if (result.isSuccess()) {
            RemoteOperation getShareOp = new GetShareRemoteOperation(share.getRemoteId());
            result = getShareOp.execute(client);
            if (result.isSuccess()) {
                share = (OCShare) result.getData().get(0);
                // TODO check permissions are being saved
                updateData(share);
            }
        }

        return result;
    }

    private void updateData(OCShare share) {
        // Update DB with the response
        share.setPath(path);   // TODO - check if may be moved to UpdateRemoteShareOperation
        share.setFolder(path.endsWith(FileUtils.PATH_SEPARATOR));

        share.setPasswordProtected(!TextUtils.isEmpty(password));
        getStorageManager().saveShare(share);
    }

    public String getPassword() {
        return this.password;
    }

    public String getPath() {
        return this.path;
    }

    public void setPermissions(int permissions) {
        this.permissions = permissions;
    }

    public void setExpirationDateInMillis(long expirationDateInMillis) {
        this.expirationDateInMillis = expirationDateInMillis;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}

