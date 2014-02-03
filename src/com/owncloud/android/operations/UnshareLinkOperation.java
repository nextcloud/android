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

import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.lib.network.OwnCloudClient;
import com.owncloud.android.lib.operations.common.OCShare;
import com.owncloud.android.lib.operations.common.RemoteOperationResult;
import com.owncloud.android.lib.operations.common.RemoteOperationResult.ResultCode;
import com.owncloud.android.lib.operations.remote.UnshareLinkRemoteOperation;
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
    
    public UnshareLinkOperation(OCFile file) {
        mFile = file;
    }

    @Override
    protected RemoteOperationResult run(OwnCloudClient client) {
        RemoteOperationResult result  = null;
        
        // Get Share for a file
        OCShare share = getStorageManager().getShareByPath(mFile.getRemotePath());
        
        if (share != null) {
            UnshareLinkRemoteOperation operation = new UnshareLinkRemoteOperation((int) share.getIdRemoteShared());
            result = operation.execute(client);

            if (result.isSuccess()) {
                Log_OC.d(TAG, "Share id = " + share.getIdRemoteShared() + " deleted");

                mFile.setShareByLink(false);
                mFile.setPublicLink("");
                getStorageManager().saveFile(mFile);
                getStorageManager().removeShare(share);
                
            }
        } else {
            result = new RemoteOperationResult(ResultCode.FILE_NOT_FOUND);
        }

        return result;
    }

}
