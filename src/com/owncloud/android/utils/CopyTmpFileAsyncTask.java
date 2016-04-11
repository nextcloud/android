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

import android.accounts.Account;
import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.widget.Toast;

import com.owncloud.android.R;
import com.owncloud.android.files.services.FileUploader;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.operations.UploadFileOperation;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.lang.ref.WeakReference;

/**
 * AsyncTask to copy a file from a uri in a temporal file
 */
public class CopyTmpFileAsyncTask  extends AsyncTask<Object, Void, String> {

    private final String TAG = CopyTmpFileAsyncTask.class.getSimpleName();

    /**
     * Helper method building a correct array of parameters to be passed to {@link #execute(Object[])} )}
     *
     * Just packages the received parameters in correct order, doesn't check anything about them.
     *
     * @return  Correct array of parameters to be passed to {@link #execute(Object[])}
     */
    public final static Object[] makeParamsToExecute(
        Account account,
        Uri sourceUri,
        String remotePath,
        Integer numCacheFile
    ) {

        return new Object[] {
            account,
            sourceUri,
            remotePath,
            numCacheFile
        };
    }


    /**
     * Listener in main thread to be notified when the task ends. Held in a WeakReference assuming that it's
     * lifespan is associated with an Activity context, that could be finished by the user before the AsyncTask
     * ends.
     */
    private final WeakReference<OnCopyTmpFileTaskListener> mListener;

    /**
     * Reference to application context, used to access app resources. Holding it should not be a problem,
     * since it needs to exist until the end of the AsyncTask although the caller Activity were finished
     * before.
     */
    private final Context mAppContext;


    private int mIndex;

    public CopyTmpFileAsyncTask(OnCopyTmpFileTaskListener listener, Context context) {
        mListener = new WeakReference<OnCopyTmpFileTaskListener>(listener);
        mAppContext = context.getApplicationContext();
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
        String pathToCopiedFile = null;

        if (params != null && params.length == 4) {
            Account account = (Account) params[0];
            Uri uri = (Uri) params[1];
            String remotePath = (String) params[2];
            mIndex = ((Integer) params[3]);  // TODO really?

            InputStream inputStream = null;
            FileOutputStream outputStream = null;
            String fullTempPath = null;

            ContentResolver contentResolver = mAppContext.getContentResolver();
            // TODO: test that it's safe for URLs with temporary access;
            //      alternative: receive InputStream in another parameter

            try {
                fullTempPath = FileStorageUtils.getTemporalPath(account.name) + remotePath;
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

                pathToCopiedFile = fullTempPath;

            } catch (Exception e) {
                Log_OC.e(TAG, "Exception while copying " + uri.toString() + " to temporary file", e);

                // clean
                if (fullTempPath != null) {
                    File f = new File(fullTempPath);
                    if (f.exists()) {
                        if (!f.delete()) {
                            Log_OC.e(TAG, "Could not delete temporary file " + fullTempPath);
                        }
                    }
                }

            } finally {
                if (inputStream != null) {
                    try {
                        inputStream.close();
                    } catch (Exception e) {
                        Log_OC.w(TAG, "Ignoring exception of inputStream closure");
                    }
                }

                if (outputStream != null) {
                    try {
                        outputStream.close();
                    } catch (Exception e) {
                        Log_OC.w(TAG, "Ignoring exception of outStream closure");
                    }
                }
            }

            if (pathToCopiedFile != null) {
                requestUpload(
                    account,
                    pathToCopiedFile,
                    remotePath,
                    contentResolver.getType(uri)
                );
                // mRemoteCacheData.get(index),

            } else {
                String message = String.format(
                    mAppContext.getString(R.string.uploader_error_forbidden_content),
                    mAppContext.getString(R.string.app_name)
                );
                Toast.makeText(mAppContext, message, Toast.LENGTH_LONG).show();
                Log_OC.d(TAG, message);
            }

        } else {
            throw new IllegalArgumentException("Error in parameters number");
        }

        return pathToCopiedFile;
    }

    private void requestUpload(Account account, String localPath, String remotePath, String mimeType) {
        FileUploader.UploadRequester requester = new FileUploader.UploadRequester();
        requester.uploadNewFile(
            mAppContext,
            account,
            localPath,
            remotePath,
            FileUploader.LOCAL_BEHAVIOUR_MOVE,  // the copy was already done, let's take advantage and move it
            // into the OC folder so that the folder was not
            mimeType,
            false,      // do not create parent folder if not existent
            UploadFileOperation.CREATED_BY_USER // TODO , different category?
        );
    }

    @Override
    protected void onPostExecute(String result) {
        OnCopyTmpFileTaskListener listener = mListener.get();
        if (listener!= null) {
            listener.onTmpFileCopied(result, mIndex);
        } else {
            Log_OC.i(TAG, "User left Uploader activity before the temporal copies were finished ");
        }
    }

    /*
     * Interface to retrieve data from recognition task
     */
    public interface OnCopyTmpFileTaskListener{

        void onTmpFileCopied(String result, int index);
    }
}
