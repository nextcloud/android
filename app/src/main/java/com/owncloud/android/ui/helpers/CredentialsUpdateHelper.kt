/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.owncloud.android.ui.helpers

import android.accounts.Account
import android.accounts.AccountManager
import android.content.Context
import android.content.Intent
import androidx.annotation.WorkerThread
import com.nextcloud.client.account.User
import com.owncloud.android.authentication.AuthenticatorActivity
import com.owncloud.android.datamodel.FileDataStorageManager
import com.owncloud.android.lib.common.OwnCloudAccount
import com.owncloud.android.lib.common.OwnCloudClientManagerFactory
import com.owncloud.android.lib.common.accounts.AccountUtils
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.lib.resources.users.CheckRemoteWipeRemoteOperation
import com.owncloud.android.operations.CheckCurrentCredentialsOperation

class CredentialsUpdateHelper(private val context: Context) {

    companion object {
        private const val TAG = "CredentialsUpdateHelper"
    }

    @WorkerThread
    fun areCurrentCredentialsValid(user: User): Boolean = runCatching {
        val client = OwnCloudClientManagerFactory.getDefaultSingleton()
            .getClientFor(user.toOwnCloudAccount(), context)
        val storageManager = FileDataStorageManager(user, context.contentResolver)
        CheckCurrentCredentialsOperation(user, storageManager).execute(client).isSuccess
    }.getOrElse {
        Log_OC.e(TAG, "Error while checking credentials of ${user.accountName}", it)
        false
    }

    @WorkerThread
    fun isRemoteWipeRequested(account: Account): Boolean =
        CheckRemoteWipeRemoteOperation().execute(account, context).isSuccess

    @Throws(AccountUtils.AccountNotFoundException::class)
    fun invalidateCredentials(account: Account) {
        val ocAccount = OwnCloudAccount(account, context)
        val client = OwnCloudClientManagerFactory.getDefaultSingleton().removeClientFor(ocAccount) ?: return
        val credentials = client.credentials ?: return
        val accountManager = AccountManager.get(context)

        if (credentials.authTokenExpires()) {
            accountManager.invalidateAuthToken(account.type, credentials.authToken)
        } else {
            accountManager.clearPassword(account)
        }
    }

    fun createUpdateCredentialsIntent(account: Account): Intent =
        Intent(context, AuthenticatorActivity::class.java).apply {
            putExtra(AuthenticatorActivity.EXTRA_ACCOUNT, account)
            putExtra(AuthenticatorActivity.EXTRA_ACTION, AuthenticatorActivity.ACTION_UPDATE_EXPIRED_TOKEN)
            addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
        }
}
