/*
 *   ownCloud Android client application
 *
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
package com.owncloud.android.ui.helpers;

import android.content.ContentResolver;
import android.net.Uri;
import android.os.Parcelable;

import com.nextcloud.client.account.User;
import com.owncloud.android.R;
import com.owncloud.android.files.services.FileUploader;
import com.owncloud.android.files.services.NameCollisionPolicy;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.operations.UploadFileOperation;
import com.owncloud.android.ui.activity.FileActivity;
import com.owncloud.android.ui.asynctasks.CopyAndUploadContentUrisTask;
import com.owncloud.android.ui.fragment.TaskRetainerFragment;
import com.owncloud.android.utils.UriUtils;

import java.util.ArrayList;
import java.util.List;

import androidx.fragment.app.FragmentManager;

/**
 * This class examines URIs pointing to files to upload and then requests {@link FileUploader} to upload them.
 * <p>
 * URIs with scheme file:// do not require any previous processing, their path is sent to {@link FileUploader} to find
 * the source file.
 * <p>
 * URIs with scheme content:// are handling assuming that file is in private storage owned by a different app, and that
 * persistence permission is not granted. Due to this, contents of the file are temporary copied by the OC app, and then
 * passed {@link FileUploader}.
 */
public class UriUploader {

    private final String TAG = UriUploader.class.getSimpleName();

    private final FileActivity mActivity;
    private final List<Parcelable> mUrisToUpload;
    private final CopyAndUploadContentUrisTask.OnCopyTmpFilesTaskListener mCopyTmpTaskListener;

    private final int mBehaviour;

    private final String mUploadPath;
    private User user;
    private final boolean mShowWaitingDialog;

    private UriUploaderResultCode mCode = UriUploaderResultCode.OK;

    public enum UriUploaderResultCode {
        OK,
        ERROR_UNKNOWN,
        ERROR_NO_FILE_TO_UPLOAD,
        ERROR_READ_PERMISSION_NOT_GRANTED
    }

    public UriUploader(
            FileActivity activity,
            List<Parcelable> uris,
            String uploadPath,
            User user,
            int behaviour,
            boolean showWaitingDialog,
            CopyAndUploadContentUrisTask.OnCopyTmpFilesTaskListener copyTmpTaskListener
    ) {
        mActivity = activity;
        mUrisToUpload = uris;
        mUploadPath = uploadPath;
        this.user = user;
        mBehaviour = behaviour;
        mShowWaitingDialog = showWaitingDialog;
        mCopyTmpTaskListener = copyTmpTaskListener;
    }

    public UriUploaderResultCode uploadUris() {

        try {

            List<Uri> contentUris = new ArrayList<>();
            List<String> contentRemotePaths = new ArrayList<>();

            int schemeFileCounter = 0;

            for (Parcelable sourceStream : mUrisToUpload) {
                Uri sourceUri = (Uri) sourceStream;
                if (sourceUri != null) {
                    String displayName = UriUtils.getDisplayNameForUri(sourceUri, mActivity);

                    if (displayName == null) {
                        throw new IllegalStateException("DisplayName may not be null!");
                    }

                    String remotePath = mUploadPath + displayName;

                    if (ContentResolver.SCHEME_CONTENT.equals(sourceUri.getScheme())) {
                        contentUris.add(sourceUri);
                        contentRemotePaths.add(remotePath);

                    } else if (ContentResolver.SCHEME_FILE.equals(sourceUri.getScheme())) {
                        /// file: uris should point to a local file, should be safe let FileUploader handle them
                        requestUpload(sourceUri.getPath(), remotePath);
                        schemeFileCounter++;
                    }
                }
            }

            if (!contentUris.isEmpty()) {
                /// content: uris will be copied to temporary files before calling {@link FileUploader}
                copyThenUpload(contentUris.toArray(new Uri[0]),
                        contentRemotePaths.toArray(new String[0]));

            } else if (schemeFileCounter == 0) {
                mCode = UriUploaderResultCode.ERROR_NO_FILE_TO_UPLOAD;

            }

        } catch (SecurityException e) {
            mCode = UriUploaderResultCode.ERROR_READ_PERMISSION_NOT_GRANTED;
            Log_OC.e(TAG, "Permissions fail", e);

        } catch (Exception e) {
            mCode = UriUploaderResultCode.ERROR_UNKNOWN;
            Log_OC.e(TAG, "Unexpected error", e);

        }
        return mCode;
    }

    /**
     * Requests the upload of a file in the local file system to {@link FileUploader} service.
     *
     * The original file will be left in its original location, and will not be duplicated.
     * As a side effect, the user will see the file as not uploaded when accesses to the OC app.
     * This is considered as acceptable, since when a file is shared from another app to OC,
     * the usual workflow will go back to the original app.
     *
     * @param localPath     Absolute path in the local file system to the file to upload.
     * @param remotePath    Absolute path in the current OC account to set to the uploaded file.
     */
    private void requestUpload(String localPath, String remotePath) {
        FileUploader.uploadNewFile(
            mActivity,
            user.toPlatformAccount(),
            localPath,
            remotePath,
            mBehaviour,
            null,       // MIME type will be detected from file name
            false,      // do not create parent folder if not existent
            UploadFileOperation.CREATED_BY_USER,
            false,
            false,
            NameCollisionPolicy.ASK_USER
        );
    }

    /**
     *
     * @param sourceUris        Array of content:// URIs to the files to upload
     * @param remotePaths       Array of absolute paths to set to the uploaded files
     */
    private void copyThenUpload(Uri[] sourceUris, String... remotePaths) {
        if (mShowWaitingDialog) {
            mActivity.showLoadingDialog(mActivity.getResources().
                    getString(R.string.wait_for_tmp_copy_from_private_storage));
        }

        CopyAndUploadContentUrisTask copyTask = new CopyAndUploadContentUrisTask
                (mCopyTmpTaskListener, mActivity);

        FragmentManager fm = mActivity.getSupportFragmentManager();

        // Init Fragment without UI to retain AsyncTask across configuration changes
        TaskRetainerFragment taskRetainerFragment =
                (TaskRetainerFragment) fm.findFragmentByTag(TaskRetainerFragment.FTAG_TASK_RETAINER_FRAGMENT);

        if (taskRetainerFragment != null) {
            taskRetainerFragment.setTask(copyTask);
        }

        copyTask.execute(
                CopyAndUploadContentUrisTask.makeParamsToExecute(
                    user,
                    sourceUris,
                    remotePaths,
                    mBehaviour,
                    mActivity.getContentResolver()
                )
        );
    }
}
