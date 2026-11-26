/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.owncloud.android.ui.activity.settings.storage.migration

import android.accounts.Account
import android.accounts.AccountManager
import android.content.ContentResolver
import android.content.Context
import com.owncloud.android.MainApp
import com.owncloud.android.R
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.seconds

class AccountSyncPauser(context: Context) {

    private val authority = context.getString(R.string.authority)
    private val accounts: Array<Account> =
        AccountManager.get(context).getAccountsByType(MainApp.getAccountType(context))

    suspend fun <T> runPaused(onProgress: suspend (Int) -> Unit, block: suspend () -> T): T {
        onProgress(R.string.file_migration_saving_accounts_configuration)
        val syncStates = BooleanArray(accounts.size) { ContentResolver.getSyncAutomatically(accounts[it], authority) }

        try {
            onProgress(R.string.file_migration_waiting_for_unfinished_sync)
            accounts.forEach { ContentResolver.setSyncAutomatically(it, authority, false) }
            waitForUnfinishedSynchronizations()

            return block()
        } finally {
            onProgress(R.string.file_migration_restoring_accounts_configuration)
            accounts.forEachIndexed { index, account ->
                ContentResolver.setSyncAutomatically(account, authority, syncStates[index])
            }
        }
    }

    private suspend fun waitForUnfinishedSynchronizations() {
        accounts.forEach { account ->
            while (ContentResolver.isSyncActive(account, authority)) {
                delay(SYNC_POLL_INTERVAL)
            }
        }
    }

    companion object {
        private val SYNC_POLL_INTERVAL = 1.seconds
    }
}
