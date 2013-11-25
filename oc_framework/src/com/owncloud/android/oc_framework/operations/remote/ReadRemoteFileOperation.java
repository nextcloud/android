package com.owncloud.android.oc_framework.operations.remote;

import org.apache.http.HttpStatus;
import org.apache.jackrabbit.webdav.DavConstants;
import org.apache.jackrabbit.webdav.MultiStatus;
import org.apache.jackrabbit.webdav.client.methods.PropFindMethod;

import android.util.Log;

import com.owncloud.android.oc_framework.network.webdav.WebdavClient;
import com.owncloud.android.oc_framework.network.webdav.WebdavEntry;
import com.owncloud.android.oc_framework.network.webdav.WebdavUtils;
import com.owncloud.android.oc_framework.operations.RemoteOperation;
import com.owncloud.android.oc_framework.operations.RemoteOperationResult;

public class ReadRemoteFileOperation extends RemoteOperation {

	private static final String TAG = ReadRemoteFileOperation.class.getSimpleName();

	private String mRemotePath;
	private WebdavEntry mWe;

	public WebdavEntry getWEntry() {
		return mWe;
	}
	
	public ReadRemoteFileOperation(String remotePath) {
		mRemotePath = remotePath;
	}

	@Override
	protected RemoteOperationResult run(WebdavClient client) {
		RemoteOperationResult result = null;
        PropFindMethod query = null;
        
        try {
            // remote request 
            query = new PropFindMethod(client.getBaseUri() + WebdavUtils.encodePath(mRemotePath),
                    DavConstants.PROPFIND_ALL_PROP,
                    DavConstants.DEPTH_1);
            int status = client.executeMethod(query);

            // check and process response
            if (isMultiStatus(status)) {
            	MultiStatus dataInServer = query.getResponseBodyAsMultiStatus();
            	// parse data from remote folder 
            	mWe = new WebdavEntry(dataInServer.getResponses()[0], client.getBaseUri().getPath());
            	result = new RemoteOperationResult(true, status, query.getResponseHeaders());
            } else {
                // synchronization failed
                client.exhaustResponse(query.getResponseBodyAsStream());
                result = new RemoteOperationResult(false, status, query.getResponseHeaders());
            }

        } catch (Exception e) {
            result = new RemoteOperationResult(e);
            

        } finally {
            if (query != null)
                query.releaseConnection();  // let the connection available for other methods
            if (result.isSuccess()) {
                Log.i(TAG, "Synchronized "  + mRemotePath + ": " + result.getLogMessage());
            } else {
                if (result.isException()) {
                    Log.e(TAG, "Synchronized " + mRemotePath  + ": " + result.getLogMessage(), result.getException());
                } else {
                    Log.e(TAG, "Synchronized " + mRemotePath + ": " + result.getLogMessage());
                }
            }
            
        }
        return result;
	}

    public boolean isMultiStatus(int status) {
        return (status == HttpStatus.SC_MULTI_STATUS); 
    }
	
}
