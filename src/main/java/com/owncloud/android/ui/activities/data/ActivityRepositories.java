package com.owncloud.android.ui.activities.data;

import android.support.annotation.NonNull;

public class ActivityRepositories {

    private ActivityRepositories() {
        // No instance
    }

    private static ActivitiesRepository repository = null;

    public static synchronized ActivitiesRepository getRepository(@NonNull ActivitiesServiceApi activitiesServiceApi) {
        return new RemoteActivitiesRepository(activitiesServiceApi);
    }

}

