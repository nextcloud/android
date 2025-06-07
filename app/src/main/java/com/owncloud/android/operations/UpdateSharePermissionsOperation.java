/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2020-2021 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2018 Andy Scherzinger <info@andy-scherzinger.de>
 * SPDX-FileCopyrightText: 2015 ownCloud Inc.
 * SPDX-FileCopyrightText: 2015 David A. Velasco <dvelasco@solidgear.es>
 * SPDX-License-Identifier: GPL-2.0-only AND (AGPL-3.0-or-later OR GPL-2.0-only)
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

        OCShare share = getStorageManager().getShareById(shareId); // ShareType.USER | ShareType.GROUP | ShareType.FEDERATED_GROUP

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

