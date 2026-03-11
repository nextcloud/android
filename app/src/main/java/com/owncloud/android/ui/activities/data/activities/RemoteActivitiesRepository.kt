/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-FileCopyrightText: 2018 Edvard Holst <edvard.holst@gmail.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.ui.activities.data.activities

import com.nextcloud.common.NextcloudClient
import com.owncloud.android.ui.activities.data.activities.ActivitiesRepository.LoadActivitiesCallback
import com.owncloud.android.ui.activities.data.activities.ActivitiesServiceApi.ActivitiesServiceCallback

class RemoteActivitiesRepository(private val activitiesServiceApi: ActivitiesServiceApi) : ActivitiesRepository {
    override fun getActivities(lastGiven: Long, callback: LoadActivitiesCallback) {
        activitiesServiceApi.getAllActivities(
            lastGiven,
            object : ActivitiesServiceCallback<List<Any>> {
                override fun onLoaded(activities: List<Any>, client: NextcloudClient, lastGiven: Long) {
                    callback.onActivitiesLoaded(activities, client, lastGiven)
                }

                override fun onError(error: String) {
                    callback.onActivitiesLoadedError(error)
                }
            }
        )
    }
}
