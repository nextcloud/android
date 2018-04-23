package com.owncloud.android.ui.activities.data;

import com.owncloud.android.ui.activities.data.activities.ActivitiesRepository;
import com.owncloud.android.ui.activities.data.activities.ActivitiesServiceApiImpl;
import com.owncloud.android.ui.activities.data.activities.ActivityRepositories;
import com.owncloud.android.ui.activities.data.files.FileRepositories;
import com.owncloud.android.ui.activities.data.files.FilesRepository;
import com.owncloud.android.ui.activities.data.files.FilesServiceApiImpl;

public class Injection {

    public static ActivitiesRepository provideActivitiesRepository() {
        return ActivityRepositories.getRepository(new ActivitiesServiceApiImpl());
    }

    public static FilesRepository provideFilesRepository() {
        return FileRepositories.getRepository(new FilesServiceApiImpl());
    }
}
