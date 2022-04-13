/*
 * Nextcloud Android client application
 *
 * @author Chris Narkiewicz
 * Copyright (C) 2020 Chris Narkiewicz <hello@ezaquarii.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package com.owncloud.android.ui.unifiedsearch

import com.nextcloud.android.lib.resources.search.UnifiedSearchProvidersRemoteOperation
import com.nextcloud.common.NextcloudClient
import com.owncloud.android.lib.common.SearchProviders
import com.owncloud.android.lib.common.utils.Log_OC

class GetSearchProvidersTask(
    private val client: NextcloudClient
) : () -> GetSearchProvidersTask.Result {

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
