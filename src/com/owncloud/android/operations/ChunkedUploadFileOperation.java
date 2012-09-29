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
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.util.Random;

import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.methods.PutMethod;

import android.util.Log;

import eu.alefzero.webdav.ChunkFromFileChannelRequestEntity;
import eu.alefzero.webdav.OnDatatransferProgressListener;
import eu.alefzero.webdav.WebdavClient;
import eu.alefzero.webdav.WebdavUtils;

public class ChunkedUploadFileOperation extends UploadFileOperation {
    
    private static final long CHUNK_SIZE = 102400;
    private static final String OC_CHUNKED_HEADER = "OC-Chunked";
    private static final String TAG = ChunkedUploadFileOperation.class.getSimpleName();

    public ChunkedUploadFileOperation(  String localPath, 
                                        String remotePath, 
                                        String mimeType, 
                                        boolean isInstant, 
                                        boolean forceOverwrite, 
                                        OnDatatransferProgressListener dataTransferProgressListener) {
        
        super(localPath, remotePath, mimeType, isInstant, forceOverwrite, dataTransferProgressListener);
    }

    @Override
    protected int uploadFile(WebdavClient client) throws HttpException, IOException {
        int status = -1;

        PutMethod put = null;
        FileChannel channel = null;
        FileLock lock = null;
        RandomAccessFile raf = null;
        try {
            File file = new File(getLocalPath());
            raf = new RandomAccessFile(file, "rw");
            channel = raf.getChannel();
            lock = channel.tryLock();
            ChunkFromFileChannelRequestEntity entity = new ChunkFromFileChannelRequestEntity(channel, getMimeType(), CHUNK_SIZE);
            entity.setOnDatatransferProgressListener(getDataTransferListener());
            long offset = 0;
            String uriPrefix = client.getBaseUri() + WebdavUtils.encodePath(getRemotePath()) + "-chunking-" + Math.abs((new Random()).nextInt(9000)+1000) + "-" ;
            long chunkCount = (long) Math.ceil((double)file.length() / CHUNK_SIZE);
            for (int chunkIndex = 0; chunkIndex < chunkCount ; chunkIndex++, offset += CHUNK_SIZE) {
                put = new PutMethod(uriPrefix + chunkCount + "-" + chunkIndex);
                put.addRequestHeader(OC_CHUNKED_HEADER, OC_CHUNKED_HEADER);
                entity.setOffset(offset);
                put.setRequestEntity(entity);
                status = client.executeMethod(put);
                client.exhaustResponse(put.getResponseBodyAsStream());
                Log.d(TAG, "Upload of " + getLocalPath() + " to " + getRemotePath() + ", chunk index " + chunkIndex + ", count " + chunkCount + ", HTTP result status " + status);
                if (!isSuccess(status))
                    break;
            }
            
        } finally {
            if (lock != null)
                lock.release();
            if (channel != null)
                channel.close();
            if (raf != null)
                raf.close();
            if (put != null)
                put.releaseConnection();    // let the connection available for other methods
        }
        return status;
    }

}
