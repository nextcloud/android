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

import android.app.Application
import android.content.Context
import android.content.res.Resources
import android.net.Uri
import androidx.annotation.VisibleForTesting
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import com.nextcloud.client.account.CurrentAccountProvider
import com.nextcloud.client.core.AsyncRunner
import com.nextcloud.client.network.ClientFactory
import com.nextcloud.client.network.ConnectivityService
import com.owncloud.android.R
import com.owncloud.android.datamodel.FileDataStorageManager
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.lib.common.SearchResult
import com.owncloud.android.lib.common.SearchResultEntry
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.ui.asynctasks.GetRemoteFileTask
import javax.inject.Inject

@Suppress("LongParameterList")
class UnifiedSearchViewModel(application: Application) : AndroidViewModel(application), IUnifiedSearchViewModel {
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

    private lateinit var currentAccountProvider: CurrentAccountProvider
    private lateinit var runner: AsyncRunner
    private lateinit var clientFactory: ClientFactory
    private lateinit var resources: Resources
    private lateinit var connectivityService: ConnectivityService

    private val context: Context
        get() = getApplication<Application>().applicationContext

    private lateinit var repository: IUnifiedSearchRepository
    private var loadingStarted: Boolean = false
    private var results: MutableMap<ProviderID, UnifiedSearchMetadata> = mutableMapOf()

    override val isLoading = MutableLiveData(false)
    override val searchResults = MutableLiveData<List<UnifiedSearchSection>>(mutableListOf())
    override val error = MutableLiveData("")
    override val query = MutableLiveData<String>()
    override val browserUri = MutableLiveData<Uri>()
    override val file = MutableLiveData<OCFile>()

    @Inject
    constructor(
        application: Application,
        currentAccountProvider: CurrentAccountProvider,
        runner: AsyncRunner,
        clientFactory: ClientFactory,
        resources: Resources,
        connectivityService: ConnectivityService
    ) : this(application) {
        this.currentAccountProvider = currentAccountProvider
        this.runner = runner
        this.clientFactory = clientFactory
        this.resources = resources
        this.connectivityService = connectivityService

        repository = UnifiedSearchRemoteRepository(
            clientFactory,
            currentAccountProvider,
            runner
        )
    }

    open fun startLoading(query: String) {
        if (!loadingStarted) {
            loadingStarted = true
            this.query.value = query
            initialQuery()
        }
    }

    /**
     * Clears data and queries all available providers
     */
    override fun initialQuery() {
        doWithConnectivityCheck {
            results = mutableMapOf()
            searchResults.value = mutableListOf()
            val queryTerm = query.value.orEmpty()

            if (isLoading.value != true && queryTerm.isNotBlank()) {
                isLoading.value = true
                repository.queryAll(queryTerm, this::onSearchResult, this::onError, this::onSearchFinished)
            }
        }
    }

    override fun loadMore(provider: ProviderID) {
        doWithConnectivityCheck {
            val queryTerm = query.value.orEmpty()

            if (isLoading.value != true && queryTerm.isNotBlank()) {
                results[provider]?.nextCursor()?.let { cursor ->
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
    }

    private fun doWithConnectivityCheck(block: () -> Unit) {
        when (connectivityService.connectivity.isConnected) {
            false -> {
                error.value = resources.getString(R.string.offline_mode)
                if (isLoading.value == true) {
                    isLoading.value = false
                }
            }
            else -> block()
        }
    }

    override fun openResult(result: SearchResultEntry) {
        if (result.isFile) {
            openFile(result.remotePath())
        } else {
            this.browserUri.value = getResultUri(result)
        }
    }

    private fun getResultUri(result: SearchResultEntry): Uri {
        val uri = Uri.parse(result.resourceUrl)
        return when (uri.host) {
            null -> {
                val serverUrl = currentAccountProvider.user.server.uri.toString()
                val fullUrl = serverUrl + result.resourceUrl
                Uri.parse(fullUrl)
            }
            else -> uri
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
                FileDataStorageManager(user, context.contentResolver),
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

        if (result.success) {
            val providerMeta = results[result.provider] ?: UnifiedSearchMetadata()
            providerMeta.results.add(result.result)

            results[result.provider] = providerMeta
            genSearchResultsFromMeta()
        }

        Log_OC.d(TAG, "onSearchResult: Provider '${result.provider}', success: ${result.success}")
        if (result.success) {
            Log_OC.d(TAG, "onSearchResult: Provider '${result.provider}', result count: ${result.result.entries.size}")
        }
    }

    private fun genSearchResultsFromMeta() {
        searchResults.value = results
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

    private fun onSearchFinished(success: Boolean) {
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
            file.value = result.file
        } else {
            error.value = "Error showing search result"
        }
    }

    override fun setQuery(query: String) {
        this.query.value = query
    }

    @VisibleForTesting
    fun setConnectivityService(connectivityService: ConnectivityService) {
        this.connectivityService = connectivityService
    }
}
