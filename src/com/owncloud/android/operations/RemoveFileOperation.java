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

import org.apache.commons.httpclient.HttpStatus;
import org.apache.jackrabbit.webdav.client.methods.DeleteMethod;

import android.util.Log;

import com.owncloud.android.datamodel.DataStorageManager;
import com.owncloud.android.datamodel.OCFile;

import eu.alefzero.webdav.WebdavClient;
import eu.alefzero.webdav.WebdavUtils;

/**
 * Remote operation performing the removal of a remote file or folder in the ownCloud server.
 * 
 * @author David A. Velasco
 */
public class RemoveFileOperation extends RemoteOperation {
    
    private static final String TAG = RemoveFileOperation.class.getSimpleName();

    private static final int REMOVE_READ_TIMEOUT = 10000;
    private static final int REMOVE_CONNECTION_TIMEOUT = 5000;
    
    OCFile mFileToRemove;
    boolean mDeleteLocalCopy;
    DataStorageManager mDataStorageManager;
    
    
    /**
     * Constructor
     * 
     * @param fileToRemove          OCFile instance describing the remote file or folder to remove from the server
     * @param deleteLocalCopy       When 'true', and a local copy of the file exists, it is also removed.
     * @param storageManager        Reference to the local database corresponding to the account where the file is contained. 
     */
    public RemoveFileOperation(OCFile fileToRemove, boolean deleteLocalCopy, DataStorageManager storageManager) {
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
    protected RemoteOperationResult run(WebdavClient client) {
        RemoteOperationResult result = null;
        DeleteMethod delete = null;
        try {
            delete = new DeleteMethod(client.getBaseUri() + WebdavUtils.encodePath(mFileToRemove.getRemotePath()));
            int status = client.executeMethod(delete, REMOVE_READ_TIMEOUT, REMOVE_CONNECTION_TIMEOUT);
            if (delete.succeeded() || status == HttpStatus.SC_NOT_FOUND) {
                if (mFileToRemove.isDirectory()) {
                    mDataStorageManager.removeDirectory(mFileToRemove, true, mDeleteLocalCopy);
                } else {
                    mDataStorageManager.removeFile(mFileToRemove, mDeleteLocalCopy);
                }
            }
            delete.getResponseBodyAsString();   // exhaust the response, although not interesting
            result = new RemoteOperationResult((delete.succeeded() || status == HttpStatus.SC_NOT_FOUND), status);
            Log.i(TAG, "Remove " + mFileToRemove.getRemotePath() + ": " + result.getLogMessage());
            
        } catch (Exception e) {
            result = new RemoteOperationResult(e);
            Log.e(TAG, "Remove " + mFileToRemove.getRemotePath() + ": " + result.getLogMessage(), e);
            
        } finally {
            if (delete != null)
                delete.releaseConnection();
        }
        return result;
    }
    
}
