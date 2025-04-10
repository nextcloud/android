/*
 * Nextcloud Android client application
 *
 * @author TSI-mc
 * Copyright (C) 2021 TSI-mc
 * Copyright (C) 2021 Nextcloud GmbH
 *
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */

package com.owncloud.android.operations;

import android.text.TextUtils;

import com.owncloud.android.datamodel.FileDataStorageManager;
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
    private String label;
    private String attributes;

    /**
     * Constructor
     *
     * @param share {@link OCShare} to update. Mandatory argument
     *              <p>
     *              this will be triggered while creating new share
     */
    public UpdateShareInfoOperation(OCShare share, FileDataStorageManager storageManager) {
        super(storageManager);

        this.share = share;
        expirationDateInMillis = 0L;
        note = null;
    }

    /**
     * Constructor
     *
     * @param shareId {@link OCShare} to update. Mandatory argument
     *                <p>
     *                this will be triggered while modifying existing share
     */
    public UpdateShareInfoOperation(long shareId, FileDataStorageManager storageManager) {
        super(storageManager);
        
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

        // Update remote share
        UpdateShareRemoteOperation updateOp = new UpdateShareRemoteOperation(share.getRemoteId());
        updateOp.setExpirationDate(expirationDateInMillis);
        updateOp.setHideFileDownload(hideFileDownload);
        if (!TextUtils.isEmpty(note)) {
            updateOp.setNote(note);
        }
        if (permissions > -1) {
            updateOp.setPermissions(permissions);
        }
        updateOp.setPassword(password);
        updateOp.setLabel(label);
        updateOp.setAttributes(attributes);

        RemoteOperationResult result = updateOp.execute(client);

        if (result.isSuccess()) {
            RemoteOperation getShareOp = new GetShareRemoteOperation(share.getRemoteId());
            result = getShareOp.execute(client);

            //only update the share in storage if shareId is available
            //this will be triggered by editing existing share
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

    public void setAttributes(String attributes) {
        this.attributes = attributes;
    }

    public void setPermissions(int permissions) {
        this.permissions = permissions;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setLabel(String label) {
        this.label = label;
    }
}

