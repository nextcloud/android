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

import android.content.Context;

import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.lib.network.OwnCloudClient;
import com.owncloud.android.lib.operations.common.OCShare;
import com.owncloud.android.lib.operations.common.RemoteOperationResult;
import com.owncloud.android.lib.operations.common.RemoteOperationResult.ResultCode;
import com.owncloud.android.lib.operations.remote.ExistenceCheckRemoteOperation;
import com.owncloud.android.lib.operations.remote.RemoveRemoteShareOperation;
import com.owncloud.android.operations.common.SyncOperation;
import com.owncloud.android.utils.Log_OC;

/**
 * Unshare file/folder
 * Save the data in Database
 * 
 * @author masensio
 */
public class UnshareLinkOperation extends SyncOperation {

    private static final String TAG = UnshareLinkOperation.class.getSimpleName();
    
    private OCFile mFile;
    private Context mContext;
    
    
    public UnshareLinkOperation(OCFile file, Context context) {
        mFile = file;
        mContext = context;
    }

    @Override
    protected RemoteOperationResult run(OwnCloudClient client) {
        RemoteOperationResult result  = null;
        
        // Get Share for a file
        String path = mFile.getRemotePath();
        if (mFile.isFolder()) {
            path = path.substring(0, path.length()-1); // Remove last /
        }
        OCShare share = getStorageManager().getShareByPath(path);
        
        if (share != null) {
            RemoveRemoteShareOperation operation = new RemoveRemoteShareOperation((int) share.getIdRemoteShared());
            result = operation.execute(client);

            if (result.isSuccess() || result.getCode() == ResultCode.SHARE_NOT_FOUND) {
                Log_OC.d(TAG, "Share id = " + share.getIdRemoteShared() + " deleted");

                mFile.setShareByLink(false);
                mFile.setPublicLink("");
                getStorageManager().saveFile(mFile);
                getStorageManager().removeShare(share);
                
                if (result.getCode() == ResultCode.SHARE_NOT_FOUND) {
                    if (existsFile(client, mFile.getRemotePath())) {
                        result = new RemoteOperationResult(ResultCode.OK);
                    } else {
                        getStorageManager().removeFile(mFile, true, true);
                    }
                }
            } 
                
        } else {
            result = new RemoteOperationResult(ResultCode.SHARE_NOT_FOUND);
        }

        return result;
    }
    
    private boolean existsFile(OwnCloudClient client, String remotePath){
        ExistenceCheckRemoteOperation existsOperation = new ExistenceCheckRemoteOperation(remotePath, mContext, false);
        RemoteOperationResult result = existsOperation.execute(client);
        return result.isSuccess();
    }

}
