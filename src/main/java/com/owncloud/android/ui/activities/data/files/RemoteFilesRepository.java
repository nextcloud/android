package com.owncloud.android.ui.activities.data.files;

import android.support.annotation.NonNull;

import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.ui.activity.BaseActivity;

class RemoteFilesRepository implements FilesRepository {

    private final FilesServiceApi mFilesServiceApi;

    public RemoteFilesRepository(@NonNull FilesServiceApi filesServiceApi) {
        mFilesServiceApi = filesServiceApi;
    }


    @Override
    public void readRemoteFile(String path, BaseActivity activity, boolean isSharingSupported, @NonNull ReadRemoteFileCallback callback) {
        mFilesServiceApi.readRemoteFile(path, activity, isSharingSupported,
                new FilesServiceApi.FilesServiceCallback<OCFile>() {
                    @Override
                    public void onLoaded(OCFile ocFile) {
                        callback.onFileLoaded(ocFile);
                    }

                    @Override
                    public void onError(String error) {
                        callback.onFileLoadError(error);
                    }
                });
    }
}
