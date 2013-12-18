/* ownCloud Android client application
 *   Copyright (C) 2012-2013 ownCloud Inc.
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License version 2,
 *   as published by the Free Software Foundation.
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

package com.owncloud.android.oc_framework.operations.remote;

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

import android.util.Log;

import com.owncloud.android.oc_framework.network.webdav.OnDatatransferProgressListener;
import com.owncloud.android.oc_framework.network.webdav.WebdavClient;
import com.owncloud.android.oc_framework.network.webdav.WebdavUtils;
import com.owncloud.android.oc_framework.operations.OperationCancelledException;
import com.owncloud.android.oc_framework.operations.RemoteFile;
import com.owncloud.android.oc_framework.operations.RemoteOperation;
import com.owncloud.android.oc_framework.operations.RemoteOperationResult;

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
    private long mModificationTimestamp = 0;
    private GetMethod mGet;
    
    private RemoteFile mRemoteFile;
    private String mTemporalFolder;
	
	public DownloadRemoteFileOperation(RemoteFile remoteFile, String temporalFolder) {
		mRemoteFile = remoteFile;
		mTemporalFolder = temporalFolder;
	}

	@Override
	protected RemoteOperationResult run(WebdavClient client) {
		RemoteOperationResult result = null;
        
        /// download will be performed to a temporal file, then moved to the final location
        File tmpFile = new File(getTmpPath());
        
        /// perform the download
        try {
        	tmpFile.getParentFile().mkdirs();
        	int status = downloadFile(client, tmpFile);
        	result = new RemoteOperationResult(isSuccess(status), status, (mGet != null ? mGet.getResponseHeaders() : null));
        	Log.i(TAG, "Download of " + mRemoteFile.getRemotePath() + " to " + getTmpPath() + ": " + result.getLogMessage());

        } catch (Exception e) {
            result = new RemoteOperationResult(e);
            Log.e(TAG, "Download of " + mRemoteFile.getRemotePath() + " to " + getTmpPath() + ": " + result.getLogMessage(), e);
        }
        
        return result;
	}

	
    protected int downloadFile(WebdavClient client, File targetFile) throws HttpException, IOException, OperationCancelledException {
        int status = -1;
        boolean savedFile = false;
        mGet = new GetMethod(client.getBaseUri() + WebdavUtils.encodePath(mRemoteFile.getRemotePath()));
        Iterator<OnDatatransferProgressListener> it = null;
        
        FileOutputStream fos = null;
        try {
            status = client.executeMethod(mGet);
            if (isSuccess(status)) {
                targetFile.createNewFile();
                BufferedInputStream bis = new BufferedInputStream(mGet.getResponseBodyAsStream());
                fos = new FileOutputStream(targetFile);
                long transferred = 0;

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
                            it.next().onTransferProgress(readResult, transferred, mRemoteFile.getLength(), targetFile.getName());
                        }
                    }
                }
                savedFile = true;
                Header modificationTime = mGet.getResponseHeader("Last-Modified");
                if (modificationTime != null) {
                    Date d = WebdavUtils.parseResponseDate((String) modificationTime.getValue());
                    mModificationTimestamp = (d != null) ? d.getTime() : 0;
                }
                
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
        return mTemporalFolder + mRemoteFile.getRemotePath();
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
