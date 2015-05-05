/**
 *   ownCloud Android client application
 *
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

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;

import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.ui.activity.FileActivity;

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
    private String mAccountName;
    private ContentResolver mContentResolver;
    private int mIndex;

    public int getIndex(){
        return mIndex;
    }

    public CopyTmpFileAsyncTask(Activity activity) {
        mContentResolver = ((FileActivity) activity).getContentResolver();
        mAccountName = ((FileActivity) activity).getAccount().name;
        mListener = new WeakReference<OnCopyTmpFileTaskListener>((OnCopyTmpFileTaskListener)activity);
    }

    @Override
    protected String doInBackground(Object[] params) {
        String result = null;

        if (params.length == 3) {
            Uri uri = (Uri) params[0];
            String filePath = (String) params[1];
            mIndex = ((Integer) params[2]).intValue();

            String fullTempPath = FileStorageUtils.getTemporalPath(mAccountName) + filePath;
            InputStream inputStream = null;
            FileOutputStream outputStream = null;

            try {
                inputStream = mContentResolver.openInputStream(uri);
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
            Log_OC.e(TAG, "Error in parameters number");
        }

        return result;
    }

    @Override
    protected void onPostExecute(String result) {

        OnCopyTmpFileTaskListener listener = mListener.get();
        if (listener!= null)
        {
            listener.OnCopyTmpFileTaskListener(result, mIndex);
        }
    }

    /*
     * Interface to retrieve data from recognition task
     */
    public interface OnCopyTmpFileTaskListener{

        void OnCopyTmpFileTaskListener(String result, int index);
    }
}
