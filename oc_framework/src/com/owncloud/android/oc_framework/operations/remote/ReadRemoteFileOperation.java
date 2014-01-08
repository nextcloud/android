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

import java.util.ArrayList;

import org.apache.http.HttpStatus;
import org.apache.jackrabbit.webdav.DavConstants;
import org.apache.jackrabbit.webdav.MultiStatus;
import org.apache.jackrabbit.webdav.client.methods.PropFindMethod;

import android.util.Log;

import com.owncloud.android.oc_framework.network.webdav.WebdavClient;
import com.owncloud.android.oc_framework.network.webdav.WebdavEntry;
import com.owncloud.android.oc_framework.network.webdav.WebdavUtils;
import com.owncloud.android.oc_framework.operations.RemoteFile;
import com.owncloud.android.oc_framework.operations.RemoteOperation;
import com.owncloud.android.oc_framework.operations.RemoteOperationResult;


/**
 * Remote operation performing the read a file from the ownCloud server.
 * 
 * @author David A. Velasco
 * @author masensio
 */

public class ReadRemoteFileOperation extends RemoteOperation {

    private static final String TAG = ReadRemoteFileOperation.class.getSimpleName();
    private static final int SYNC_READ_TIMEOUT = 10000;
    private static final int SYNC_CONNECTION_TIMEOUT = 5000;
    
    private String mRemotePath;
    
	
    /**
     * Constructor
     * 
     * @param remotePath		Remote path of the file. 
     */
    public ReadRemoteFileOperation(String remotePath) {
    	mRemotePath = remotePath;
    }

    /**
     * Performs the read operation.
     * 
     * @param client		Client object to communicate with the remote ownCloud server.
     */
    @Override
    protected RemoteOperationResult run(WebdavClient client) {
    	PropFindMethod propfind = null;
    	RemoteOperationResult result = null;

    	/// take the duty of check the server for the current state of the file there
    	try {
    		propfind = new PropFindMethod(client.getBaseUri() + WebdavUtils.encodePath(mRemotePath),
    				DavConstants.PROPFIND_ALL_PROP,
    				DavConstants.DEPTH_0);
    		int status;
    		status = client.executeMethod(propfind, SYNC_READ_TIMEOUT, SYNC_CONNECTION_TIMEOUT);

    		boolean isMultiStatus = status == HttpStatus.SC_MULTI_STATUS;
    		if (isMultiStatus) {
    			// Parse response
    			MultiStatus resp = propfind.getResponseBodyAsMultiStatus();
				WebdavEntry we = new WebdavEntry(resp.getResponses()[0], client.getBaseUri().getPath());
				RemoteFile remoteFile = new RemoteFile(we);
				ArrayList<RemoteFile> files = new ArrayList<RemoteFile>();
				files.add(remoteFile);

    			// Result of the operation
    			result = new RemoteOperationResult(true, status, propfind.getResponseHeaders());
    			result.setData(files);
    			
    		} else {
    			client.exhaustResponse(propfind.getResponseBodyAsStream());
    			result = new RemoteOperationResult(false, status, propfind.getResponseHeaders());
    		}

    	} catch (Exception e) {
    		result = new RemoteOperationResult(e);
    		e.printStackTrace();
    		Log.e(TAG, "Synchronizing  file " + mRemotePath + ": " + result.getLogMessage(), result.getException());
    	} finally {
    		if (propfind != null)
    			propfind.releaseConnection();
    	}
    	return result;
    }

}
