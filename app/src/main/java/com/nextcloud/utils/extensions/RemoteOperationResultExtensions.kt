/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2024 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.utils.extensions

import androidx.annotation.StringRes
import com.owncloud.android.MainApp
import com.owncloud.android.R
import com.owncloud.android.lib.common.operations.RemoteOperation
import com.owncloud.android.lib.common.operations.RemoteOperationResult
import com.owncloud.android.lib.common.operations.RemoteOperationResult.ResultCode
import com.owncloud.android.utils.ErrorMessageAdapter

fun Pair<RemoteOperationResult<*>?, RemoteOperation<*>?>?.getErrorMessage(): String {
    val result = this?.first ?: return MainApp.string(R.string.unexpected_error_occurred)
    val operation = this.second ?: return MainApp.string(R.string.unexpected_error_occurred)
    return ErrorMessageAdapter.getErrorCauseMessage(result, operation, MainApp.getAppContext().resources)
}

@StringRes
fun RemoteOperationResult<*>.connectionErrorMessageId(): Int? = when (code) {
    ResultCode.NO_NETWORK_CONNECTION -> R.string.auth_no_net_conn_title
    ResultCode.HOST_NOT_AVAILABLE, ResultCode.WRONG_CONNECTION -> R.string.network_host_not_available
    ResultCode.TIMEOUT -> R.string.auth_timeout_title
    ResultCode.MAINTENANCE_MODE -> R.string.maintenance_mode
    else -> null
}

fun ResultCode.isFileSpecificError(): Boolean {
    val generalErrorCodes = listOf(
        ResultCode.INSTANCE_NOT_CONFIGURED,
        ResultCode.QUOTA_EXCEEDED,
        ResultCode.LOCAL_STORAGE_FULL,
        ResultCode.WRONG_CONNECTION,
        ResultCode.UNAUTHORIZED,
        ResultCode.OK_NO_SSL,
        ResultCode.MAINTENANCE_MODE,
        ResultCode.UNTRUSTED_DOMAIN,
        ResultCode.ACCOUNT_NOT_THE_SAME,
        ResultCode.ACCOUNT_EXCEPTION,
        ResultCode.ACCOUNT_NOT_NEW,
        ResultCode.ACCOUNT_NOT_FOUND,
        ResultCode.ACCOUNT_USES_STANDARD_PASSWORD,
        ResultCode.INCORRECT_ADDRESS,
        ResultCode.BAD_OC_VERSION,
        ResultCode.LOCKED // most likely following upload will fail as well, server still in progress
    )

    return !generalErrorCodes.contains(this)
}

fun ResultCode.isUserCancellation(): Boolean = this == ResultCode.CANCELLED || this == ResultCode.USER_CANCELLED

fun ResultCode.isConflict(): Boolean {
    val errorCodes = listOf(
        ResultCode.SYNC_CONFLICT,
        ResultCode.CONFLICT
    )

    return errorCodes.contains(this)
}
