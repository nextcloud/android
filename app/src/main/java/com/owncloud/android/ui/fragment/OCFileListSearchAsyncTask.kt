/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2022 Álvaro Brey <alvaro@alvarobrey.com>
 * SPDX-FileCopyrightText: 2022 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.owncloud.android.ui.fragment

import android.annotation.SuppressLint
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
                fragment.setEmptyListMessage(EmptyListState.LOADING)
            }
        }
    }

    override fun doInBackground(vararg voids: Void): Boolean {
        val fragment = fragmentReference.get()
        if (fragment?.context == null || isCancelled) {
            return java.lang.Boolean.FALSE
        }

        fragment.setTitle()
        lateinit var remoteOperationResult: RemoteOperationResult<List<Any>>
        try {
            remoteOperationResult = remoteOperation.execute(currentUser, fragment.context)
        } catch (_: UnsupportedOperationException) {
            remoteOperationResult = remoteOperation.executeNextcloudClient(currentUser, fragment.requireContext())
        }

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

    @SuppressLint("NotifyDataSetChanged")
    override fun onPostExecute(bool: Boolean) {
        fragmentReference.get()?.let { fragment ->
            if (!isCancelled) {
                fragment.adapter.notifyDataSetChanged()
            }
        }
    }
}
