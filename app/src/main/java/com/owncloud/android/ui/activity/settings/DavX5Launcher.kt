/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.owncloud.android.ui.activity.settings

import android.content.Intent
import android.net.Uri
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.nextcloud.client.account.User
import com.nextcloud.client.account.UserAccountManager
import com.nextcloud.client.utils.IntentUtil
import com.nextcloud.repository.ClientRepository
import com.nextcloud.utils.SnackbarUtil
import com.owncloud.android.R
import com.owncloud.android.authentication.AuthenticatorActivity
import com.owncloud.android.ui.activity.settings.util.isOk
import com.owncloud.android.ui.activity.settings.util.registerForResult
import kotlinx.coroutines.launch

class DavX5Launcher(
    private val fragment: Fragment,
    private val user: User,
    private val clientRepository: ClientRepository
) {

    private val activity = fragment.requireActivity()
    private var serverBaseUri: Uri? = null

    private val loginLauncher = fragment.registerForResult { result ->
        if (result.isOk) {
            SnackbarUtil.show(activity, R.string.prefs_calendar_contacts_sync_setup_successful)
        }
    }

    fun loadServerBaseUri() {
        fragment.lifecycleScope.launch {
            serverBaseUri = clientRepository.getOwncloudClient()?.baseUri
        }
    }

    fun launchLogin() {
        val loginIntent = Intent().setClassName(DAVX5_PACKAGE, DAVX5_LOGIN_ACTIVITY)
        if (activity.packageManager.resolveActivity(loginIntent, 0) == null) {
            openStore()
            return
        }

        serverBaseUri?.let { addServerExtras(loginIntent, it) }
        loginIntent.putExtra(EXTRA_USERNAME, UserAccountManager.getUsername(user))

        loginLauncher.launch(loginIntent)
    }

    private fun addServerExtras(loginIntent: Intent, baseUri: Uri) {
        loginIntent.putExtra(EXTRA_URL, "$baseUri$DAV_PATH")
        loginIntent.putExtra(EXTRA_LOGIN_FLOW, LOGIN_FLOW_ENABLED)
        loginIntent.data = "$baseUri${AuthenticatorActivity.WEB_LOGIN}".toUri()
        loginIntent.putExtra(EXTRA_DAV_PATH, DAV_PATH)
    }

    private fun openStore() {
        val installIntent = Intent(Intent.ACTION_VIEW, MARKET_URL.toUri())
        if (installIntent.resolveActivity(activity.packageManager) != null) {
            activity.startActivity(installIntent)
            return
        }

        IntentUtil.startLinkIntent(activity, FDROID_URL)
        SnackbarUtil.show(activity, R.string.prefs_calendar_contacts_no_store_error)
    }

    companion object {
        private const val DAVX5_PACKAGE = "at.bitfire.davdroid"
        private const val DAVX5_LOGIN_ACTIVITY = "$DAVX5_PACKAGE.ui.setup.LoginActivity"
        private const val MARKET_URL = "market://details?id=$DAVX5_PACKAGE"
        private const val FDROID_URL = "https://f-droid.org/packages/$DAVX5_PACKAGE/"
        private const val DAV_PATH = "/remote.php/dav"
        private const val EXTRA_URL = "url"
        private const val EXTRA_LOGIN_FLOW = "loginFlow"
        private const val EXTRA_DAV_PATH = "davPath"
        private const val EXTRA_USERNAME = "username"
        private const val LOGIN_FLOW_ENABLED = 1
    }
}
