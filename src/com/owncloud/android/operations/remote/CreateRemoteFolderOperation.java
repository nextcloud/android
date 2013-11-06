package com.owncloud.android.operations.remote;

import org.apache.commons.httpclient.HttpStatus;
import org.apache.jackrabbit.webdav.client.methods.MkColMethod;

import com.owncloud.android.Log_OC;
import com.owncloud.android.operations.RemoteOperation;
import com.owncloud.android.operations.RemoteOperationResult;
import com.owncloud.android.utils.FileStorageUtils;

import eu.alefzero.webdav.WebdavClient;
import eu.alefzero.webdav.WebdavUtils;

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
     * Contructor
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
        
        try {
            mkcol = new MkColMethod(client.getBaseUri() + WebdavUtils.encodePath(mRemotePath));
            int status =  client.executeMethod(mkcol, READ_TIMEOUT, CONNECTION_TIMEOUT);
            if (!mkcol.succeeded() && mkcol.getStatusCode() == HttpStatus.SC_CONFLICT && mCreateFullPath) {
                result = createParentFolder(FileStorageUtils.getParentPath(mRemotePath), client);
                status = client.executeMethod(mkcol, READ_TIMEOUT, CONNECTION_TIMEOUT);    // second (and last) try
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

    
    private RemoteOperationResult createParentFolder(String parentPath, WebdavClient client) {
        RemoteOperation operation = new CreateRemoteFolderOperation(  parentPath,
                                                                mCreateFullPath);
        return operation.execute(client);
    }
}
