/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.owncloud.android.authentication.loginflow

import android.content.Intent
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.browser.auth.AuthTabIntent
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import com.nextcloud.utils.SnackbarUtil
import com.owncloud.android.R
import com.owncloud.android.lib.common.utils.Log_OC

class LoginBrowserLauncher(private val activity: ComponentActivity) {

    private val authTabResultLauncher = AuthTabIntent.registerActivityResultLauncher(activity) { result ->
        Log_OC.d(TAG, "Auth Tab closed with result code ${result.resultCode}")
    }

    fun open(url: String) {
        if (url.isBlank()) {
            Log_OC.e(TAG, "cannot open browser, login URL is blank")
            SnackbarUtil.show(activity, R.string.invalid_url)
            return
        }

        val uri = url.toUri()
        if (launchAuthTab(uri) || launchExternalBrowser(uri)) {
            return
        }

        Log_OC.e(TAG, "no browser available to open login page")
        SnackbarUtil.show(activity, R.string.authenticator_activity_no_web_browser_found)
    }

    private fun launchAuthTab(uri: Uri): Boolean = try {
        val toolbarColor = ContextCompat.getColor(activity, R.color.primary)
        val authTabIntent = AuthTabIntent.Builder().setColorScheme(toolbarColor).build()
        authTabIntent.launch(authTabResultLauncher, uri, activity.getString(R.string.login_data_own_scheme))
        Log_OC.d(TAG, "login page opened in Auth Tab")
        true
    } catch (e: Exception) {
        Log_OC.e(TAG, "Auth Tab launch failed: ${e.javaClass.simpleName}")
        false
    }

    private fun launchExternalBrowser(uri: Uri): Boolean {
        try {
            val intent = Intent(Intent.ACTION_VIEW, uri)
            if (intent.resolveActivity(activity.packageManager) != null) {
                activity.startActivity(intent)
                Log_OC.d(TAG, "login page opened in external browser")
                return true
            }
        } catch (e: Exception) {
            Log_OC.e(TAG, "external browser launch failed: ${e.javaClass.simpleName}")
        }
        return false
    }

    companion object {
        private const val TAG = "LoginBrowserLauncher"
    }
}
