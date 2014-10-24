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

import com.owncloud.android.lib.common.OwnCloudClient;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.common.operations.RemoteOperationResult.ResultCode;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.lib.resources.files.ExistenceCheckRemoteOperation;
import com.owncloud.android.lib.resources.shares.OCShare;
import com.owncloud.android.lib.resources.shares.RemoveRemoteShareOperation;
import com.owncloud.android.lib.resources.shares.ShareType;

import com.owncloud.android.operations.common.SyncOperation;

/**
 * Unshare file/folder
 * Save the data in Database
 * 
 * @author masensio
 */
public class UnshareLinkOperation extends SyncOperation {

    private static final String TAG = UnshareLinkOperation.class.getSimpleName();
    
    private String mRemotePath;
    private Context mContext;
    
    
    public UnshareLinkOperation(String remotePath, Context context) {
        mRemotePath = remotePath;
        mContext = context;
    }

    @Override
    protected RemoteOperationResult run(OwnCloudClient client) {
        RemoteOperationResult result  = null;
        
        // Get Share for a file
        OCShare share = getStorageManager().getFirstShareByPathAndType(mRemotePath, ShareType.PUBLIC_LINK);
        
        if (share != null) {
            RemoveRemoteShareOperation operation = new RemoveRemoteShareOperation((int) share.getIdRemoteShared());
            result = operation.execute(client);

            if (result.isSuccess() || result.getCode() == ResultCode.SHARE_NOT_FOUND) {
                Log_OC.d(TAG, "Share id = " + share.getIdRemoteShared() + " deleted");

                OCFile file = getStorageManager().getFileByPath(mRemotePath);
                file.setShareByLink(false);
                file.setPublicLink("");
                getStorageManager().saveFile(file);
                getStorageManager().removeShare(share);
                
                if (result.getCode() == ResultCode.SHARE_NOT_FOUND) {
                    if (existsFile(client, file.getRemotePath())) {
                        result = new RemoteOperationResult(ResultCode.OK);
                    } else {
                        getStorageManager().removeFile(file, true, true);
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
