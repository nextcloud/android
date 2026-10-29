/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.owncloud.android.providers.documentstorage

import com.owncloud.android.lib.common.operations.RemoteOperationResult
import com.owncloud.android.lib.common.utils.Log_OC
import java.io.FileNotFoundException

internal fun fileNotFoundException(message: String, cause: Throwable): FileNotFoundException =
    FileNotFoundException(message).apply { initCause(cause) }

@Throws(FileNotFoundException::class)
internal inline fun RemoteOperationResult<*>.requireSuccess(tag: String, failureMessage: () -> String) {
    if (isSuccess) {
        return
    }

    Log_OC.e(tag, toString())
    throw FileNotFoundException(failureMessage())
}
