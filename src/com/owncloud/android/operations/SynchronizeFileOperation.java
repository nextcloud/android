package com.owncloud.android.operations;

import org.apache.http.HttpStatus;
import org.apache.jackrabbit.webdav.MultiStatus;
import org.apache.jackrabbit.webdav.client.methods.PropFindMethod;

import android.accounts.Account;
import android.content.Context;
import android.util.Log;

import com.owncloud.android.datamodel.DataStorageManager;
import com.owncloud.android.datamodel.OCFile;

import eu.alefzero.webdav.WebdavClient;
import eu.alefzero.webdav.WebdavEntry;
import eu.alefzero.webdav.WebdavUtils;

public class SynchronizeFileOperation extends RemoteOperation {

    private String TAG = SynchronizeFileOperation.class.getSimpleName();
    
    private String mRemotePath;
    
    private DataStorageManager mStorageManager;
    
    private Account mAccount;
    
    public SynchronizeFileOperation(
            String remotePath, 
            DataStorageManager dataStorageManager, 
            Account account, 
            Context context ) {
        mRemotePath = remotePath;
        mStorageManager = dataStorageManager;
        mAccount = account;
    }

    @Override
    protected RemoteOperationResult run(WebdavClient client) {
        PropFindMethod propfind = null;
        RemoteOperationResult result = null;
        try {
          propfind = new PropFindMethod(client.getBaseUri() + WebdavUtils.encodePath(mRemotePath));
          int status = client.executeMethod(propfind);
          boolean isMultiStatus = status == HttpStatus.SC_MULTI_STATUS;
          Boolean isConflict = Boolean.FALSE;
          if (isMultiStatus) {
              MultiStatus resp = propfind.getResponseBodyAsMultiStatus();
              WebdavEntry we = new WebdavEntry(resp.getResponses()[0],
                                               client.getBaseUri().getPath());
              OCFile file = fillOCFile(we);
              OCFile oldFile = mStorageManager.getFileByPath(file.getRemotePath());
              if (oldFile.getFileLength() != file.getFileLength() ||
                  oldFile.getModificationTimestamp() != file.getModificationTimestamp()) {
                  isConflict = Boolean.TRUE;
              }
              
          } else {
              client.exhaustResponse(propfind.getResponseBodyAsStream());
          }
          
          result = new RemoteOperationResult(isMultiStatus, status);
          result.setExtraData(isConflict);
          Log.i(TAG, "Synchronizing " + mAccount.name + ", file " + mRemotePath + ": " + result.getLogMessage());
        } catch (Exception e) {
            result = new RemoteOperationResult(e);
            Log.e(TAG, "Synchronizing " + mAccount.name + ", file " + mRemotePath + ": " + result.getLogMessage(), result.getException());

        } finally {
            if (propfind != null)
                propfind.releaseConnection();
        }
        return result;
    }
    
    /**
     * Creates and populates a new {@link OCFile} object with the data read from the server.
     * 
     * @param we        WebDAV entry read from the server for a WebDAV resource (remote file or folder).
     * @return          New OCFile instance representing the remote resource described by we.
     */
    private OCFile fillOCFile(WebdavEntry we) {
        OCFile file = new OCFile(we.decodedPath());
        file.setCreationTimestamp(we.createTimestamp());
        file.setFileLength(we.contentLength());
        file.setMimetype(we.contentType());
        file.setModificationTimestamp(we.modifiedTimesamp());
        file.setLastSyncDate(System.currentTimeMillis());
        return file;
    }

}
