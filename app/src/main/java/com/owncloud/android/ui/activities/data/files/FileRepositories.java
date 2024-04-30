/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2018 Edvard Holst <edvard.holst@gmail.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.ui.activities.data.files;

import androidx.annotation.NonNull;

public final class FileRepositories {

    private FileRepositories() {
        // No instance
    }

    public static synchronized FilesRepository getRepository(@NonNull FilesServiceApi filesServiceApi) {
        return new RemoteFilesRepository(filesServiceApi);
    }
}
