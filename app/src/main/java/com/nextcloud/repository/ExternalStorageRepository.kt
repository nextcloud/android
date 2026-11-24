/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.repository

import com.nextcloud.android.lib.resources.externalstorage.model.ExternalStorageCredentials

interface ExternalStorageRepository {
    suspend fun fetchMounts(): ExternalStorageMountsResult

    suspend fun authenticateMount(mountId: Long, credentials: ExternalStorageCredentials): Boolean
}
