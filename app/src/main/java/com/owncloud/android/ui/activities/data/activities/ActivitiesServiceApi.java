/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2018 Edvard Holst <edvard.holst@gmail.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.ui.activities.data.activities;

import com.nextcloud.common.NextcloudClient;
import com.owncloud.android.lib.resources.activities.model.Activity;

import java.util.List;

/**
 * Defines an interface to the Activities service API. All ({@link Activity}) data requests should
 * be piped through this interface.
 */

public interface ActivitiesServiceApi {

    interface ActivitiesServiceCallback<T> {
        void onLoaded(T activities, NextcloudClient client, long lastGiven);
        void onError (String error);
    }

    void getAllActivities(long lastGiven, ActivitiesServiceApi.ActivitiesServiceCallback<List<Object>> callback);

}
