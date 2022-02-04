/*
 * Nextcloud Android client application
 *
 * @author TSI-mc
 * Copyright (C) 2021 TSI-mc
 * Copyright (C) 2021 Nextcloud GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package com.owncloud.android.operations;

import android.text.TextUtils;

import com.owncloud.android.datamodel.FileDataStorageManager;
import com.owncloud.android.lib.common.OwnCloudClient;
import com.owncloud.android.lib.common.operations.RemoteOperation;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.lib.resources.shares.GetShareRemoteOperation;
import com.owncloud.android.lib.resources.shares.OCShare;
import com.owncloud.android.lib.resources.shares.ShareType;
import com.owncloud.android.lib.resources.shares.UpdateShareRemoteOperation;
import com.owncloud.android.operations.common.SyncOperation;
import com.owncloud.android.operations.share_download_limit.DeleteShareDownloadLimitRemoteOperation;
import com.owncloud.android.operations.share_download_limit.UpdateShareDownloadLimitRemoteOperation;


/**
 * Updates an existing private share for a given file.
 */
public class UpdateShareInfoOperation extends SyncOperation {

    private static final String TAG = UpdateShareInfoOperation.class.getSimpleName();
    private OCShare share;
    private long shareId;
    private long expirationDateInMillis;
    private String note;
    private boolean hideFileDownload;
    private int permissions = -1;
    private String password;
    private String label;

    //download limit for link share
    private int downloadLimit;

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

        RemoteOperationResult result = updateOp.execute(client);

        if (result.isSuccess()) {
            RemoteOperation getShareOp = new GetShareRemoteOperation(share.getRemoteId());
            result = getShareOp.execute(client);

            //only update the share in storage if shareId is available
            //this will be triggered by editing existing share
            if (result.isSuccess() && shareId > 0) {
                OCShare ocShare = (OCShare) result.getData().get(0);
                ocShare.setPasswordProtected(!TextUtils.isEmpty(password));

                executeShareDownloadLimitOperation(client,ocShare);

                getStorageManager().saveShare(ocShare);
            }

        }

        return result;
    }

    /**
     * method will be used to update or delete the download limit for the particular share
     * @param client
     * @param ocShare share object
     */
    private void executeShareDownloadLimitOperation(OwnCloudClient client,OCShare ocShare) {
        //if share type is of Link Share then only we have to update the download limit if configured by user
        if (ocShare.getShareType() == ShareType.PUBLIC_LINK && !ocShare.isFolder()){

            //if download limit it greater than 0 then update the limit
            //else delete the download limit
            if (downloadLimit > 0){
                //api will update the download limit for the particular share
                UpdateShareDownloadLimitRemoteOperation updateShareDownloadLimitRemoteOperation =
                    new UpdateShareDownloadLimitRemoteOperation(ocShare.getToken(), downloadLimit);

                RemoteOperationResult downloadLimitOp =
                    updateShareDownloadLimitRemoteOperation.execute(client);
                if(downloadLimitOp.isSuccess()) {
                    Log_OC.d(TAG, "Download limit updated for the share.");
                }
            }else{
                //api will delete the download limit for the particular share
                DeleteShareDownloadLimitRemoteOperation limitRemoteOperation =
                    new DeleteShareDownloadLimitRemoteOperation(ocShare.getToken());

                RemoteOperationResult deleteDownloadLimitOp =
                    limitRemoteOperation.execute(client);
                if(deleteDownloadLimitOp.isSuccess()) {
                    Log_OC.d(TAG, "Download limit delete for the share.");
                }
            }

        }
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

    public void setLabel(String label) {
        this.label = label;
    }

    public void setDownloadLimit(int downloadLimit) {
        this.downloadLimit = downloadLimit;
    }
}

