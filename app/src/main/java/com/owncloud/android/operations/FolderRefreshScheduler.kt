/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.owncloud.android.operations

import androidx.lifecycle.lifecycleScope
import com.owncloud.android.lib.common.operations.RemoteOperationResult
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.lib.resources.files.CheckEtagRemoteOperation
import com.owncloud.android.ui.activity.FileDisplayActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FolderRefreshScheduler(private val activity: FileDisplayActivity) {
    companion object {
        private const val ETAG_POLL_INTERVAL_MS = 30_000L
        private const val TAG = "FolderRefreshScheduler"
    }

    private var job: Job? = null

    fun start() {
        stop()

        job = activity.lifecycleScope.launch {
            while (true) {
                delay(ETAG_POLL_INTERVAL_MS)
                checkAndRefreshIfETagChanged()
            }
        }

        Log_OC.d(TAG, "ETag polling started (interval=${ETAG_POLL_INTERVAL_MS}ms)")
    }

    fun stop() {
        job?.cancel()
        job = null
        Log_OC.d(TAG, "ETag polling stopped")
    }

    @Suppress("ReturnCount", "TooGenericExceptionCaught")
    private suspend fun checkAndRefreshIfETagChanged() {
        if (activity.isFinishing || activity.isSearchOpen()) return

        val fragment = activity.listOfFilesFragment ?: return
        if (fragment.isSearchFragment) return

        val currentDir = activity.getCurrentDir() ?: return
        val currentUser = activity.user.orElse(null) ?: return

        val localEtag = currentDir.etag ?: ""

        Log_OC.d(TAG, "eTag poll → checking '${currentDir.remotePath}' (local eTag='$localEtag')")

        val result = withContext(Dispatchers.IO) {
            try {
                CheckEtagRemoteOperation(currentDir.remotePath, localEtag).execute(currentUser, activity)
            } catch (e: Exception) {
                Log_OC.e(TAG, "eTag poll: could not create client — ${e.message}")
                null
            }
        } ?: return

        when (result.code) {
            RemoteOperationResult.ResultCode.ETAG_CHANGED -> {
                Log_OC.i(TAG, "eTag poll → eTag changed for '${currentDir.remotePath}', triggering sync")
                activity.startSyncFolderOperation(currentDir, ignoreETag = true)
            }

            RemoteOperationResult.ResultCode.ETAG_UNCHANGED -> {
                Log_OC.d(TAG, "eTag poll → no change for '${currentDir.remotePath}'")
            }

            RemoteOperationResult.ResultCode.FILE_NOT_FOUND -> {
                Log_OC.w(TAG, "eTag poll → directory not found on server, refreshing to handle deletion")
                activity.startSyncFolderOperation(currentDir, ignoreETag = true)
            }

            else -> {
                Log_OC.w(TAG, "eTag poll → unexpected result code: ${result.code}")
            }
        }
    }
}
