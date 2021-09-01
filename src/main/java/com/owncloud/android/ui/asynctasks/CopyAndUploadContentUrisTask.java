/**
 *   ownCloud Android client application
 *
 *   @author masensio
 *   @author Juan Carlos González Cabrero
 *   @author David A. Velasco
 *   Copyright (C) 2016 ownCloud Inc.
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
package com.owncloud.android.ui.asynctasks;

import android.accounts.Account;
import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.DocumentsContract;
import android.widget.Toast;

import com.nextcloud.client.account.User;
import com.owncloud.android.R;
import com.owncloud.android.files.services.FileUploader;
import com.owncloud.android.files.services.NameCollisionPolicy;
import com.owncloud.android.lib.common.operations.RemoteOperationResult.ResultCode;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.operations.UploadFileOperation;
import com.owncloud.android.utils.FileStorageUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.lang.ref.WeakReference;

/**
 * AsyncTask to copy a file from a uri in a temporal file
 */
public class CopyAndUploadContentUrisTask extends AsyncTask<Object, Void, ResultCode> {

    private final String TAG = CopyAndUploadContentUrisTask.class.getSimpleName();

    /**
     * Listener in main thread to be notified when the task ends. Held in a WeakReference assuming that its
     * lifespan is associated with an Activity context, that could be finished by the user before the AsyncTask
     * ends.
     */
    private WeakReference<OnCopyTmpFilesTaskListener> mListener;

    /**
     * Reference to application context, used to access app resources. Holding it should not be a problem,
     * since it needs to exist until the end of the AsyncTask although the caller Activity were finished
     * before.
     */
    private final Context mAppContext;

    /**
     * Helper method building a correct array of parameters to be passed to {@link #execute(Object[])} )}
     *
     * Just packages the received parameters in correct order, doesn't check anything about them.
     *
     * @param   user                user uploading shared files
     * @param   sourceUris          Array of "content://" URIs to the files to be uploaded.
     * @param   remotePaths         Array of absolute paths in the OC account to set to the uploaded files.
     * @param   behaviour           Indicates what to do with the local file once uploaded.
     * @param   contentResolver     {@link ContentResolver} instance with appropriate permissions to open the
     *                              URIs in 'sourceUris'.
     *
     * Handling this parameter in {@link #doInBackground(Object[])} keeps an indirect reference to the
     * caller Activity, what is technically wrong, since it will be held in memory
     * (with all its associated resources) until the task finishes even though the user leaves the Activity.
     *
     * But we really, really, really want that the files are copied to temporary files in the OC folder and then
     * uploaded, even if the user gets bored of waiting while the copy finishes. And we can't forward the job to
     * another {@link Context}, because if any of the content:// URIs is constrained by a TEMPORARY READ PERMISSION,
     * trying to open it will fail with a {@link SecurityException} after the user leaves the ReceiveExternalFilesActivity Activity. We
     * really tried it.
     *
     * So we are doomed to leak here for the best interest of the user. Please, don't do similar in other places.
     *
     * Any idea to prevent this while keeping the functionality will be welcome.
     *
     * @return  Correct array of parameters to be passed to {@link #execute(Object[])}
     */
    public static Object[] makeParamsToExecute(
        User user,
        Uri[] sourceUris,
        String[] remotePaths,
        int behaviour,
        ContentResolver contentResolver
    ) {

        return new Object[] {
            user,
            sourceUris,
            remotePaths,
            Integer.valueOf(behaviour),
            contentResolver
        };
    }

    public CopyAndUploadContentUrisTask(
        OnCopyTmpFilesTaskListener listener,
        Context context
    ) {
        mListener = new WeakReference<>(listener);
        mAppContext = context.getApplicationContext();
    }

