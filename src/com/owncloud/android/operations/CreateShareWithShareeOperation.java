/**
 *   ownCloud Android client application
 *
 *   @author masensio
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

/**
 * Creates a new private share for a given file
 */


import android.accounts.Account;

import com.owncloud.android.datamodel.FileDataStorageManager;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.lib.common.OwnCloudClient;
import com.owncloud.android.lib.common.operations.RemoteOperation;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.resources.files.FileUtils;
import com.owncloud.android.lib.resources.shares.CreateRemoteShareOperation;
import com.owncloud.android.lib.resources.shares.GetRemoteSharesForFileOperation;
import com.owncloud.android.lib.resources.shares.OCShare;
import com.owncloud.android.lib.resources.shares.ShareType;
import com.owncloud.android.operations.common.SyncOperation;

public class CreateShareWithShareeOperation extends SyncOperation {

    protected FileDataStorageManager mStorageManager;

    private String mPath;
    private String mShareeName;
    private ShareType mShareType;
    private int mPermissions;

    /**
     * Constructor.
     *
     * @param path          Full path of the file/folder being shared.
     * @param shareeName    User or group name of the target sharee.
     * @param shareType     Type of share determines type of sharee; {@link ShareType#USER} and {@link ShareType#GROUP}
     *                      are the only valid values for the moment.
     * @param permissions   Share permissions key as detailed in
     *                      https://doc.owncloud.org/server/8.2/developer_manual/core/ocs-share-api.html .
     */
    public CreateShareWithShareeOperation(String path, String shareeName, ShareType shareType, int permissions) {
        if (!ShareType.USER.equals(shareType) && !ShareType.GROUP.equals(shareType)
            && !ShareType.FEDERATED.equals(shareType)) {
            throw new IllegalArgumentException("Illegal share type " + shareType);
        }
        mPath = path;
        mShareeName = shareeName;
        mShareType = shareType;
        mPermissions = permissions;
    }

    @Override
    protected RemoteOperationResult run(OwnCloudClient client) {

        CreateRemoteShareOperation operation = new CreateRemoteShareOperation(
                mPath,
                mShareType,
                mShareeName,
                false,
                "",
                mPermissions
        );
        operation.setGetShareDetails(true);
        RemoteOperationResult result = operation.execute(client);

        
        if (result.isSuccess()) {
            if (result.getData().size() > 0) {
                OCShare share = (OCShare) result.getData().get(0);
                updateData(share);
            } 
        }
        
        return result;
    }
    
    public String getPath() {
        return mPath;
    }

    private void updateData(OCShare share) {
        // Update DB with the response
        share.setPath(mPath);
        share.setIsFolder(mPath.endsWith(FileUtils.PATH_SEPARATOR));

        getStorageManager().saveShare(share);
        
        // Update OCFile with data from share: ShareByLink  and publicLink
        OCFile file = getStorageManager().getFileByPath(mPath);
        if (file!=null) {
            file.setShareWithSharee(true);    // TODO - this should be done by the FileContentProvider, as part of getStorageManager().saveShare(share)
            getStorageManager().saveFile(file);
        }
    }

}
