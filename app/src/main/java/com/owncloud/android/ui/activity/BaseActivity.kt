/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-FileCopyrightText: 2016-2024 Nextcloud GmbH and Nextcloud contributors
 * SPDX-FileCopyrightText: 2024 TSI-mc <surinder.kumar@t-systems.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.ui.activity

import android.accounts.Account
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.nextcloud.android.common.ui.util.extensions.applyEdgeToEdgeWithSystemBarPadding
import com.nextcloud.client.account.User
import com.nextcloud.client.account.UserAccountManager
import com.nextcloud.client.di.Injectable
import com.nextcloud.client.mixins.MixinRegistry
import com.nextcloud.client.mixins.SessionMixin
import com.nextcloud.client.preferences.AppPreferences
import com.nextcloud.repository.ClientRepository
import com.owncloud.android.datamodel.FileDataStorageManager
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.lib.resources.status.OCCapability
import java.util.Optional
import javax.inject.Inject

abstract class BaseActivity :
    AppCompatActivity(),
    Injectable {

    protected var enableAccountHandling = true

    private val mixinRegistry = MixinRegistry()
    private lateinit var sessionMixin: SessionMixin

    @Inject
    lateinit var accountManager: UserAccountManager

    @Inject
    lateinit var preferences: AppPreferences

    @Inject
    lateinit var fileDataStorageManager: FileDataStorageManager

    @Inject
    lateinit var clientRepository: ClientRepository

    val userAccountManager: UserAccountManager
        get() = accountManager

    val capabilities: Optional<OCCapability>
        get() = sessionMixin.getCapabilities()

    val account: Account
        get() = sessionMixin.currentAccount

    val user: Optional<User>
        get() = sessionMixin.getUser()

    open fun getStorageManager(): FileDataStorageManager = fileDataStorageManager

    override fun onCreate(savedInstanceState: Bundle?) {
        applyEdgeToEdgeWithSystemBarPadding()
        super.onCreate(savedInstanceState)
        sessionMixin = SessionMixin(this, accountManager)
        mixinRegistry.add(sessionMixin)

        if (enableAccountHandling) {
            mixinRegistry.onCreate(savedInstanceState)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mixinRegistry.onDestroy()
    }

    override fun onPause() {
        super.onPause()
        mixinRegistry.onPause()
    }

    override fun onResume() {
        super.onResume()
        if (enableAccountHandling) {
            mixinRegistry.onResume()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        mixinRegistry.onNewIntent(intent)
    }

    override fun onRestart() {
        Log_OC.v(TAG, "onRestart() start")
        super.onRestart()
        if (enableAccountHandling) {
            mixinRegistry.onRestart()
        }
    }

    @Deprecated("Use setUser(User) instead")
    protected open fun setAccount(account: Account) {
        sessionMixin.setAccount(account)
    }

    fun setUser(user: User) {
        sessionMixin.setUser(user)
    }

    protected open fun startAccountCreation() {
        sessionMixin.startAccountCreation()
    }

    companion object {
        private val TAG = BaseActivity::class.java.simpleName
    }
}
