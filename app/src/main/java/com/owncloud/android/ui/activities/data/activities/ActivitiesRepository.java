/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2018 Edvard Holst <edvard.holst@gmail.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
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
        void onActivitiesLoaded(List<Object> activities, NextcloudClient client, long lastGiven);
        void onActivitiesLoadedError(String error);
    }

    void getActivities(long lastGiven, @NonNull LoadActivitiesCallback callback);
}
