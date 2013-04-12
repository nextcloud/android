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

package eu.alefzero.webdav;

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

import com.owncloud.android.network.ProgressiveDataTransferer;

import eu.alefzero.webdav.OnDatatransferProgressListener;

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
            while (mChannel.position() < mOffset + mChunkSize && mChannel.position() < mChannel.size()) {
                readCount = mChannel.read(mBuffer);
                out.write(mBuffer.array(), 0, readCount);
                mBuffer.clear();
                mTransferred += readCount;
                synchronized (mDataTransferListeners) {
                    it = mDataTransferListeners.iterator();
                    while (it.hasNext()) {
                        it.next().onTransferProgress(readCount, mTransferred, size, mFile.getName());
                    }
                }
            }
            
        } catch (IOException io) {
            Log.e(TAG, io.getMessage());
            throw new RuntimeException("Ugly solution to workaround the default policy of retries when the server falls while uploading ; temporal fix; really", io);   
            
        }
    }

}