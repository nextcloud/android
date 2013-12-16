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

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.methods.PutMethod;
import org.apache.commons.httpclient.methods.RequestEntity;
import org.apache.http.HttpStatus;

import com.owncloud.android.oc_framework.network.ProgressiveDataTransferer;
import com.owncloud.android.oc_framework.network.webdav.FileRequestEntity;
import com.owncloud.android.oc_framework.network.webdav.OnDatatransferProgressListener;
import com.owncloud.android.oc_framework.network.webdav.WebdavClient;
import com.owncloud.android.oc_framework.network.webdav.WebdavUtils;
import com.owncloud.android.oc_framework.operations.OperationCancelledException;
import com.owncloud.android.oc_framework.operations.RemoteOperation;
import com.owncloud.android.oc_framework.operations.RemoteOperationResult;

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
	protected RemoteOperationResult run(WebdavClient client) {
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

	protected int uploadFile(WebdavClient client) throws HttpException, IOException, OperationCancelledException {
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
