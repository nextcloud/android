/*
 * Nextcloud Android client application
 *
 * @author Chris Narkiewicz
 * Copyright (C) 2020 Chris Narkiewicz <hello@ezaquarii.com>
 * Copyright (C) 2020 Nextcloud GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package com.nextcloud.client.mixins

import android.accounts.Account
import android.app.Activity
import android.content.ContentResolver
import android.content.Intent
import android.os.Bundle
import com.nextcloud.client.account.User
import com.nextcloud.client.account.UserAccountManager
import com.nextcloud.java.util.Optional
import com.owncloud.android.datamodel.FileDataStorageManager
import com.owncloud.android.lib.resources.status.OCCapability
import com.owncloud.android.ui.activity.BaseActivity

/**
 * Session mixin collects all account / user handling logic currently
 * spread over various activities.
 *
 * It is an intermediary step facilitating comprehensive rework of
 * account handling logic.
 */
class SessionMixin constructor(
    private val activity: Activity,
    private val contentResolver: ContentResolver,
    private val accountManager: UserAccountManager
) : ActivityMixin {

    private companion object {
        private val TAG = BaseActivity::class.java.simpleName
    }

    var currentAccount: Account? = null
        private set
    var storageManager: FileDataStorageManager? = null
        private set
    var capabilities: OCCapability? = null
        private set

    fun setAccount(account: Account?) {
        val validAccount = account != null && accountManager.setCurrentOwnCloudAccount(account.name)
        if (validAccount) {
            currentAccount = account
        } else {
            swapToDefaultAccount()
        }

        currentAccount?.let {
            val storageManager = FileDataStorageManager(getUser().get(), contentResolver)
            this.storageManager = storageManager
            this.capabilities = storageManager.getCapability(it.name)
        }
    }

    fun setUser(user: User) {
        setAccount(user.toPlatformAccount())
    }

    fun getUser(): Optional<User> = when (val it = this.currentAccount) {
        null -> Optional.empty()
        else -> accountManager.getUser(it.name)
    }

    /**
     * Tries to swap the current ownCloud [Account] for other valid and existing.
     *
     * If no valid ownCloud [Account] exists, then the user is requested
     * to create a new ownCloud [Account].
     */
    private fun swapToDefaultAccount() {
        // default to the most recently used account
        val newAccount = accountManager.currentAccount
        if (newAccount == null) {
            // no account available: force account creation
            startAccountCreation()
        } else {
            currentAccount = newAccount
        }
    }

    /**
     * Launches the account creation activity.
     */
    fun startAccountCreation() {
        accountManager.startAccountCreation(activity)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        val current = accountManager.currentAccount
        val currentAccount = this.currentAccount
        if (current != null && currentAccount != null && !currentAccount.name.equals(current.name)) {
            this.currentAccount = current
        }
    }

    /**
     *  Since ownCloud {@link Account} can be managed from the system setting menu, the existence of the {@link
     *  Account} associated to the instance must be checked every time it is restarted.
     */
    override fun onRestart() {
        super.onRestart()
        val validAccount = currentAccount != null && accountManager.exists(currentAccount)
        if (!validAccount) {
            swapToDefaultAccount()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val account = accountManager.currentAccount
        setAccount(account)
    }
}
