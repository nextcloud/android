package com.owncloud.android.ui.activities.data.files;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.ui.activity.BaseActivity;

/**
 * Main entry point for accessing remote files
 */
public interface FilesRepository {
    interface ReadRemoteFileCallback {
        void onFileLoaded(@Nullable OCFile ocFile);
        void onFileLoadError(String error);
    }

    void readRemoteFile(String path, BaseActivity activity, boolean isSharingSupported,
                        @NonNull ReadRemoteFileCallback callback);
}
