/**
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


/**
 * Updates an existing public share for a given file
 */

public class UpdateShareViaLinkOperation extends SyncOperation {

    private String mPath;
    private String mPassword;
    private Boolean mPublicUpload;
    private long mExpirationDateInMillis;

    /**
     * Constructor
     *
     * @param path          Full path of the file/folder being shared. Mandatory argument
     */
    public UpdateShareViaLinkOperation(String path) {

        mPath = path;
        mPassword = null;
        mExpirationDateInMillis = 0;
        mPublicUpload = null;
    }


    /**
     * Set password to update in public link.
     *
     * @param password      Password to set to the public link.
     *                      Empty string clears the current password.
     *                      Null results in no update applied to the password.
     */
    public void setPassword(String password) {
        mPassword = password;
    }


    /**
     * Set expiration date to update in Share resource.
     *
     * @param expirationDateInMillis    Expiration date to set to the public link.
     *                                  A negative value clears the current expiration date.
     *                                  Zero value (start-of-epoch) results in no update done on
     *                                  the expiration date.
     */
    public void setExpirationDate(long expirationDateInMillis) {
        mExpirationDateInMillis = expirationDateInMillis;
    }

    /**
     * Enable upload permissions to update in Share resource.
     *
     * @param publicUpload    Upload Permission to set to the public link.
     *                        Null results in no update applied to the upload permission.
     */
    public void setPublicUpload(Boolean publicUpload) {
        mPublicUpload = publicUpload;
    }


    @Override
    protected RemoteOperationResult run(OwnCloudClient client) {

        OCShare publicShare = getStorageManager().getFirstShareByPathAndType(
                mPath,
                ShareType.PUBLIC_LINK,
                ""
        );

        if (publicShare == null) {
            // TODO try to get remote share before failing?
            return new RemoteOperationResult(
                    RemoteOperationResult.ResultCode.SHARE_NOT_FOUND
            );
        }

        // Update remote share with password
        UpdateRemoteShareOperation updateOp = new UpdateRemoteShareOperation(
            publicShare.getRemoteId()
        );
        updateOp.setPassword(mPassword);
        updateOp.setExpirationDate(mExpirationDateInMillis);
        updateOp.setPublicUpload(mPublicUpload);
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

    public String getPath() {
        return mPath;
    }

    public String getPassword() {
        return mPassword;
    }

    private void updateData(OCShare share) {
        // Update DB with the response
        share.setPath(mPath);
        if (mPath.endsWith(FileUtils.PATH_SEPARATOR)) {
            share.setIsFolder(true);
        } else {
            share.setIsFolder(false);
        }

        getStorageManager().saveShare(share);   // TODO info about having a password? ask to Gonzalo

        // Update OCFile with data from share: ShareByLink  and publicLink
        // TODO check & remove if not needed
        OCFile file = getStorageManager().getFileByPath(mPath);
        if (file != null) {
            file.setPublicLink(share.getShareLink());
            file.setShareViaLink(true);
            getStorageManager().saveFile(file);
        }
    }

}

