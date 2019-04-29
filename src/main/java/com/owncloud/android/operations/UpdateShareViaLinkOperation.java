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
import com.owncloud.android.lib.resources.shares.GetRemoteShareOperation;
import com.owncloud.android.lib.resources.shares.OCShare;
import com.owncloud.android.lib.resources.shares.ShareType;
import com.owncloud.android.lib.resources.shares.UpdateRemoteShareOperation;
import com.owncloud.android.operations.common.SyncOperation;

import lombok.Getter;
import lombok.Setter;


/**
 * Updates an existing public share for a given file
 */
public class UpdateShareViaLinkOperation extends SyncOperation {

    @Getter private String path;
    @Getter @Setter private String password;
    /** Enable upload permissions to update in Share resource. */
    @Setter private Boolean publicUploadOnFolder;
    @Setter private Boolean publicUploadOnFile;
    @Setter private Boolean hideFileDownload;
    @Setter private long expirationDateInMillis;

    /**
     * Constructor
     *
     * @param path          Full path of the file/folder being shared. Mandatory argument
     */
    public UpdateShareViaLinkOperation(String path) {
        this.path = path;
        expirationDateInMillis = 0;
    }

    @Override
    protected RemoteOperationResult run(OwnCloudClient client) {
        OCShare publicShare = getStorageManager().getFirstShareByPathAndType(path, ShareType.PUBLIC_LINK, "");

        if (publicShare == null) {
            // TODO try to get remote share before failing?
            return new RemoteOperationResult(RemoteOperationResult.ResultCode.SHARE_NOT_FOUND);
        }

        UpdateRemoteShareOperation updateOp = new UpdateRemoteShareOperation(publicShare.getRemoteId());
        updateOp.setPassword(password);
        updateOp.setExpirationDate(expirationDateInMillis);
        updateOp.setPublicUploadOnFolder(publicUploadOnFolder);
        updateOp.setPublicUploadOnFile(publicUploadOnFile);
        updateOp.setHideFileDownload(hideFileDownload);
        RemoteOperationResult result = updateOp.execute(client);

        if (result.isSuccess()) {
            // Retrieve updated share / save directly with password? -> no; the password is not to be saved
            RemoteOperation getShareOp = new GetRemoteShareOperation(publicShare.getRemoteId());
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
}
