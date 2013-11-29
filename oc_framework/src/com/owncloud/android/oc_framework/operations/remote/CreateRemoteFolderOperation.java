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

package com.owncloud.android.oc_framework.operations.remote;

import org.apache.commons.httpclient.HttpStatus;
import org.apache.jackrabbit.webdav.client.methods.MkColMethod;

import android.util.Log;

import com.owncloud.android.oc_framework.network.webdav.WebdavClient;
import com.owncloud.android.oc_framework.network.webdav.WebdavUtils;
import com.owncloud.android.oc_framework.operations.RemoteOperation;
import com.owncloud.android.oc_framework.operations.RemoteOperationResult;
import com.owncloud.android.oc_framework.operations.RemoteOperationResult.ResultCode;
import com.owncloud.android.oc_framework.utils.FileUtils;



/**
 * Remote operation performing the creation of a new folder in the ownCloud server.
 * 
 * @author David A. Velasco 
 * @author masensio
 *
 */
public class CreateRemoteFolderOperation extends RemoteOperation {
    
    private static final String TAG = CreateRemoteFolderOperation.class.getSimpleName();

    private static final int READ_TIMEOUT = 10000;
    private static final int CONNECTION_TIMEOUT = 5000;
    

    protected String mRemotePath;
    protected boolean mCreateFullPath;
    
    /**
     * Constructor
     * 
     * @param remotePath            Full path to the new directory to create in the remote server.
     * @param createFullPath        'True' means that all the ancestor folders should be created if don't exist yet.
     */
    public CreateRemoteFolderOperation(String remotePath, boolean createFullPath) {
        mRemotePath = remotePath;
        mCreateFullPath = createFullPath;
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
        
        boolean noInvalidChars = FileUtils.isValidPath(mRemotePath);
        if (noInvalidChars) {
        	try {
        		mkcol = new MkColMethod(client.getBaseUri() + WebdavUtils.encodePath(mRemotePath));
        		int status =  client.executeMethod(mkcol, READ_TIMEOUT, CONNECTION_TIMEOUT);
        		if (!mkcol.succeeded() && mkcol.getStatusCode() == HttpStatus.SC_CONFLICT && mCreateFullPath) {
        			result = createParentFolder(FileUtils.getParentPath(mRemotePath), client);
        			status = client.executeMethod(mkcol, READ_TIMEOUT, CONNECTION_TIMEOUT);    // second (and last) try
        		}

        		result = new RemoteOperationResult(mkcol.succeeded(), status, mkcol.getResponseHeaders());
        		Log.d(TAG, "Create directory " + mRemotePath + ": " + result.getLogMessage());
        		client.exhaustResponse(mkcol.getResponseBodyAsStream());

        	} catch (Exception e) {
        		result = new RemoteOperationResult(e);
        		Log.e(TAG, "Create directory " + mRemotePath + ": " + result.getLogMessage(), e);

        	} finally {
        		if (mkcol != null)
        			mkcol.releaseConnection();
        	}
        } else {
        	result = new RemoteOperationResult(ResultCode.INVALID_CHARACTER_IN_NAME);
        }
        
        return result;
    }

    
    private RemoteOperationResult createParentFolder(String parentPath, WebdavClient client) {
        RemoteOperation operation = new CreateRemoteFolderOperation(parentPath,
                                                                mCreateFullPath);
        return operation.execute(client);
    }
    
   

}
