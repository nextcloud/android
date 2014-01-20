/* ownCloud Android Library is available under MIT license
 *   Copyright (C) 2014 ownCloud (http://www.owncloud.org/)
 *   
 *   Permission is hereby granted, free of charge, to any person obtaining a copy
 *   of this software and associated documentation files (the "Software"), to deal
 *   in the Software without restriction, including without limitation the rights
 *   to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *   copies of the Software, and to permit persons to whom the Software is
 *   furnished to do so, subject to the following conditions:
 *   
 *   The above copyright notice and this permission notice shall be included in
 *   all copies or substantial portions of the Software.
 *   
 *   THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, 
 *   EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 *   MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND 
 *   NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS 
 *   BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN 
 *   ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN 
 *   CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 *   THE SOFTWARE.
 *
 */

package com.owncloud.android.lib.operations.remote;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.http.HttpStatus;

import android.util.Log;

import com.owncloud.android.lib.network.OnDatatransferProgressListener;
import com.owncloud.android.lib.network.OwnCloudClient;
import com.owncloud.android.lib.network.webdav.WebdavUtils;
import com.owncloud.android.lib.operations.common.OperationCancelledException;
import com.owncloud.android.lib.operations.common.RemoteOperation;
import com.owncloud.android.lib.operations.common.RemoteOperationResult;

/**
 * Remote operation performing the download of a remote file in the ownCloud server.
 * 
 * @author David A. Velasco
 * @author masensio
 */

public class DownloadRemoteFileOperation extends RemoteOperation {
	
	private static final String TAG = DownloadRemoteFileOperation.class.getSimpleName();
    
	private Set<OnDatatransferProgressListener> mDataTransferListeners = new HashSet<OnDatatransferProgressListener>();
    private final AtomicBoolean mCancellationRequested = new AtomicBoolean(false);
    //private long mModificationTimestamp = 0;
    private GetMethod mGet;
    
    private String mRemotePath;
    private String mDownloadFolderPath;
	
	public DownloadRemoteFileOperation(String remotePath, String downloadFolderPath) {
		mRemotePath = remotePath;
		mDownloadFolderPath = downloadFolderPath;
	}

	@Override
	protected RemoteOperationResult run(OwnCloudClient client) {
		RemoteOperationResult result = null;
        
        /// download will be performed to a temporal file, then moved to the final location
        File tmpFile = new File(getTmpPath());
        
        /// perform the download
        try {
        	tmpFile.getParentFile().mkdirs();
        	int status = downloadFile(client, tmpFile);
        	result = new RemoteOperationResult(isSuccess(status), status, (mGet != null ? mGet.getResponseHeaders() : null));
        	Log.i(TAG, "Download of " + mRemotePath + " to " + getTmpPath() + ": " + result.getLogMessage());

        } catch (Exception e) {
            result = new RemoteOperationResult(e);
            Log.e(TAG, "Download of " + mRemotePath + " to " + getTmpPath() + ": " + result.getLogMessage(), e);
        }
        
        return result;
	}

	
    protected int downloadFile(OwnCloudClient client, File targetFile) throws HttpException, IOException, OperationCancelledException {
        int status = -1;
        boolean savedFile = false;
        mGet = new GetMethod(client.getBaseUri() + WebdavUtils.encodePath(mRemotePath));
        Iterator<OnDatatransferProgressListener> it = null;
        
        FileOutputStream fos = null;
        try {
            status = client.executeMethod(mGet);
            if (isSuccess(status)) {
                targetFile.createNewFile();
                BufferedInputStream bis = new BufferedInputStream(mGet.getResponseBodyAsStream());
                fos = new FileOutputStream(targetFile);
                long transferred = 0;
                
                Header contentLength = mGet.getResponseHeader("Content-Length");
                long totalToTransfer = (contentLength != null && contentLength.getValue().length() >0) ? Long.parseLong(contentLength.getValue()) : 0;

                byte[] bytes = new byte[4096];
                int readResult = 0;
                while ((readResult = bis.read(bytes)) != -1) {
                    synchronized(mCancellationRequested) {
                        if (mCancellationRequested.get()) {
                            mGet.abort();
                            throw new OperationCancelledException();
                        }
                    }
                    fos.write(bytes, 0, readResult);
                    transferred += readResult;
                    synchronized (mDataTransferListeners) {
                        it = mDataTransferListeners.iterator();
                        while (it.hasNext()) {
                            it.next().onTransferProgress(readResult, transferred, totalToTransfer, targetFile.getName());
                        }
                    }
                }
                savedFile = true;
                /*
                Header modificationTime = mGet.getResponseHeader("Last-Modified");
                if (modificationTime != null) {
                    Date d = WebdavUtils.parseResponseDate((String) modificationTime.getValue());
                    mModificationTimestamp = (d != null) ? d.getTime() : 0;
                }
                */
                
            } else {
                client.exhaustResponse(mGet.getResponseBodyAsStream());
            }
                
        } finally {
            if (fos != null) fos.close();
            if (!savedFile && targetFile.exists()) {
                targetFile.delete();
            }
            mGet.releaseConnection();    // let the connection available for other methods
        }
        return status;
    }
    
    private boolean isSuccess(int status) {
        return (status == HttpStatus.SC_OK);
    }
    
    private String getTmpPath() {
        return mDownloadFolderPath + mRemotePath;
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
    
    public void cancel() {
        mCancellationRequested.set(true);   // atomic set; there is no need of synchronizing it
    }
}
