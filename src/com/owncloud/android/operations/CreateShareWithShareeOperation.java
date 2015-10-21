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


import com.owncloud.android.datamodel.FileDataStorageManager;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.lib.common.OwnCloudClient;
import com.owncloud.android.lib.common.operations.RemoteOperation;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.resources.files.FileUtils;
import com.owncloud.android.lib.resources.shares.CreateRemoteShareOperation;
import com.owncloud.android.lib.resources.shares.OCShare;
import com.owncloud.android.lib.resources.shares.ShareType;
import com.owncloud.android.operations.common.SyncOperation;

public class CreateShareWithShareeOperation extends SyncOperation {

    private static final int READ_ONLY = 1;

    protected FileDataStorageManager mStorageManager;

    private String mPath;
    private String mTargetName;
    private boolean mWithGroup;

    /**
     * Constructor
     * @param path          Full path of the file/folder being shared. Mandatory argument
     */
    public CreateShareWithShareeOperation(
            String path,
            String targetName,
            boolean withGroup
    ) {

        mPath = path;
        mTargetName = targetName;
        mWithGroup = withGroup;
    }

    @Override
    protected RemoteOperationResult run(OwnCloudClient client) {
        // Check if the share link already exists
        // TODO or not
        /*
        RemoteOperation operation = new GetRemoteSharesForFileOperation(mPath, false, false);
        RemoteOperationResult result = operation.execute(client);
        if (!result.isSuccess() || result.getData().size() <= 0) {
        */

        RemoteOperation operation = new CreateRemoteShareOperation(
                mPath,
                (mWithGroup ? ShareType.GROUP : ShareType.USER),
                mTargetName,
                false,
                "",
                READ_ONLY
        );
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
        if (mPath.endsWith(FileUtils.PATH_SEPARATOR)) {
            share.setIsFolder(true);
        } else {
            share.setIsFolder(false);
        }
        share.setPermissions(READ_ONLY);
        
        getStorageManager().saveShare(share);
        
        // Update OCFile with data from share: ShareByLink  and publicLink
        OCFile file = getStorageManager().getFileByPath(mPath);
        if (file!=null) {
            file.setShareWithSharee(true);    // TODO - this should be done by the FileContentProvider, as part of getStorageManager().saveShare(share)
            getStorageManager().saveFile(file);
        }
    }

}
