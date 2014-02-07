/* ownCloud Android client application
 *   Copyright (C) 2014 ownCloud Inc.
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
 * Creates a new share from a given file
 * 
 * @author masensio
 *
 */

import android.content.Intent;

import com.owncloud.android.datamodel.FileDataStorageManager;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.lib.network.OwnCloudClient;
import com.owncloud.android.lib.operations.common.OCShare;
import com.owncloud.android.lib.operations.common.RemoteOperationResult;
import com.owncloud.android.lib.operations.common.ShareType;
import com.owncloud.android.lib.operations.remote.CreateShareRemoteOperation;
import com.owncloud.android.lib.utils.FileUtils;
import com.owncloud.android.operations.common.SyncOperation;
import com.owncloud.android.utils.Log_OC;

public class CreateShareOperation extends SyncOperation {

    private static final String TAG = CreateShareOperation.class.getSimpleName();

    protected FileDataStorageManager mStorageManager;

    private String mPath;
    private ShareType mShareType;
    private String mShareWith;
    private boolean mPublicUpload;
    private String mPassword;
    private int mPermissions;
    private Intent mSendIntent;

    /**
     * Constructor
     * @param path          Full path of the file/folder being shared. Mandatory argument
     * @param shareType     ‘0’ = user, ‘1’ = group, ‘3’ = Public link. Mandatory argument
     * @param shareWith     User/group ID with who the file should be shared.  This is mandatory for shareType of 0 or 1
     * @param publicUpload  If ‘false’ (default) public cannot upload to a public shared folder. 
     *                      If ‘true’ public can upload to a shared folder. Only available for public link shares
     * @param password      Password to protect a public link share. Only available for public link shares
     * @param permissions   1 - Read only – Default for “public” shares
     *                      2 - Update
     *                      4 - Create
     *                      8 - Delete
     *                      16- Re-share
     *                      31- All above – Default for “private” shares
     *                      For user or group shares.
     *                      To obtain combinations, add the desired values together.  
     *                      For instance, for “Re-Share”, “delete”, “read”, “update”, add 16+8+2+1 = 27.
     */
    public CreateShareOperation(String path, ShareType shareType, String shareWith, boolean publicUpload, 
            String password, int permissions, Intent sendIntent) {

        mPath = path;
        mShareType = shareType;
        mShareWith = shareWith;
        mPublicUpload = publicUpload;
        mPassword = password;
        mPermissions = permissions;
        mSendIntent = sendIntent;
    }

    @Override
    protected RemoteOperationResult run(OwnCloudClient client) {
        CreateShareRemoteOperation operation = new CreateShareRemoteOperation(mPath, mShareType, mShareWith, mPublicUpload, mPassword, mPermissions);
        RemoteOperationResult result = operation.execute(client);

        if (result.isSuccess()) {

            if (result.getData().size() > 0) {
                OCShare share = (OCShare) result.getData().get(0);

                // Update DB with the response
                if (mPath.endsWith(FileUtils.PATH_SEPARATOR)) {
                    share.setPath(mPath.substring(0, mPath.length()-1));
                    share.setIsFolder(true);
                    
                } else {
                    share.setPath(mPath);
                    share.setIsFolder(false);
                }
                share.setPermissions(mPermissions);
                
                getStorageManager().saveShare(share);
                
                // Update OCFile with data from share: ShareByLink  and publicLink
                OCFile file = getStorageManager().getFileByPath(mPath);
                if (file!=null) {
                    mSendIntent.putExtra(Intent.EXTRA_TEXT, share.getShareLink());
                    file.setPublicLink(share.getShareLink());
                    file.setShareByLink(true);
                    getStorageManager().saveFile(file);
                    Log_OC.d(TAG, "Public Link = " + file.getPublicLink());

                }
            }
        }


        return result;
    }
    
    
    public Intent getSendIntent() {
        return mSendIntent;
    }

}
