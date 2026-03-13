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

/**
 * Defines an interface to the Activities service API. All ([Activity]) data requests should
 * be piped through this interface.
 */
interface ActivitiesServiceApi {
    interface ActivitiesServiceCallback<T> {
        fun onLoaded(activities: T, client: NextcloudClient, lastGiven: Long)
        fun onError(error: String)
    }

    fun getAllActivities(
        lifecycleScope: CoroutineScope,
        lastGiven: Long,
        callback: ActivitiesServiceCallback<List<Any>>
    )
}
