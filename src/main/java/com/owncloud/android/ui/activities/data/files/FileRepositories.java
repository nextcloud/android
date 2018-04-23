package com.owncloud.android.ui.activities.data.files;

import android.support.annotation.NonNull;

public class FileRepositories {

    private FileRepositories() {
        // No instance
    }

    private static FilesRepository repository = null;

    public static synchronized FilesRepository getRepository(@NonNull FilesServiceApi filesServiceApi) {
        return new RemoteFilesRepository(filesServiceApi);
    }
}
