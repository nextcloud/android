/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.client.login.repository

import com.nextcloud.client.login.model.LoginFlowResponse

interface LoginFlowRepository {
    fun requestSession(loginEndpointUrl: String): LoginFlowResponse

    fun poll(pollUrl: String, token: String): LoginFlowResponse
}
