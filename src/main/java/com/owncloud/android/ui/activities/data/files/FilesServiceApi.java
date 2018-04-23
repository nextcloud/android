package com.owncloud.android.ui.activities.data.files;

import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.ui.activity.BaseActivity;

/**
 * Defines an interface to the Files service API. All {{@link OCFile}} remote data requests
 * should be piped through this interface.
 */
public interface FilesServiceApi {

    interface FilesServiceCallback<T> {
        void onLoaded(OCFile ocFile);
        void onError(String error);
    }

    void readRemoteFile(String fileUrl, BaseActivity activity, boolean isSharingSupported,
                        FilesServiceApi.FilesServiceCallback<OCFile> callback);
}
