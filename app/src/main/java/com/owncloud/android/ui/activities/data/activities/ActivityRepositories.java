/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2018 Edvard Holst <edvard.holst@gmail.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.ui.activities.data.activities;

import androidx.annotation.NonNull;

public final class ActivityRepositories {

    private ActivityRepositories() {
        // No instance
    }

    public static synchronized ActivitiesRepository getRepository(@NonNull ActivitiesServiceApi activitiesServiceApi) {
        return new RemoteActivitiesRepository(activitiesServiceApi);
    }
}
