/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2020 Chris Narkiewicz <hello@ezaquarii.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.ui.unifiedsearch

import com.nextcloud.android.lib.resources.search.UnifiedSearchProvidersRemoteOperation
import com.nextcloud.common.NextcloudClient
import com.owncloud.android.lib.common.SearchProviders
import com.owncloud.android.lib.common.utils.Log_OC

class GetSearchProvidersTask(private val client: NextcloudClient) : () -> GetSearchProvidersTask.Result {

    companion object {
        const val TAG = "GetSearchProviders"
    }

    data class Result(val success: Boolean = false, val providers: SearchProviders = SearchProviders())

    override fun invoke(): Result {
        Log_OC.d(TAG, "Getting search providers")
        val result = UnifiedSearchProvidersRemoteOperation().execute(client)

        Log_OC.d(TAG, "Task finished: " + result.isSuccess)
        return when {
            result.isSuccess && result.resultData != null -> {
                Result(
                    success = true,
                    providers = result.resultData
                )
            }
            else -> Result()
        }
    }
}
