/*
 *   ownCloud Android client application
 *
 *   @author David A. Velasco
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
import com.owncloud.android.lib.resources.shares.GetShareRemoteOperation;
import com.owncloud.android.lib.resources.shares.OCShare;
import com.owncloud.android.lib.resources.shares.UpdateShareRemoteOperation;
import com.owncloud.android.operations.common.SyncOperation;

import java.util.List;


/**
 * Updates an existing public share for a given file
 */
public class UpdateShareViaLinkOperation extends SyncOperation<List<OCShare>> {
    private String password;
    private Boolean hideFileDownload;
    private long expirationDateInMillis;
    private final long shareId;
    private String label;

    public UpdateShareViaLinkOperation(long shareId) {
        expirationDateInMillis = 0;
        this.shareId = shareId;
    }

    @Override
    protected RemoteOperationResult<List<OCShare>> run(OwnCloudClient client) {
        OCShare publicShare = getStorageManager().getShareById(shareId);

        UpdateShareRemoteOperation updateOp = new UpdateShareRemoteOperation(publicShare.getRemoteId());
        updateOp.setPassword(password);
        updateOp.setExpirationDate(expirationDateInMillis);
        updateOp.setHideFileDownload(hideFileDownload);
        updateOp.setLabel(label);

        RemoteOperationResult<List<OCShare>> result = updateOp.execute(client);

        if (result.isSuccess()) {
            // Retrieve updated share / save directly with password? -> no; the password is not to be saved
            result = new GetShareRemoteOperation(publicShare.getRemoteId()).execute(client);
            if (result.isSuccess()) {
                OCShare share = result.getResultData().get(0);
                getStorageManager().saveShare(share);
            }
        }

        return result;
    }

    public String getPassword() {
        return this.password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setHideFileDownload(Boolean hideFileDownload) {
        this.hideFileDownload = hideFileDownload;
    }

    public void setExpirationDateInMillis(long expirationDateInMillis) {
        this.expirationDateInMillis = expirationDateInMillis;
    }

    public void setLabel(String label) {
        this.label = label;
    }
}
