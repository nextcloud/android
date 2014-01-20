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

package com.owncloud.android.lib.network;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.apache.commons.httpclient.methods.RequestEntity;


import android.util.Log;


/**
 * A RequestEntity that represents a PIECE of a file.
 * 
 * @author David A. Velasco
 */
public class ChunkFromFileChannelRequestEntity implements RequestEntity, ProgressiveDataTransferer {

    private static final String TAG = ChunkFromFileChannelRequestEntity.class.getSimpleName();
    
    //private final File mFile;
    private final FileChannel mChannel;
    private final String mContentType;
    private final long mChunkSize;
    private final File mFile;
    private long mOffset;
    private long mTransferred;
    Set<OnDatatransferProgressListener> mDataTransferListeners = new HashSet<OnDatatransferProgressListener>();
    private ByteBuffer mBuffer = ByteBuffer.allocate(4096);

    public ChunkFromFileChannelRequestEntity(final FileChannel channel, final String contentType, long chunkSize, final File file) {
        super();
        if (channel == null) {
            throw new IllegalArgumentException("File may not be null");
        }
        if (chunkSize <= 0) {
            throw new IllegalArgumentException("Chunk size must be greater than zero");
        }
        mChannel = channel;
        mContentType = contentType;
        mChunkSize = chunkSize;
        mFile = file;
        mOffset = 0;
        mTransferred = 0;
    }
    
    public void setOffset(long offset) {
        mOffset = offset;
    }
    
    public long getContentLength() {
        try {
            return Math.min(mChunkSize, mChannel.size() - mChannel.position());
        } catch (IOException e) {
            return mChunkSize;
        }
    }

    public String getContentType() {
        return mContentType;
    }

    public boolean isRepeatable() {
        return true;
    }
    
    @Override
    public void addDatatransferProgressListener(OnDatatransferProgressListener listener) {
        synchronized (mDataTransferListeners) {
            mDataTransferListeners.add(listener);
        }
    }
    
    @Override
    public void addDatatransferProgressListeners(Collection<OnDatatransferProgressListener> listeners) {
        synchronized (mDataTransferListeners) {
            mDataTransferListeners.addAll(listeners);
        }
    }
    
    @Override
    public void removeDatatransferProgressListener(OnDatatransferProgressListener listener) {
        synchronized (mDataTransferListeners) {
            mDataTransferListeners.remove(listener);
        }
    }
    
    
    public void writeRequest(final OutputStream out) throws IOException {
        int readCount = 0;
        Iterator<OnDatatransferProgressListener> it = null;
        
       try {
            mChannel.position(mOffset);
            long size = mFile.length();
            if (size == 0) size = -1;
            long maxCount = Math.min(mOffset + mChunkSize, mChannel.size());
            while (mChannel.position() < maxCount) {
                readCount = mChannel.read(mBuffer);
                out.write(mBuffer.array(), 0, readCount);
                mBuffer.clear();
                if (mTransferred < maxCount) {  // condition to avoid accumulate progress for repeated chunks
                    mTransferred += readCount;
                }
                synchronized (mDataTransferListeners) {
                    it = mDataTransferListeners.iterator();
                    while (it.hasNext()) {
                        it.next().onTransferProgress(readCount, mTransferred, size, mFile.getAbsolutePath());
                    }
                }
            }
            
        } catch (IOException io) {
            Log.e(TAG, io.getMessage());
            throw new RuntimeException("Ugly solution to workaround the default policy of retries when the server falls while uploading ; temporal fix; really", io);   
            
        }
    }

}