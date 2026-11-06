/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.nextcloud.utils.headers

import android.accounts.Account
import android.accounts.AccountManager
import android.content.Context
import androidx.core.net.toUri
import com.nextcloud.common.CustomHeaders
import com.owncloud.android.MainApp
import com.owncloud.android.lib.common.accounts.AccountUtils
import com.owncloud.android.lib.common.utils.Log_OC

/**
 * Custom headers are stored per account and registered for the origin of the account at app start.
 */
object AccountCustomHeaders {
    private const val TAG = "AccountCustomHeaders"
    private const val KEY_CUSTOM_HEADERS = "oc_custom_headers"

    @JvmStatic
    fun registerAll(context: Context) {
        val accountManager = AccountManager.get(context)
        accountManager.getAccountsByType(MainApp.getAccountType(context)).forEach { register(accountManager, it) }
    }

    /**
     * Drops the headers of removed accounts and keeps the ones of the remaining accounts.
     */
    @JvmStatic
    fun reconcile(context: Context) {
        CustomHeaders.origins.forEach { CustomHeaders.unregister(it) }
        registerAll(context)
    }

    fun store(context: Context, account: Account, json: String) {
        AccountManager.get(context).setUserData(account, KEY_CUSTOM_HEADERS, json)
    }

    fun read(context: Context, account: Account): Map<String, String> =
        CustomHeaders.fromJson(AccountManager.get(context).getUserData(account, KEY_CUSTOM_HEADERS)).orEmpty()

    /**
     * Empty headers remove the stored ones. Takes effect for every following request.
     */
    fun replace(context: Context, account: Account, headers: Map<String, String>) {
        val json = headers.takeIf { it.isNotEmpty() }?.let { CustomHeaders.toJson(it) }
        AccountManager.get(context).setUserData(account, KEY_CUSTOM_HEADERS, json)
        reconcile(context)
    }

    private fun register(accountManager: AccountManager, account: Account) {
        val headers = CustomHeaders.fromJson(accountManager.getUserData(account, KEY_CUSTOM_HEADERS)) ?: return
        val baseUri = accountManager.getUserData(account, AccountUtils.Constants.KEY_OC_BASE_URL)?.toUri()
        if (!CustomHeaders.register(baseUri, headers)) {
            Log_OC.e(TAG, "custom headers of an account are invalid and will be ignored")
        }
    }
}
