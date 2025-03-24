/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.repository

import com.nextcloud.common.NextcloudClient
import com.owncloud.android.lib.common.OwnCloudClient

interface ClientRepositoryType {
    fun getNextcloudClient(onComplete: (NextcloudClient) -> Unit)

    suspend fun getNextcloudClient(): NextcloudClient?

    fun getOwncloudClient(onComplete: (OwnCloudClient) -> Unit)

    suspend fun getOwncloudClient(): OwnCloudClient?
}
