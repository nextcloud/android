/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.owncloud.android.authentication.account

import android.accounts.Account
import android.accounts.AccountManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.preference.PreferenceManager
import androidx.core.content.edit
import androidx.core.os.bundleOf
import com.nextcloud.client.account.UserAccountManager
import com.owncloud.android.lib.common.OwnCloudAccount
import com.owncloud.android.lib.common.OwnCloudClientManagerFactory
import com.owncloud.android.lib.common.UserInfo
import com.owncloud.android.lib.common.accounts.AccountUtils
import com.owncloud.android.operations.GetServerInfoOperation.ServerInfo

class AuthenticatorAccountStore(
    private val context: Context,
    private val platformAccountManager: AccountManager,
    private val userAccountManager: UserAccountManager
) {

    fun baseUrl(account: Account): String =
        platformAccountManager.getUserData(account, AccountUtils.Constants.KEY_OC_BASE_URL).orEmpty()

    /**
     * Registers [account] and returns the result intent for the AccountAuthenticator that started the login.
     */
    fun addAccount(
        account: Account,
        password: String?,
        loginName: String?,
        serverInfo: ServerInfo,
        userInfo: UserInfo
    ): Intent {
        platformAccountManager.addAccountExplicitly(account, password, null)
        platformAccountManager.notifyAccountAuthenticated(account)
        selectAccountIfNoneSelected(account.name)

        val authenticatorResult = Intent().apply {
            putExtra(AccountManager.KEY_ACCOUNT_TYPE, account.type)
            putExtra(AccountManager.KEY_ACCOUNT_NAME, account.name)
            putExtra(AccountManager.KEY_USERDATA, loginName)
        }
        storeUserData(account, serverInfo, userInfo)
        return authenticatorResult
    }

    /**
     * Stores the new [password] and kills the session kept by OwnCloudClientManager so that a new one will be
     * created with the new credentials when needed.
     *
     * @return the response for the AccountAuthenticator that started the login
     */
    @Throws(AccountUtils.AccountNotFoundException::class)
    fun updateCredentials(account: Account, password: String?): Bundle {
        val response = bundleOf(
            AccountManager.KEY_ACCOUNT_NAME to account.name,
            AccountManager.KEY_ACCOUNT_TYPE to account.type,
            AccountManager.KEY_AUTHTOKEN to password
        )
        platformAccountManager.setPassword(account, password)

        OwnCloudClientManagerFactory.getDefaultSingleton().removeClientFor(OwnCloudAccount(account, context))
        return response
    }

    private fun selectAccountIfNoneSelected(accountName: String) {
        if (!userAccountManager.user.isAnonymous) {
            return
        }

        @Suppress("DEPRECATION")
        PreferenceManager.getDefaultSharedPreferences(context).edit {
            putString(PREF_SELECTED_ACCOUNT_NAME, accountName)
        }
    }

    private fun storeUserData(account: Account, serverInfo: ServerInfo, userInfo: UserInfo) {
        platformAccountManager.run {
            setUserData(account, AccountUtils.Constants.KEY_OC_VERSION, serverInfo.mVersion.version)
            setUserData(account, AccountUtils.Constants.KEY_OC_BASE_URL, serverInfo.mBaseUrl)
            setUserData(account, AccountUtils.Constants.KEY_DISPLAY_NAME, userInfo.displayName)
            setUserData(account, AccountUtils.Constants.KEY_USER_ID, userInfo.id)
            setUserData(
                account,
                AccountUtils.Constants.KEY_OC_ACCOUNT_VERSION,
                UserAccountManager.ACCOUNT_VERSION_WITH_PROPER_ID.toString()
            )
        }
    }

    companion object {
        private const val PREF_SELECTED_ACCOUNT_NAME = "select_oc_account"
    }
}
