/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.owncloud.android.authentication.dialog

import android.text.Html
import android.text.Spanned
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.nextcloud.android.lib.resources.users.GenerateOneTimeAppPasswordRemoteOperation
import com.nextcloud.common.NextcloudClient
import com.owncloud.android.R
import com.owncloud.android.authentication.AuthenticatorActivity
import com.owncloud.android.authentication.LoginUrlInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Credentials.basic
import androidx.core.net.toUri

class LoginDialog(private val activity: AuthenticatorActivity) {
    companion object {
        private const val HTTPS = "https://"
        private const val HTTP = "http://"
        private const val BOLD_START_TAG = "<b>"
        private const val BOLD_END_TAG = "</b>"
        private const val ONE_TIME_LOGIN_SUFFIX = "onetime-login/"
    }

    // region public methods
    fun showDeepLinkDialog(info: LoginUrlInfo) {
        showDialog(info, onPositive = {
            val loginText: TextView = activity.findViewById(R.id.loginInfo)
            loginText.text = String.format(
                activity.getString(R.string.direct_login_text),
                info.loginName,
                info.server
            )
            activity.login(info)
        })
    }

    fun showLoginConfirmationDialog(info: LoginUrlInfo, resultData: String) {
        showDialog(info, onPositive = {
            val onetimePrefix: String =
                activity.getString(R.string.login_data_own_scheme) + AuthenticatorActivity.PROTOCOL_SUFFIX +
                    ONE_TIME_LOGIN_SUFFIX
            if (resultData.startsWith(onetimePrefix)) {
                parseAndLoginFromOneTimeCode(onetimePrefix, resultData)
            } else {
                activity.parseAndLoginFromWebView(resultData)
            }
        })
    }
    // endregion

    // region private methods
    private fun parseAndLoginFromOneTimeCode(onetimePrefix: String, resultData: String) {
        activity.lifecycleScope.launch(Dispatchers.IO) {
            val data = AuthenticatorActivity.parseLoginDataUrl(onetimePrefix, resultData)
            val operation = GenerateOneTimeAppPasswordRemoteOperation()
            val credentials = basic(data.loginName, data.appPassword)
            val client =
                NextcloudClient(data.server.toUri(), data.loginName, credentials, activity)
            val otpResult = client.execute(operation)
            withContext(Dispatchers.Main) {
                if (!otpResult.isSuccess) {
                    activity.onOTPFailed()
                    return@withContext
                }

                val result = otpResult.resultData
                if (result == null) {
                    activity.onOTPFailed()
                    return@withContext
                }

                activity.onOTPCompleted(data, result)
            }
        }
    }

    private fun getDialogMessage(info: LoginUrlInfo): Spanned {
        val message = String.format(
            activity.getString(R.string.direct_login_confirm_message),
            BOLD_START_TAG + info.loginName + BOLD_END_TAG,
            BOLD_START_TAG + info.server.replace(HTTPS, "").replace(HTTP, "") + BOLD_END_TAG
        )

        return Html.fromHtml(message, Html.FROM_HTML_MODE_COMPACT)
    }

    private fun showDialog(info: LoginUrlInfo, onPositive: () -> Unit) {
        MaterialAlertDialogBuilder(activity)
            .setTitle(R.string.direct_login_confirm_title)
            .setMessage(getDialogMessage(info))
            .setPositiveButton(R.string.common_yes) { dialog, _ ->
                dialog.dismiss()
                onPositive()
            }
            .setNegativeButton(R.string.common_no) { dialog, _ ->
                dialog.dismiss()
                activity.intent = null
                activity.finish()
            }
            .setCancelable(false)
            .show()
    }
    // endregion
}
