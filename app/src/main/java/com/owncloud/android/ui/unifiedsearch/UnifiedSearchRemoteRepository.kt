/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2020 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2020 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.ui.unifiedsearch

import com.nextcloud.client.account.CurrentAccountProvider
import com.nextcloud.client.core.AsyncRunner
import com.nextcloud.client.network.ClientFactory
import com.nextcloud.common.NextcloudClient
import com.owncloud.android.lib.common.SearchProviders
import com.owncloud.android.lib.common.utils.Log_OC
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class UnifiedSearchRemoteRepository(
    private val clientFactory: ClientFactory,
    private val currentAccountProvider: CurrentAccountProvider,
    private val asyncRunner: AsyncRunner
) : IUnifiedSearchRepository {

    private var providers: SearchProviders? = null
    private val tag = "UnifiedSearchRemoteRepository"

    private fun runAsyncWithNcClient(callback: (client: NextcloudClient) -> Unit) {
        val coroutineExceptionHandler = CoroutineExceptionHandler { _, exception ->
            Log_OC.d(tag, "CoroutineExceptionHandler got at runAsyncWithNcClient $exception")
        }

        CoroutineScope(Dispatchers.IO).launch(coroutineExceptionHandler) {
            val client = clientFactory.createNextcloudClient(currentAccountProvider.user)
            callback(client)
        }
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
                runAsyncWithNcClient { client ->
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
        runAsyncWithNcClient { client ->
            val task = SearchOnProviderTask(query, provider, client, cursor)
            asyncRunner.postQuickTask(
                task,
                onResult = {
                    onResult(UnifiedSearchResult(provider, it.success, it.searchResult))
                    onFinished(it.success)
                },
                onError
            )
        }
    }

    fun fetchProviders(onResult: (SearchProviders) -> Unit, onError: (Throwable) -> Unit) {
        Log_OC.d(this, "fetchProviders")
        if (this.providers != null) {
            onResult(this.providers!!)
        } else {
            runAsyncWithNcClient { client ->
                val task = GetSearchProvidersTask(client)
                asyncRunner.postQuickTask(
                    task,
                    onResult = { onResult(it.providers) },
                    onError = onError
                )
            }
        }
    }
}
