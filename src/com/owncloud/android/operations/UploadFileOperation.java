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

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.methods.PutMethod;
import org.apache.http.HttpStatus;

import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.operations.RemoteOperation;
import com.owncloud.android.operations.RemoteOperationResult;

import eu.alefzero.webdav.FileRequestEntity;
import eu.alefzero.webdav.OnDatatransferProgressListener;
import eu.alefzero.webdav.WebdavClient;
import eu.alefzero.webdav.WebdavUtils;
import android.accounts.Account;
import android.util.Log;

/**
 * Remote operation performing the upload of a file to an ownCloud server
 * 
 * @author David A. Velasco
 */
public class UploadFileOperation extends RemoteOperation {
    
    private static final String TAG = UploadFileOperation.class.getCanonicalName();

    private Account mAccount;
    private OCFile mFile;
    private String mRemotePath = null;
    private boolean mIsInstant = false;
    private boolean mRemoteFolderToBeCreated = false;
    private boolean mForceOverwrite = false;
    PutMethod mPutMethod = null;
    private Set<OnDatatransferProgressListener> mDataTransferListeners = new HashSet<OnDatatransferProgressListener>();
    private final AtomicBoolean mCancellationRequested = new AtomicBoolean(false);
    
    public UploadFileOperation( Account account,
                                OCFile file,
                                boolean isInstant, 
                                boolean forceOverwrite) {
        if (account == null)
            throw new IllegalArgumentException("Illegal NULL account in UploadFileOperation creation");
        if (file == null)
            throw new IllegalArgumentException("Illegal NULL file in UploadFileOperation creation");
        if (file.getStoragePath() == null || file.getStoragePath().length() <= 0 || !(new File(file.getStoragePath()).exists())) {
            throw new IllegalArgumentException("Illegal file in UploadFileOperation; storage path invalid or file not found: " + file.getStoragePath());
        }
        
        mAccount = account;
        mFile = file;
        mRemotePath = file.getRemotePath();
        mIsInstant = isInstant;
        mForceOverwrite = forceOverwrite;
    }


    public Account getAccount() {
        return mAccount;
    }
    
    public OCFile getFile() {
        return mFile;
    }
    
    public String getStoragePath() {
        return mFile.getStoragePath();
    }

    public String getRemotePath() {
        //return mFile.getRemotePath(); // DON'T MAKE THIS ; the remotePath used can be different to mFile.getRemotePath() if mForceOverwrite is 'false'; see run(...)
        return mRemotePath;
    }

    public String getMimeType() {
        return mFile.getMimetype();
    }
    
    public boolean isInstant() {
        return mIsInstant;
    }

    public boolean isRemoteFolderToBeCreated() {
        return mRemoteFolderToBeCreated;
    }
    
    public void setRemoteFolderToBeCreated() {
        mRemoteFolderToBeCreated = true;
    }

    public boolean getForceOverwrite() {
        return mForceOverwrite;
    }
    
    
    public Set<OnDatatransferProgressListener> getDataTransferListeners() {
        return mDataTransferListeners;
    }
    
    public void addDatatransferProgressListener (OnDatatransferProgressListener listener) {
        mDataTransferListeners.add(listener);
    }
    

    @Override
    protected RemoteOperationResult run(WebdavClient client) {
        RemoteOperationResult result = null;
        boolean nameCheckPassed = false;
        try {
            /// rename the file to upload, if necessary
            if (!mForceOverwrite) {
                mRemotePath = getAvailableRemotePath(client, mRemotePath);
            }
        
            /// perform the upload
            nameCheckPassed = true;
            synchronized(mCancellationRequested) {
                if (mCancellationRequested.get()) {
                    throw new OperationCancelledException();
                } else {
                    mPutMethod = new PutMethod(client.getBaseUri() + WebdavUtils.encodePath(mRemotePath));
                }
            }
            int status = uploadFile(client);
            result = new RemoteOperationResult(isSuccess(status), status);
            Log.i(TAG, "Upload of " + mFile.getStoragePath() + " to " + mRemotePath + ": " + result.getLogMessage());

        } catch (Exception e) {
            // TODO something cleaner
            if (mCancellationRequested.get()) {
                result = new RemoteOperationResult(new OperationCancelledException());
            } else {
                result = new RemoteOperationResult(e);
            }
            Log.e(TAG, "Upload of " + mFile.getStoragePath() + " to " + mRemotePath + ": " + result.getLogMessage() + (nameCheckPassed?"":" (while checking file existence in server)"), result.getException());
        }
        
        return result;
    }

    
    public boolean isSuccess(int status) {
        return ((status == HttpStatus.SC_OK || status == HttpStatus.SC_CREATED || status == HttpStatus.SC_NO_CONTENT));
    }
    
    
    protected int uploadFile(WebdavClient client) throws HttpException, IOException, OperationCancelledException {
        int status = -1;
        try {
            File f = new File(mFile.getStoragePath());
            FileRequestEntity entity = new FileRequestEntity(f, getMimeType());
            entity.addOnDatatransferProgressListeners(mDataTransferListeners);
            mPutMethod.setRequestEntity(entity);
            status = client.executeMethod(mPutMethod);
            client.exhaustResponse(mPutMethod.getResponseBodyAsStream());
            
        } finally {
            mPutMethod.releaseConnection();    // let the connection available for other methods
        }
        return status;
    }
    
    /**
     * Checks if remotePath does not exist in the server and returns it, or adds a suffix to it in order to avoid the server
     * file is overwritten.
     * 
     * @param string
     * @return
     */
    private String getAvailableRemotePath(WebdavClient wc, String remotePath) throws Exception {
        boolean check = wc.existsFile(remotePath);
        if (!check) {
            return remotePath;
        }
    
        int pos = remotePath.lastIndexOf(".");
        String suffix = "";
        String extension = "";
        if (pos >= 0) {
            extension = remotePath.substring(pos+1);
            remotePath = remotePath.substring(0, pos);
        }
        int count = 2;
        do {
            suffix = " (" + count + ")";
            if (pos >= 0)
                check = wc.existsFile(remotePath + suffix + "." + extension);
            else
                check = wc.existsFile(remotePath + suffix);
            count++;
        } while (check);

        if (pos >=0) {
            return remotePath + suffix + "." + extension;
        } else {
            return remotePath + suffix;
        }
    }


    public void cancel() {
        synchronized(mCancellationRequested) {
            mCancellationRequested.set(true);
            if (mPutMethod != null)
                mPutMethod.abort();
        }
    }


}
