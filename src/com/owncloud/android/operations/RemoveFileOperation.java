/**
 *   ownCloud Android client application
 *
 *   @author David A. Velasco
 *   @author masensio
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
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.common.operations.RemoteOperationResult.ResultCode;
import com.owncloud.android.lib.resources.files.RemoveRemoteFileOperation;
import com.owncloud.android.operations.common.SyncOperation;


/**
 * Remote operation performing the removal of a remote file or folder in the ownCloud server.
 */
public class RemoveFileOperation extends SyncOperation {
    
    // private static final String TAG = RemoveFileOperation.class.getSimpleName();
    
    OCFile mFileToRemove;
    String mRemotePath;
    boolean mOnlyLocalCopy;
    
    
    /**
     * Constructor
     * 
     * @param remotePath            RemotePath of the OCFile instance describing the remote file or 
     *                              folder to remove from the server
     * @param onlyLocalCopy         When 'true', and a local copy of the file exists, only this is 
     *                              removed.
     */
    public RemoveFileOperation(String remotePath, boolean onlyLocalCopy) {
        mRemotePath = remotePath;
        mOnlyLocalCopy = onlyLocalCopy;
    }
    
    
    /**
     * Getter for the file to remove (or removed, if the operation was successfully performed).
     * 
     * @return      File to remove or already removed.
     */
    public OCFile getFile() {
        return mFileToRemove;
    }
    
    /**
     * Performs the remove operation
     * 
     * @param   client      Client object to communicate with the remote ownCloud server.
     */
    @Override
    protected RemoteOperationResult run(OwnCloudClient client) {
        RemoteOperationResult result = null;
        
        mFileToRemove = getStorageManager().getFileByPath(mRemotePath);

        boolean localRemovalFailed = false;
        if (!mOnlyLocalCopy) {
            RemoveRemoteFileOperation operation = new RemoveRemoteFileOperation(mRemotePath);
            result = operation.execute(client);
            if (result.isSuccess() || result.getCode() == ResultCode.FILE_NOT_FOUND) {
                localRemovalFailed = !(getStorageManager().removeFile(mFileToRemove, true, true));
            }
            
        } else {
            localRemovalFailed = !(getStorageManager().removeFile(mFileToRemove, false, true));
            if (!localRemovalFailed) {
                result = new RemoteOperationResult(ResultCode.OK);
            }
        }
        
        if (localRemovalFailed) {
            result = new RemoteOperationResult(ResultCode.LOCAL_STORAGE_NOT_REMOVED);
        }
        
        return result;
    }
    
}
