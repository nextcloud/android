/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2020-2021 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2015 ownCloud Inc.
 * SPDX-FileCopyrightText: 2015 David A. Velasco <dvelasco@solidgear.es>
 * SPDX-License-Identifier: GPL-2.0-only AND (AGPL-3.0-or-later OR GPL-2.0-only)
 */
package com.owncloud.android.operations;

import com.owncloud.android.datamodel.FileDataStorageManager;
import com.owncloud.android.lib.common.OwnCloudClient;
import com.owncloud.android.lib.common.operations.RemoteOperation;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.resources.shares.GetShareRemoteOperation;
import com.owncloud.android.lib.resources.shares.OCShare;
import com.owncloud.android.lib.resources.shares.UpdateShareRemoteOperation;
import com.owncloud.android.operations.common.SyncOperation;

/**
 * Updates an existing public share for a given file
 */
public class UpdateShareViaLinkOperation extends SyncOperation {
    private String password;
    private Boolean hideFileDownload;
    private long expirationDateInMillis;
    private final long shareId;
    private String label;

    public UpdateShareViaLinkOperation(long shareId, FileDataStorageManager storageManager) {
        super(storageManager);

        expirationDateInMillis = 0;
        this.shareId = shareId;
    }

    @Override
    protected RemoteOperationResult run(OwnCloudClient client) {
        OCShare publicShare = getStorageManager().getShareById(shareId);

        UpdateShareRemoteOperation updateOp = new UpdateShareRemoteOperation(publicShare.getRemoteId());
        updateOp.setPassword(password);
        updateOp.setExpirationDate(expirationDateInMillis);
        updateOp.setHideFileDownload(hideFileDownload);
        updateOp.setLabel(label);

        RemoteOperationResult result = updateOp.execute(client);

        if (result.isSuccess()) {
            // Retrieve updated share / save directly with password? -> no; the password is not to be saved
            RemoteOperation getShareOp = new GetShareRemoteOperation(publicShare.getRemoteId());
            result = getShareOp.execute(client);
            if (result.isSuccess()) {
                OCShare share = (OCShare) result.getData().get(0);
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
