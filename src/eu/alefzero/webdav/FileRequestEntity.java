package eu.alefzero.webdav;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.apache.commons.httpclient.methods.RequestEntity;

import eu.alefzero.webdav.OnDatatransferProgressListener;

import android.util.Log;


/**
 * A RequestEntity that represents a File.
 * 
 */
public class FileRequestEntity implements RequestEntity {

    final File mFile;
    final String mContentType;
    Set<OnDatatransferProgressListener> mListeners = new HashSet<OnDatatransferProgressListener>();

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
    
    public void addOnDatatransferProgressListener(OnDatatransferProgressListener listener) {
        mListeners.add(listener);
    }
    
    public void addOnDatatransferProgressListeners(Collection<OnDatatransferProgressListener> listeners) {
        mListeners.addAll(listeners);
    }
    
    public void removeOnDatatransferProgressListener(OnDatatransferProgressListener listener) {
        mListeners.remove(listener);
    }
    
    
    @Override
    public void writeRequest(final OutputStream out) throws IOException {
        //byte[] tmp = new byte[4096];
        ByteBuffer tmp = ByteBuffer.allocate(4096);
        int i = 0;
        
        // TODO(bprzybylski): each mem allocation can throw OutOfMemoryError we need to handle it
        //                    globally in some fashionable manner
        RandomAccessFile raf = new RandomAccessFile(mFile, "rw");
        FileChannel channel = raf.getChannel();
        FileLock lock = channel.tryLock();
        Iterator<OnDatatransferProgressListener> it = null;
        try {
            while ((i = channel.read(tmp)) >= 0) {
                out.write(tmp.array(), 0, i);
                tmp.clear();
                it = mListeners.iterator();
                while (it.hasNext()) {
                    it.next().onTransferProgress(i);
                }
            }
            
        } catch (IOException io) {
            Log.e("FileRequestException", io.getMessage());
            throw new RuntimeException("Ugly solution to workaround the default policy of retries when the server falls while uploading ; temporal fix; really", io);   
            
        } finally {
            lock.release();
            channel.close();
            raf.close();
        }
    }

}