/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.owncloud.android.operations.upload

import com.owncloud.android.lib.common.operations.RemoteOperationResult
import org.apache.commons.httpclient.HttpStatus

enum class RemoteFileExistence {
    EXISTS,
    DOES_NOT_EXIST,
    UNAUTHORIZED;

    companion object {
        @JvmStatic
        fun fromExistenceCheck(result: RemoteOperationResult<*>): RemoteFileExistence = when {
            result.httpCode == HttpStatus.SC_UNAUTHORIZED -> UNAUTHORIZED
            result.isSuccess -> EXISTS
            else -> DOES_NOT_EXIST
        }
    }
}
