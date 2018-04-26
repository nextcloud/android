package com.owncloud.android.ui.activities.data.activities;

import android.support.annotation.NonNull;

public class ActivityRepositories {

    private ActivityRepositories() {
        // No instance
    }

    public static synchronized ActivitiesRepository getRepository(@NonNull ActivitiesServiceApi activitiesServiceApi) {
        return new RemoteActivitiesRepository(activitiesServiceApi);
    }

}

