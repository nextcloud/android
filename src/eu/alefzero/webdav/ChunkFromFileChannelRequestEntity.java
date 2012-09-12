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

package eu.alefzero.webdav;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

import org.apache.commons.httpclient.methods.RequestEntity;

import eu.alefzero.webdav.OnDatatransferProgressListener;

import android.util.Log;


/**
 * A RequestEntity that represents a PIECE of a file.
 * 
 * @author David A. Velasco
 */
public class ChunkFromFileChannelRequestEntity implements RequestEntity {

    private static final String TAG = ChunkFromFileChannelRequestEntity.class.getSimpleName();
    
    //private final File mFile;
    private final FileChannel mChannel;
    private final String mContentType;
    private final long mSize;
    private long mOffset;
    private OnDatatransferProgressListener mListener;
    private ByteBuffer mBuffer = ByteBuffer.allocate(4096);

    public ChunkFromFileChannelRequestEntity(final FileChannel channel, final String contentType, long size) {
        super();
        if (channel == null) {
            throw new IllegalArgumentException("File may not be null");
        }
        if (size <= 0) {
            throw new IllegalArgumentException("Size must be greater than zero");
        }
        mChannel = channel;
        mContentType = contentType;
        mSize = size;
        mOffset = 0;
    }
    
    public void setOffset(long offset) {
        mOffset = offset;
    }
    
    public long getContentLength() {
        try {
            return Math.min(mSize, mChannel.size() - mChannel.position());
        } catch (IOException e) {
            return mSize;
        }
    }

    public String getContentType() {
        return mContentType;
    }

    public boolean isRepeatable() {
        return true;
    }
    
    public void setOnDatatransferProgressListener(OnDatatransferProgressListener listener) {
        mListener = listener;
    }
    
    public void writeRequest(final OutputStream out) throws IOException {
        int readCount = 0;
        
       try {
            //while ((i = instream.read(tmp)) >= 0) {
            mChannel.position(mOffset);
            while (mChannel.position() < mOffset + mSize) {
                readCount = mChannel.read(mBuffer);
                out.write(mBuffer.array(), 0, readCount);
                mBuffer.clear();
                if (mListener != null) 
                    mListener.transferProgress(readCount);
            }
            
        } catch (IOException io) {
            Log.e(TAG, io.getMessage());
            throw new RuntimeException("Ugly solution to workaround the default policy of retries when the server falls while uploading ; temporal fix; really", io);   
            
        }
    }

}