/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2018 Edvard Holst <edvard.holst@gmail.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.ui.activities.data.files;

import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.ui.activity.BaseActivity;

import androidx.annotation.NonNull;

public class RemoteFilesRepository implements FilesRepository {

    private final FilesServiceApi filesServiceApi;

    public RemoteFilesRepository(@NonNull FilesServiceApi filesServiceApi) {
        this.filesServiceApi = filesServiceApi;
    }


    @Override
    public void readRemoteFile(String path, BaseActivity activity, @NonNull ReadRemoteFileCallback callback) {
        filesServiceApi.readRemoteFile(path, activity, new FilesServiceApi.FilesServiceCallback<>() {
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
