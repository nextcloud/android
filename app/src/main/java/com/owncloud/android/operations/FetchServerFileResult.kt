/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.owncloud.android.operations

import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.lib.common.operations.RemoteOperationResult

internal sealed interface FetchServerFileResult {
    data class Found(val file: OCFile) : FetchServerFileResult
    data object Missing : FetchServerFileResult
    data class Failed(val result: RemoteOperationResult<*>) : FetchServerFileResult
}
