package com.owncloud.android.ui.activities.data;

import android.content.Context;

import com.owncloud.android.lib.common.OwnCloudClient;
import com.owncloud.android.lib.resources.activities.models.Activity;

import java.util.List;

/**
 * Defines an interface to the Activities service API. All ({@link Activity}) data requests should
 * be piped through this interface.
 */

public interface ActivitiesServiceApi {

    interface ActivitiesServiceCallback<T> {
        void onLoaded (T activities, OwnCloudClient client, boolean clear);
        void onError (String error);
    }

    void getAllActivities(String pageUrl, ActivitiesServiceApi.ActivitiesServiceCallback<List<Object>> callback);

}
