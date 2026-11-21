/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.utils.e2ee

import com.owncloud.android.R
import com.owncloud.android.lib.common.operations.RemoteOperationResult
import com.owncloud.android.lib.common.operations.RemoteOperationResult.ResultCode
import org.json.JSONException
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import java.io.IOException
import java.net.SocketTimeoutException
import javax.net.ssl.SSLHandshakeException

class E2EEKeyRequestErrorTest {
    @Test
    fun identifiesRateLimitingDespiteTheLibraryReportingAnUnhandledHttpCode() {
        assertFailure(
            response(httpCode = 429, code = ResultCode.UNHANDLED_HTTP_CODE),
            R.string.e2ee_key_request_rate_limited
        )
    }

    @Test
    fun distinguishesAuthenticationAccessAndMissingResources() {
        assertFailure(response(httpCode = 401), R.string.e2ee_key_request_unauthorized)
        assertFailure(response(httpCode = 403), R.string.e2ee_key_request_forbidden)
        assertFailure(response(httpCode = 404), R.string.e2ee_key_request_not_found)
        assertFailure(response(httpCode = 302, code = ResultCode.UNAUTHORIZED), R.string.e2ee_key_request_unauthorized)
    }

    @Test
    fun distinguishesClientRejectionsServerErrorsAndTimeouts() {
        listOf(400, 405, 409, 422, 499).forEach {
            assertFailure(response(httpCode = it), R.string.e2ee_key_request_rejected)
        }
        listOf(500, 502, 503, 599).forEach {
            assertFailure(response(httpCode = it), R.string.e2ee_key_request_server_error)
        }
        listOf(408, 504).forEach {
            assertFailure(response(httpCode = it), R.string.e2ee_key_request_timeout)
        }
    }

    @Test
    fun preservesNetworkAndTlsFailureReasonsWithoutAnHttpResponse() {
        listOf(
            ResultCode.NO_NETWORK_CONNECTION,
            ResultCode.HOST_NOT_AVAILABLE,
            ResultCode.INCORRECT_ADDRESS,
            ResultCode.WRONG_CONNECTION
        ).forEach {
            assertFailure(response(code = it), R.string.e2ee_key_request_connection_failed)
        }
        listOf(ResultCode.SSL_ERROR, ResultCode.SSL_RECOVERABLE_PEER_UNVERIFIED).forEach {
            assertFailure(response(code = it), R.string.e2ee_key_request_tls_error)
        }
        assertFailure(response(code = ResultCode.TIMEOUT), R.string.e2ee_key_request_timeout)
    }

    @Test
    fun keepsOperationAuthenticationFailuresSeparateFromConnectionFailures() {
        listOf(ResultCode.UNAUTHORIZED, ResultCode.OAUTH2_ERROR, ResultCode.OAUTH2_ERROR_ACCESS_DENIED).forEach {
            assertFailure(response(code = it), R.string.e2ee_key_request_unauthorized)
        }
        assertFailure(response(code = ResultCode.FORBIDDEN), R.string.e2ee_key_request_forbidden)
    }

    @Test
    fun identifiesAnUnreadableSuccessfulHttpResponseWithoutClaimingTheEncryptionKeyIsInvalid() {
        assertFailure(
            response(httpCode = 200, exception = JSONException("Missing public-key")),
            R.string.e2ee_key_request_invalid_response
        )
        assertFailure(response(code = ResultCode.WRONG_SERVER_RESPONSE), R.string.e2ee_key_request_invalid_response)
    }

    @Test
    fun usesExceptionDetailsWhenTheLibraryHasNoSpecificResultCode() {
        assertFailure(response(exception = SSLHandshakeException("TLS")), R.string.e2ee_key_request_tls_error)
        assertFailure(response(exception = SocketTimeoutException()), R.string.e2ee_key_request_timeout)
        assertFailure(response(exception = IOException()), R.string.e2ee_key_request_connection_failed)
        assertFailure(response(exception = IllegalStateException()), R.string.e2ee_key_request_failed)
    }

    @Test
    fun prioritizesAnExplicitServerRejectionOverResponseParsingErrors() {
        assertFailure(
            response(httpCode = 429, exception = JSONException("Not JSON")),
            R.string.e2ee_key_request_rate_limited
        )
    }

    private fun assertFailure(response: RemoteOperationResult<String>, expectedDescription: Int) {
        assertEquals(expectedDescription, E2EEKeyRequestError.from(response).descriptionId)
    }

    private fun response(
        httpCode: Int = -1,
        code: ResultCode = ResultCode.UNKNOWN_ERROR,
        exception: Exception? = null
    ): RemoteOperationResult<String> = mock {
        on { this.httpCode } doReturn httpCode
        on { this.code } doReturn code
        on { this.exception } doReturn exception
    }
}
