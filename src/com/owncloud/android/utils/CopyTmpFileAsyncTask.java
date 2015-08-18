/**
 *   ownCloud Android client application
 *
 *   @author masensio
 *   Copyright (C) 2015 ownCloud Inc.
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
package com.owncloud.android.utils;

import android.content.ContentResolver;
import android.net.Uri;
import android.os.AsyncTask;

import com.owncloud.android.lib.common.utils.Log_OC;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.lang.ref.WeakReference;

/**
 * AsyncTask to copy a file from a uri in a temporal file
 */
public class CopyTmpFileAsyncTask  extends AsyncTask<Object, Void, String> {

    private final String TAG = CopyTmpFileAsyncTask.class.getSimpleName();
    private final WeakReference<OnCopyTmpFileTaskListener> mListener;
    private int mIndex;

    public int getIndex(){
        return mIndex;
    }

    public CopyTmpFileAsyncTask(OnCopyTmpFileTaskListener listener) {
        mListener = new WeakReference<OnCopyTmpFileTaskListener>(listener);
    }

    /**
     * Params for execute:
     * - Uri: uri of file
     * - String: path for saving the file into the app
     * - int: index of upload
     * - String: accountName
     * - ContentResolver: content resolver
     */
    @Override
    protected String doInBackground(Object[] params) {
        String result = null;

        if (params != null && params.length == 5) {
            Uri uri = (Uri) params[0];
            String filePath = (String) params[1];
            mIndex = ((Integer) params[2]).intValue();
            String accountName = (String) params[3];
            ContentResolver contentResolver = (ContentResolver) params[4];

            String fullTempPath = FileStorageUtils.getTemporalPath(accountName) + filePath;
            InputStream inputStream = null;
            FileOutputStream outputStream = null;

            try {
                inputStream = contentResolver.openInputStream(uri);
                File cacheFile = new File(fullTempPath);
                File tempDir = cacheFile.getParentFile();
                if (!tempDir.exists()) {
                    tempDir.mkdirs();
                }
                cacheFile.createNewFile();
                outputStream = new FileOutputStream(fullTempPath);
                byte[] buffer = new byte[4096];

                int count = 0;

                while ((count = inputStream.read(buffer)) > 0) {
                    outputStream.write(buffer, 0, count);
                }

                outputStream.close();
                inputStream.close();

                result = fullTempPath;
            } catch (Exception e) {
                 Log_OC.e(TAG, "Exception ", e);
                if (inputStream != null) {
                    try {
                        inputStream.close();
                    } catch (Exception e1) {
                        Log_OC.e(TAG, "Input Stream Exception ", e1);
                    }
                }

                if (outputStream != null) {
                    try {
                        outputStream.close();
                    } catch (Exception e1) {
                        Log_OC.e(TAG, "Output Stream Exception ", e1);
                    }
                }

                if (fullTempPath != null) {
                    File f = new File(fullTempPath);
                    f.delete();
                }
                result =  null;
            }
        } else {
             throw new IllegalArgumentException("Error in parameters number");
        }

        return result;
    }

    @Override
    protected void onPostExecute(String result) {

        OnCopyTmpFileTaskListener listener = mListener.get();
        if (listener!= null)
        {
            listener.onTmpFileCopied(result, mIndex);
        }
    }

    /*
     * Interface to retrieve data from recognition task
     */
    public interface OnCopyTmpFileTaskListener{

        void onTmpFileCopied(String result, int index);
    }
}
