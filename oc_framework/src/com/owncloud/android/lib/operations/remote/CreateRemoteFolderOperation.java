/* ownCloud Android Library is available under MIT license
 *   Copyright (C) 2014 ownCloud (http://www.owncloud.org/)
 *   
 *   Permission is hereby granted, free of charge, to any person obtaining a copy
 *   of this software and associated documentation files (the "Software"), to deal
 *   in the Software without restriction, including without limitation the rights
 *   to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *   copies of the Software, and to permit persons to whom the Software is
 *   furnished to do so, subject to the following conditions:
 *   
 *   The above copyright notice and this permission notice shall be included in
 *   all copies or substantial portions of the Software.
 *   
 *   THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, 
 *   EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 *   MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND 
 *   NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS 
 *   BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN 
 *   ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN 
 *   CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 *   THE SOFTWARE.
 *
 */

package com.owncloud.android.lib.operations.remote;

import org.apache.commons.httpclient.HttpStatus;
import org.apache.jackrabbit.webdav.client.methods.MkColMethod;

import android.util.Log;

import com.owncloud.android.lib.network.OwnCloudClient;
import com.owncloud.android.lib.network.webdav.WebdavUtils;
import com.owncloud.android.lib.operations.common.RemoteOperation;
import com.owncloud.android.lib.operations.common.RemoteOperationResult;
import com.owncloud.android.lib.operations.common.RemoteOperationResult.ResultCode;
import com.owncloud.android.lib.utils.FileUtils;



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
    protected RemoteOperationResult run(OwnCloudClient client) {
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

    
    private RemoteOperationResult createParentFolder(String parentPath, OwnCloudClient client) {
        RemoteOperation operation = new CreateRemoteFolderOperation(parentPath,
                                                                mCreateFullPath);
        return operation.execute(client);
    }
    
   

}
