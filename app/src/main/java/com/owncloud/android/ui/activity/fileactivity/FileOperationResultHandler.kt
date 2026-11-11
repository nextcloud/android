/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.owncloud.android.ui.activity.fileactivity

import android.accounts.AuthenticatorException
import com.nextcloud.utils.SnackbarUtil
import com.owncloud.android.lib.common.operations.RemoteOperation
import com.owncloud.android.lib.common.operations.RemoteOperationResult
import com.owncloud.android.lib.common.operations.RemoteOperationResult.ResultCode
import com.owncloud.android.operations.GetSharesForFileOperation
import com.owncloud.android.operations.SynchronizeFileOperation
import com.owncloud.android.operations.SynchronizeFolderOperation
import com.owncloud.android.ui.activity.ConflictsResolveActivity
import com.owncloud.android.ui.activity.FileActivity
import com.owncloud.android.utils.ErrorMessageAdapter

class FileOperationResultHandler(private val activity: FileActivity) {

    fun onOperationFinish(operation: RemoteOperation<*>?, result: RemoteOperationResult<*>) {
        when {
            result.isAuthenticationFailure() -> onAuthenticationFailure(operation, result)

            !result.isSuccess && result.code == ResultCode.SSL_RECOVERABLE_PEER_UNVERIFIED ->
                activity.showUntrustedCertDialog(result)

            operation == null || operation is SynchronizeFolderOperation ->
                onSynchronizeFolderOperationFinish(operation, result)

            operation is SynchronizeFileOperation -> onSynchronizeFileOperationFinish(operation, result)

            operation is GetSharesForFileOperation -> onGetSharesForFileOperationFinish(operation, result)
        }
    }

    private fun RemoteOperationResult<*>.isAuthenticationFailure(): Boolean =
        !isSuccess && (code == ResultCode.UNAUTHORIZED || (isException && exception is AuthenticatorException))

    private fun onAuthenticationFailure(operation: RemoteOperation<*>?, result: RemoteOperationResult<*>) {
        activity.requestCredentialsUpdate()
        if (result.code == ResultCode.UNAUTHORIZED) {
            showErrorCause(operation, result)
        }
    }

    private fun onSynchronizeFolderOperationFinish(operation: RemoteOperation<*>?, result: RemoteOperationResult<*>) {
        when {
            result.isSuccess -> activity.updateFileFromDB()
            result.code != ResultCode.CANCELLED -> showErrorCause(operation, result)
        }
    }

    private fun onGetSharesForFileOperationFinish(
        operation: GetSharesForFileOperation,
        result: RemoteOperationResult<*>
    ) {
        if (result.isSuccess || result.code == ResultCode.SHARE_NOT_FOUND) {
            activity.updateFileFromDB()
        } else {
            showErrorCause(operation, result)
        }
    }

    private fun onSynchronizeFileOperationFinish(
        operation: SynchronizeFileOperation,
        result: RemoteOperationResult<*>
    ) {
        if (result.isSuccess) {
            if (!operation.transferWasRequested) {
                showErrorCause(operation, result)
            }
            activity.supportInvalidateOptionsMenu()
            return
        }

        if (result.code != ResultCode.SYNC_CONFLICT) return

        val currentUser = activity.user.orElseThrow { RuntimeException() }
        val intent = ConflictsResolveActivity.createIntent(
            operation.localFile,
            currentUser,
            NO_CONFLICT_UPLOAD_ID,
            null,
            activity
        )
        activity.startActivity(intent)
    }

    private fun showErrorCause(operation: RemoteOperation<*>?, result: RemoteOperationResult<*>) {
        SnackbarUtil.show(activity, ErrorMessageAdapter.getErrorCauseMessage(result, operation, activity.resources))
    }

    companion object {
        private const val NO_CONFLICT_UPLOAD_ID = -1L
    }
}
