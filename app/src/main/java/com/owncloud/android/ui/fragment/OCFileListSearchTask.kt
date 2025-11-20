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
import com.owncloud.android.R
import com.owncloud.android.datamodel.FileDataStorageManager
import com.owncloud.android.lib.common.operations.RemoteOperation
import com.owncloud.android.lib.common.operations.RemoteOperationResult
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.lib.resources.files.SearchRemoteOperation
import com.owncloud.android.ui.events.SearchEvent
import com.owncloud.android.utils.DisplayUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.lang.ref.WeakReference

@Suppress("LongParameterList")
@SuppressLint("NotifyDataSetChanged")
class OCFileListSearchTask(
    containerActivity: FileFragment.ContainerActivity,
    fragment: OCFileListFragment,
    private val remoteOperation: RemoteOperation<List<Any>>,
    private val currentUser: User,
    private val event: SearchEvent,
    private val taskTimeout: Long
) {
    companion object {
        private const val TAG = "OCFileListSearchTask"
    }

    private val activityReference: WeakReference<FileFragment.ContainerActivity> = WeakReference(containerActivity)
    private val fragmentReference: WeakReference<OCFileListFragment> = WeakReference(fragment)

    private val fileDataStorageManager: FileDataStorageManager?
        get() = activityReference.get()?.storageManager

    private var job: Job? = null

    @Suppress("TooGenericExceptionCaught", "DEPRECATION", "ReturnCount")
    fun execute() {
        Log_OC.d(TAG, "search task running, query: ${event.searchType}")
        val fragment = fragmentReference.get() ?: return
        val context = fragment.context ?: return

        job = fragment.lifecycleScope.launch(Dispatchers.IO) {
            val filesInDb = when (event.searchType) {
                SearchRemoteOperation.SearchType.SHARED_FILTER -> {
                    fileDataStorageManager?.fileDao?.getSharedFiles(currentUser.accountName)
                }
                SearchRemoteOperation.SearchType.FAVORITE_SEARCH -> {
                    fileDataStorageManager?.fileDao?.getFavoriteFiles(currentUser.accountName)
                }
                else -> {
                    null
                }
            }?.map { fileDataStorageManager?.createFileInstance(it) }

            withContext(Dispatchers.Main) {
                if (fragment.isAdded && fragment.searchFragment) {
                    fragment.adapter.setData(
                        filesInDb,
                        fragment.currentSearchType,
                        fileDataStorageManager,
                        fragment.mFile,
                        false
                    )
                }
            }

            val result: RemoteOperationResult<List<Any>>? = try {
                withTimeoutOrNull(taskTimeout) {
                    remoteOperation.execute(currentUser, context)
                } ?: remoteOperation.executeNextcloudClient(currentUser, context)
            } catch (e: Exception) {
                Log_OC.e(TAG, "exception execute: ", e)
                null
            }

            withContext(Dispatchers.Main) {
                if (!fragment.isAdded || !fragment.searchFragment) {
                    Log_OC.e(TAG, "cannot search, fragment is not ready")
                    return@withContext
                }

                if (result?.isSuccess == true) {
                    if (result.resultData.isEmpty()) {
                        fragment.setEmptyListMessage(SearchType.NO_SEARCH)
                        return@withContext
                    }

                    fragment.searchEvent = event
                    fragment.adapter.setData(
                        result.resultData,
                        fragment.currentSearchType,
                        fileDataStorageManager,
                        fragment.mFile,
                        true
                    )
                    return@withContext
                }

                fragment.activity?.let {
                    DisplayUtils.showSnackMessage(it, R.string.error_fetching_sharees)
                }
            }
        }
    }

    fun cancel() = job?.cancel(null)

    fun isFinished(): Boolean = job?.isCompleted == true
}
