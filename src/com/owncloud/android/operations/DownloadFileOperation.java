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

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.http.HttpStatus;

import com.owncloud.android.files.services.FileDownloader;
import com.owncloud.android.operations.RemoteOperation;
import com.owncloud.android.operations.RemoteOperationResult;

import eu.alefzero.webdav.OnDatatransferProgressListener;
import eu.alefzero.webdav.WebdavClient;
import eu.alefzero.webdav.WebdavUtils;
import android.accounts.Account;
import android.util.Log;
import android.webkit.MimeTypeMap;

/**
 * Remote operation performing the download of a file to an ownCloud server
 * 
 * @author David A. Velasco
 */
public class DownloadFileOperation extends RemoteOperation {
    
    private static final String TAG = DownloadFileOperation.class.getCanonicalName();

    private Account mAccount = null;
    private String mLocalPath = null;
    private String mRemotePath = null;
    private String mMimeType = null;
    private long mSize = -1;
    private Boolean mCancellationRequested = false;
    
    private Set<OnDatatransferProgressListener> mDataTransferListeners = new HashSet<OnDatatransferProgressListener>();

    
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
    
    public long getSize() {
        return mSize;
    }
    
    
    public DownloadFileOperation( Account account, 
                                String localPath, 
                                String remotePath, 
                                String mimeType, 
                                long size,
                                boolean forceOverwrite) {
        
        if (account == null)
            throw new IllegalArgumentException("Illegal null account in DownloadFileOperation creation");
        if (localPath == null)
            throw new IllegalArgumentException("Illegal null local path in DownloadFileOperation creation");
        if (remotePath == null)
            throw new IllegalArgumentException("Illegal null remote path in DownloadFileOperation creation");
        
        mAccount = account;
        mLocalPath = localPath;
        mRemotePath = remotePath;
        mMimeType = mimeType;
        if (mMimeType == null) {
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
        mSize = size;
    }
    
    public void addDatatransferProgressListener (OnDatatransferProgressListener listener) {
        mDataTransferListeners.add(listener);
    }
    
    
    
    @Override
    protected RemoteOperationResult run(WebdavClient client) {
        RemoteOperationResult result = null;
        File newFile = null;
        boolean moved = false;
        
        /// download will be in a temporal file
        File tmpFile = new File(FileDownloader.getTemporalPath(mAccount.name) + mLocalPath);
        
        /// perform the download
        try {
            tmpFile.getParentFile().mkdirs();
            int status = downloadFile(client, tmpFile);
            if (isSuccess(status)) {
                newFile = new File(FileDownloader.getSavePath(mAccount.name) + mLocalPath);
                newFile.getParentFile().mkdirs();
                moved = tmpFile.renameTo(newFile);
            }
            if (!moved)
                result = new RemoteOperationResult(RemoteOperationResult.ResultCode.STORAGE_ERROR_MOVING_FROM_TMP);
            else
                result = new RemoteOperationResult(isSuccess(status), status);
            Log.i(TAG, "Download of " + mLocalPath + " to " + mRemotePath + ": " + result.getLogMessage());
            
        } catch (Exception e) {
            result = new RemoteOperationResult(e);
            Log.e(TAG, "Download of " + mRemotePath + " to " + mLocalPath + ": " + result.getLogMessage(), e);
        }
        
        return result;
    }

    
    public boolean isSuccess(int status) {
        return (status == HttpStatus.SC_OK);
    }
    
    
    protected int downloadFile(WebdavClient client, File targetFile) throws HttpException, IOException, OperationCancelledException {
        int status = -1;
        boolean savedFile = false;
        GetMethod get = new GetMethod(client.getBaseUri() + WebdavUtils.encodePath(mRemotePath));
        Iterator<OnDatatransferProgressListener> it = null;
        
        try {
            status = client.executeMethod(get);
            if (isSuccess(status)) {
                targetFile.createNewFile();
                BufferedInputStream bis = new BufferedInputStream(get.getResponseBodyAsStream());
                FileOutputStream fos = new FileOutputStream(targetFile);
                long transferred = 0;

                byte[] bytes = new byte[4096];
                int readResult = 0;
                while ((readResult = bis.read(bytes)) != -1) {
                    synchronized(mCancellationRequested) {
                        if (mCancellationRequested) {
                            throw new OperationCancelledException();
                        }
                    }
                    fos.write(bytes, 0, readResult);
                    transferred += readResult;
                    it = mDataTransferListeners.iterator();
                    while (it.hasNext()) {
                        it.next().onTransferProgress(readResult, transferred, mSize, targetFile.getName());
                    }
                }
                fos.close();
                savedFile = true;
                
            } else {
                client.exhaustResponse(get.getResponseBodyAsStream());
            }
                
        } finally {
            if (!savedFile && targetFile.exists()) {
                targetFile.delete();
            }
            get.releaseConnection();    // let the connection available for other methods
        }
        return status;
    }

    
    public void cancel() {
        synchronized(mCancellationRequested) {
            mCancellationRequested = true;
        }
    }
    
}
