/*
 * Nextcloud Android client application
 *
 * @author Tobias Kaminsky
 * Copyright (C) 2020 Tobias Kaminsky
 * Copyright (C) 2020 Nextcloud GmbH
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
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package com.owncloud.android.ui.unifiedsearch

import com.nextcloud.client.account.CurrentAccountProvider
import com.nextcloud.client.core.AsyncRunner
import com.nextcloud.client.network.ClientFactory
import com.owncloud.android.lib.common.SearchProviders
import com.owncloud.android.lib.common.utils.Log_OC

class UnifiedSearchRemoteRepository(
    private val clientFactory: ClientFactory,
    private val currentAccountProvider: CurrentAccountProvider,
    private val asyncRunner: AsyncRunner
) : IUnifiedSearchRepository {

    private var providers: SearchProviders? = null

    override fun refresh() {
        TODO("Not yet implemented")
    }

    override fun startLoading() {
        TODO("Not yet implemented")
    }

    override fun loadMore(
        query: String,
        onResult: (UnifiedSearchResults) -> Unit,
        onError: (Throwable) -> Unit
    ) {
        Log_OC.d(this, "loadMore")
        fetchProviders(
            onResult = { result ->
                val providerIds = result.providers.map { it.id }
                val client = clientFactory.createNextcloudClient(currentAccountProvider.user)
                val task = SearchOnProvidersTask(query, providerIds, client)
                asyncRunner.postQuickTask(
                    task = task,
                    onResult = {
                        onResult(UnifiedSearchResults(it.success, it.searchResults))
                    },
                    onError = onError
                )
            },
            onError = onError
        )
    }

    private fun fetchProviders(onResult: (SearchProviders) -> Unit, onError: (Throwable) -> Unit) {
        Log_OC.d(this, "fetchProviders")
        if (this.providers != null) {
            onResult(this.providers!!)
        } else {
            val client = clientFactory.createNextcloudClient(currentAccountProvider.user)
            val task = GetSearchProvidersTask(client)
            asyncRunner.postQuickTask(
                task,
                onResult = { onResult(it.providers) },
                onError = onError
            )
        }
    }
}
