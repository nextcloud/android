/* ownCloud Android client application
 *   Copyright (C) 2012 Bartek Przybylski
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
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.util.Random;

import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.methods.PutMethod;

import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.network.ProgressiveDataTransferer;

import android.accounts.Account;
import android.util.Log;

import eu.alefzero.webdav.ChunkFromFileChannelRequestEntity;
import eu.alefzero.webdav.WebdavClient;
import eu.alefzero.webdav.WebdavUtils;

public class ChunkedUploadFileOperation extends UploadFileOperation {
    
    private static final long CHUNK_SIZE = 1024000;
    private static final String OC_CHUNKED_HEADER = "OC-Chunked";
    private static final String TAG = ChunkedUploadFileOperation.class.getSimpleName();

    public ChunkedUploadFileOperation(  Account account,
                                        OCFile file,
                                        boolean isInstant, 
                                        boolean forceOverwrite,
                                        int localBehaviour) {
        
        super(account, file, isInstant, forceOverwrite, localBehaviour);
    }

    @Override
    protected int uploadFile(WebdavClient client) throws HttpException, IOException {
        int status = -1;

        FileChannel channel = null;
        RandomAccessFile raf = null;
        try {
            File file = new File(getStoragePath());
            raf = new RandomAccessFile(file, "r");
            channel = raf.getChannel();
            mEntity = new ChunkFromFileChannelRequestEntity(channel, getMimeType(), CHUNK_SIZE, file);
            ((ProgressiveDataTransferer)mEntity).addDatatransferProgressListeners(getDataTransferListeners());
            long offset = 0;
            String uriPrefix = client.getBaseUri() + WebdavUtils.encodePath(getRemotePath()) + "-chunking-" + Math.abs((new Random()).nextInt(9000)+1000) + "-" ;
            long chunkCount = (long) Math.ceil((double)file.length() / CHUNK_SIZE);
            for (int chunkIndex = 0; chunkIndex < chunkCount ; chunkIndex++, offset += CHUNK_SIZE) {
                mPutMethod = new PutMethod(uriPrefix + chunkCount + "-" + chunkIndex);
                mPutMethod.addRequestHeader(OC_CHUNKED_HEADER, OC_CHUNKED_HEADER);
                ((ChunkFromFileChannelRequestEntity)mEntity).setOffset(offset);
                mPutMethod.setRequestEntity(mEntity);
                status = client.executeMethod(mPutMethod);
                client.exhaustResponse(mPutMethod.getResponseBodyAsStream());
                Log.d(TAG, "Upload of " + getStoragePath() + " to " + getRemotePath() + ", chunk index " + chunkIndex + ", count " + chunkCount + ", HTTP result status " + status);
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
