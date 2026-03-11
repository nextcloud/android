/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2018 Edvard Holst <edvard.holst@gmail.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
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

    void readRemoteFile(String fileUrl, BaseActivity activity, FilesServiceApi.FilesServiceCallback<OCFile> callback);
}
