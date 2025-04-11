/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.repository

import com.nextcloud.common.NextcloudClient
import com.owncloud.android.lib.common.OwnCloudClient

/**
 * Interface defining methods to retrieve Nextcloud and OwnCloudClient clients.
 * Provides both callback-based and suspend function versions for flexibility in usage.
 */
interface ClientRepository {
    /**
     * Retrieves an instance of [NextcloudClient] using a callback.
     *
     * @param onComplete A callback function that receives the [NextcloudClient] instance once available.
     */
    fun getNextcloudClient(onComplete: (NextcloudClient) -> Unit)

    /**
     * Retrieves an instance of [NextcloudClient] as a suspend function.
     *
     * @return The [NextcloudClient] instance, or `null` if it cannot be retrieved.
     */
    suspend fun getNextcloudClient(): NextcloudClient?

    /**
     * Retrieves an instance of [OwnCloudClient] using a callback.
     *
     * @param onComplete A callback function that receives the [OwnCloudClient] instance once available.
     */
    fun getOwncloudClient(onComplete: (OwnCloudClient) -> Unit)

    /**
     * Retrieves an instance of [OwnCloudClient] as a suspend function.
     *
     * @return The [OwnCloudClient] instance, or `null` if it cannot be retrieved.
     */
    suspend fun getOwncloudClient(): OwnCloudClient?
}
