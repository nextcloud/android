/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.utils.e2ee

import androidx.annotation.StringRes
import com.owncloud.android.R
import com.owncloud.android.lib.common.operations.RemoteOperationResult
import com.owncloud.android.lib.common.operations.RemoteOperationResult.ResultCode
import com.owncloud.android.ui.dialog.setupEncryption.model.DownloadKeyResult
import org.apache.commons.httpclient.HttpStatus
import org.json.JSONException
import java.io.IOException
import java.net.SocketTimeoutException
import javax.net.ssl.SSLException

object E2EEKeyRequestError {
    private const val HTTP_TOO_MANY_REQUESTS = 429
    private const val LAST_SERVER_ERROR = 599
    private const val LAST_CLIENT_ERROR = 499
    private val SERVER_ERRORS = HttpStatus.SC_INTERNAL_SERVER_ERROR..LAST_SERVER_ERROR
    private val CLIENT_ERRORS = HttpStatus.SC_BAD_REQUEST..LAST_CLIENT_ERROR

    fun from(result: RemoteOperationResult<*>): DownloadKeyResult.RequestFailed {
        val descriptionId = when (result.httpCode) {
            HttpStatus.SC_UNAUTHORIZED -> R.string.e2ee_key_request_unauthorized
            HttpStatus.SC_FORBIDDEN -> R.string.e2ee_key_request_forbidden
            HttpStatus.SC_NOT_FOUND -> R.string.e2ee_key_request_not_found
            HttpStatus.SC_REQUEST_TIMEOUT, HttpStatus.SC_GATEWAY_TIMEOUT -> R.string.e2ee_key_request_timeout
            HTTP_TOO_MANY_REQUESTS -> R.string.e2ee_key_request_rate_limited
            in SERVER_ERRORS -> R.string.e2ee_key_request_server_error
            in CLIENT_ERRORS -> R.string.e2ee_key_request_rejected
            else -> operationError(result)
        }
        return DownloadKeyResult.RequestFailed(descriptionId)
    }

    @StringRes
    private fun operationError(result: RemoteOperationResult<*>): Int = when (result.code) {
        ResultCode.UNAUTHORIZED, ResultCode.OAUTH2_ERROR, ResultCode.OAUTH2_ERROR_ACCESS_DENIED ->
            R.string.e2ee_key_request_unauthorized

        ResultCode.FORBIDDEN -> R.string.e2ee_key_request_forbidden

        ResultCode.TIMEOUT -> R.string.e2ee_key_request_timeout

        ResultCode.SSL_ERROR, ResultCode.SSL_RECOVERABLE_PEER_UNVERIFIED -> R.string.e2ee_key_request_tls_error

        ResultCode.NO_NETWORK_CONNECTION, ResultCode.HOST_NOT_AVAILABLE, ResultCode.INCORRECT_ADDRESS,
        ResultCode.WRONG_CONNECTION -> R.string.e2ee_key_request_connection_failed

        ResultCode.WRONG_SERVER_RESPONSE -> R.string.e2ee_key_request_invalid_response

        else -> when (result.exception) {
            is JSONException -> R.string.e2ee_key_request_invalid_response
            is SSLException -> R.string.e2ee_key_request_tls_error
            is SocketTimeoutException -> R.string.e2ee_key_request_timeout
            is IOException -> R.string.e2ee_key_request_connection_failed
            else -> R.string.e2ee_key_request_failed
        }
    }
}
