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

import com.nextcloud.common.NextcloudClient;

import java.util.List;

import androidx.annotation.NonNull;

/**
 * Main entry point for accessing activities data.
 */
public interface ActivitiesRepository {
    interface LoadActivitiesCallback {
        void onActivitiesLoaded(List<Object> activities, NextcloudClient client, int lastGiven);
        void onActivitiesLoadedError(String error);
    }

    void getActivities(int lastGiven, @NonNull LoadActivitiesCallback callback);
}
