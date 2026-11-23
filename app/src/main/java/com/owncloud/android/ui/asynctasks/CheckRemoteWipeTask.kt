/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.owncloud.android.ui.asynctasks

import android.accounts.Account
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.WorkerThread
import androidx.lifecycle.lifecycleScope
import com.nextcloud.client.jobs.BackgroundJobManager
import com.nextcloud.utils.extensions.isActive
import com.nextcloud.utils.SnackbarUtil
import com.owncloud.android.R
import com.owncloud.android.lib.common.accounts.AccountUtils
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.ui.activity.FileActivity
import com.owncloud.android.ui.helpers.CredentialsUpdateHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CheckRemoteWipeTask(private val fileActivity: FileActivity) {
    companion object {
        private const val TAG = "CheckRemoteWipeTask"
    }

    private val credentialsUpdateHelper = CredentialsUpdateHelper(fileActivity)

    private val credentialsUpdateLauncher =
        fileActivity.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { }

    fun execute(account: Account, backgroundJobManager: BackgroundJobManager): Job =
        fileActivity.lifecycleScope.launch {
            val isWipeRequested = withContext(Dispatchers.IO + NonCancellable) {
                startAccountRemovalIfWipeRequested(account, backgroundJobManager)
            }

            if (isWipeRequested || !fileActivity.isActive()) {
                return@launch
            }

            performCredentialsUpdate(account)
        }

    private fun performCredentialsUpdate(account: Account) {
        try {
            credentialsUpdateHelper.invalidateCredentials(account)
            credentialsUpdateLauncher.launch(credentialsUpdateHelper.createUpdateCredentialsIntent(account))
        } catch (_: AccountUtils.AccountNotFoundException) {
            SnackbarUtil.show(fileActivity, R.string.auth_account_does_not_exist)
        }
    }

    @WorkerThread
    private fun startAccountRemovalIfWipeRequested(
        account: Account,
        backgroundJobManager: BackgroundJobManager
    ): Boolean {
        if (!credentialsUpdateHelper.isRemoteWipeRequested(account)) {
            Log_OC.e(TAG, "Check for remote wipe not needed -> update credentials")
            return false
        }

        backgroundJobManager.startAccountRemovalJob(account.name, true)
        return true
    }
}
