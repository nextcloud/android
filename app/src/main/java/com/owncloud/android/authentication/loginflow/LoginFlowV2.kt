/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.owncloud.android.authentication.loginflow

import com.google.gson.JsonParser
import com.nextcloud.model.HTTPStatusCodes
import com.nextcloud.operations.PostMethod
import com.nextcloud.repository.ClientRepository
import com.owncloud.android.lib.common.utils.Log_OC
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.FormBody

class LoginFlowV2(
    private val clientRepository: ClientRepository,
    private val scope: CoroutineScope,
    private val onCredentialsReceived: (response: String, status: Int) -> Unit
) {
    var isCompleted = false

    private var authObject: AuthObject? = null
    private var fallbackToken: String? = null
    private var baseUrl: String? = null
    private var pollingJob: Job? = null
    private val pollRequests = Channel<Unit>(Channel.CONFLATED)

    val hasPollToken: Boolean
        get() = !authObject?.poll?.token.isNullOrEmpty()

    val pendingAuthObjectJson: String?
        get() = authObject?.takeIf { !isCompleted }?.let { AuthObject.toJson(it) }

    fun restore(authObjectJson: String) {
        authObject = AuthObject.fromJson(authObjectJson)
    }

    fun clearPendingLogin() {
        authObject = null
    }

    suspend fun requestLoginResponse(url: String): String {
        baseUrl = url
        return withContext(Dispatchers.IO) { postLoginRequest(url) }
    }

    fun parseLoginUrl(response: String): String? = loginUrlFromAuthObject(response) ?: loginUrlFromJsonObject(response)

    fun poll() {
        pollRequests.trySend(Unit)
        if (pollingJob?.isActive == true) {
            return
        }

        pollingJob = scope.launch(Dispatchers.IO) {
            while (isActive) {
                withTimeoutOrNull(POLL_INTERVAL_MS) { pollRequests.receive() }
                if (!isCompleted && !pollOnce()) {
                    return@launch
                }
            }
        }
    }

    fun stopPolling() {
        pollingJob?.cancel()
    }

    private fun postLoginRequest(url: String): String = try {
        val post = PostMethod(url, false, FormBody.Builder().build())
        val status = post.execute(clientRepository.getPlainClient())
        Log_OC.d(TAG, "login URL request finished with status $status")
        post.getResponseBodyAsString()
    } catch (e: Exception) {
        Log_OC.e(TAG, "login URL request failed: ${e.javaClass.simpleName}")
        ""
    }

    private fun loginUrlFromAuthObject(response: String): String? {
        try {
            val parsedAuthObject = AuthObject.fromJson(response)
            authObject = parsedAuthObject
            if (parsedAuthObject.login.isNotEmpty()) {
                return parsedAuthObject.login
            }
            Log_OC.e(TAG, "login URL missing in response, trying fallback parser")
        } catch (e: Exception) {
            Log_OC.e(TAG, "response parsing failed, trying fallback parser: ${e.javaClass.simpleName}")
        }
        return null
    }

    private fun loginUrlFromJsonObject(response: String): String? {
        try {
            val jsonObject = JsonParser.parseString(response).asJsonObject
            fallbackToken = jsonObject.getAsJsonObject(JSON_KEY_POLL).get(JSON_KEY_TOKEN).asString
            val loginUrl: String? = jsonObject.get(JSON_KEY_LOGIN).asString
            if (!loginUrl.isNullOrEmpty()) {
                return loginUrl
            }
            Log_OC.e(TAG, "fallback parser found no login URL")
        } catch (e: Exception) {
            Log_OC.e(TAG, "fallback parser failed: ${e.javaClass.simpleName}")
        }
        return null
    }

    private suspend fun pollOnce(): Boolean {
        val (status, response) = try {
            requestCredentials()
        } catch (e: Exception) {
            Log_OC.e(TAG, "poll failed, polling stopped: ${e.javaClass.simpleName}")
            return false
        }

        if (status == HTTPStatusCodes.NOT_FOUND.code) {
            Log_OC.d(TAG, "access not granted in browser yet, polling continues")
            return true
        }

        Log_OC.d(TAG, "poll finished with status $status")
        if (status == HTTPStatusCodes.SUCCESS.code && response.isNotEmpty()) {
            withContext(Dispatchers.Main) { onCredentialsReceived(response, status) }
        }
        return true
    }

    private fun requestCredentials(): Pair<Int, String> {
        val (pollUrl, token) = pollEndpointAndToken()
        val requestBody = FormBody.Builder()
            .add(JSON_KEY_TOKEN, requireNotNull(token))
            .build()

        val post = PostMethod(pollUrl, false, requestBody)
        val status = post.execute(clientRepository.getPlainClient())
        return status to post.getResponseBodyAsString()
    }

    private fun pollEndpointAndToken(): Pair<String, String?> {
        val poll = authObject?.poll
        if (poll != null && poll.endpoint.isNotEmpty() && poll.token.isNotEmpty()) {
            return poll.endpoint to poll.token
        }

        Log_OC.w(TAG, "poll endpoint or token missing, using fallback endpoint and token")
        return "$baseUrl$FALLBACK_POLL_PATH" to fallbackToken
    }

    companion object {
        private const val TAG = "LoginFlowV2"
        private const val POLL_INTERVAL_MS = 30_000L
        private const val FALLBACK_POLL_PATH = "/poll"
        private const val JSON_KEY_POLL = "poll"
        private const val JSON_KEY_TOKEN = "token"
        private const val JSON_KEY_LOGIN = "login"
    }
}
