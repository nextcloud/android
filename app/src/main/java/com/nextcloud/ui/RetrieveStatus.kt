/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2024 Edvard Holst <edvard.holst@gmail.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.ui

import com.nextcloud.client.account.User
import com.nextcloud.client.network.ClientFactory
import com.owncloud.android.lib.resources.users.GetStatusRemoteOperation
import com.owncloud.android.lib.resources.users.Status
import com.owncloud.android.lib.resources.users.StatusType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

suspend fun retrieveUserStatus(user: User, clientFactory: ClientFactory): Status = withContext(Dispatchers.IO) {
    try {
        val client = clientFactory.createNextcloudClient(user)
        val result = GetStatusRemoteOperation().execute(client)
        if (result.isSuccess && result.resultData is Status) {
            result.resultData as Status
        } else {
            offlineStatus()
        }
    } catch (e: ClientFactory.CreationException) {
        offlineStatus()
    } catch (e: IOException) {
        offlineStatus()
    }
}

private fun offlineStatus() = Status(StatusType.OFFLINE, "", "", -1)
