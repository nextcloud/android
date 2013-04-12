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

package eu.alefzero.webdav;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
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
 * A RequestEntity that represents a File.
 * 
 */
public class FileRequestEntity implements RequestEntity, ProgressiveDataTransferer {

    final File mFile;
    final String mContentType;
    Set<OnDatatransferProgressListener> mDataTransferListeners = new HashSet<OnDatatransferProgressListener>();

    public FileRequestEntity(final File file, final String contentType) {
        super();
        this.mFile = file;
        this.mContentType = contentType;
        if (file == null) {
            throw new IllegalArgumentException("File may not be null");
        }
    }
    
    @Override
    public long getContentLength() {
        return mFile.length();
    }

    @Override
    public String getContentType() {
        return mContentType;
    }

    @Override
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
    
    
    @Override
    public void writeRequest(final OutputStream out) throws IOException {
        //byte[] tmp = new byte[4096];
        ByteBuffer tmp = ByteBuffer.allocate(4096);
        int readResult = 0;
        
        // TODO(bprzybylski): each mem allocation can throw OutOfMemoryError we need to handle it
        //                    globally in some fashionable manner
        RandomAccessFile raf = new RandomAccessFile(mFile, "r");
        FileChannel channel = raf.getChannel();
        Iterator<OnDatatransferProgressListener> it = null;
        long transferred = 0;
        long size = mFile.length();
        if (size == 0) size = -1;
        try {
            while ((readResult = channel.read(tmp)) >= 0) {
                out.write(tmp.array(), 0, readResult);
                tmp.clear();
                transferred += readResult;
                synchronized (mDataTransferListeners) {
                    it = mDataTransferListeners.iterator();
                    while (it.hasNext()) {
                        it.next().onTransferProgress(readResult, transferred, size, mFile.getName());
                    }
                }
            }
            
        } catch (IOException io) {
            Log.e("FileRequestException", io.getMessage());
            throw new RuntimeException("Ugly solution to workaround the default policy of retries when the server falls while uploading ; temporal fix; really", io);   
            
        } finally {
            channel.close();
            raf.close();
        }
    }

}
