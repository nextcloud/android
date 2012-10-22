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

import java.io.IOException;
import java.util.List;
import java.util.Vector;

import org.apache.http.HttpStatus;
import org.apache.jackrabbit.webdav.DavException;
import org.apache.jackrabbit.webdav.MultiStatus;
import org.apache.jackrabbit.webdav.client.methods.PropFindMethod;

import android.accounts.Account;
import android.content.Intent;
import android.util.Log;

import com.owncloud.android.datamodel.FileDataStorageManager;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.files.services.FileDownloader;

import eu.alefzero.webdav.WebdavClient;
import eu.alefzero.webdav.WebdavEntry;
import eu.alefzero.webdav.WebdavUtils;


/**
 * Remote operation performing the synchronization a the contents of a remote folder with the local database
 * 
 * @author David A. Velasco
 */
public class SynchronizeFolderOperation extends RemoteOperation {

    private static final String TAG = SynchronizeFolderOperation.class.getCanonicalName();

    /** Remote folder to synchronize */
    private String mRemotePath;
    
    /** Timestamp for the synchronization in progress */
    private long mCurrentSyncTime;
    
    /** Id of the folder to synchronize in the local database */
    private long mParentId;
    
    /** Access to the local database */
    private FileDataStorageManager mStorageManager;
    
    /** Account where the file to synchronize belongs */
    private Account mAccount;
    
    
    SynchronizeFolderOperation(String remotePath, long currentSyncTime, long parentId, FileDataStorageManager storageManager, Account account) {
        mRemotePath = remotePath;
        mCurrentSyncTime = currentSyncTime;
        mParentId = parentId;
        mStorageManager = storageManager;
        mAccount = account;
    }
    
    
    @Override
    protected RemoteOperationResult run(WebdavClient client) {
        RemoteOperationResult result = null;
        
        // code before in FileSyncAdapter.fetchData
        PropFindMethod query = null;
        Vector<OCFile> children = null;
        try {
            Log.d(TAG, "Fetching files in " + mRemotePath);
            
            // remote request 
            query = new PropFindMethod(client.getBaseUri() + WebdavUtils.encodePath(mRemotePath));
            int status = client.executeMethod(query);
            
            // check and process response
            if (isMultiStatus(status)) { 
                /// TODO take into account all the possible status per child-resource
                
                MultiStatus resp = query.getResponseBodyAsMultiStatus();
            
                // read contents in folder
                List<OCFile> updatedFiles = new Vector<OCFile>(resp.getResponses().length - 1);
                for (int i = 1; i < resp.getResponses().length; ++i) {
                    WebdavEntry we = new WebdavEntry(resp.getResponses()[i], client.getBaseUri().getPath());
                    OCFile file = fillOCFile(we);
                    file.setParentId(mParentId);
                    OCFile oldFile = mStorageManager.getFileByPath(file.getRemotePath());
                    if (oldFile != null) {
                        if (oldFile.keepInSync() && file.getModificationTimestamp() > oldFile.getModificationTimestamp()) {
                            requestContentDownload(file);
                        }
                        file.setKeepInSync(oldFile.keepInSync());
                    }
                
                    updatedFiles.add(file);
                }
                
                
                // save updated contents in local database; all at once, trying to get a best performance in database update (not a big deal, indeed)
                mStorageManager.saveFiles(updatedFiles);

                
                // removal of obsolete files
                children = mStorageManager.getDirectoryContent(mStorageManager.getFileById(mParentId));
                OCFile file;
                String currentSavePath = FileDownloader.getSavePath(mAccount.name);
                for (int i=0; i < children.size(); ) {
                    file = children.get(i);
                    if (file.getLastSyncDate() != mCurrentSyncTime) {
                        Log.d(TAG, "removing file: " + file);
                        mStorageManager.removeFile(file, (file.isDown() && file.getStoragePath().startsWith(currentSavePath)));
                        children.remove(i);
                    } else {
                        i++;
                    }
                }
                
            } else {
                client.exhaustResponse(query.getResponseBodyAsStream());
            }
            
            // prepare result object
            result = new RemoteOperationResult(isMultiStatus(status), status);
            Log.i(TAG, "Synchronization of " + mRemotePath + ": " + result.getLogMessage());
            
            
        } catch (Exception e) {
            // TODO - distinguish IOException and DavException in RemoteOperationResult constructor 
            result = new RemoteOperationResult(e);
            Log.e(TAG, "Synchronization of " + mRemotePath + ": " + result.getLogMessage(), result.getException());

        } finally {
            if (query != null)
                query.releaseConnection();  // let the connection available for other methods
        }
        
        return result;
    }
    
    
    public boolean isMultiStatus(int status) {
        return (status == HttpStatus.SC_MULTI_STATUS); 
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
        file.setLastSyncDate(mCurrentSyncTime);
        return file;
    }
    
    
    /** 
     * Probably this code would be better in other place
     */
    private void requestContentDownload(OCFile file) {
        Intent intent = new Intent(this.getContext(), FileDownloader.class);
        intent.putExtra(FileDownloader.EXTRA_ACCOUNT, mAccount);
        intent.putExtra(FileDownloader.EXTRA_FILE, file);
        getContext().startService(intent);
    }


}
