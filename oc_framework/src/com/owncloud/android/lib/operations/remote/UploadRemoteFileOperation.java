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

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.methods.PutMethod;
import org.apache.commons.httpclient.methods.RequestEntity;
import org.apache.http.HttpStatus;

import com.owncloud.android.lib.network.FileRequestEntity;
import com.owncloud.android.lib.network.OnDatatransferProgressListener;
import com.owncloud.android.lib.network.OwnCloudClient;
import com.owncloud.android.lib.network.ProgressiveDataTransferer;
import com.owncloud.android.lib.network.webdav.WebdavUtils;
import com.owncloud.android.lib.operations.common.OperationCancelledException;
import com.owncloud.android.lib.operations.common.RemoteOperation;
import com.owncloud.android.lib.operations.common.RemoteOperationResult;

/**
 * Remote operation performing the upload of a remote file to the ownCloud server.
 * 
 * @author David A. Velasco
 * @author masensio
 */

public class UploadRemoteFileOperation extends RemoteOperation {


	protected String mStoragePath;
	protected String mRemotePath;
	protected String mMimeType;
	protected PutMethod mPutMethod = null;
	
	private final AtomicBoolean mCancellationRequested = new AtomicBoolean(false);
	protected Set<OnDatatransferProgressListener> mDataTransferListeners = new HashSet<OnDatatransferProgressListener>();

	protected RequestEntity mEntity = null;

	public UploadRemoteFileOperation(String storagePath, String remotePath, String mimeType) {
		mStoragePath = storagePath;
		mRemotePath = remotePath;
		mMimeType = mimeType;	
	}

	@Override
	protected RemoteOperationResult run(OwnCloudClient client) {
		RemoteOperationResult result = null;

		try {
			// / perform the upload
			synchronized (mCancellationRequested) {
				if (mCancellationRequested.get()) {
					throw new OperationCancelledException();
				} else {
					mPutMethod = new PutMethod(client.getBaseUri() + WebdavUtils.encodePath(mRemotePath));
				}
			}

			int status = uploadFile(client);

			result  = new RemoteOperationResult(isSuccess(status), status, (mPutMethod != null ? mPutMethod.getResponseHeaders() : null));

		} catch (Exception e) {
			// TODO something cleaner with cancellations
			if (mCancellationRequested.get()) {
				result = new RemoteOperationResult(new OperationCancelledException());
			} else {
				result = new RemoteOperationResult(e);
			}
		}
		return result;
	}

	public boolean isSuccess(int status) {
		return ((status == HttpStatus.SC_OK || status == HttpStatus.SC_CREATED || status == HttpStatus.SC_NO_CONTENT));
	}

	protected int uploadFile(OwnCloudClient client) throws HttpException, IOException, OperationCancelledException {
		int status = -1;
		try {
			File f = new File(mStoragePath);
			mEntity  = new FileRequestEntity(f, mMimeType);
			synchronized (mDataTransferListeners) {
				((ProgressiveDataTransferer)mEntity).addDatatransferProgressListeners(mDataTransferListeners);
			}
			mPutMethod.setRequestEntity(mEntity);
			status = client.executeMethod(mPutMethod);
			client.exhaustResponse(mPutMethod.getResponseBodyAsStream());

		} finally {
			mPutMethod.releaseConnection(); // let the connection available for other methods
		}
		return status;
	}
	
    public Set<OnDatatransferProgressListener> getDataTransferListeners() {
        return mDataTransferListeners;
    }
    
    public void addDatatransferProgressListener (OnDatatransferProgressListener listener) {
        synchronized (mDataTransferListeners) {
            mDataTransferListeners.add(listener);
        }
        if (mEntity != null) {
            ((ProgressiveDataTransferer)mEntity).addDatatransferProgressListener(listener);
        }
    }
    
    public void removeDatatransferProgressListener(OnDatatransferProgressListener listener) {
        synchronized (mDataTransferListeners) {
            mDataTransferListeners.remove(listener);
        }
        if (mEntity != null) {
            ((ProgressiveDataTransferer)mEntity).removeDatatransferProgressListener(listener);
        }
    }
    
    public void cancel() {
        synchronized (mCancellationRequested) {
            mCancellationRequested.set(true);
            if (mPutMethod != null)
                mPutMethod.abort();
        }
    }

}
