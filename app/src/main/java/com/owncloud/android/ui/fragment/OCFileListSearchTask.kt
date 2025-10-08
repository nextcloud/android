/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-FileCopyrightText: 2022 Álvaro Brey <alvaro@alvarobrey.com>
 * SPDX-FileCopyrightText: 2022 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.owncloud.android.ui.fragment

import android.annotation.SuppressLint
import androidx.lifecycle.lifecycleScope
import com.nextcloud.client.account.User
import com.owncloud.android.datamodel.FileDataStorageManager
import com.owncloud.android.lib.common.operations.RemoteOperation
import com.owncloud.android.lib.common.operations.RemoteOperationResult
import com.owncloud.android.ui.events.SearchEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.lang.ref.WeakReference

@SuppressLint("NotifyDataSetChanged", "LongParameterList")
class OCFileListSearchTask(
    containerActivity: FileFragment.ContainerActivity,
    fragment: OCFileListFragment,
    private val remoteOperation: RemoteOperation<List<Any>>,
    private val currentUser: User,
    private val event: SearchEvent,
    private val taskTimeout: Long
) {
    private val activityReference: WeakReference<FileFragment.ContainerActivity> = WeakReference(containerActivity)
    private val fragmentReference: WeakReference<OCFileListFragment> = WeakReference(fragment)

    private val fileDataStorageManager: FileDataStorageManager?
        get() = activityReference.get()?.storageManager

    private fun RemoteOperationResult<out Any>.hasSuccessfulResult() = this.isSuccess && this.resultData != null

    private var job: Job? = null

    @Suppress("TooGenericExceptionCaught", "DEPRECATION")
    fun execute() {
        fragmentReference.get()?.let { fragment ->
            job = fragment.lifecycleScope.launch(Dispatchers.IO) {
                val result = withTimeoutOrNull(taskTimeout) {
                    if (!isActive) {
                        false
                    } else {
                        fragment.setTitle()
                        lateinit var remoteOperationResult: RemoteOperationResult<List<Any>>
                        try {
                            remoteOperationResult = remoteOperation.execute(currentUser, fragment.requireContext())
                        } catch (_: Exception) {
                            remoteOperationResult =
                                remoteOperation.executeNextcloudClient(currentUser, fragment.requireContext())
                        }

                        if (remoteOperationResult.hasSuccessfulResult() && isActive && fragment.searchFragment) {
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
                        remoteOperationResult.isSuccess
                    }
                } ?: false

                withContext(Dispatchers.Main) {
                    if (result && isActive) {
                        fragment.adapter.notifyDataSetChanged()
                    } else {
                        fragment.setEmptyListMessage(EmptyListState.ERROR)
                    }
                }
            }
        }
    }

    fun cancel() = job?.cancel(null)

    fun isFinished(): Boolean = job?.isCompleted == true
}
