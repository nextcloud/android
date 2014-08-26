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

import java.io.File;
import java.io.IOException;

import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.lib.common.OwnCloudClient;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.common.operations.RemoteOperationResult.ResultCode;
import com.owncloud.android.lib.resources.files.MoveRemoteFileOperation;
import com.owncloud.android.lib.resources.files.RenameRemoteFileOperation;
import com.owncloud.android.operations.common.SyncOperation;
import com.owncloud.android.utils.FileStorageUtils;
import com.owncloud.android.utils.Log_OC;

import android.accounts.Account;


/**
 * Operation mmoving an {@link OCFile} to a different folder.
 * 
 * @author David A. Velasco
 */
public class MoveFileOperation extends SyncOperation {
    
    private static final String TAG = MoveFileOperation.class.getSimpleName();
    
    private String mPath;
    private Account mAccount;
    private String mNewParentPath;
    private OCFile mFile;
    private String mNewPath;

    
    
    /**
     * Constructor
     * 
     * @param path              Remote path of the {@link OCFile} to move.
     * @param newParentPath     Path to the folder where the file will be moved into. 
     * @param account           OwnCloud account containing both the file and the target folder 
     */
    public MoveFileOperation(String path, String newParentPath, Account account) {
        mPath = path;
        mNewParentPath = newParentPath;
        mAccount = account;
        
        mFile = null;
        mNewPath = "";
    }
  
    public OCFile getFile() {
        return mFile;
    }
    
    
    /**
     * Performs the operation.
     * 
     * @param   client      Client object to communicate with the remote ownCloud server.
     */
    @Override
    protected RemoteOperationResult run(OwnCloudClient client) {
        RemoteOperationResult result = null;

        
        mFile = getStorageManager().getFileByPath(mPath);
        
        try {
            /// 1. check move validity
            
            
            /// 2. remove move 
            MoveRemoteFileOperation operation = new MoveRemoteFileOperation(
                    mFile.getRemotePath(), 
                    mNewPath, 
                    false
            );
            result = operation.execute(client);

            
            /// 3. local move
            if (result.isSuccess()) {
                //moveLocaly();
                /*
                if (mFile.isFolder()) {
                    saveLocalDirectory();

                } else {
                    saveLocalFile();
                }
                */
            }
            
            
            String parentPath = (new File(mFile.getRemotePath())).getParent();
            parentPath = (parentPath.endsWith(OCFile.PATH_SEPARATOR)) 
                    ? parentPath 
                    : parentPath + OCFile.PATH_SEPARATOR;
            
            mNewPath = mNewParentPath + mFile.getFileName();
            if (mFile.isFolder() && !mNewPath.endsWith(OCFile.PATH_SEPARATOR)) {
                mNewPath += OCFile.PATH_SEPARATOR;
            }

            // check local overwrite
            if (getStorageManager().getFileByPath(mPath) != null) {
                return new RemoteOperationResult(ResultCode.INVALID_OVERWRITE);
            }
            
            /*
        } catch (IOException e) {
            Log_OC.e(TAG, "Move " + mFile.getRemotePath() + " to " + 
                    (mNewParentPath==null) + ": " + 
                    ((result!= null) ? result.getLogMessage() : ""), e);*/
        } finally {
            
        }

        return result;
    }

    
    private void saveLocalDirectory() {
        getStorageManager().moveFolder(mFile, mNewPath);
        String localPath = FileStorageUtils.getDefaultSavePathFor(mAccount.name, mFile);
        File localDir = new File(localPath);
        if (localDir.exists()) {
            localDir.renameTo(new File(FileStorageUtils.getSavePath(mAccount.name) + mNewPath));
            // TODO - if renameTo fails, children files that are already down will result unlinked
        }
    }

    private void saveLocalFile() {
        /*
        mFile.setRFileName(mNewName);   <<< NO >
        
        // try to move the local copy of the file
        if (mFile.isDown()) {
            File f = new File(mFile.getStoragePath());
            String parentStoragePath = f.getParent();
            if (!parentStoragePath.endsWith(File.separator))
                parentStoragePath += File.separator;
            if (f.renameTo(new File())) {
                mFile.setStoragePath(parentStoragePath + mNewName);
            }
            // else - NOTHING: the link to the local file is kept although the local name can't be updated
            // TODO - study conditions when this could be a problem
        }
        
        getStorageManager().saveFile(mFile);
        */
    }

    /**
     * Checks if the new name to set is valid in the file system 
     * 
     * The only way to be sure is trying to create a file with that name. It's made in the temporal directory
     * for downloads, out of any account, and then removed. 
     * 
     * IMPORTANT: The test must be made in the same file system where files are download. The internal storage
     * could be formatted with a different file system.
     * 
     * TODO move this method, and maybe FileDownload.get***Path(), to a class with utilities specific for the interactions with the file system
     * 
     * @return              'True' if a temporal file named with the name to set could be created in the file system where 
     *                      local files are stored.
     * @throws IOException  When the temporal folder can not be created.
     */
    private boolean isValidNewName() throws IOException {
        
        return true;
        /*
        // check tricky names
        if (mNewName == null || mNewName.length() <= 0 || mNewName.contains(File.separator) || mNewName.contains("%")) { 
            return false;
        }
        // create a test file
        String tmpFolderName = FileStorageUtils.getTemporalPath("");
        File testFile = new File(tmpFolderName + mNewName);
        File tmpFolder = testFile.getParentFile();
        tmpFolder.mkdirs();
        if (!tmpFolder.isDirectory()) {
            throw new IOException("Unexpected error: temporal directory could not be created");
        }
        try {
            testFile.createNewFile();   // return value is ignored; it could be 'false' because the file already existed, that doesn't invalidate the name
        } catch (IOException e) {
            Log_OC.i(TAG, "Test for validity of name " + mNewName + " in the file system failed");
            return false;
        }
        boolean result = (testFile.exists() && testFile.isFile());
        
        // cleaning ; result is ignored, since there is not much we could do in case of failure, but repeat and repeat...
        testFile.delete();
        
        return result;
        */
    }

}
