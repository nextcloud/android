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
package com.owncloud.android.ui.fragment

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.lib.resources.activities.model.Activity

class UnifiedSearchTestViewModel : ViewModel() {

    val activities: MutableLiveData<List<Activity>> = MutableLiveData<List<Activity>>(emptyList())
    val error: MutableLiveData<String> = MutableLiveData<String>("")
    val query: MutableLiveData<String> = MutableLiveData()
    private var loadingStarted: Boolean = false
    private var last: Int = -1

    fun refresh() {
        last = -1
        activities.value = emptyList()
        loadMore()
    }

    fun startLoading(query: String) {
        if (!loadingStarted) {
            loadingStarted = true
            this.query.value = query
            loadMore()
        }
    }

    fun loadMore() {
        Log_OC.d(this, "loadMore")
//        if (isLoading.value != true) {
//            val client = clientFactory.createNextcloudClient(currentUser.user)
//            val task = SearchOnProviderTask("test", "files", client)
//            runner.postQuickTask(task, onResult = this::onSearchResult, onError = this::onError)
//            isLoading.value = true
//        }
    }

//    fun openFile(fileUrl: String) {
//        if (isLoading.value == false) {
//            (isLoading as MutableLiveData).value = true
//            val user = currentUser.user
//            val task = GetRemoteFileTask(
//                context,
//                fileUrl,
//                clientFactory.create(currentUser.user),
//                FileDataStorageManager(user.toPlatformAccount(), contentResolver),
//                user
//            )
//            runner.postQuickTask(task, onResult = this::onFileRequestResult)
//        }
//    }

    fun clearError() {
        error.value = ""
    }

    private fun onError(error: Throwable) {
        Log_OC.d("Unified Search", "Error: " + error.stackTrace)
    }

//    private fun onSearchResult(result: SearchOnProviderTask.Result) {
//        isLoading.value = false
//
//        if (result.success) {
//           // activities.value = result.searchResult.entries
//        } else {
//            error.value = resources.getString(R.string.search_error)
//        }
//
//        Log_OC.d("Unified Search", "Success: " + result.success)
//        Log_OC.d("Unified Search", "Size: " + result.searchResult.entries.size)
//    }

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

//    private fun onFileRequestResult(result: GetRemoteFileTask.Result) {
//        (isLoading as MutableLiveData).value = false
//        if (result.success) {
//            (file as MutableLiveData).value = result.file
//        } else {
//            (file as MutableLiveData).value = null
//        }
//    }
}
