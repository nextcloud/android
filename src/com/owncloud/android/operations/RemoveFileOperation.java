/* ownCloud Android client application
 *   Copyright (C) 2012-2013 ownCloud Inc.
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

import com.owncloud.android.datamodel.FileDataStorageManager;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.lib.network.OwnCloudClient;
import com.owncloud.android.lib.operations.common.RemoteOperation;
import com.owncloud.android.lib.operations.common.RemoteOperationResult;
import com.owncloud.android.lib.operations.common.RemoteOperationResult.ResultCode;
import com.owncloud.android.lib.operations.remote.RemoveRemoteFileOperation;


/**
 * Remote operation performing the removal of a remote file or folder in the ownCloud server.
 * 
 * @author David A. Velasco
 */
public class RemoveFileOperation extends RemoteOperation {
    
    // private static final String TAG = RemoveFileOperation.class.getSimpleName();
    
    OCFile mFileToRemove;
    boolean mDeleteLocalCopy;
    FileDataStorageManager mDataStorageManager;
    
    
    /**
     * Constructor
     * 
     * @param fileToRemove          OCFile instance describing the remote file or folder to remove from the server
     * @param deleteLocalCopy       When 'true', and a local copy of the file exists, it is also removed.
     * @param storageManager        Reference to the local database corresponding to the account where the file is contained. 
     */
    public RemoveFileOperation(OCFile fileToRemove, boolean deleteLocalCopy, FileDataStorageManager storageManager) {
        mFileToRemove = fileToRemove;
        mDeleteLocalCopy = deleteLocalCopy;
        mDataStorageManager = storageManager;
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
        
        RemoveRemoteFileOperation operation = new RemoveRemoteFileOperation(mFileToRemove.getRemotePath());
        result = operation.execute(client);
        
        if (result.isSuccess() || result.getCode() == ResultCode.FILE_NOT_FOUND) {
            mDataStorageManager.removeFile(mFileToRemove, true, mDeleteLocalCopy);
        }
        
        return result;
    }
    
}
