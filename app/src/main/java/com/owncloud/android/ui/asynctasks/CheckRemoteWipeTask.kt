/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.owncloud.android.ui.asynctasks

import android.accounts.Account
import android.accounts.AccountManager
import android.content.Intent
import com.nextcloud.client.jobs.BackgroundJobManager
import com.owncloud.android.R
import com.owncloud.android.authentication.AuthenticatorActivity
import com.owncloud.android.lib.common.OwnCloudAccount
import com.owncloud.android.lib.common.OwnCloudClientManagerFactory
import com.owncloud.android.lib.common.accounts.AccountUtils
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.lib.resources.users.CheckRemoteWipeRemoteOperation
import com.owncloud.android.ui.activity.FileActivity
import com.owncloud.android.utils.DisplayUtils
import java.lang.ref.WeakReference

class CheckRemoteWipeTask(
    private val backgroundJobManager: BackgroundJobManager,
    private val account: Account,
    private val fileActivityWeakReference: WeakReference<FileActivity?>
) {
    fun execute() {
        val fileActivity = fileActivityWeakReference.get() ?: run {
            Log_OC.e(this, "Check for remote wipe: no context available")
            return
        }

        val checkWipeResult = CheckRemoteWipeRemoteOperation().execute(account, fileActivity)

        if (checkWipeResult.isSuccess) {
            backgroundJobManager.startAccountRemovalJob(account.name, true)
        } else {
            Log_OC.e(this, "Check for remote wipe not needed -> update credentials")
            performCredentialsUpdate(fileActivity)
        }
    }

    private fun performCredentialsUpdate(fileActivity: FileActivity) {
        try {
            val ocAccount = OwnCloudAccount(account, fileActivity)
            val client = OwnCloudClientManagerFactory.getDefaultSingleton().removeClientFor(ocAccount)

            client?.credentials?.let { credentials ->
                val accountManager = AccountManager.get(fileActivity)
                if (credentials.authTokenExpires()) {
                    accountManager.invalidateAuthToken(account.type, credentials.authToken)
                } else {
                    accountManager.clearPassword(account)
                }
            }

            val intent = Intent(fileActivity, AuthenticatorActivity::class.java).apply {
                putExtra(AuthenticatorActivity.EXTRA_ACCOUNT, account)
                putExtra(AuthenticatorActivity.EXTRA_ACTION, AuthenticatorActivity.ACTION_UPDATE_EXPIRED_TOKEN)
                addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
            }
            fileActivity.startActivityForResult(intent, FileActivity.REQUEST_CODE__UPDATE_CREDENTIALS)
        } catch (_: AccountUtils.AccountNotFoundException) {
            DisplayUtils.showSnackMessage(fileActivity, R.string.auth_account_does_not_exist)
        }
    }
}
