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

import android.content.Context
import android.content.res.Resources
import androidx.annotation.VisibleForTesting
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.nextcloud.client.account.CurrentAccountProvider
import com.nextcloud.client.core.AsyncRunner
import com.nextcloud.client.network.ClientFactory
import com.owncloud.android.R
import com.owncloud.android.datamodel.FileDataStorageManager
import com.owncloud.android.lib.common.SearchResult
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.ui.asynctasks.GetRemoteFileTask
import javax.inject.Inject

@Suppress("LongParameterList")
class UnifiedSearchViewModel() : ViewModel() {
    companion object {
        private const val TAG = "UnifiedSearchViewModel"
        private const val DEFAULT_LIMIT = 5
        private const val FILES_PROVIDER_ID = "files"
    }

    private data class UnifiedSearchMetadata(
        var results: MutableList<SearchResult> = mutableListOf()
    ) {
        fun nextCursor(): Int? = results.lastOrNull()?.cursor?.toInt()
        fun name(): String? = results.lastOrNull()?.name
        fun isFinished(): Boolean {
            if (results.isEmpty()) {
                return false
            }
            val lastResult = results.last()
            return when {
                !lastResult.isPaginated -> true
                lastResult.entries.size < DEFAULT_LIMIT -> true
                else -> false
            }
        }
    }

    lateinit var currentAccountProvider: CurrentAccountProvider
    lateinit var runner: AsyncRunner
    lateinit var clientFactory: ClientFactory
    lateinit var resources: Resources
    lateinit var context: Context

    private lateinit var repository: IUnifiedSearchRepository
    private var loadingStarted: Boolean = false
    private var metaResults: MutableMap<ProviderID, UnifiedSearchMetadata> = mutableMapOf()

    val isLoading: MutableLiveData<Boolean> = MutableLiveData<Boolean>(false)
    val searchResults = MutableLiveData<List<UnifiedSearchSection>>(mutableListOf())
    val error: MutableLiveData<String> = MutableLiveData<String>("")
    val query: MutableLiveData<String> = MutableLiveData()

    @Inject
    constructor(
        currentAccountProvider: CurrentAccountProvider,
        runner: AsyncRunner,
        clientFactory: ClientFactory,
        resources: Resources,
        context: Context
    ) : this() {
        this.currentAccountProvider = currentAccountProvider
        this.runner = runner
        this.clientFactory = clientFactory
        this.resources = resources
        this.context = context

        repository = UnifiedSearchRemoteRepository(
            clientFactory,
            currentAccountProvider,
            runner
        )
    }

    open fun refresh() {
        searchResults.value = mutableListOf()
        startLoading(query.value.orEmpty())
    }

    open fun startLoading(query: String) {
        if (!loadingStarted) {
            loadingStarted = true
            this.query.value = query
            queryAll()
        }
    }

    fun queryAll() {
        val queryTerm = query.value.orEmpty()

        if (isLoading.value != true && queryTerm.isNotBlank()) {
            isLoading.value = true
            repository.queryAll(queryTerm, this::onSearchResult, this::onError, this::onSearchFinished)
        }
    }

    open fun loadMore(provider: ProviderID) {
        val queryTerm = query.value.orEmpty()

        if (isLoading.value != true && queryTerm.isNotBlank()) {
            metaResults[provider]?.nextCursor()?.let { cursor ->
                isLoading.value = true
                repository.queryProvider(
                    queryTerm,
                    provider,
                    cursor,
                    this::onSearchResult,
                    this::onError,
                    this::onSearchFinished
                )
            }
        }
    }

    fun openFile(fileUrl: String) {
        if (isLoading.value == false) {
            isLoading.value = true
            val user = currentAccountProvider.user
            val task = GetRemoteFileTask(
                context,
                fileUrl,
                clientFactory.create(currentAccountProvider.user),
                FileDataStorageManager(user.toPlatformAccount(), context.contentResolver),
                user
            )
            runner.postQuickTask(task, onResult = this::onFileRequestResult)
        }
    }

    open fun clearError() {
        error.value = ""
    }

    fun onError(error: Throwable) {
        Log_OC.e(TAG, "Error: " + error.stackTrace)
    }

    @Synchronized
    fun onSearchResult(result: UnifiedSearchResult) {
        isLoading.value = false

        if (result.success) {
            val providerMeta = metaResults[result.provider] ?: UnifiedSearchMetadata()
            providerMeta.results.add(result.result)

            metaResults[result.provider] = providerMeta
            genSearchResultsFromMeta()
        }

        Log_OC.d(TAG, "onSearchResult: Provider '${result.provider}', success: ${result.success}")
        if (result.success) {
            Log_OC.d(TAG, "onSearchResult: Provider '${result.provider}', result count: ${result.result.entries.size}")
        }
    }

    private fun genSearchResultsFromMeta() {
        searchResults.value = metaResults
            .filter { it.value.results.isNotEmpty() }
            .map { (key, value) ->
                UnifiedSearchSection(
                    providerID = key,
                    name = value.name()!!,
                    entries = value.results.flatMap { it.entries },
                    hasMoreResults = !value.isFinished()
                )
            }
            .sortedWith { o1, o2 ->
                // TODO sort with sort order from server providers?
                when {
                    o1.providerID == FILES_PROVIDER_ID -> -1
                    o2.providerID == FILES_PROVIDER_ID -> 1
                    else -> 0
                }
            }
    }

    fun onSearchFinished(success: Boolean) {
        Log_OC.d(TAG, "onSearchFinished: success: $success")
        isLoading.value = false
        if (!success) {
            error.value = resources.getString(R.string.search_error)
        }
    }

    @VisibleForTesting
    fun setRepository(repository: IUnifiedSearchRepository) {
        this.repository = repository
    }

    private fun onFileRequestResult(result: GetRemoteFileTask.Result) {
        isLoading.value = false
        if (result.success) {
            // unifiedSearchFragment.showFile(result.file)
            // (file as MutableLiveData).value = result.file
        } else {
            error.value = "Error showing search result"
        }
    }
}
