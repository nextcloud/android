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

import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.methods.PutMethod;
import org.apache.http.HttpStatus;

import com.owncloud.android.operations.RemoteOperation;
import com.owncloud.android.operations.RemoteOperationResult;

import eu.alefzero.webdav.FileRequestEntity;
import eu.alefzero.webdav.OnDatatransferProgressListener;
import eu.alefzero.webdav.WebdavClient;
import eu.alefzero.webdav.WebdavUtils;
import android.accounts.Account;
import android.util.Log;
import android.webkit.MimeTypeMap;

/**
 * Remote operation performing the upload of a file to an ownCloud server
 * 
 * @author David A. Velasco
 */
public class UploadFileOperation extends RemoteOperation {
    
    private static final String TAG = UploadFileOperation.class.getCanonicalName();

    private Account mAccount = null;
    private String mLocalPath = null;
    private String mRemotePath = null;
    private String mMimeType = null;
    private boolean mIsInstant = false;
    private boolean mForceOverwrite = false;
    private Set<OnDatatransferProgressListener> mDataTransferListeners = new HashSet<OnDatatransferProgressListener>();

    
    public UploadFileOperation( Account account,
                                String localPath, 
                                String remotePath, 
                                String mimeType, 
                                boolean isInstant, 
                                boolean forceOverwrite) {
        if (account == null)
            throw new IllegalArgumentException("Illegal null account in UploadFileOperation creation");
        if (localPath == null || localPath.length() <= 0)
            throw new IllegalArgumentException("Illegal null or empty localPath in UploadFileOperation creation");
        if (remotePath == null || remotePath.length() <= 0)
            throw new IllegalArgumentException("Illegal null or empty remotePath in UploadFileOperation creation");

        mAccount = account;
        mLocalPath = localPath;
        mRemotePath = remotePath;
        mMimeType = mimeType;
        if (mMimeType == null || mMimeType.length() <= 0) {
            try {
                mMimeType = MimeTypeMap.getSingleton()
                        .getMimeTypeFromExtension(
                                localPath.substring(localPath.lastIndexOf('.') + 1));
            } catch (IndexOutOfBoundsException e) {
                Log.e(TAG, "Trying to find out MIME type of a file without extension: " + localPath);
            }
        }
        if (mMimeType == null) {
            mMimeType = "application/octet-stream";
        }
        mIsInstant = isInstant;
        mForceOverwrite = forceOverwrite;
    }


    public Account getAccount() {
        return mAccount;
    }
    
    public String getLocalPath() {
        return mLocalPath;
    }

    public String getRemotePath() {
        return mRemotePath;
    }

    public String getMimeType() {
        return mMimeType;
    }
    
    public boolean isInstant() {
        return mIsInstant;
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
            int status = uploadFile(client);
            result = new RemoteOperationResult(isSuccess(status), status);
            Log.i(TAG, "Upload of " + mLocalPath + " to " + mRemotePath + ": " + result.getLogMessage());
            
        } catch (Exception e) {
            result = new RemoteOperationResult(e);
            Log.e(TAG, "Upload of " + mLocalPath + " to " + mRemotePath + ": " + result.getLogMessage() + (nameCheckPassed?"":" (while checking file existence in server)"), e);
            
        }
        
        return result;
    }

    
    public boolean isSuccess(int status) {
        return ((status == HttpStatus.SC_OK || status == HttpStatus.SC_CREATED || status == HttpStatus.SC_NO_CONTENT));
    }
    
    
    protected int uploadFile(WebdavClient client) throws HttpException, IOException {
        int status = -1;
        PutMethod put = new PutMethod(client.getBaseUri() + WebdavUtils.encodePath(mRemotePath));
        
        try {
            File f = new File(mLocalPath);
            FileRequestEntity entity = new FileRequestEntity(f, mMimeType);
            entity.addOnDatatransferProgressListeners(mDataTransferListeners);
            put.setRequestEntity(entity);
            status = client.executeMethod(put);
            client.exhaustResponse(put.getResponseBodyAsStream());
            
        } finally {
            put.releaseConnection();    // let the connection available for other methods
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


}
