/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.client.login.repository

import com.nextcloud.client.login.model.LoginResponse
import com.nextcloud.client.network.ClientFactory
import com.nextcloud.operations.PostMethod
import okhttp3.FormBody
import javax.inject.Inject

class LoginRepositoryImpl @Inject constructor(private val clientFactory: ClientFactory) : LoginRepository {

    override fun requestSession(loginEndpointUrl: String): LoginResponse =
        PostMethod(loginEndpointUrl, false, FormBody.Builder().build()).send()

    override fun poll(pollUrl: String, token: String): LoginResponse {
        val body = FormBody.Builder().add(TOKEN_PARAMETER, token).build()
        return PostMethod(pollUrl, false, body).send()
    }

    private fun PostMethod.send(): LoginResponse {
        val status = execute(clientFactory.createPlainClient())
        return LoginResponse(status, getResponseBodyAsString())
    }

    companion object {
        private const val TOKEN_PARAMETER = "token"
    }
}
