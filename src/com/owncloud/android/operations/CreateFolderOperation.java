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

import java.io.File;

import org.apache.commons.httpclient.HttpStatus;
import org.apache.jackrabbit.webdav.client.methods.MkColMethod;

import com.owncloud.android.Log_OC;
import com.owncloud.android.datamodel.FileDataStorageManager;
import com.owncloud.android.datamodel.OCFile;


import eu.alefzero.webdav.WebdavClient;
import eu.alefzero.webdav.WebdavUtils;

/**
 * Remote operation performing the creation of a new folder in the ownCloud server.
 * 
 * @author David A. Velasco 
 */
public class CreateFolderOperation extends RemoteOperation {
    
    private static final String TAG = CreateFolderOperation.class.getSimpleName();

    private static final int READ_TIMEOUT = 10000;
    private static final int CONNECTION_TIMEOUT = 5000;
    
    protected String mRemotePath;
    protected boolean mCreateFullPath;
    protected FileDataStorageManager mStorageManager;
    
    /**
     * Constructor
     * 
     * @param remotePath            Full path to the new directory to create in the remote server.
     * @param createFullPath        'True' means that all the ancestor folders should be created if don't exist yet.
     * @param storageManager        Reference to the local database corresponding to the account where the file is contained. 
     */
    public CreateFolderOperation(String remotePath, boolean createFullPath, FileDataStorageManager storageManager) {
        mRemotePath = remotePath;
        mCreateFullPath = createFullPath;
        mStorageManager = storageManager;
    }
    
    
    /**
     * Performs the operation
     * 
     * @param   client      Client object to communicate with the remote ownCloud server.
     */
    @Override
    protected RemoteOperationResult run(WebdavClient client) {
        RemoteOperationResult result = null;
        MkColMethod mkcol = null;
        try {
            mkcol = new MkColMethod(client.getBaseUri() + WebdavUtils.encodePath(mRemotePath));
            int status =  client.executeMethod(mkcol, READ_TIMEOUT, CONNECTION_TIMEOUT);
            if (!mkcol.succeeded() && mkcol.getStatusCode() == HttpStatus.SC_CONFLICT && mCreateFullPath) {
                result = createParentFolder(getParentPath(), client);
                status = client.executeMethod(mkcol, READ_TIMEOUT, CONNECTION_TIMEOUT);    // second (and last) try
            }
            if (mkcol.succeeded()) {
                // Save new directory in local database
                OCFile newDir = new OCFile(mRemotePath);
                newDir.setMimetype("DIR");
                long parentId = mStorageManager.getFileByPath(getParentPath()).getFileId();
                newDir.setParentId(parentId);
                newDir.setModificationTimestamp(System.currentTimeMillis());
                mStorageManager.saveFile(newDir);
            }
            result = new RemoteOperationResult(mkcol.succeeded(), status, mkcol.getResponseHeaders());
            Log_OC.d(TAG, "Create directory " + mRemotePath + ": " + result.getLogMessage());
            client.exhaustResponse(mkcol.getResponseBodyAsStream());
                
        } catch (Exception e) {
            result = new RemoteOperationResult(e);
            Log_OC.e(TAG, "Create directory " + mRemotePath + ": " + result.getLogMessage(), e);
            
        } finally {
            if (mkcol != null)
                mkcol.releaseConnection();
        }
        return result;
    }


    private String getParentPath() {
        String parentPath = new File(mRemotePath).getParent();
        parentPath = parentPath.endsWith(OCFile.PATH_SEPARATOR) ? parentPath : parentPath + OCFile.PATH_SEPARATOR;
        return parentPath;
    }

    
    private RemoteOperationResult createParentFolder(String parentPath, WebdavClient client) {
        RemoteOperation operation = new CreateFolderOperation(  parentPath,
                                                                mCreateFullPath,
                                                                mStorageManager    );
        return operation.execute(client);
    }

}
