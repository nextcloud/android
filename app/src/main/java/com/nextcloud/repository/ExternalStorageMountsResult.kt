/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.repository

import com.nextcloud.android.lib.resources.externalstorage.model.ExternalStorageMount

sealed interface ExternalStorageMountsResult {
    data class Success(val mounts: List<ExternalStorageMount>) : ExternalStorageMountsResult

    data object Failure : ExternalStorageMountsResult
}
