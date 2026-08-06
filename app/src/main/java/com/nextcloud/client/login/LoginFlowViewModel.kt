/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.client.login

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewModelScope
import com.nextcloud.client.di.IoDispatcher
import com.nextcloud.client.login.model.LoginFlowFailure
import com.nextcloud.client.login.model.LoginFlowResponse
import com.nextcloud.client.login.model.LoginFlowSession
import com.nextcloud.client.login.model.LoginFlowState
import com.nextcloud.client.login.model.LoginFlowStateObserver
import com.nextcloud.client.login.repository.LoginFlowRepository
import com.nextcloud.client.login.util.LoginFlowResponseParser
import com.owncloud.android.authentication.LoginUrlInfo
import com.owncloud.android.lib.common.utils.Log_OC
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds

class LoginFlowViewModel @Inject constructor(
    private val api: LoginFlowRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : ViewModel() {

    private val _state = MutableStateFlow<LoginFlowState>(LoginFlowState.Idle)
    val state: StateFlow<LoginFlowState> = _state.asStateFlow()

    private var flowJob: Job? = null
    private var pendingBrowserLaunchUrl: String? = null
    private var credentials: LoginUrlInfo? = null

    fun observeState(owner: LifecycleOwner, observer: LoginFlowStateObserver) {
        owner.lifecycleScope.launch {
            owner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                state.collect { observer.onStateChanged(it) }
            }
        }
    }

    fun start(loginEndpointUrl: String) {
        if (loginEndpointUrl.isBlank()) {
            _state.value = LoginFlowState.Failed(LoginFlowFailure.EMPTY_SERVER_URL)
            return
        }

        if (hasOngoingSession()) {
            Log_OC.d(TAG, "Login flow v2 already running, ignoring duplicate start")
            return
        }

        _state.value = LoginFlowState.RequestingSession
        flowJob = viewModelScope.launch { requestSessionAndPoll(loginEndpointUrl) }
    }

    fun consumePendingBrowserLaunch(): String? = pendingBrowserLaunchUrl.also { pendingBrowserLaunchUrl = null }

    fun consumeCredentials(): LoginUrlInfo? = credentials.also { credentials = null }

    fun isCompleted(): Boolean = _state.value == LoginFlowState.Completed

    fun reset() {
        flowJob?.cancel()
        flowJob = null
        pendingBrowserLaunchUrl = null
        credentials = null
        _state.value = LoginFlowState.Idle
    }

    private fun hasOngoingSession(): Boolean = when (_state.value) {
        LoginFlowState.RequestingSession, LoginFlowState.Completed -> true
        is LoginFlowState.AwaitingApproval -> true
        else -> false
    }

    private suspend fun requestSessionAndPoll(loginEndpointUrl: String) {
        val response = request { api.requestSession(loginEndpointUrl) }
        if (response == null || response.body.isEmpty()) {
            _state.value = LoginFlowState.Failed(LoginFlowFailure.EMPTY_RESPONSE)
            return
        }

        val session = LoginFlowResponseParser.parseSession(loginEndpointUrl, response.body)
        if (session == null) {
            _state.value = LoginFlowState.Failed(LoginFlowFailure.MALFORMED_RESPONSE)
            return
        }

        pendingBrowserLaunchUrl = session.loginUrl
        _state.value = LoginFlowState.AwaitingApproval(session)

        val approved = withTimeoutOrNull(POLL_TIMEOUT_MILLIS.milliseconds) { pollUntilApproved(session) }
        if (approved == null) {
            Log_OC.d(TAG, "Login flow v2 timed out before the user granted access")
            _state.value = LoginFlowState.Failed(LoginFlowFailure.TIMED_OUT)
        }
    }

    private suspend fun pollUntilApproved(session: LoginFlowSession): LoginUrlInfo {
        while (true) {
            val response = request { api.poll(session.pollUrl, session.pollToken) }
            val polledCredentials = response?.let { LoginFlowResponseParser.parseCredentials(it) }

            if (polledCredentials != null) {
                credentials = polledCredentials
                _state.value = LoginFlowState.Completed
                return polledCredentials
            }

            delay(POLL_INTERVAL_MILLIS.milliseconds)
        }
    }

    private suspend fun request(block: () -> LoginFlowResponse): LoginFlowResponse? {
        val result = runCatching { withContext(ioDispatcher) { block() } }

        result.exceptionOrNull()?.let { throwable ->
            if (throwable is CancellationException) {
                throw throwable
            }
            Log_OC.d(TAG, "Login flow v2 request failed, retrying: " + throwable.message)
        }

        return result.getOrNull()
    }

    companion object {
        private const val TAG = "LoginFlowV2ViewModel"

        private const val POLL_INTERVAL_MILLIS = 1000L

        private const val POLL_TIMEOUT_MILLIS = 20 * 60 * 1000L
    }
}
