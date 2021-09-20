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
    lateinit var currentAccountProvider: CurrentAccountProvider
    lateinit var runner: AsyncRunner
    lateinit var clientFactory: ClientFactory
    lateinit var resources: Resources
    lateinit var context: Context

    private lateinit var repository: IUnifiedSearchRepository
    private var loadingStarted: Boolean = false
    private var last: Int = -1

    val isLoading: MutableLiveData<Boolean> = MutableLiveData<Boolean>(false)
    val searchResults = MutableLiveData<MutableMap<ProviderID, MutableList<SearchResult>>>(mutableMapOf())
    val error: MutableLiveData<String> = MutableLiveData<String>("")
    val query: MutableLiveData<String> = MutableLiveData()

    companion object {
        private const val TAG = "UnifiedSearchViewModel"
    }

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
        last = -1
        searchResults.value = mutableMapOf()
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
            isLoading.value = true
            val providerResults = searchResults.value?.get(provider)
            val cursor = providerResults?.filter { it.cursor != null }?.maxOfOrNull { it.cursor!!.toInt() }
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
        Log_OC.d(TAG, "Error: " + error.stackTrace)
    }

    @Synchronized
    fun onSearchResult(result: UnifiedSearchResult) {
        isLoading.value = false

        if (result.success) {
            val currentValues: MutableMap<ProviderID, MutableList<SearchResult>> = searchResults.value ?: mutableMapOf()
            val providerValues = currentValues[result.provider] ?: mutableListOf()
            providerValues.add(result.result)
            currentValues.put(result.provider, providerValues)
            searchResults.value = currentValues
        }

        Log_OC.d(TAG, "onSearchResult: Provider '${result.provider}', success: ${result.success}")
        if (result.success) {
            Log_OC.d(TAG, "onSearchResult: Provider '${result.provider}', result count: ${result.result.entries.size}")
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

//    private fun onActivitiesRequestResult(result: GetActivityListTask.Result) {
//        isLoading.value = false
//        if (result.success) {
//            val existingActivities = activities.value ?: emptyList()
//            val newActivities = listOf(existingActivities, result.activities).flatten()
//            last = result.last
//            activities.value = newActivities
//        } else {
//            error.value = resources.getString(R.string.activities_error)
//        }
//    }

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
