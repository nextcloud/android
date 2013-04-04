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

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.http.HttpStatus;

import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.operations.RemoteOperation;
import com.owncloud.android.operations.RemoteOperationResult;
import com.owncloud.android.utils.FileStorageUtils;

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
    
    private static final String TAG = DownloadFileOperation.class.getSimpleName();

    private Account mAccount;
    private OCFile mFile;
    private Set<OnDatatransferProgressListener> mDataTransferListeners = new HashSet<OnDatatransferProgressListener>();
    private final AtomicBoolean mCancellationRequested = new AtomicBoolean(false);
    private long mModificationTimestamp = 0;

    
    public DownloadFileOperation(Account account, OCFile file) {
        if (account == null)
            throw new IllegalArgumentException("Illegal null account in DownloadFileOperation creation");
        if (file == null)
            throw new IllegalArgumentException("Illegal null file in DownloadFileOperation creation");
        
        mAccount = account;
        mFile = file;
    }


    public Account getAccount() {
        return mAccount;
    }
    
    public OCFile getFile() {
        return mFile;
    }

    public String getSavePath() {
        String path = mFile.getStoragePath();   // re-downloads should be done over the original file 
        if (path != null && path.length() > 0) {
            return path;
        }
        return FileStorageUtils.getDefaultSavePathFor(mAccount.name, mFile);
    }
    
    public String getTmpPath() {
        return FileStorageUtils.getTemporalPath(mAccount.name) + mFile.getRemotePath();
    }
    
    public String getRemotePath() {
        return mFile.getRemotePath();
    }

    public String getMimeType() {
        String mimeType = mFile.getMimetype();
        if (mimeType == null || mimeType.length() <= 0) {
            try {
                mimeType = MimeTypeMap.getSingleton()
                    .getMimeTypeFromExtension(
                            mFile.getRemotePath().substring(mFile.getRemotePath().lastIndexOf('.') + 1));
            } catch (IndexOutOfBoundsException e) {
                Log.e(TAG, "Trying to find out MIME type of a file without extension: " + mFile.getRemotePath());
            }
        }
        if (mimeType == null) {
            mimeType = "application/octet-stream";
        }
        return mimeType;
    }
    
    public long getSize() {
        return mFile.getFileLength();
    }
    
    public long getModificationTimestamp() {
        return (mModificationTimestamp > 0) ? mModificationTimestamp : mFile.getModificationTimestamp();
    }
    
    
    public void addDatatransferProgressListener (OnDatatransferProgressListener listener) {
        synchronized (mDataTransferListeners) {
            mDataTransferListeners.add(listener);
        }
    }
    
    public void removeDatatransferProgressListener(OnDatatransferProgressListener listener) {
        synchronized (mDataTransferListeners) {
            mDataTransferListeners.remove(listener);
        }
    }

    @Override
    protected RemoteOperationResult run(WebdavClient client) {
        RemoteOperationResult result = null;
        File newFile = null;
        boolean moved = true;
        
        /// download will be performed to a temporal file, then moved to the final location
        File tmpFile = new File(getTmpPath());
        
        /// perform the download
        try {
            tmpFile.getParentFile().mkdirs();
            int status = downloadFile(client, tmpFile);
            if (isSuccess(status)) {
                newFile = new File(getSavePath());
                newFile.getParentFile().mkdirs();
                moved = tmpFile.renameTo(newFile);
            }
            if (!moved)
                result = new RemoteOperationResult(RemoteOperationResult.ResultCode.LOCAL_STORAGE_NOT_MOVED);
            else
                result = new RemoteOperationResult(isSuccess(status), status);
            Log.i(TAG, "Download of " + mFile.getRemotePath() + " to " + getSavePath() + ": " + result.getLogMessage());
            
        } catch (Exception e) {
            result = new RemoteOperationResult(e);
            Log.e(TAG, "Download of " + mFile.getRemotePath() + " to " + getSavePath() + ": " + result.getLogMessage(), e);
        }
        
        return result;
    }

    
    public boolean isSuccess(int status) {
        return (status == HttpStatus.SC_OK);
    }
    
    
    protected int downloadFile(WebdavClient client, File targetFile) throws HttpException, IOException, OperationCancelledException {
        int status = -1;
        boolean savedFile = false;
        GetMethod get = new GetMethod(client.getBaseUri() + WebdavUtils.encodePath(mFile.getRemotePath()));
        Iterator<OnDatatransferProgressListener> it = null;
        
        FileOutputStream fos = null;
        try {
            status = client.executeMethod(get);
            if (isSuccess(status)) {
                targetFile.createNewFile();
                BufferedInputStream bis = new BufferedInputStream(get.getResponseBodyAsStream());
                fos = new FileOutputStream(targetFile);
                long transferred = 0;

                byte[] bytes = new byte[4096];
                int readResult = 0;
                while ((readResult = bis.read(bytes)) != -1) {
                    synchronized(mCancellationRequested) {
                        if (mCancellationRequested.get()) {
                            get.abort();
                            throw new OperationCancelledException();
                        }
                    }
                    fos.write(bytes, 0, readResult);
                    transferred += readResult;
                    synchronized (mDataTransferListeners) {
                        it = mDataTransferListeners.iterator();
                        while (it.hasNext()) {
                            it.next().onTransferProgress(readResult, transferred, mFile.getFileLength(), targetFile.getName());
                        }
                    }
                }
                savedFile = true;
                Header modificationTime = get.getResponseHeader("Last-Modified");
                if (modificationTime != null) {
                    Date d = WebdavUtils.parseResponseDate((String) modificationTime.getValue());
                    mModificationTimestamp = (d != null) ? d.getTime() : 0;
                }
                
            } else {
                client.exhaustResponse(get.getResponseBodyAsStream());
            }
                
        } finally {
            if (fos != null) fos.close();
            if (!savedFile && targetFile.exists()) {
                targetFile.delete();
            }
            get.releaseConnection();    // let the connection available for other methods
        }
        return status;
    }

    
    public void cancel() {
        mCancellationRequested.set(true);   // atomic set; there is no need of synchronizing it
    }


}
