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

import java.io.File;

import org.apache.jackrabbit.webdav.client.methods.DavMethodBase;

import android.util.Log;

import com.owncloud.android.oc_framework.network.webdav.WebdavClient;
import com.owncloud.android.oc_framework.network.webdav.WebdavUtils;
import com.owncloud.android.oc_framework.operations.RemoteOperation;
import com.owncloud.android.oc_framework.operations.RemoteOperationResult;
import com.owncloud.android.oc_framework.operations.RemoteOperationResult.ResultCode;
import com.owncloud.android.oc_framework.utils.FileUtils;


/**
 * Remote operation performing the rename of a remote file or folder in the ownCloud server.
 * 
 * @author David A. Velasco
 * @author masensio
 */
public class RenameRemoteFileOperation extends RemoteOperation {

	private static final String TAG = RenameRemoteFileOperation.class.getSimpleName();

	private static final int RENAME_READ_TIMEOUT = 10000;
	private static final int RENAME_CONNECTION_TIMEOUT = 5000;

    private String mOldName;
    private String mOldRemotePath;
    private String mNewName;
    private String mNewRemotePath;
    
    
    /**
     * Constructor
     * 
     * @param oldName			Old name of the file.
     * @param oldRemotePath		Old remote path of the file. 
     * @param newName			New name to set as the name of file.
     * @param isFolder			'true' for folder and 'false' for files
     */
	public RenameRemoteFileOperation(String oldName, String oldRemotePath, String newName, boolean isFolder) {
		mOldName = oldName;
		mOldRemotePath = oldRemotePath;
		mNewName = newName;
		
        String parent = (new File(mOldRemotePath)).getParent();
        parent = (parent.endsWith(FileUtils.PATH_SEPARATOR)) ? parent : parent + FileUtils.PATH_SEPARATOR; 
        mNewRemotePath =  parent + mNewName;
        if (isFolder) {
            mNewRemotePath += FileUtils.PATH_SEPARATOR;
        }
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
        
        boolean noInvalidChars = FileUtils.isValidPath(mNewRemotePath);
        
        if (noInvalidChars) {
        try {
        	
            if (mNewName.equals(mOldName)) {
                return new RemoteOperationResult(ResultCode.OK);
            }
        
            
            // check if a file with the new name already exists
            if (client.existsFile(mNewRemotePath)) {
            	return new RemoteOperationResult(ResultCode.INVALID_OVERWRITE);
            }
            
            move = new LocalMoveMethod( client.getBaseUri() + WebdavUtils.encodePath(mOldRemotePath),
            		client.getBaseUri() + WebdavUtils.encodePath(mNewRemotePath));
            int status = client.executeMethod(move, RENAME_READ_TIMEOUT, RENAME_CONNECTION_TIMEOUT);
            
            move.getResponseBodyAsString(); // exhaust response, although not interesting
            result = new RemoteOperationResult(move.succeeded(), status, move.getResponseHeaders());
            Log.i(TAG, "Rename " + mOldRemotePath + " to " + mNewRemotePath + ": " + result.getLogMessage());
            
        } catch (Exception e) {
            result = new RemoteOperationResult(e);
            Log.e(TAG, "Rename " + mOldRemotePath + " to " + ((mNewRemotePath==null) ? mNewName : mNewRemotePath) + ": " + result.getLogMessage(), e);
            
        } finally {
            if (move != null)
                move.releaseConnection();
        }
        } else {
        	result = new RemoteOperationResult(ResultCode.INVALID_CHARACTER_IN_NAME);
        }
        	
        return result;
	}
	
	/**
	 * Move operation
	 * 
	 */
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
