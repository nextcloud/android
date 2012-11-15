/* ownCloud Android client application
 *   Copyright (C) 2012 Bartek Przybylski
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
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

import org.apache.http.HttpStatus;
import org.apache.jackrabbit.webdav.MultiStatus;
import org.apache.jackrabbit.webdav.client.methods.PropFindMethod;

import android.accounts.Account;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.owncloud.android.datamodel.DataStorageManager;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.files.services.FileDownloader;
import com.owncloud.android.files.services.FileUploader;
import com.owncloud.android.operations.RemoteOperationResult.ResultCode;

import eu.alefzero.webdav.WebdavClient;
import eu.alefzero.webdav.WebdavEntry;
import eu.alefzero.webdav.WebdavUtils;

public class SynchronizeFileOperation extends RemoteOperation {

    private String TAG = SynchronizeFileOperation.class.getSimpleName();
    private String mRemotePath;
    private DataStorageManager mStorageManager;
    private Account mAccount;
    private boolean mSyncFileContents;
    private boolean mLocalChangeAlreadyKnown;
    private Context mContext;
    
    private boolean mTransferWasRequested = false;
    
    public SynchronizeFileOperation(
            String remotePath, 
            DataStorageManager dataStorageManager, 
            Account account, 
            boolean syncFileContents,
            boolean localChangeAlreadyKnown,
            Context context) {
        
        mRemotePath = remotePath;
        mStorageManager = dataStorageManager;
        mAccount = account;
        mSyncFileContents = syncFileContents;
        mLocalChangeAlreadyKnown = localChangeAlreadyKnown;
        mContext = context;
    }

    
    @Override
    protected RemoteOperationResult run(WebdavClient client) {
        
        PropFindMethod propfind = null;
        RemoteOperationResult result = null;
        mTransferWasRequested = false;
        try {
            OCFile localFile = mStorageManager.getFileByPath(mRemotePath);
            
            if (!localFile.isDown()) {
                /// easy decision
                requestForDownload(localFile);
                result = new RemoteOperationResult(ResultCode.OK);
                
            } else {
                /// local copy in the device -> need to think a bit more before do nothing
                
                propfind = new PropFindMethod(client.getBaseUri() + WebdavUtils.encodePath(mRemotePath));
                int status = client.executeMethod(propfind);
                boolean isMultiStatus = status == HttpStatus.SC_MULTI_STATUS;
                if (isMultiStatus) {
                    MultiStatus resp = propfind.getResponseBodyAsMultiStatus();
                    WebdavEntry we = new WebdavEntry(resp.getResponses()[0],
                                               client.getBaseUri().getPath());
                    OCFile serverFile = fillOCFile(we);
              
                    /// check changes in server and local file
                    boolean serverChanged = false;
                    if (serverFile.getEtag() != null) {
                        serverChanged = (!serverFile.getEtag().equals(localFile.getEtag()));
                    } else {
                        // server without etags
                        serverChanged = (serverFile.getModificationTimestamp() > localFile.getModificationTimestamp());
                    }
                    boolean localChanged = (mLocalChangeAlreadyKnown || localFile.getLocalModificationTimestamp() > localFile.getLastSyncDateForData());
              
                    /// decide action to perform depending upon changes
                    if (localChanged && serverChanged) {
                        // conflict
                        result = new RemoteOperationResult(ResultCode.SYNC_CONFLICT);
                  
                    } else if (localChanged) {
                        if (mSyncFileContents) {
                            requestForUpload(localFile);
                            // the local update of file properties will be done by the FileUploader service when the upload finishes
                        } else {
                            // NOTHING TO DO HERE: updating the properties of the file in the server without uploading the contents would be stupid; 
                            // So, an instance of SynchronizeFileOperation created with syncFileContents == false is completely useless when we suspect
                            // that an upload is necessary (for instance, in FileObserverService).
                        }
                        result = new RemoteOperationResult(ResultCode.OK);
                  
                    } else if (serverChanged) {
                        if (mSyncFileContents) {
                            requestForDownload(serverFile);
                            // the update of local data will be done later by the FileUploader service when the upload finishes
                        } else {
                            // TODO CHECK: is this really useful in some point in the code?
                            serverFile.setKeepInSync(localFile.keepInSync());
                            serverFile.setParentId(localFile.getParentId());
                            mStorageManager.saveFile(serverFile);
                            
                        }
                        result = new RemoteOperationResult(ResultCode.OK);
              
                    } else {
                        // nothing changed, nothing to do
                        result = new RemoteOperationResult(ResultCode.OK);
                    }
              
                } else {
                    client.exhaustResponse(propfind.getResponseBodyAsStream());
                    result = new RemoteOperationResult(false, status);
                }
          
            }
            
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
     * Requests for an upload to the FileUploader service
     * 
     * @param localFile     OCFile object representing the file to upload
     */
    private void requestForUpload(OCFile localFile) {
        Intent i = new Intent(mContext, FileUploader.class);
        i.putExtra(FileUploader.KEY_ACCOUNT, mAccount);
        i.putExtra(FileUploader.KEY_REMOTE_FILE, mRemotePath);
        i.putExtra(FileUploader.KEY_LOCAL_FILE, localFile.getStoragePath());
        i.putExtra(FileUploader.KEY_UPLOAD_TYPE, FileUploader.UPLOAD_SINGLE_FILE);
        i.putExtra(FileUploader.KEY_FORCE_OVERWRITE, true);
        mContext.startService(i);
        mTransferWasRequested = true;
    }


    /**
     * Requests for a download to the FileDownloader service
     * 
     * @param file     OCFile object representing the file to download
     */
    private void requestForDownload(OCFile file) {
        Intent i = new Intent(mContext, FileDownloader.class);
        i.putExtra(FileDownloader.EXTRA_ACCOUNT, mAccount);
        i.putExtra(FileDownloader.EXTRA_FILE, file);
        mContext.startService(i);
        mTransferWasRequested = true;
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
        file.setLastSyncDateForProperties(System.currentTimeMillis());
        file.setLastSyncDateForData(0);
        return file;
    }


    public boolean transferWasRequested() {
        return mTransferWasRequested;
    }

}
