package com.owncloud.android.ui.activities.data.files;

import android.support.annotation.NonNull;

public class FileRepositories {

    private FileRepositories() {
        // No instance
    }

    public static synchronized FilesRepository getRepository(@NonNull FilesServiceApi filesServiceApi) {
        return new RemoteFilesRepository(filesServiceApi);
    }
}
