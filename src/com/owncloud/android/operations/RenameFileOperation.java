/* ownCloud Android client application
 *   Copyright (C) 2012 Bartek Przybylski
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
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

import android.util.Log;

import com.owncloud.android.datamodel.DataStorageManager;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.files.services.FileDownloader;
import com.owncloud.android.operations.RemoteOperationResult.ResultCode;

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
    private String mNewName;
    private DataStorageManager mStorageManager;
    
    
    /**
     * Constructor
     * 
     * @param file                  OCFile instance describing the remote file or folder to rename
     * @param newName               New name to set as the name of file.
     * @param storageManager        Reference to the local database corresponding to the account where the file is contained. 
     */
    public RenameFileOperation(OCFile file, String newName, DataStorageManager storageManager) {
        mFile = file;
        mNewName = newName;
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
        //MoveMethod move = null;   // TODO find out why not use this
        String newRemotePath = null;
        try {
            if (mNewName.equals(mFile.getFileName())) {
                return new RemoteOperationResult(ResultCode.OK);
            }
        
            newRemotePath = (new File(mFile.getRemotePath())).getParent() + mNewName;
            
            // check if the new name is valid in the local file system
            if (!isValidNewName()) {
                return new RemoteOperationResult(ResultCode.INVALID_LOCAL_FILE_NAME);
            }
        
            // check if a remote file with the new name already exists
            if (client.existsFile(newRemotePath)) {
                return new RemoteOperationResult(ResultCode.INVALID_OVERWRITE);
            }
            /*move = new MoveMethod( client.getBaseUri() + WebdavUtils.encodePath(mFile.getRemotePath()), 
                                                client.getBaseUri() + WebdavUtils.encodePath(newRemotePath),
                                                false);*/
            move = new LocalMoveMethod( client.getBaseUri() + WebdavUtils.encodePath(mFile.getRemotePath()),
                                        client.getBaseUri() + WebdavUtils.encodePath(newRemotePath));
            int status = client.executeMethod(move, RENAME_READ_TIMEOUT, RENAME_CONNECTION_TIMEOUT);
            if (move.succeeded()) {

                // create new OCFile instance for the renamed file
                OCFile newFile = obtainUpdatedFile();
                OCFile oldFile = mFile;
                mFile = newFile; 
                
                // try to rename the local copy of the file
                if (oldFile.isDown()) {
                    File f = new File(oldFile.getStoragePath());
                    String newStoragePath = f.getParent() + mNewName;
                    if (f.renameTo(new File(newStoragePath))) {
                        mFile.setStoragePath(newStoragePath);
                    }
                    // else - NOTHING: the link to the local file is kept although the local name can't be updated
                    // TODO - study conditions when this could be a problem
                }
                
                mStorageManager.removeFile(oldFile, false);
                mStorageManager.saveFile(mFile);
                
            }
            
            move.getResponseBodyAsString(); // exhaust response, although not interesting
            result = new RemoteOperationResult(move.succeeded(), status);
            Log.i(TAG, "Rename " + mFile.getRemotePath() + " to " + newRemotePath + ": " + result.getLogMessage());
            
        } catch (Exception e) {
            result = new RemoteOperationResult(e);
            Log.e(TAG, "Rename " + mFile.getRemotePath() + " to " + ((newRemotePath==null) ? mNewName : newRemotePath) + ": " + result.getLogMessage(), e);
            
        } finally {
            if (move != null)
                move.releaseConnection();
        }
        return result;
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
        String tmpFolder = FileDownloader.getTemporalPath("");
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


    /**
     * Creates a new OCFile for the new remote name of the renamed file.
     * 
     * @return      OCFile object with the same information than mFile, but the renamed remoteFile and the storagePath (empty)
     */
    private OCFile obtainUpdatedFile() {
        OCFile file = new OCFile(mStorageManager.getFileById(mFile.getParentId()).getRemotePath() + mNewName);
        file.setCreationTimestamp(mFile.getCreationTimestamp());
        file.setFileId(mFile.getFileId());
        file.setFileLength(mFile.getFileLength());
        file.setKeepInSync(mFile.keepInSync());
        file.setLastSyncDate(mFile.getLastSyncDate());
        file.setMimetype(mFile.getMimetype());
        file.setModificationTimestamp(mFile.getModificationTimestamp());
        file.setParentId(mFile.getParentId());
        return file;
    }


    // move operation - TODO: find out why org.apache.jackrabbit.webdav.client.methods.MoveMethod is not used instead ¿?
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
