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
import com.nextcloud.client.login.model.LoginFailure
import com.nextcloud.client.login.model.LoginResponse
import com.nextcloud.client.login.model.LoginSession
import com.nextcloud.client.login.model.LoginState
import com.nextcloud.client.login.model.LoginStateObserver
import com.nextcloud.client.login.repository.LoginRepository
import com.nextcloud.client.login.util.LoginResponseParser
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

class LoginViewModel @Inject constructor(
    private val api: LoginRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : ViewModel() {

    private val _state = MutableStateFlow<LoginState>(LoginState.Idle)
    val state: StateFlow<LoginState> = _state.asStateFlow()

    private var flowJob: Job? = null
    private var pendingBrowserLaunchUrl: String? = null
    private var credentials: LoginUrlInfo? = null

    fun observeState(owner: LifecycleOwner, observer: LoginStateObserver) {
        owner.lifecycleScope.launch {
            owner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                state.collect { notify(it, observer) }
            }
        }
    }

    private fun notify(state: LoginState, observer: LoginStateObserver) = when (state) {
        is LoginState.AwaitingApproval -> observer.onAwaitingApproval()
        LoginState.Completed -> observer.onLoginCompleted()
        is LoginState.Failed -> observer.onLoginFailed(state.reason)
        LoginState.Idle, LoginState.RequestingSession -> Unit
    }

    fun start(loginEndpointUrl: String) {
        if (loginEndpointUrl.isBlank()) {
            _state.value = LoginState.Failed(LoginFailure.EMPTY_SERVER_URL)
            return
        }

        if (hasOngoingSession()) {
            Log_OC.d(TAG, "Login already running, ignoring duplicate start")
            return
        }

        _state.value = LoginState.RequestingSession
        flowJob = viewModelScope.launch { requestSessionAndPoll(loginEndpointUrl) }
    }

    fun consumePendingBrowserLaunch(): String? = pendingBrowserLaunchUrl.also { pendingBrowserLaunchUrl = null }

    fun consumeCredentials(): LoginUrlInfo? = credentials.also { credentials = null }

    fun isCompleted(): Boolean = _state.value == LoginState.Completed

    fun isAwaitingApproval(): Boolean = _state.value is LoginState.AwaitingApproval

    fun reset() {
        flowJob?.cancel()
        flowJob = null
        pendingBrowserLaunchUrl = null
        credentials = null
        _state.value = LoginState.Idle
    }

    private fun hasOngoingSession(): Boolean = when (_state.value) {
        LoginState.RequestingSession, LoginState.Completed -> true
        is LoginState.AwaitingApproval -> true
        else -> false
    }

    private suspend fun requestSessionAndPoll(loginEndpointUrl: String) {
        val response = request { api.requestSession(loginEndpointUrl) }
        if (response == null || response.body.isEmpty()) {
            _state.value = LoginState.Failed(LoginFailure.EMPTY_RESPONSE)
            return
        }

        val session = LoginResponseParser.parseSession(loginEndpointUrl, response.body)
        if (session == null) {
            _state.value = LoginState.Failed(LoginFailure.MALFORMED_RESPONSE)
            return
        }

        pendingBrowserLaunchUrl = session.loginUrl
        _state.value = LoginState.AwaitingApproval(session)

        val approved = withTimeoutOrNull(POLL_TIMEOUT_MILLIS.milliseconds) { pollUntilApproved(session) }
        if (approved == null) {
            Log_OC.d(TAG, "Login timed out before the user granted access")
            _state.value = LoginState.Failed(LoginFailure.TIMED_OUT)
        }
    }

    private suspend fun pollUntilApproved(session: LoginSession): LoginUrlInfo {
        while (true) {
            val response = request { api.poll(session.pollUrl, session.pollToken) }
            val polledCredentials = response?.let { LoginResponseParser.parseCredentials(it) }

            if (polledCredentials != null) {
                credentials = polledCredentials
                _state.value = LoginState.Completed
                return polledCredentials
            }

            delay(POLL_INTERVAL_MILLIS.milliseconds)
        }
    }

    private suspend fun request(block: () -> LoginResponse): LoginResponse? {
        val result = runCatching { withContext(ioDispatcher) { block() } }

        result.exceptionOrNull()?.let { throwable ->
            if (throwable is CancellationException) {
                throw throwable
            }
            Log_OC.d(TAG, "Login request failed, retrying: " + throwable.message)
        }

        return result.getOrNull()
    }

    companion object {
        private const val TAG = "LoginViewModel"

        private const val POLL_INTERVAL_MILLIS = 1000L

        private const val POLL_TIMEOUT_MILLIS = 20 * 60 * 1000L
    }
}
