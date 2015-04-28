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

import com.owncloud.android.MainApp;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.lib.common.OwnCloudClient;
import com.owncloud.android.lib.resources.files.CreateRemoteFolderOperation;
import com.owncloud.android.lib.common.operations.OnRemoteOperationListener;
import com.owncloud.android.lib.common.operations.RemoteOperation;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.operations.common.SyncOperation;
import com.owncloud.android.utils.FileStorageUtils;


/**
 * Access to remote operation performing the creation of a new folder in the ownCloud server.
 * Save the new folder in Database
 */
public class CreateFolderOperation extends SyncOperation implements OnRemoteOperationListener{
    
    private static final String TAG = CreateFolderOperation.class.getSimpleName();
    
    protected String mRemotePath;
    protected boolean mCreateFullPath;
    
    /**
     * Constructor
     * 
     * @param createFullPath        'True' means that all the ancestor folders should be created
     *                              if don't exist yet.
     */
    public CreateFolderOperation(String remotePath, boolean createFullPath) {
        mRemotePath = remotePath;
        mCreateFullPath = createFullPath;
        
    }


    @Override
    protected RemoteOperationResult run(OwnCloudClient client) {
        CreateRemoteFolderOperation operation = new CreateRemoteFolderOperation(mRemotePath,
                mCreateFullPath);
        RemoteOperationResult result =  operation.execute(client);
        
        if (result.isSuccess()) {
            saveFolderInDB();
        } else {
            Log_OC.e(TAG, mRemotePath + "hasn't been created");
        }
        
        return result;
    }

    @Override
    public void onRemoteOperationFinish(RemoteOperation operation, RemoteOperationResult result) {
        if (operation instanceof CreateRemoteFolderOperation) {
            onCreateRemoteFolderOperationFinish((CreateRemoteFolderOperation)operation, result);
        }
    }
    
    
    private void onCreateRemoteFolderOperationFinish(CreateRemoteFolderOperation operation,
                                                     RemoteOperationResult result) {
       if (result.isSuccess()) {
           saveFolderInDB();
       } else {
           Log_OC.e(TAG, mRemotePath + "hasn't been created");
       }
    }

    /**
     * Save new directory in local database
     */
    public void saveFolderInDB() {
        if (mCreateFullPath && getStorageManager().
                getFileByPath(FileStorageUtils.getParentPath(mRemotePath)) == null){// When parent
                                                                                    // of remote path
                                                                                    // is not created 
            String[] subFolders = mRemotePath.split("/");
            String composedRemotePath = "/";

            // For each antecesor folders create them recursively
            for (int i=0; i<subFolders.length; i++) {
                String subFolder =  subFolders[i];
                if (!subFolder.isEmpty()) {
                    composedRemotePath = composedRemotePath + subFolder + "/";
                    mRemotePath = composedRemotePath;
                    saveFolderInDB();
                }
            }
        } else { // Create directory on DB
            OCFile newDir = new OCFile(mRemotePath);
            newDir.setMimetype("DIR");
            long parentId = getStorageManager().
                    getFileByPath(FileStorageUtils.getParentPath(mRemotePath)).getFileId();
            newDir.setParentId(parentId);
            newDir.setModificationTimestamp(System.currentTimeMillis());
            getStorageManager().saveFile(newDir);

            Log_OC.d(TAG, "Create directory " + mRemotePath + " in Database");
        }
    }
}
