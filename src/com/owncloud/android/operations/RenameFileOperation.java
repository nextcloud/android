/* ownCloud Android client application
 *   Copyright (C) 2012-2013 ownCloud Inc.
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 2 of the License, or
 *   (at your option) any later version.
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

import org.apache.jackrabbit.webdav.client.methods.DavMethodBase;
//import org.apache.jackrabbit.webdav.client.methods.MoveMethod;

import android.accounts.Account;
import android.util.Log;

import com.owncloud.android.datamodel.DataStorageManager;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.operations.RemoteOperationResult.ResultCode;
import com.owncloud.android.utils.FileStorageUtils;

import eu.alefzero.webdav.WebdavClient;
import eu.alefzero.webdav.WebdavUtils;

/**
 * Remote operation performing the rename of a remote file (or folder?) in the ownCloud server.
 * 
 * @author David A. Velasco
 */
public class RenameFileOperation extends RemoteOperation {
    
    private static final String TAG = RemoveFileOperation.class.getSimpleName();

    private static final int RENAME_READ_TIMEOUT = 10000;
    private static final int RENAME_CONNECTION_TIMEOUT = 5000;
    

    private OCFile mFile;
    private Account mAccount;
    private String mNewName;
    private String mNewRemotePath;
    private DataStorageManager mStorageManager;
    
    
    /**
     * Constructor
     * 
     * @param file                  OCFile instance describing the remote file or folder to rename
     * @param account               OwnCloud account containing the remote file 
     * @param newName               New name to set as the name of file.
     * @param storageManager        Reference to the local database corresponding to the account where the file is contained. 
     */
    public RenameFileOperation(OCFile file, Account account, String newName, DataStorageManager storageManager) {
        mFile = file;
        mAccount = account;
        mNewName = newName;
        mNewRemotePath = null;
        mStorageManager = storageManager;
    }
  
    public OCFile getFile() {
        return mFile;
    }
    
    
    /**
     * Performs the rename operation.
     * 
     * @param   client      Client object to communicate with the remote ownCloud server.
     */
    @Override
    protected RemoteOperationResult run(WebdavClient client) {
        RemoteOperationResult result = null;
        
        LocalMoveMethod move = null;
        mNewRemotePath = null;
        try {
            if (mNewName.equals(mFile.getFileName())) {
                return new RemoteOperationResult(ResultCode.OK);
            }
        
            String parent = (new File(mFile.getRemotePath())).getParent();
            parent = (parent.endsWith(OCFile.PATH_SEPARATOR)) ? parent : parent + OCFile.PATH_SEPARATOR; 
            mNewRemotePath =  parent + mNewName;
            if (mFile.isDirectory()) {
                mNewRemotePath += OCFile.PATH_SEPARATOR;
            }
            
            // check if the new name is valid in the local file system
            if (!isValidNewName()) {
                return new RemoteOperationResult(ResultCode.INVALID_LOCAL_FILE_NAME);
            }
        
            // check if a file with the new name already exists
            if (client.existsFile(mNewRemotePath) ||                             // remote check could fail by network failure, or by indeterminate behavior of HEAD for folders ... 
                    mStorageManager.getFileByPath(mNewRemotePath) != null) {     // ... so local check is convenient
                return new RemoteOperationResult(ResultCode.INVALID_OVERWRITE);
            }
            move = new LocalMoveMethod( client.getBaseUri() + WebdavUtils.encodePath(mFile.getRemotePath()),
                                        client.getBaseUri() + WebdavUtils.encodePath(mNewRemotePath));
            int status = client.executeMethod(move, RENAME_READ_TIMEOUT, RENAME_CONNECTION_TIMEOUT);
            if (move.succeeded()) {

                if (mFile.isDirectory()) {
                    saveLocalDirectory();
                    
                } else {
                    saveLocalFile();
                    
                }
             
            /* 
             *} else if (mFile.isDirectory() && (status == 207 || status >= 500)) {
             *   // TODO 
             *   // if server fails in the rename of a folder, some children files could have been moved to a folder with the new name while some others
             *   // stayed in the old folder;
             *   //
             *   // easiest and heaviest solution is synchronizing the parent folder (or the full account);
             *   //
             *   // a better solution is synchronizing the folders with the old and new names;
             *}
             */
                
            }
            
            move.getResponseBodyAsString(); // exhaust response, although not interesting
            result = new RemoteOperationResult(move.succeeded(), status);
            Log.i(TAG, "Rename " + mFile.getRemotePath() + " to " + mNewRemotePath + ": " + result.getLogMessage());
            
        } catch (Exception e) {
            result = new RemoteOperationResult(e);
            Log.e(TAG, "Rename " + mFile.getRemotePath() + " to " + ((mNewRemotePath==null) ? mNewName : mNewRemotePath) + ": " + result.getLogMessage(), e);
            
        } finally {
            if (move != null)
                move.releaseConnection();
        }
        return result;
    }

    
    private void saveLocalDirectory() {
        mStorageManager.moveDirectory(mFile, mNewRemotePath);
        String localPath = FileStorageUtils.getDefaultSavePathFor(mAccount.name, mFile);
        File localDir = new File(localPath);
        if (localDir.exists()) {
            localDir.renameTo(new File(FileStorageUtils.getSavePath(mAccount.name) + mNewRemotePath));
            // TODO - if renameTo fails, children files that are already down will result unlinked
        }
    }

    private void saveLocalFile() {
        mFile.setFileName(mNewName);
        
        // try to rename the local copy of the file
        if (mFile.isDown()) {
            File f = new File(mFile.getStoragePath());
            String parentStoragePath = f.getParent();
            if (!parentStoragePath.endsWith(File.separator))
                parentStoragePath += File.separator;
            if (f.renameTo(new File(parentStoragePath + mNewName))) {
                mFile.setStoragePath(parentStoragePath + mNewName);
            }
            // else - NOTHING: the link to the local file is kept although the local name can't be updated
            // TODO - study conditions when this could be a problem
        }
        
        mStorageManager.saveFile(mFile);
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
     * @return      'True' if a temporal file named with the name to set could be created in the file system where 
     *              local files are stored.
     */
    private boolean isValidNewName() {
        // check tricky names
        if (mNewName == null || mNewName.length() <= 0 || mNewName.contains(File.separator) || mNewName.contains("%")) { 
            return false;
        }
        // create a test file
        String tmpFolder = FileStorageUtils.getTemporalPath("");
        File testFile = new File(tmpFolder + mNewName);
        try {
            testFile.createNewFile();   // return value is ignored; it could be 'false' because the file already existed, that doesn't invalidate the name
        } catch (IOException e) {
            Log.i(TAG, "Test for validity of name " + mNewName + " in the file system failed");
            return false;
        }
        boolean result = (testFile.exists() && testFile.isFile());
        
        // cleaning ; result is ignored, since there is not much we could do in case of failure, but repeat and repeat...
        testFile.delete();
        
        return result;
    }


    // move operation
    private class LocalMoveMethod extends DavMethodBase {

        public LocalMoveMethod(String uri, String dest) {
            super(uri);
            addRequestHeader(new org.apache.commons.httpclient.Header("Destination", dest));
        }

        @Override
        public String getName() {
            return "MOVE";
        }

        @Override
        protected boolean isSuccess(int status) {
            return status == 201 || status == 204;
        }
            
    }
    

}
