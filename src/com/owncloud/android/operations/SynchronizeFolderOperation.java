/* ownCloud Android client application
 *   Copyright (C) 2012-2013 ownCloud Inc.
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 2 of the License, or
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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import org.apache.http.HttpStatus;
import org.apache.jackrabbit.webdav.MultiStatus;
import org.apache.jackrabbit.webdav.client.methods.PropFindMethod;

import android.accounts.Account;
import android.content.Context;
import android.util.Log;

import com.owncloud.android.datamodel.DataStorageManager;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.operations.RemoteOperationResult.ResultCode;
import com.owncloud.android.utils.FileStorageUtils;

import eu.alefzero.webdav.WebdavClient;
import eu.alefzero.webdav.WebdavEntry;
import eu.alefzero.webdav.WebdavUtils;


/**
 * Remote operation performing the synchronization a the contents of a remote folder with the local database
 * 
 * @author David A. Velasco
 */
public class SynchronizeFolderOperation extends RemoteOperation {

    private static final String TAG = SynchronizeFolderOperation.class.getSimpleName();

    /** Remote folder to synchronize */
    private String mRemotePath;
    
    /** Timestamp for the synchronization in progress */
    private long mCurrentSyncTime;
    
    /** Id of the folder to synchronize in the local database */
    private long mParentId;
    
    /** Access to the local database */
    private DataStorageManager mStorageManager;
    
    /** Account where the file to synchronize belongs */
    private Account mAccount;
    
    /** Android context; necessary to send requests to the download service; maybe something to refactor */
    private Context mContext;
    
    /** Files and folders contained in the synchronized folder */
    private List<OCFile> mChildren;

    private int mConflictsFound;

    private int mFailsInFavouritesFound;

    private Map<String, String> mForgottenLocalFiles;
    
    
    public SynchronizeFolderOperation(  String remotePath, 
                                        long currentSyncTime, 
                                        long parentId, 
                                        DataStorageManager dataStorageManager, 
                                        Account account, 
                                        Context context ) {
        mRemotePath = remotePath;
        mCurrentSyncTime = currentSyncTime;
        mParentId = parentId;
        mStorageManager = dataStorageManager;
        mAccount = account;
        mContext = context;
        mForgottenLocalFiles = new HashMap<String, String>();
    }
    
    
    public int getConflictsFound() {
        return mConflictsFound;
    }
    
    public int getFailsInFavouritesFound() {
        return mFailsInFavouritesFound;
    }
    
    public Map<String, String> getForgottenLocalFiles() {
        return mForgottenLocalFiles;
    }
    
