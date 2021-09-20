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

    override fun queryAll(
        query: String,
        onResult: (UnifiedSearchResult) -> Unit,
        onError: (Throwable) -> Unit,
        onFinished: (Boolean) -> Unit
    ) {
        Log_OC.d(this, "queryAll")
        fetchProviders(
            onResult = { result ->
                val providerIds = result.providers.map { it.id }
                var openRequests = providerIds.size
                var anyError = false
                val client = clientFactory.createNextcloudClient(currentAccountProvider.user)
                providerIds
                    .forEach { provider ->
                        val task = SearchOnProviderTask(query, provider, client)
                        asyncRunner.postQuickTask(
                            task = task,
                            onResult = {
                                openRequests--
                                anyError = anyError || !it.success
                                onResult(UnifiedSearchResult(provider, it.success, it.searchResult))
                                if (openRequests == 0) {
                                    onFinished(!anyError)
                                }
                            },
                            onError = {
                                openRequests--
                                anyError = true
                                onError(it)
                                if (openRequests == 0) {
                                    onFinished(!anyError)
                                }
                            }
                        )
                    }
            },
            onError = onError
        )
    }

    override fun queryProvider(
        query: String,
        provider: ProviderID,
        cursor: Int?,
        onResult: (UnifiedSearchResult) -> Unit,
        onError: (Throwable) -> Unit,
        onFinished: (Boolean) -> Unit
    ) {
        Log_OC.d(
            this,
            "queryProvider() called with: query = $query, provider = $provider, cursor = $cursor"
        )
        val client = clientFactory.createNextcloudClient(currentAccountProvider.user)
        val task = SearchOnProviderTask(query, provider, client, cursor)
        asyncRunner.postQuickTask(
            task,
            onResult = {
                onResult(UnifiedSearchResult(provider, it.success, it.searchResult))
                onFinished(!it.success)
            },
            onError
        )
    }

    fun fetchProviders(onResult: (SearchProviders) -> Unit, onError: (Throwable) -> Unit) {
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
