package com.owncloud.android.oc_framework.operations.remote;

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
     * @param oldRemotePath		Old remote path of the file. For folders it starts and ends by "/"
     * @param newName			New name to set as the name of file.
     * @param newRemotePath		New remote path to move the file, for folders it starts and ends by "/"
     */
	public RenameRemoteFileOperation(String oldName, String oldRemotePath, String newName, String newRemotePath) {
		mOldName = oldName;
		mOldRemotePath = oldRemotePath;
		mNewName = newName;
		mNewRemotePath = newRemotePath;
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
        
        boolean noInvalidChars = FileUtils.validateName(mNewName);
        
        if (noInvalidChars) {
        try {
        	
            if (mNewName.equals(mOldName)) {
                return new RemoteOperationResult(ResultCode.OK);
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
