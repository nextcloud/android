/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.owncloud.android.authentication.status

import android.content.res.Resources
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.owncloud.android.R
import com.owncloud.android.lib.common.operations.RemoteOperationResult
import com.owncloud.android.lib.common.operations.RemoteOperationResult.ResultCode
import com.owncloud.android.utils.ErrorMessageAdapter

class AuthenticatorStatusMapper(private val resources: Resources) {

    fun forServerCheck(result: RemoteOperationResult<*>, isHostUrlPlainHttp: Boolean): AuthenticatorStatus =
        when (val code = result.code) {
            ResultCode.OK_SSL -> secureConnection()

            ResultCode.OK_NO_SSL, ResultCode.OK -> plainConnection(isHostUrlPlainHttp)

            ResultCode.NO_NETWORK_CONNECTION -> noNetworkConnection()

            ResultCode.OK_REDIRECT_TO_NON_SECURE_CONNECTION ->
                status(R.drawable.ic_lock_open_white, R.string.auth_redirect_non_secure_connection_title)

            ResultCode.UNKNOWN_ERROR -> AuthenticatorStatus(R.drawable.ic_alert, unknownErrorMessage(result))

            else -> SERVER_ERROR_MESSAGES[code]?.let { alert(it) } ?: AuthenticatorStatus.NONE
        }

    fun forAuthentication(
        result: RemoteOperationResult<*>,
        isHostUrlPlainHttp: Boolean,
        showWebViewLoginUrl: Boolean,
        currentText: String?
    ): AuthenticatorStatus = when (result.code) {
        ResultCode.OK_SSL -> secureConnection()

        ResultCode.OK_NO_SSL, ResultCode.OK -> if (showWebViewLoginUrl) {
            plainConnection(isHostUrlPlainHttp)
        } else {
            AuthenticatorStatus(R.drawable.ic_alert, currentText)
        }

        ResultCode.NO_NETWORK_CONNECTION -> noNetworkConnection()

        ResultCode.SSL_RECOVERABLE_PEER_UNVERIFIED -> alert(R.string.auth_ssl_unverified_server_title)

        ResultCode.TIMEOUT -> alert(R.string.auth_timeout_title)

        ResultCode.HOST_NOT_AVAILABLE -> alert(R.string.auth_unknown_host_title)

        ResultCode.ACCOUNT_NOT_NEW -> alert(R.string.auth_account_not_new)

        else -> AuthenticatorStatus(
            R.drawable.ic_alert,
            ErrorMessageAdapter.getErrorCauseMessage(result, null, resources)
        )
    }

    fun isFatalAuthenticationError(result: RemoteOperationResult<*>): Boolean =
        result.code !in NON_FATAL_AUTHENTICATION_CODES

    private fun secureConnection() = status(R.drawable.ic_lock_white, R.string.auth_secure_connection)

    private fun noNetworkConnection() = status(R.drawable.no_network, R.string.auth_no_net_conn_title)

    private fun plainConnection(isHostUrlPlainHttp: Boolean): AuthenticatorStatus = if (isHostUrlPlainHttp) {
        status(R.drawable.ic_ok, R.string.auth_connection_established)
    } else {
        status(R.drawable.ic_lock_open_white, R.string.auth_nossl_plain_ok_title)
    }

    private fun unknownErrorMessage(result: RemoteOperationResult<*>): String {
        val exceptionMessage = result.exception?.message
        return if (exceptionMessage.isNullOrEmpty()) {
            resources.getString(R.string.auth_unknown_error_title)
        } else {
            resources.getString(R.string.auth_unknown_error_exception_title, exceptionMessage)
        }
    }

    private fun alert(@StringRes message: Int) = status(R.drawable.ic_alert, message)

    private fun status(@DrawableRes icon: Int, @StringRes message: Int) =
        AuthenticatorStatus(icon, resources.getString(message))

    companion object {
        private val NON_FATAL_AUTHENTICATION_CODES = setOf(
            ResultCode.OK_SSL,
            ResultCode.OK_NO_SSL,
            ResultCode.OK,
            ResultCode.NO_NETWORK_CONNECTION,
            ResultCode.SSL_RECOVERABLE_PEER_UNVERIFIED,
            ResultCode.TIMEOUT,
            ResultCode.HOST_NOT_AVAILABLE
        )

        private val SERVER_ERROR_MESSAGES = mapOf(
            ResultCode.SSL_RECOVERABLE_PEER_UNVERIFIED to R.string.auth_ssl_unverified_server_title,
            ResultCode.BAD_OC_VERSION to R.string.auth_bad_oc_version_title,
            ResultCode.WRONG_CONNECTION to R.string.auth_wrong_connection_title,
            ResultCode.TIMEOUT to R.string.auth_timeout_title,
            ResultCode.INCORRECT_ADDRESS to R.string.auth_incorrect_address_title,
            ResultCode.SSL_ERROR to R.string.auth_ssl_general_error_title,
            ResultCode.UNAUTHORIZED to R.string.auth_unauthorized,
            ResultCode.HOST_NOT_AVAILABLE to R.string.auth_unknown_host_title,
            ResultCode.INSTANCE_NOT_CONFIGURED to R.string.auth_not_configured_title,
            ResultCode.FILE_NOT_FOUND to R.string.auth_incorrect_path_title,
            ResultCode.OAUTH2_ERROR to R.string.auth_oauth_error,
            ResultCode.OAUTH2_ERROR_ACCESS_DENIED to R.string.auth_oauth_error_access_denied,
            ResultCode.UNHANDLED_HTTP_CODE to R.string.auth_unknown_error_http_title,
            ResultCode.MAINTENANCE_MODE to R.string.maintenance_mode,
            ResultCode.UNTRUSTED_DOMAIN to R.string.untrusted_domain
        )
    }
}
