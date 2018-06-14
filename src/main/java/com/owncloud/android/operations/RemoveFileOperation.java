/*
 *   ownCloud Android client application
 *
 *   @author David A. Velasco
 *   @author masensio
 *   @author Tobias Kaminsky
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

import android.accounts.Account;
import android.content.Context;

import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.datamodel.ThumbnailsCacheManager;
import com.owncloud.android.lib.common.OwnCloudClient;
import com.owncloud.android.lib.common.operations.RemoteOperation;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.common.operations.RemoteOperationResult.ResultCode;
import com.owncloud.android.lib.resources.files.RemoveRemoteFileOperation;
import com.owncloud.android.operations.common.SyncOperation;


/**
 * Remote operation performing the removal of a remote file or folder in the ownCloud server.
 */
public class RemoveFileOperation extends SyncOperation {

    private OCFile fileToRemove;
    private String remotePath;
    private boolean onlyLocalCopy;
    private Account account;
    private boolean inBackground;
    private Context context;
    
    
    /**
     * Constructor
     * 
     * @param remotePath            RemotePath of the OCFile instance describing the remote file or 
     *                              folder to remove from the server
     * @param onlyLocalCopy         When 'true', and a local copy of the file exists, only this is 
     *                              removed.
     */
    public RemoveFileOperation(String remotePath, boolean onlyLocalCopy, Account account, boolean inBackground,
                               Context context) {
        this.remotePath = remotePath;
        this.onlyLocalCopy = onlyLocalCopy;
        this.account = account;
        this.inBackground = inBackground;
        this.context = context;
    }
    
    
    /**
     * Getter for the file to remove (or removed, if the operation was successfully performed).
     * 
     * @return      File to remove or already removed.
     */
    public OCFile getFile() {
        return fileToRemove;
    }

    public boolean isInBackground() {
        return inBackground;
    }
    
    /**
     * Performs the remove operation
     * 
     * @param   client      Client object to communicate with the remote ownCloud server.
     */
    @Override
    protected RemoteOperationResult run(OwnCloudClient client) {
        RemoteOperationResult result = null;
        RemoteOperation operation;

        fileToRemove = getStorageManager().getFileByPath(remotePath);

        // store resized image
        ThumbnailsCacheManager.generateResizedImage(fileToRemove);

        boolean localRemovalFailed = false;
        if (!onlyLocalCopy) {

            if (fileToRemove.isEncrypted() &&
                    android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
                OCFile parent = getStorageManager().getFileByPath(fileToRemove.getParentRemotePath());
                operation = new RemoveRemoteEncryptedFileOperation(remotePath, parent.getLocalId(), account, context,
                        fileToRemove.getEncryptedFileName());
            } else {
                operation = new RemoveRemoteFileOperation(remotePath);
            }
            result = operation.execute(client);
            if (result.isSuccess() || result.getCode() == ResultCode.FILE_NOT_FOUND) {
                localRemovalFailed = !(getStorageManager().removeFile(fileToRemove, true, true));
            }
            
        } else {
            localRemovalFailed = !(getStorageManager().removeFile(fileToRemove, false, true));
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
