/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.owncloud.android.ui.asynctasks

import android.accounts.Account
import androidx.annotation.WorkerThread
import androidx.lifecycle.lifecycleScope
import com.nextcloud.client.jobs.BackgroundJobManager
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.ui.activity.FileActivity
import com.owncloud.android.ui.helpers.CredentialsUpdateHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CheckRemoteWipeTask(
    private val backgroundJobManager: BackgroundJobManager,
    private val account: Account,
    private val fileActivity: FileActivity
) {
    companion object {
        private const val TAG = "CheckRemoteWipeTask"
    }

    fun execute(): Job = fileActivity.lifecycleScope.launch {
        val isWipeRequested = withContext(Dispatchers.IO + NonCancellable) {
            startAccountRemovalIfWipeRequested()
        }

        if (isWipeRequested || fileActivity.isFinishing || fileActivity.isDestroyed) {
            return@launch
        }

        fileActivity.performCredentialsUpdate(account)
    }

    @WorkerThread
    private fun startAccountRemovalIfWipeRequested(): Boolean {
        if (!CredentialsUpdateHelper(fileActivity).isRemoteWipeRequested(account)) {
            Log_OC.e(TAG, "Check for remote wipe not needed -> update credentials")
            return false
        }

        backgroundJobManager.startAccountRemovalJob(account.name, true)
        return true
    }
}
