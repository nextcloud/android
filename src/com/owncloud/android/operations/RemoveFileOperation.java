/* ownCloud Android client application
 *   Copyright (C) 2012-2014 ownCloud Inc.
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
 * 
 * @author David A. Velasco
 * @author masensio
 */
public class RemoveFileOperation extends SyncOperation {
    
    // private static final String TAG = RemoveFileOperation.class.getSimpleName();
    
    OCFile mFileToRemove;
    String mRemotePath;
    boolean mDeleteLocalCopy;
    
    
    /**
     * Constructor
     * 
     * @param remotePath            RemotePath of the OCFile instance describing the remote file or folder to remove from the server
     * @param deleteLocalCopy       When 'true', and a local copy of the file exists, it is also removed.
     */
    public RemoveFileOperation(String remotePath, boolean deleteLocalCopy) {
        mRemotePath = remotePath;
        mDeleteLocalCopy = deleteLocalCopy;
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
        
        RemoveRemoteFileOperation operation = new RemoveRemoteFileOperation(mRemotePath);
        result = operation.execute(client);
        
        mFileToRemove = getStorageManager().getFileByPath(mRemotePath);
        
        if (result.isSuccess() || result.getCode() == ResultCode.FILE_NOT_FOUND) {
            getStorageManager().removeFile(mFileToRemove, true, mDeleteLocalCopy);
        }
        
        return result;
    }
    
}
