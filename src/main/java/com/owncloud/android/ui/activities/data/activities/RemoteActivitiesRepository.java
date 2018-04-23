package com.owncloud.android.ui.activities.data.activities;

import android.support.annotation.NonNull;

import com.owncloud.android.lib.common.OwnCloudClient;

import java.util.List;

public class RemoteActivitiesRepository implements ActivitiesRepository {

    private final ActivitiesServiceApi mActivitiesServiceApi;

    public RemoteActivitiesRepository(@NonNull ActivitiesServiceApi activitiesServiceApi) {
        mActivitiesServiceApi = activitiesServiceApi;
    }


    @Override
    public void getActivities(String pageUrl, @NonNull LoadActivitiesCallback callback) {
        mActivitiesServiceApi.getAllActivities(pageUrl, new ActivitiesServiceApi.ActivitiesServiceCallback<List<Object>>() {
            @Override
            public void onLoaded(List<Object> activities, OwnCloudClient client,
                                 String nextPageUrl) {
                callback.onActivitiesLoaded(activities, client, nextPageUrl);
            }

            @Override
            public void onError(String error) {
                callback.onActivitiesLoadedError(error);
            }
        });
    }
}
