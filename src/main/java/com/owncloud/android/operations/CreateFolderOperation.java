/*
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

import com.nextcloud.client.logger.Logger;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.lib.common.OwnCloudClient;
import com.owncloud.android.lib.common.operations.OnRemoteOperationListener;
import com.owncloud.android.lib.common.operations.RemoteOperation;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.lib.resources.files.CreateFolderRemoteOperation;
import com.owncloud.android.lib.resources.files.ReadFolderRemoteOperation;
import com.owncloud.android.lib.resources.files.model.RemoteFile;
import com.owncloud.android.operations.common.SyncOperation;
import com.owncloud.android.utils.FileStorageUtils;
import com.owncloud.android.utils.MimeType;

import javax.inject.Inject;

import static com.owncloud.android.datamodel.OCFile.PATH_SEPARATOR;
import static com.owncloud.android.datamodel.OCFile.ROOT_PATH;


/**
 * Access to remote operation performing the creation of a new folder in the ownCloud server.
 * Save the new folder in Database
 */
public class CreateFolderOperation extends SyncOperation implements OnRemoteOperationListener{

    private static final String TAG = CreateFolderOperation.class.getSimpleName();

    protected String mRemotePath;
    private boolean mCreateFullPath;
    private RemoteFile createdRemoteFolder;
    @Inject Logger logger;

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
    public RemoteOperationResult run(OwnCloudClient client) {
        RemoteOperationResult result = createCreateFolderRemoteOperation(mRemotePath, mCreateFullPath).execute(client);

        if (result.isSuccess()) {
            RemoteOperationResult remoteFolderOperationResult = new ReadFolderRemoteOperation(mRemotePath)
                .execute(client);

            createdRemoteFolder = (RemoteFile) remoteFolderOperationResult.getData().get(0);
            saveFolderInDB();
        } else {
            Log_OC.e(TAG, mRemotePath + " hasn't been created");
        }

        return result;
    }

    public CreateFolderRemoteOperation createCreateFolderRemoteOperation(String remotePath, boolean createFullPath) {
        return new CreateFolderRemoteOperation(remotePath, createFullPath);
    }

    public ReadFolderRemoteOperation createReadFolderRemoteOperation(String remotePath) {
        return new ReadFolderRemoteOperation(remotePath);
    }

    @Override
    public void onRemoteOperationFinish(RemoteOperation operation, RemoteOperationResult result) {
        if (operation instanceof CreateFolderRemoteOperation) {
            onCreateRemoteFolderOperationFinish(result);
        }
    }

    private void onCreateRemoteFolderOperationFinish(RemoteOperationResult result) {
       if (result.isSuccess()) {
           saveFolderInDB();
       } else {
           Log_OC.e(TAG, mRemotePath + " hasn't been created");
       }
    }

    /**
     * Save new directory in local database.
     */
    private void saveFolderInDB() {
        if (mCreateFullPath && getStorageManager().
                getFileByPath(FileStorageUtils.getParentPath(mRemotePath)) == null){// When parent
                                                                                    // of remote path
                                                                                    // is not created
            String[] subFolders = mRemotePath.split(PATH_SEPARATOR);
            String composedRemotePath = ROOT_PATH;

            // For each ancestor folders create them recursively
            for (String subFolder : subFolders) {
                if (!subFolder.isEmpty()) {
                    composedRemotePath = composedRemotePath + subFolder + PATH_SEPARATOR;
                    mRemotePath = composedRemotePath;
                    saveFolderInDB();
                }
            }
        } else { // Create directory on DB
            OCFile newDir = new OCFile(mRemotePath);
            newDir.setMimeType(MimeType.DIRECTORY);
            long parentId = getStorageManager().getFileByPath(FileStorageUtils.getParentPath(mRemotePath)).getFileId();
            newDir.setParentId(parentId);
            newDir.setRemoteId(createdRemoteFolder.getRemoteId());
            newDir.setModificationTimestamp(System.currentTimeMillis());
            newDir.setEncrypted(FileStorageUtils.checkEncryptionStatus(newDir, getStorageManager()));
            newDir.setPermissions(createdRemoteFolder.getPermissions());
            getStorageManager().saveFile(newDir);

            logger.d(TAG, "Create directory " + mRemotePath + " in Database");
        }
    }

    public String getRemotePath() {
        return mRemotePath;
    }
}
