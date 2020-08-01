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
package com.owncloud.android.ui.activities

import android.content.ContentResolver
import android.content.Context
import android.content.res.Resources
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.nextcloud.client.account.CurrentAccountProvider
import com.nextcloud.client.core.AsyncRunner
import com.nextcloud.client.core.SingleLiveEvent
import com.nextcloud.client.network.ClientFactory
import com.owncloud.android.R
import com.owncloud.android.datamodel.FileDataStorageManager
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.lib.resources.activities.model.Activity
import javax.inject.Inject

@Suppress("LongParameterList") // legacy code had large dependencies
class ActivitiesViewModel @Inject constructor(
    private val context: Context,
    private val contentResolver: ContentResolver,
    private val currentUser: CurrentAccountProvider,
    private val runner: AsyncRunner,
    private val clientFactory: ClientFactory,
    private val resources: Resources
) : ViewModel() {

    val isLoading: LiveData<Boolean> = MutableLiveData<Boolean>(false)
    val activities: LiveData<List<Activity>> = MutableLiveData<List<Activity>>(emptyList())
    val error: LiveData<String> = MutableLiveData<String>("")
    val file: LiveData<OCFile> = SingleLiveEvent<OCFile>()

    private var loadingStarted: Boolean = false
    private var last: Int = -1

    fun refresh() {
        last = -1
        (activities as MutableLiveData).value = emptyList()
        loadMore()
    }

    fun startLoading() {
        if (!loadingStarted) {
            loadingStarted = true
            loadMore()
        }
    }

    fun loadMore() {
        if (isLoading.value != true) {
            val client = clientFactory.create(currentUser.user)
            val task = GetActivityListTask(last, client)
            runner.postQuickTask(task, onResult = this::onActivitiesRequestResult)
            (isLoading as MutableLiveData).value = true
        }
    }

    fun openFile(fileUrl: String) {
        if (isLoading.value == false) {
            (isLoading as MutableLiveData).value = true
            val user = currentUser.user
            val task = GetRemoteFileTask(
                context,
                fileUrl,
                clientFactory.create(currentUser.user),
                FileDataStorageManager(user.toPlatformAccount(), contentResolver),
                user
            )
            runner.postQuickTask(task, onResult = this::onFileRequestResult)
        }
    }

    fun clearError() {
        (error as MutableLiveData).value = ""
    }

    private fun onActivitiesRequestResult(result: GetActivityListTask.Result) {
        (isLoading as MutableLiveData).value = false
        if (result.success) {
            val existingActivities = activities.value ?: emptyList()
            val newActivities = listOf(existingActivities, result.activities).flatten()
            last = result.last
            (activities as MutableLiveData).value = newActivities
        } else {
            (error as MutableLiveData).value = resources.getString(R.string.activities_error)
        }
    }

    private fun onFileRequestResult(result: GetRemoteFileTask.Result) {
        (isLoading as MutableLiveData).value = false
        if (result.success) {
            (file as MutableLiveData).value = result.file
        } else {
            (file as MutableLiveData).value = null
        }
    }
}