    /**
     * Returns the list of files and folders contained in the synchronized folder, if called after synchronization is complete.
     * 
     * @return      List of files and folders contained in the synchronized folder.
     */
    public List<OCFile> getChildren() {
        return mChildren;
    }
    
    
    @Override
    protected RemoteOperationResult run(WebdavClient client) {
        RemoteOperationResult result = null;
        mFailsInFavouritesFound = 0;
        mConflictsFound = 0;
        mForgottenLocalFiles.clear();
        
        // code before in FileSyncAdapter.fetchData
        PropFindMethod query = null;
        try {
            Log.d(TAG, "Synchronizing " + mAccount.name + ", fetching files in " + mRemotePath);
            
            // remote request 
            query = new PropFindMethod(client.getBaseUri() + WebdavUtils.encodePath(mRemotePath));
            int status = client.executeMethod(query);
            
            // check and process response   - /// TODO take into account all the possible status per child-resource
            if (isMultiStatus(status)) { 
                MultiStatus resp = query.getResponseBodyAsMultiStatus();
            
                // synchronize properties of the parent folder, if necessary
                if (mParentId == DataStorageManager.ROOT_PARENT_ID) {
                    WebdavEntry we = new WebdavEntry(resp.getResponses()[0], client.getBaseUri().getPath());
                    OCFile parent = fillOCFile(we);
                    mStorageManager.saveFile(parent);
                    mParentId = parent.getFileId();
                }
                
                // read contents in folder
                List<OCFile> updatedFiles = new Vector<OCFile>(resp.getResponses().length - 1);
                List<SynchronizeFileOperation> filesToSyncContents = new Vector<SynchronizeFileOperation>();
                for (int i = 1; i < resp.getResponses().length; ++i) {
                    /// new OCFile instance with the data from the server
                    WebdavEntry we = new WebdavEntry(resp.getResponses()[i], client.getBaseUri().getPath());
                    OCFile file = fillOCFile(we);
                    
                    /// set data about local state, keeping unchanged former data if existing
                    file.setLastSyncDateForProperties(mCurrentSyncTime);
                    OCFile oldFile = mStorageManager.getFileByPath(file.getRemotePath());
                    if (oldFile != null) {
                        file.setKeepInSync(oldFile.keepInSync());
                        file.setLastSyncDateForData(oldFile.getLastSyncDateForData());
                        file.setModificationTimestampAtLastSyncForData(oldFile.getModificationTimestampAtLastSyncForData());    // must be kept unchanged when the file contents are not updated
                        checkAndFixForeignStoragePath(oldFile);
                        file.setStoragePath(oldFile.getStoragePath());
                    }

                    /// scan default location if local copy of file is not linked in OCFile instance
                    if (file.getStoragePath() == null && !file.isDirectory()) {
                        File f = new File(FileStorageUtils.getDefaultSavePathFor(mAccount.name, file));
                        if (f.exists()) {
                            file.setStoragePath(f.getAbsolutePath());
                            file.setLastSyncDateForData(f.lastModified());
                        }
                    }
                    
                    /// prepare content synchronization for kept-in-sync files
                    if (file.keepInSync()) {
                        SynchronizeFileOperation operation = new SynchronizeFileOperation(  oldFile,        
                                                                                            file, 
                                                                                            mStorageManager,
                                                                                            mAccount,       
                                                                                            true, 
                                                                                            false,          
                                                                                            mContext
                                                                                            );
                        filesToSyncContents.add(operation);
                    }
                
                    updatedFiles.add(file);
                }
                                
                // save updated contents in local database; all at once, trying to get a best performance in database update (not a big deal, indeed)
                mStorageManager.saveFiles(updatedFiles);
                
                // request for the synchronization of files AFTER saving last properties
                SynchronizeFileOperation op = null;
                RemoteOperationResult contentsResult = null;
                for (int i=0; i < filesToSyncContents.size(); i++) {
                    op = filesToSyncContents.get(i);
                    contentsResult = op.execute(client);   // returns without waiting for upload or download finishes
                    if (!contentsResult.isSuccess()) {
                        if (contentsResult.getCode() == ResultCode.SYNC_CONFLICT) {
                            mConflictsFound++;
                        } else {
                            mFailsInFavouritesFound++;
                            if (contentsResult.getException() != null) {
                                Log.d(TAG, "Error while synchronizing favourites : " +  contentsResult.getLogMessage(), contentsResult.getException());
                            } else {
                                Log.d(TAG, "Error while synchronizing favourites : " + contentsResult.getLogMessage());
                            }
                        }
                    }   // won't let these fails break the synchronization process
                }

                    
                // removal of obsolete files
                mChildren = mStorageManager.getDirectoryContent(mStorageManager.getFileById(mParentId));
                OCFile file;
                String currentSavePath = FileStorageUtils.getSavePath(mAccount.name);
                for (int i=0; i < mChildren.size(); ) {
                    file = mChildren.get(i);
                    if (file.getLastSyncDateForProperties() != mCurrentSyncTime) {
                        Log.d(TAG, "removing file: " + file);
                        mStorageManager.removeFile(file, (file.isDown() && file.getStoragePath().startsWith(currentSavePath)));
                        mChildren.remove(i);
                    } else {
                        i++;
                    }
                }
                
            } else {
                client.exhaustResponse(query.getResponseBodyAsStream());
            }
            
            // prepare result object
            if (isMultiStatus(status)) {
                if (mConflictsFound > 0  || mFailsInFavouritesFound > 0) { 
                    result = new RemoteOperationResult(ResultCode.SYNC_CONFLICT);   // should be different result, but will do the job
                            
                } else {
                    result = new RemoteOperationResult(true, status);
                }
            } else {
                result = new RemoteOperationResult(false, status);
            }
            Log.i(TAG, "Synchronizing " + mAccount.name + ", folder " + mRemotePath + ": " + result.getLogMessage());
            
            
        } catch (Exception e) {
            result = new RemoteOperationResult(e);
            Log.e(TAG, "Synchronizing " + mAccount.name + ", folder " + mRemotePath + ": " + result.getLogMessage(), result.getException());

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
        file.setModificationTimestamp(we.modifiedTimestamp());
        file.setParentId(mParentId);
        return file;
    }
    

    /**
     * Checks the storage path of the OCFile received as parameter. If it's out of the local ownCloud folder,
     * tries to copy the file inside it. 
     * 
     * If the copy fails, the link to the local file is nullified. The account of forgotten files is kept in 
     * {@link #mForgottenLocalFiles}
     * 
     * @param file      File to check and fix.
     */
    private void checkAndFixForeignStoragePath(OCFile file) {
        String storagePath = file.getStoragePath();
        String expectedPath = FileStorageUtils.getDefaultSavePathFor(mAccount.name, file);
        if (storagePath != null && !storagePath.equals(expectedPath)) {
            /// fix storagePaths out of the local ownCloud folder
            File originalFile = new File(storagePath);
            if (FileStorageUtils.getUsableSpace(mAccount.name) < originalFile.length()) {
                mForgottenLocalFiles.put(file.getRemotePath(), storagePath);
                file.setStoragePath(null);
                    
            } else {
                InputStream in = null;
                OutputStream out = null;
                try {
                    File expectedFile = new File(expectedPath);
                    File expectedParent = expectedFile.getParentFile();
                    expectedParent.mkdirs();
                    if (!expectedParent.isDirectory()) {
                        throw new IOException("Unexpected error: parent directory could not be created");
                    }
                    expectedFile.createNewFile();
                    if (!expectedFile.isFile()) {
                        throw new IOException("Unexpected error: target file could not be created");
                    }                    
                    in = new FileInputStream(originalFile);
                    out = new FileOutputStream(expectedFile);
                    byte[] buf = new byte[1024];
                    int len;
                    while ((len = in.read(buf)) > 0){
                        out.write(buf, 0, len);
                    }
                    file.setStoragePath(expectedPath);
                    
                } catch (Exception e) {
                    Log.e(TAG, "Exception while copying foreign file " + expectedPath, e);
                    mForgottenLocalFiles.put(file.getRemotePath(), storagePath);
                    file.setStoragePath(null);
                    
                } finally {
                    try {
                        if (in != null) in.close();
                    } catch (Exception e) {
                        Log.d(TAG, "Weird exception while closing input stream for " + storagePath + " (ignoring)", e);
                    }
                    try {
                        if (out != null) out.close();
                    } catch (Exception e) {
                        Log.d(TAG, "Weird exception while closing output stream for " + expectedPath + " (ignoring)", e);
                    }
                }
            }
        }
    }


}
