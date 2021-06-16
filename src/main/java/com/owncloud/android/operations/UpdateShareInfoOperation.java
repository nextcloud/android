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

import com.owncloud.android.lib.common.OwnCloudClient;
import com.owncloud.android.lib.common.operations.RemoteOperation;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.resources.shares.GetShareRemoteOperation;
import com.owncloud.android.lib.resources.shares.OCShare;
import com.owncloud.android.lib.resources.shares.UpdateShareRemoteOperation;
import com.owncloud.android.operations.common.SyncOperation;


/**
 * Updates an existing private share for a given file.
 */
public class UpdateShareInfoOperation extends SyncOperation {

    private OCShare share;
    private long shareId;
    private long expirationDateInMillis;
    private String note;
    private boolean hideFileDownload;
    private int permissions = -1;
    private String password;

    /**
     * Constructor
     *
     * @param share {@link OCShare} to update. Mandatory argument
     */
    public UpdateShareInfoOperation(OCShare share) {
        this.share = share;
        expirationDateInMillis = 0L;
        note = null;
    }

    public UpdateShareInfoOperation(long shareId) {
        this.shareId = shareId;
        expirationDateInMillis = 0L;
        note = null;
    }

    @Override
    protected RemoteOperationResult run(OwnCloudClient client) {

        OCShare share;
        if (shareId > 0) {
            share = getStorageManager().getShareById(shareId);
        } else {
            share = this.share;
        }

        if (share == null) {
            // TODO try to get remote share before failing?
            return new RemoteOperationResult(RemoteOperationResult.ResultCode.SHARE_NOT_FOUND);
        }

        // Update remote share with password
        UpdateShareRemoteOperation updateOp = new UpdateShareRemoteOperation(share.getRemoteId());
        updateOp.setExpirationDate(expirationDateInMillis);
        updateOp.setHideFileDownload(hideFileDownload);
        if (!TextUtils.isEmpty(note)) {
            updateOp.setNote(note);
        }
        if (permissions > -1) {
            updateOp.setPermissions(permissions);
        }
        if (!TextUtils.isEmpty(password)) {
            updateOp.setPassword(password);
        }

        RemoteOperationResult result = updateOp.execute(client);

        if (result.isSuccess()) {
            RemoteOperation getShareOp = new GetShareRemoteOperation(share.getRemoteId());
            result = getShareOp.execute(client);
            if (result.isSuccess() && shareId > 0) {
                OCShare ocShare = (OCShare) result.getData().get(0);
                ocShare.setPasswordProtected(!TextUtils.isEmpty(password));
                getStorageManager().saveShare(ocShare);
            }

        }

        return result;
    }

    public void setExpirationDateInMillis(long expirationDateInMillis) {
        this.expirationDateInMillis = expirationDateInMillis;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public void setHideFileDownload(boolean hideFileDownload) {
        this.hideFileDownload = hideFileDownload;
    }

    public void setPermissions(int permissions) {
        this.permissions = permissions;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}

