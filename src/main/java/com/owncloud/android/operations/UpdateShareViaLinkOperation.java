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

import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.lib.common.OwnCloudClient;
import com.owncloud.android.lib.common.operations.RemoteOperation;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.resources.files.FileUtils;
import com.owncloud.android.lib.resources.shares.GetShareRemoteOperation;
import com.owncloud.android.lib.resources.shares.OCShare;
import com.owncloud.android.lib.resources.shares.UpdateShareRemoteOperation;
import com.owncloud.android.operations.common.SyncOperation;


/**
 * Updates an existing public share for a given file
 */
public class UpdateShareViaLinkOperation extends SyncOperation {

    private String path;
    private String password;
    /** Enable upload permissions to update in Share resource. */
    private Boolean publicUploadOnFolder;
    private Boolean publicUploadOnFile;
    private Boolean hideFileDownload;
    private long expirationDateInMillis;
    private long shareId;

    /**
     * Constructor
     *
     * @param path Full path of the file/folder being shared. Mandatory argument
     */
    public UpdateShareViaLinkOperation(String path, long shareId) {
        this.path = path;
        expirationDateInMillis = 0;
        this.shareId = shareId;
    }

    @Override
    protected RemoteOperationResult run(OwnCloudClient client) {
        OCShare publicShare = getStorageManager().getShareById(shareId);

        UpdateShareRemoteOperation updateOp = new UpdateShareRemoteOperation(publicShare.getRemoteId());
        updateOp.setPassword(password);
        updateOp.setExpirationDate(expirationDateInMillis);
        updateOp.setPublicUploadOnFolder(publicUploadOnFolder);
        updateOp.setPublicUploadOnFile(publicUploadOnFile);
        updateOp.setHideFileDownload(hideFileDownload);
        RemoteOperationResult result = updateOp.execute(client);

        if (result.isSuccess()) {
            // Retrieve updated share / save directly with password? -> no; the password is not to be saved
            RemoteOperation getShareOp = new GetShareRemoteOperation(publicShare.getRemoteId());
            result = getShareOp.execute(client);
            if (result.isSuccess()) {
                OCShare share = (OCShare) result.getData().get(0);
                updateData(share);
            }
        }

        return result;
    }

    private void updateData(OCShare share) {
        // Update DB with the response
        share.setPath(path);
        if (path.endsWith(FileUtils.PATH_SEPARATOR)) {
            share.setFolder(true);
        } else {
            share.setFolder(false);
        }

        getStorageManager().saveShare(share);   // TODO info about having a password? ask to Gonzalo

        // Update OCFile with data from share: ShareByLink  and publicLink
        // TODO check & remove if not needed
        OCFile file = getStorageManager().getFileByPath(path);
        if (file != null) {
            file.setPublicLink(share.getShareLink());
            file.setSharedViaLink(true);
            getStorageManager().saveFile(file);
        }
    }

    public String getPath() {
        return this.path;
    }

    public String getPassword() {
        return this.password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setPublicUploadOnFolder(Boolean publicUploadOnFolder) {
        this.publicUploadOnFolder = publicUploadOnFolder;
    }

    public void setPublicUploadOnFile(Boolean publicUploadOnFile) {
        this.publicUploadOnFile = publicUploadOnFile;
    }

    public void setHideFileDownload(Boolean hideFileDownload) {
        this.hideFileDownload = hideFileDownload;
    }

    public void setExpirationDateInMillis(long expirationDateInMillis) {
        this.expirationDateInMillis = expirationDateInMillis;
    }
}
