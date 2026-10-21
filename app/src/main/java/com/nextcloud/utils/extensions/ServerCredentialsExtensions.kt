/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.utils.extensions

import com.nextcloud.android.common.ui.network.auth.ServerCredentials
import com.nextcloud.android.common.ui.network.http.NextcloudHttpClient
import com.nextcloud.android.common.ui.network.model.dataOrElse
import com.nextcloud.android.common.ui.share.repository.ShareRemoteRepository
import com.owncloud.android.lib.common.utils.Log_OC

private const val TAG = "ServerCredentialsExtensions"

suspend fun ServerCredentials.supportsUnifiedShare(): Boolean {
    val capabilities = ShareRemoteRepository(NextcloudHttpClient.create(this))
        .fetchSharingCapabilities()
        .dataOrElse { Log_OC.e(TAG, "Failed to fetch sharing capabilities") }

    return capabilities?.isUnifiedShareEnabled == true
}
