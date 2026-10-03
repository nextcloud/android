/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.owncloud.android.ui.fragment.uploadList

import android.accounts.Account
import android.content.Intent
import androidx.activity.result.ActivityResultLauncher
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.nextcloud.client.account.User
import com.nextcloud.client.jobs.BackgroundJobManager
import com.nextcloud.utils.SnackbarUtil
import com.owncloud.android.R
import com.owncloud.android.lib.common.accounts.AccountUtils
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.ui.dialog.LoadingDialog
import com.owncloud.android.ui.helpers.CredentialsUpdateHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class UploadListCredentialsHandler(
    private val fragment: Fragment,
    private val backgroundJobManager: BackgroundJobManager,
    private val credentialsUpdateLauncher: ActivityResultLauncher<Intent>,
    private val onCredentialsValid: () -> Unit
) {
    companion object {
        private const val TAG = "UploadListCredentialsHandler"
        private const val LOADING_DIALOG_TAG = "UPLOAD_LIST_LOADING_DIALOG"
    }

    fun checkCredentials(user: User) {
        val credentialsUpdateHelper = CredentialsUpdateHelper(fragment.requireContext())
        showLoadingDialog()

        fragment.lifecycleScope.launch {
            val isValid = withContext(Dispatchers.IO) {
                credentialsUpdateHelper.areCurrentCredentialsValid(user)
            }
            dismissLoadingDialog()

            if (isValid) {
                onCredentialsValid()
                return@launch
            }

            requestCredentialsUpdate(credentialsUpdateHelper, user.toPlatformAccount())
        }
    }

    private suspend fun requestCredentialsUpdate(credentialsUpdateHelper: CredentialsUpdateHelper, account: Account) {
        val isRemoteWipeRequested = withContext(Dispatchers.IO) {
            credentialsUpdateHelper.isRemoteWipeRequested(account)
        }

        if (isRemoteWipeRequested) {
            backgroundJobManager.startAccountRemovalJob(account.name, true)
            return
        }

        try {
            withContext(Dispatchers.IO) { credentialsUpdateHelper.invalidateCredentials(account) }
            credentialsUpdateLauncher.launch(credentialsUpdateHelper.createUpdateCredentialsIntent(account))
        } catch (e: AccountUtils.AccountNotFoundException) {
            Log_OC.e(TAG, "Account not found while updating credentials: $e")
            SnackbarUtil.show(fragment, R.string.auth_account_does_not_exist)
        }
    }

    private fun showLoadingDialog() {
        val fragmentManager = fragment.childFragmentManager
        if (fragmentManager.isStateSaved) {
            return
        }

        LoadingDialog.newInstance(fragment.getString(R.string.wait_checking_credentials))
            .show(fragmentManager, LOADING_DIALOG_TAG)
    }

    private fun dismissLoadingDialog() {
        val dialog = fragment.childFragmentManager.findFragmentByTag(LOADING_DIALOG_TAG) as? DialogFragment
        dialog?.dismissAllowingStateLoss()
    }
}
