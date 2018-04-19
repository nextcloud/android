package com.owncloud.android.ui.activities.data;

import android.support.annotation.NonNull;

import com.owncloud.android.lib.common.OwnCloudClient;

import java.util.List;

/**
 * Main entry point for accessing activities data.
 */
public interface ActivitiesRepository {
    interface LoadActivitiesCallback {
        void onActivitiesLoaded(List<Object> activities, OwnCloudClient client, boolean clear);
        void onActivitiesLoadedError(String error);
    }

    void getActivities(String pageUrl, @NonNull LoadActivitiesCallback callback);
}