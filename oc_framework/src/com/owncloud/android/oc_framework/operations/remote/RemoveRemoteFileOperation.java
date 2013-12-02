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
import org.apache.jackrabbit.webdav.client.methods.DeleteMethod;

import android.util.Log;

import com.owncloud.android.oc_framework.network.webdav.WebdavClient;
import com.owncloud.android.oc_framework.network.webdav.WebdavUtils;
import com.owncloud.android.oc_framework.operations.RemoteOperation;
import com.owncloud.android.oc_framework.operations.RemoteOperationResult;

/**
 * Remote operation performing the removal of a remote file or folder in the ownCloud server.
 * 
 * @author David A. Velasco
 * @author masensio
 */
public class RemoveRemoteFileOperation extends RemoteOperation {
    private static final String TAG = RemoveRemoteFileOperation.class.getSimpleName();

    private static final int REMOVE_READ_TIMEOUT = 10000;
    private static final int REMOVE_CONNECTION_TIMEOUT = 5000;

	private String mRemotePath;

    /**
     * Constructor
     * 
     * @param remotePath	RemotePath of the remote file or folder to remove from the server
     */
	public RemoveRemoteFileOperation(String remotePath) {
		mRemotePath = remotePath;
	}

	/**
	 * Performs the rename operation.
	 * 
	 * @param client	Client object to communicate with the remote ownCloud server.
	 */
	@Override
	protected RemoteOperationResult run(WebdavClient client) {
		RemoteOperationResult result = null;
        DeleteMethod delete = null;
        
        try {
        	delete = new DeleteMethod(client.getBaseUri() + WebdavUtils.encodePath(mRemotePath));
        	int status = client.executeMethod(delete, REMOVE_READ_TIMEOUT, REMOVE_CONNECTION_TIMEOUT);
        	
        	delete.getResponseBodyAsString();   // exhaust the response, although not interesting
        	result = new RemoteOperationResult((delete.succeeded() || status == HttpStatus.SC_NOT_FOUND), status, delete.getResponseHeaders());
        	Log.i(TAG, "Remove " + mRemotePath + ": " + result.getLogMessage());

        } catch (Exception e) {
        	result = new RemoteOperationResult(e);
        	Log.e(TAG, "Remove " + mRemotePath + ": " + result.getLogMessage(), e);

        } finally {
        	if (delete != null)
        		delete.releaseConnection();
        }
        
		return result;
	}

}
