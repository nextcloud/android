/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2023 ZetaTom
 * SPDX-FileCopyrightText: 2023 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */

package com.nextcloud.utils.extensions

import android.content.Context
import com.nextcloud.common.NextcloudClient
import com.owncloud.android.lib.common.OwnCloudClient
import com.owncloud.android.lib.common.OwnCloudClientFactory

fun OwnCloudClient.toNextcloudClient(context: Context): NextcloudClient {
    return OwnCloudClientFactory.createNextcloudClient(
        baseUri,
        userId,
        credentials.toOkHttpCredentials(),
        context,
        isFollowRedirects
    )
}
