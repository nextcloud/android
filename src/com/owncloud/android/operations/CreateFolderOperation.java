/* ownCloud Android client application
 *   Copyright (C) 2012 ownCloud Inc.
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
import com.owncloud.android.lib.operations.remote.CreateRemoteFolderOperation;
import com.owncloud.android.lib.operations.common.OnRemoteOperationListener;
import com.owncloud.android.lib.operations.common.RemoteOperation;
import com.owncloud.android.lib.operations.common.RemoteOperationResult;
import com.owncloud.android.utils.FileStorageUtils;
import com.owncloud.android.utils.Log_OC;


/**
 * Access to remote operation performing the creation of a new folder in the ownCloud server.
 * Save the new folder in Database
 * 
 * @author David A. Velasco 
 * @author masensio
 */
public class CreateFolderOperation extends RemoteOperation implements OnRemoteOperationListener{
    
    private static final String TAG = CreateFolderOperation.class.getSimpleName();
    
    protected String mRemotePath;
    protected boolean mCreateFullPath;
    protected FileDataStorageManager mStorageManager;
    
    /**
     * Constructor
     * 
     * @param createFullPath        'True' means that all the ancestor folders should be created if don't exist yet.
     * @param storageManager        Reference to the local database corresponding to the account where the file is contained. 
     */
    public CreateFolderOperation(String remotePath, boolean createFullPath, FileDataStorageManager storageManager) {
        mRemotePath = remotePath;
        mCreateFullPath = createFullPath;
        mStorageManager = storageManager;
        
    }


    @Override
    protected RemoteOperationResult run(OwnCloudClient client) {
        CreateRemoteFolderOperation operation = new CreateRemoteFolderOperation(mRemotePath, mCreateFullPath);
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
    
    
    private void onCreateRemoteFolderOperationFinish(CreateRemoteFolderOperation operation, RemoteOperationResult result) {
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
        OCFile newDir = new OCFile(mRemotePath);
        newDir.setMimetype("DIR");
        long parentId = mStorageManager.getFileByPath(FileStorageUtils.getParentPath(mRemotePath)).getFileId();
        newDir.setParentId(parentId);
        newDir.setModificationTimestamp(System.currentTimeMillis());
        mStorageManager.saveFile(newDir);

        Log_OC.d(TAG, "Create directory " + mRemotePath + " in Database");

    }


}
