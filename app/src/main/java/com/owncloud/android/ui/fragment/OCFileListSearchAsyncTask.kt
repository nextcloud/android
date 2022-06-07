/*
 * Nextcloud Android client application
 *
 * @author Álvaro Brey Vilas
 * Copyright (C) 2022 Álvaro Brey Vilas
 * Copyright (C) 2022 Nextcloud GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */
package com.owncloud.android.ui.fragment

import android.os.AsyncTask
import android.os.Handler
import android.os.Looper
import com.nextcloud.client.account.User
import com.owncloud.android.datamodel.FileDataStorageManager
import com.owncloud.android.lib.common.operations.RemoteOperation
import com.owncloud.android.lib.common.operations.RemoteOperationResult
import com.owncloud.android.ui.events.SearchEvent
import java.lang.ref.WeakReference

class OCFileListSearchAsyncTask(
    containerActivity: FileFragment.ContainerActivity,
    fragment: OCFileListFragment,
    private val remoteOperation: RemoteOperation<List<Any>>,
    private val currentUser: User,
    private val event: SearchEvent
) : AsyncTask<Void, Void, Boolean>() {
    private val activityReference: WeakReference<FileFragment.ContainerActivity> = WeakReference(containerActivity)
    private val fragmentReference: WeakReference<OCFileListFragment> = WeakReference(fragment)

    private val fileDataStorageManager: FileDataStorageManager?
        get() = activityReference.get()?.storageManager

    private fun RemoteOperationResult<out Any>.hasSuccessfulResult() = this.isSuccess && this.resultData != null

    override fun onPreExecute() {
        fragmentReference.get()?.let { fragment ->
            Handler(Looper.getMainLooper()).post {
                fragment.isLoading = true
                fragment.setEmptyListLoadingMessage()
            }
        }
    }

    override fun doInBackground(vararg voids: Void): Boolean {
        val fragment = fragmentReference.get()
        if (fragment?.context == null || isCancelled) {
            return java.lang.Boolean.FALSE
        }

        fragment.setTitle()
        val remoteOperationResult = remoteOperation.execute(currentUser, fragment.context)
        if (remoteOperationResult.hasSuccessfulResult() && !isCancelled && fragment.searchFragment) {
            fragment.searchEvent = event
            if (remoteOperationResult.resultData.isNullOrEmpty()) {
                fragment.setEmptyView(event)
            } else {
                fragment.adapter.setData(
                    remoteOperationResult.resultData,
                    fragment.currentSearchType,
                    fileDataStorageManager,
                    fragment.mFile,
                    true
                )
            }
        }
        return remoteOperationResult.isSuccess
    }

    override fun onPostExecute(bool: Boolean) {
        fragmentReference.get()?.let { fragment ->
            fragment.isLoading = false
            if (!isCancelled) {
                fragment.adapter.notifyDataSetChanged()
            }
        }
    }
}
