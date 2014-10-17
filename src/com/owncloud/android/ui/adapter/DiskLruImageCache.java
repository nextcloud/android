package com.owncloud.android.ui.adapter;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.util.Log;

import com.jakewharton.disklrucache.DiskLruCache;
import com.owncloud.android.BuildConfig;
import com.owncloud.android.lib.common.utils.Log_OC;

public class DiskLruImageCache {

    private DiskLruCache mDiskCache;
    private CompressFormat mCompressFormat;
    private int mCompressQuality;
    private static final int CACHE_VERSION = 1;
    private static final int VALUE_COUNT = 1;
    private static final int IO_BUFFER_SIZE = 8 * 1024;
    private static final Pattern CAPITAL_LETTERS = Pattern.compile("[A-Z]"); 

    private StringBuffer mValidKeyBuffer = new StringBuffer(64);
    private StringBuffer mConversionBuffer = new StringBuffer(2).append('_'); 
            
    private static final String TAG = "DiskLruImageCache";

    public DiskLruImageCache( Context context,String uniqueName, int diskCacheSize,
        CompressFormat compressFormat, int quality ) throws IOException {
        final File diskCacheDir = getDiskCacheDir(context, uniqueName );
        mDiskCache = DiskLruCache.open(
                diskCacheDir, CACHE_VERSION, VALUE_COUNT, diskCacheSize 
        );
        mCompressFormat = compressFormat;
        mCompressQuality = quality;
    }

    private boolean writeBitmapToFile( Bitmap bitmap, DiskLruCache.Editor editor )
        throws IOException, FileNotFoundException {
        OutputStream out = null;
        try {
            out = new BufferedOutputStream( editor.newOutputStream( 0 ), IO_BUFFER_SIZE );
            return bitmap.compress( mCompressFormat, mCompressQuality, out );
        } finally {
            if ( out != null ) {
                out.close();
            }
        }
    }

    private File getDiskCacheDir(Context context, String uniqueName) {

    // Check if media is mounted or storage is built-in, if so, try and use external cache dir
    // otherwise use internal cache dir
        final String cachePath = context.getExternalCacheDir().getPath();

        Log_OC.d(TAG, "create dir: " + cachePath + File.separator + uniqueName);
                    
        return new File(cachePath + File.separator + uniqueName);
    }

    public void put( String key, Bitmap data ) {

        DiskLruCache.Editor editor = null;
        String validKey = convertToValidKey(key);
        try {
            editor = mDiskCache.edit( validKey );
            if ( editor == null ) {
                return;
            }

            if( writeBitmapToFile( data, editor ) ) {               
                mDiskCache.flush();
                editor.commit();
                if ( BuildConfig.DEBUG ) {
                   Log.d( "cache_test_DISK_", "image put on disk cache " + validKey );
                }
            } else {
                editor.abort();
                if ( BuildConfig.DEBUG ) {
                    Log.d( "cache_test_DISK_", "ERROR on: image put on disk cache " + validKey );
                }
            }   
        } catch (IOException e) {
            if ( BuildConfig.DEBUG ) {
                Log.d( "cache_test_DISK_", "ERROR on: image put on disk cache " + validKey );
            }
            try {
                if ( editor != null ) {
                    editor.abort();
                }
            } catch (IOException ignored) {
            }           
        }

    }

    public Bitmap getBitmap( String key ) {

        Bitmap bitmap = null;
        DiskLruCache.Snapshot snapshot = null;
        String validKey = convertToValidKey(key);
        try {

            snapshot = mDiskCache.get( validKey );
            if ( snapshot == null ) {
                return null;
            }
            final InputStream in = snapshot.getInputStream( 0 );
            if ( in != null ) {
                final BufferedInputStream buffIn = 
                new BufferedInputStream( in, IO_BUFFER_SIZE );
                bitmap = BitmapFactory.decodeStream( buffIn );              
            }   
        } catch ( IOException e ) {
            e.printStackTrace();
        } finally {
            if ( snapshot != null ) {
                snapshot.close();
            }
        }

        if ( BuildConfig.DEBUG ) {
            Log.d("cache_test_DISK_", bitmap == null ? "not found" : "image read from disk " + validKey);
        }

        return bitmap;

    }

    public boolean containsKey( String key ) {

        boolean contained = false;
        DiskLruCache.Snapshot snapshot = null;
        String validKey = convertToValidKey(key);
        try {
            snapshot = mDiskCache.get( validKey );
            contained = snapshot != null;
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if ( snapshot != null ) {
                snapshot.close();
            }
        }

        return contained;

    }

    public void clearCache() {
        if ( BuildConfig.DEBUG ) {
            Log.d( "cache_test_DISK_", "disk cache CLEARED");
        }
        try {
            mDiskCache.delete();
        } catch ( IOException e ) {
            e.printStackTrace();
        }
    }

    public File getCacheFolder() {
        return mDiskCache.getDirectory();
    }
    
    private String convertToValidKey(String key) {
        Matcher capitalLettersMatcher = CAPITAL_LETTERS.matcher(key);
        mValidKeyBuffer.delete(0, mValidKeyBuffer.length());
        mConversionBuffer.delete(1, mConversionBuffer.length());
        
        while (capitalLettersMatcher.find()) {
            mConversionBuffer.replace(1, 2, capitalLettersMatcher.group(0).toLowerCase()); 
            capitalLettersMatcher.appendReplacement(mValidKeyBuffer, mConversionBuffer.toString());
        }
        capitalLettersMatcher.appendTail(mValidKeyBuffer);
        return mValidKeyBuffer.toString();
    }

}