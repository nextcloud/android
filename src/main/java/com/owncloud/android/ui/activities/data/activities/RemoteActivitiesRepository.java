/**
 *   Nextcloud Android client application
 *
 *   Copyright (C) 2018 Edvard Holst
 *
 *   This program is free software; you can redistribute it and/or
 *   modify it under the terms of the GNU AFFERO GENERAL PUBLIC LICENSE
 *   License as published by the Free Software Foundation; either
 *   version 3 of the License, or any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU AFFERO GENERAL PUBLIC LICENSE for more details.
 *
 *   You should have received a copy of the GNU Affero General Public
 *   License along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.owncloud.android.ui.activities.data.activities;

import com.owncloud.android.lib.common.OwnCloudClient;

import java.util.List;

import androidx.annotation.NonNull;

public class RemoteActivitiesRepository implements ActivitiesRepository {

    private final ActivitiesServiceApi activitiesServiceApi;

    public RemoteActivitiesRepository(@NonNull ActivitiesServiceApi activitiesServiceApi) {
        this.activitiesServiceApi = activitiesServiceApi;
    }


    @Override
    public void getActivities(String pageUrl, @NonNull LoadActivitiesCallback callback) {
        activitiesServiceApi.getAllActivities(pageUrl, new ActivitiesServiceApi.ActivitiesServiceCallback<List<Object>>() {
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
