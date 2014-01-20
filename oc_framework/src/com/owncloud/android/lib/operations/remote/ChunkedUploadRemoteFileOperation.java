/* ownCloud Android Library is available under MIT license
 *   Copyright (C) 2014 ownCloud (http://www.owncloud.org/)
 *   Copyright (C) 2012  Bartek Przybylski
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
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.util.Random;

import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.methods.PutMethod;

import com.owncloud.android.lib.network.ChunkFromFileChannelRequestEntity;
import com.owncloud.android.lib.network.OwnCloudClient;
import com.owncloud.android.lib.network.ProgressiveDataTransferer;
import com.owncloud.android.lib.network.webdav.WebdavUtils;


import android.util.Log;


public class ChunkedUploadRemoteFileOperation extends UploadRemoteFileOperation {
    
    public static final long CHUNK_SIZE = 1024000;
    private static final String OC_CHUNKED_HEADER = "OC-Chunked";
    private static final String TAG = ChunkedUploadRemoteFileOperation.class.getSimpleName();

    public ChunkedUploadRemoteFileOperation(String storagePath, String remotePath, String mimeType) {
		 super(storagePath, remotePath, mimeType);	
	}
    
    @Override
    protected int uploadFile(OwnCloudClient client) throws HttpException, IOException {
        int status = -1;

        FileChannel channel = null;
        RandomAccessFile raf = null;
        try {
            File file = new File(mStoragePath);
            raf = new RandomAccessFile(file, "r");
            channel = raf.getChannel();
            mEntity = new ChunkFromFileChannelRequestEntity(channel, mMimeType, CHUNK_SIZE, file);
            //((ProgressiveDataTransferer)mEntity).addDatatransferProgressListeners(getDataTransferListeners());
            synchronized (mDataTransferListeners) {
				((ProgressiveDataTransferer)mEntity).addDatatransferProgressListeners(mDataTransferListeners);
			}
            
            long offset = 0;
            String uriPrefix = client.getBaseUri() + WebdavUtils.encodePath(mRemotePath) + "-chunking-" + Math.abs((new Random()).nextInt(9000)+1000) + "-" ;
            long chunkCount = (long) Math.ceil((double)file.length() / CHUNK_SIZE);
            for (int chunkIndex = 0; chunkIndex < chunkCount ; chunkIndex++, offset += CHUNK_SIZE) {
                if (mPutMethod != null) {
                    mPutMethod.releaseConnection();    // let the connection available for other methods
                }
                mPutMethod = new PutMethod(uriPrefix + chunkCount + "-" + chunkIndex);
                mPutMethod.addRequestHeader(OC_CHUNKED_HEADER, OC_CHUNKED_HEADER);
                ((ChunkFromFileChannelRequestEntity)mEntity).setOffset(offset);
                mPutMethod.setRequestEntity(mEntity);
                status = client.executeMethod(mPutMethod);
                client.exhaustResponse(mPutMethod.getResponseBodyAsStream());
                Log.d(TAG, "Upload of " + mStoragePath + " to " + mRemotePath + ", chunk index " + chunkIndex + ", count " + chunkCount + ", HTTP result status " + status);
                if (!isSuccess(status))
                    break;
            }
            
        } finally {
            if (channel != null)
                channel.close();
            if (raf != null)
                raf.close();
            if (mPutMethod != null)
                mPutMethod.releaseConnection();    // let the connection available for other methods
        }
        return status;
    }

}
