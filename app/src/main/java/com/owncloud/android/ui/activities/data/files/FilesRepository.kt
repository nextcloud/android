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
import androidx.annotation.Nullable;

/**
 * Main entry point for accessing remote files
 */
public interface FilesRepository {
    interface ReadRemoteFileCallback {
        void onFileLoaded(@Nullable OCFile ocFile);
        void onFileLoadError(String error);
    }

    void readRemoteFile(String path, BaseActivity activity, @NonNull ReadRemoteFileCallback callback);
}