    /**
     * @param params    Params to execute the task; see
     *                  {@link #makeParamsToExecute(User, Uri[], String[], int, ContentResolver)}
     *                  for further details.
     */
    @Override
    protected ResultCode doInBackground(Object[] params) {

        ResultCode result = ResultCode.UNKNOWN_ERROR;

        InputStream inputStream = null;
        FileOutputStream outputStream = null;
        String fullTempPath = null;
        Uri currentUri = null;

        try {
            User user = (User) params[0];
            Uri[] uris = (Uri[]) params[1];
            String[] remotePaths = (String[]) params[2];
            int behaviour = (Integer) params[3];
            ContentResolver leakedContentResolver = (ContentResolver) params[4];

            String currentRemotePath;

            for (int i = 0; i < uris.length; i++) {
                currentUri = uris[i];
                currentRemotePath = remotePaths[i];

                long lastModified = 0;
                try (Cursor cursor = leakedContentResolver.query(currentUri,
                                                                 null,
                                                                 null,
                                                                 null,
                                                                 null)) {
                    if (cursor != null && cursor.moveToFirst()) {
                        // this check prevents a crash when last modification time is not available on certain phones
                        int columnIndex = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_LAST_MODIFIED);
                        if (columnIndex >= 0) {
                            lastModified = cursor.getLong(columnIndex);
                        }
                    }
                }

                fullTempPath = FileStorageUtils.getTemporalPath(user.getAccountName()) + currentRemotePath;
                inputStream = leakedContentResolver.openInputStream(currentUri);
                File cacheFile = new File(fullTempPath);
                File tempDir = cacheFile.getParentFile();
                if (!tempDir.exists()) {
                    tempDir.mkdirs();
                }
                cacheFile.createNewFile();
                outputStream = new FileOutputStream(fullTempPath);
                byte[] buffer = new byte[4096];

                int count;
                while ((count = inputStream.read(buffer)) > 0) {
                    outputStream.write(buffer, 0, count);
                }

                if (lastModified != 0) {
                    try {
                        if (!cacheFile.setLastModified(lastModified)) {
                            Log_OC.w(TAG, "Could not change mtime of cacheFile");
                        }
                    } catch (SecurityException e) {
                        Log_OC.e(TAG, "Not enough permissions to change mtime of cacheFile", e);
                    } catch (IllegalArgumentException e) {
                        Log_OC.e(TAG, "Could not change mtime of cacheFile, mtime is negativ: "+lastModified, e);
                    }
                }

                requestUpload(
                    user.toPlatformAccount(),
                    fullTempPath,
                    currentRemotePath,
                    behaviour,
                    leakedContentResolver.getType(currentUri)
                );
                fullTempPath = null;
            }

            result = ResultCode.OK;

        } catch (ArrayIndexOutOfBoundsException e) {
            Log_OC.e(TAG, "Wrong number of arguments received ", e);

        } catch (ClassCastException e) {
            Log_OC.e(TAG, "Wrong parameter received ", e);

        } catch (FileNotFoundException e) {
            Log_OC.e(TAG, "Could not find source file " + currentUri, e);
            result = ResultCode.LOCAL_FILE_NOT_FOUND;

        } catch (SecurityException e) {
            Log_OC.e(TAG, "Not enough permissions to read source file " + currentUri, e);
            result = ResultCode.FORBIDDEN;

        } catch (Exception e) {
            Log_OC.e(TAG, "Exception while copying " + currentUri + " to temporary file", e);
            result =  ResultCode.LOCAL_STORAGE_NOT_COPIED;

            // clean
            if (fullTempPath != null) {
                File f = new File(fullTempPath);
                if (f.exists() && !f.delete()) {
                    Log_OC.e(TAG, "Could not delete temporary file " + fullTempPath);
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

        return result;
    }

    private void requestUpload(Account account, String localPath, String remotePath, int behaviour, String mimeType) {
        FileUploader.uploadNewFile(
                mAppContext,
                account,
                localPath,
                remotePath,
                behaviour,
                mimeType,
                false,      // do not create parent folder if not existent
                UploadFileOperation.CREATED_BY_USER,
                false,
                false,
                NameCollisionPolicy.ASK_USER
        );
    }

    @Override
    protected void onPostExecute(ResultCode result) {
        OnCopyTmpFilesTaskListener listener = mListener.get();
        if (listener!= null) {
            listener.onTmpFilesCopied(result);

        } else {
            Log_OC.i(TAG, "User left the caller activity before the temporal copies were finished ");
            if (result != ResultCode.OK) {
                // if the user left the app, report background error in a Toast
                int messageId;
                switch (result) {
                    case LOCAL_FILE_NOT_FOUND:
                        messageId = R.string.uploader_error_message_source_file_not_found;
                        break;
                    case LOCAL_STORAGE_NOT_COPIED:
                        messageId = R.string.uploader_error_message_source_file_not_copied;
                        break;
                    case FORBIDDEN:
                        messageId = R.string.uploader_error_message_read_permission_not_granted;
                        break;
                    default:
                        messageId = R.string.common_error_unknown;
                        break;
                }
                String message = String.format(
                    mAppContext.getString(messageId),
                    mAppContext.getString(R.string.app_name)
                );
                Toast.makeText(mAppContext, message, Toast.LENGTH_LONG).show();
            }
        }
    }

    /**
     * Sets the object waiting for progress report via callbacks.
     *
     * @param listener      New object to report progress via callbacks
     */
    public void setListener(OnCopyTmpFilesTaskListener listener) {
        mListener = new WeakReference<>(listener);
    }

    /**
     * Interface to retrieve data from recognition task
     */
    public interface OnCopyTmpFilesTaskListener {
        void onTmpFilesCopied(ResultCode result);
    }
}
