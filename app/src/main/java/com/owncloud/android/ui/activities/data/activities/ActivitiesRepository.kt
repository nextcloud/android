/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-FileCopyrightText: 2018 Edvard Holst <edvard.holst@gmail.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.ui.activities.data.activities

import com.nextcloud.common.NextcloudClient
import kotlinx.coroutines.CoroutineScope

interface ActivitiesRepository {
    interface LoadActivitiesCallback {
        fun onActivitiesLoaded(activities: List<Any>, client: NextcloudClient, lastGiven: Long)
        fun onActivitiesLoadedError(error: String)
    }

    fun getActivities(lifecycleScope: CoroutineScope, lastGiven: Long, callback: LoadActivitiesCallback)
}
