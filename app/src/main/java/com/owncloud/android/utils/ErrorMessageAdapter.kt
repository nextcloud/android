/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-FileCopyrightText: 2017-2021 Andy Scherzinger <info@andy-scherzinger.de>
 * SPDX-FileCopyrightText: 2017 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2016 ownCloud Inc.
 * SPDX-FileCopyrightText: 2015 María Asensio Valverde <masensio@solidgear.es>
 * SPDX-FileCopyrightText: 2026 TSI-mc <surinder.kumar@t-systems.com>
 * SPDX-License-Identifier: GPL-2.0-only AND (AGPL-3.0-or-later OR GPL-2.0-only)
 */
package com.owncloud.android.utils

import android.content.res.Resources
import androidx.annotation.StringRes
import com.owncloud.android.R
import com.owncloud.android.lib.common.operations.RemoteOperation
import com.owncloud.android.lib.common.operations.RemoteOperationResult
import com.owncloud.android.lib.common.operations.RemoteOperationResult.ResultCode
import com.owncloud.android.lib.resources.albums.RenameAlbumRemoteOperation
import com.owncloud.android.operations.CopyFileOperation
import com.owncloud.android.operations.CreateFolderOperation
import com.owncloud.android.operations.CreateShareViaLinkOperation
import com.owncloud.android.operations.CreateShareWithShareeOperation
import com.owncloud.android.operations.DownloadFileOperation
import com.owncloud.android.operations.MoveFileOperation
import com.owncloud.android.operations.RemoveFileOperation
import com.owncloud.android.operations.RenameFileOperation
import com.owncloud.android.operations.SynchronizeFileOperation
import com.owncloud.android.operations.SynchronizeFolderOperation
import com.owncloud.android.operations.UnshareOperation
import com.owncloud.android.operations.UpdateSharePermissionsOperation
import com.owncloud.android.operations.UpdateShareViaLinkOperation
import com.owncloud.android.operations.UploadFileOperation
import com.owncloud.android.operations.albums.CopyFileToAlbumOperation
import org.apache.commons.httpclient.ConnectTimeoutException
import org.apache.commons.httpclient.HttpStatus
import java.io.File
import java.net.SocketTimeoutException
import kotlin.jvm.optionals.getOrNull

object ErrorMessageAdapter {

    private val failedResultMessages: Map<ResultCode, Int> = mapOf(
        ResultCode.WRONG_CONNECTION to R.string.network_error_socket_exception,
        ResultCode.HOST_NOT_AVAILABLE to R.string.network_host_not_available,
        ResultCode.MAINTENANCE_MODE to R.string.maintenance_mode,
        ResultCode.SSL_RECOVERABLE_PEER_UNVERIFIED to
            R.string.uploads_view_upload_status_failed_ssl_certificate_not_trusted,
        ResultCode.BAD_OC_VERSION to R.string.auth_bad_oc_version_title,
        ResultCode.INCORRECT_ADDRESS to R.string.auth_incorrect_address_title,
        ResultCode.SSL_ERROR to R.string.auth_ssl_general_error_title,
        ResultCode.UNAUTHORIZED to R.string.auth_unauthorized,
        ResultCode.INSTANCE_NOT_CONFIGURED to R.string.auth_not_configured_title,
        ResultCode.FILE_NOT_FOUND to R.string.file_not_found,
        ResultCode.OAUTH2_ERROR to R.string.auth_oauth_error,
        ResultCode.OAUTH2_ERROR_ACCESS_DENIED to R.string.auth_oauth_error_access_denied,
        ResultCode.ACCOUNT_NOT_NEW to R.string.auth_account_not_new,
        ResultCode.ACCOUNT_NOT_THE_SAME to R.string.auth_account_not_the_same,
        ResultCode.QUOTA_EXCEEDED to R.string.upload_quota_exceeded,
        ResultCode.LOCKED to R.string.upload_locked_message
    )

    @JvmStatic
    fun getErrorCauseMessage(result: RemoteOperationResult<*>, operation: RemoteOperation<*>?, res: Resources): String =
        getMessageForResultAndOperation(result, operation, res)?.takeIf { it.isNotEmpty() }
            ?: getMessageForResult(result, res)?.takeIf { it.isNotEmpty() }
            ?: getMessageForOperation(operation, res)
            ?: res.getString(if (result.isSuccess) R.string.common_ok else R.string.common_error_unknown)

    @Suppress("CyclomaticComplexMethod")
    private fun getMessageForResultAndOperation(
        result: RemoteOperationResult<*>,
        operation: RemoteOperation<*>?,
        res: Resources
    ): String? = when (operation) {
        is UploadFileOperation -> getUploadMessage(result, operation, res)
        is DownloadFileOperation -> getDownloadMessage(result, operation, res)
        is RemoveFileOperation -> getRemoveMessage(result, res)
        is RenameFileOperation -> getRenameMessage(result, res)
        is SynchronizeFileOperation -> getSyncFileMessage(operation, res)
        is CreateFolderOperation -> getCreateFolderMessage(result, res)
        is CreateShareViaLinkOperation -> getCreateShareMessage(result, operation, res)
        is CreateShareWithShareeOperation -> getCreateShareMessage(result, operation, res)
        is UnshareOperation -> getUnshareMessage(result, res)
        is UpdateShareViaLinkOperation, is UpdateSharePermissionsOperation -> getUpdateShareMessage(result, res)
        is MoveFileOperation -> getMoveMessage(result, res)
        is SynchronizeFolderOperation -> getSyncFolderMessage(result, operation, res)
        is CopyFileOperation -> getCopyMessage(result, res)
        is CopyFileToAlbumOperation -> getCopyToAlbumMessage(result, res)
        is RenameAlbumRemoteOperation -> getRenameAlbumMessage(result, res)
        else -> null
    }

    private fun getSyncFileMessage(operation: SynchronizeFileOperation, res: Resources): String? =
        if (operation.transferWasRequested) null else res.getString(R.string.sync_file_nothing_to_do_msg)

    private fun getSyncFolderMessage(
        result: RemoteOperationResult<*>,
        operation: SynchronizeFolderOperation,
        res: Resources
    ): String? {
        if (result.isSuccess || result.code != ResultCode.FILE_NOT_FOUND) return null
        val folderName = operation.folderNameFromPath.getOrNull() ?: return null
        return res.getString(R.string.sync_current_folder_was_removed, folderName)
    }

    private fun getMoveMessage(result: RemoteOperationResult<*>, res: Resources): String? = when (result.code) {
        ResultCode.FILE_NOT_FOUND -> res.getString(R.string.move_file_not_found)
        ResultCode.INVALID_MOVE_INTO_DESCENDANT -> res.getString(R.string.move_file_invalid_into_descendent)
        ResultCode.INVALID_OVERWRITE -> res.getString(R.string.move_file_invalid_overwrite)
        ResultCode.FORBIDDEN -> res.getForbiddenMessage(R.string.forbidden_permissions_move)
        ResultCode.INVALID_CHARACTER_DETECT_IN_SERVER -> res.getServerForbiddenCharactersMessage()
        else -> null
    }

    private fun getUpdateShareMessage(result: RemoteOperationResult<*>, res: Resources): String? =
        result.shareApiMessage ?: when (result.code) {
            ResultCode.SHARE_NOT_FOUND -> res.getString(R.string.update_link_file_no_exist)
            ResultCode.SHARE_FORBIDDEN -> res.getForbiddenMessage(R.string.update_link_forbidden_permissions)
            else -> null
        }

    private fun getUnshareMessage(result: RemoteOperationResult<*>, res: Resources): String? =
        result.shareApiMessage ?: when (result.code) {
            ResultCode.SHARE_NOT_FOUND -> res.getString(R.string.unshare_link_file_no_exist)
            ResultCode.SHARE_FORBIDDEN -> res.getForbiddenMessage(R.string.unshare_link_forbidden_permissions)
            else -> null
        }

    private fun getCopyMessage(result: RemoteOperationResult<*>, res: Resources): String? = when (result.code) {
        ResultCode.FILE_NOT_FOUND -> res.getString(R.string.copy_file_not_found)
        ResultCode.INVALID_COPY_INTO_DESCENDANT -> res.getString(R.string.copy_file_invalid_into_descendent)
        ResultCode.INVALID_OVERWRITE -> res.getString(R.string.copy_file_invalid_overwrite)
        ResultCode.FORBIDDEN -> res.getForbiddenMessage(R.string.forbidden_permissions_copy)
        else -> null
    }

    private fun getCreateShareMessage(
        result: RemoteOperationResult<*>,
        operation: RemoteOperation<*>,
        res: Resources
    ): String? = result.shareApiMessage ?: when {
        isLinkSharePasswordRejected(result, operation) -> res.getString(R.string.share_link_password_rejected)
        result.code == ResultCode.SHARE_NOT_FOUND -> res.getString(R.string.share_link_file_no_exist)
        result.code == ResultCode.SHARE_FORBIDDEN -> res.getForbiddenMessage(R.string.share_link_forbidden_permissions)
        else -> null
    }

    private fun isLinkSharePasswordRejected(result: RemoteOperationResult<*>, operation: RemoteOperation<*>): Boolean =
        operation is CreateShareViaLinkOperation &&
            !operation.password.isNullOrEmpty() &&
            result.httpCode == HttpStatus.SC_BAD_REQUEST

    private fun getCreateFolderMessage(result: RemoteOperationResult<*>, res: Resources): String? = when (result.code) {
        ResultCode.INVALID_CHARACTER_IN_NAME -> res.getString(R.string.filename_forbidden_characters)
        ResultCode.FORBIDDEN -> res.getForbiddenMessage(R.string.forbidden_permissions_create)
        ResultCode.INVALID_CHARACTER_DETECT_IN_SERVER -> res.getServerForbiddenCharactersMessage()
        else -> null
    }

    private fun getRenameMessage(result: RemoteOperationResult<*>, res: Resources): String? = when (result.code) {
        ResultCode.INVALID_LOCAL_FILE_NAME -> res.getString(R.string.rename_local_fail_msg)
        ResultCode.FORBIDDEN -> res.getForbiddenMessage(R.string.forbidden_permissions_rename)
        ResultCode.INVALID_CHARACTER_IN_NAME -> res.getString(R.string.filename_forbidden_characters)
        ResultCode.INVALID_CHARACTER_DETECT_IN_SERVER -> res.getServerForbiddenCharactersMessage()
        else -> null
    }

    private fun getRemoveMessage(result: RemoteOperationResult<*>, res: Resources): String? {
        if (result.isSuccess) return res.getString(R.string.remove_success_msg)

        return when (result.code) {
            ResultCode.FORBIDDEN -> res.getForbiddenMessage(R.string.forbidden_permissions_delete)
            ResultCode.LOCKED -> res.getString(R.string.preview_media_unhandled_http_code_message)
            else -> null
        }
    }

    private fun getDownloadMessage(
        result: RemoteOperationResult<*>,
        operation: DownloadFileOperation,
        res: Resources
    ): String? {
        if (result.isSuccess) {
            return res.getString(R.string.downloader_download_succeeded_content, operation.savedFileName)
        }

        return when (result.code) {
            ResultCode.FILE_NOT_FOUND -> res.getString(R.string.downloader_download_file_not_found)
            ResultCode.CANNOT_CREATE_FILE -> res.getString(R.string.download_cannot_create_file)
            ResultCode.INVALID_LOCAL_FILE_NAME -> res.getString(R.string.download_download_invalid_local_file_name)
            else -> null
        }
    }

    private fun getUploadMessage(
        result: RemoteOperationResult<*>,
        operation: UploadFileOperation,
        res: Resources
    ): String? {
        if (result.isSuccess) {
            return res.getString(R.string.uploader_upload_succeeded_content_single, operation.fileName)
        }

        return when (result.code) {
            ResultCode.LOCAL_STORAGE_FULL,
            ResultCode.LOCAL_STORAGE_NOT_COPIED -> res.getString(
                R.string.error__upload__local_file_not_copied,
                operation.fileName,
                res.getString(R.string.app_name)
            )

            ResultCode.FORBIDDEN -> res.getForbiddenMessage(R.string.uploader_upload_forbidden_permissions)

            ResultCode.INVALID_CHARACTER_DETECT_IN_SERVER -> res.getServerForbiddenCharactersMessage()

            ResultCode.SYNC_CONFLICT ->
                res.getString(R.string.uploader_upload_failed_sync_conflict_error_content, operation.fileName)

            else -> null
        }
    }

    private fun getMessageForResult(result: RemoteOperationResult<*>, res: Resources): String? {
        if (result.isSuccess) return null

        val messageRes = if (result.code == ResultCode.TIMEOUT) {
            getTimeoutMessageRes(result.exception)
        } else {
            failedResultMessages[result.code] ?: return result.httpPhrase?.takeIf { it.isNotEmpty() }
        }

        return res.getString(messageRes)
    }

    @StringRes
    private fun getTimeoutMessageRes(exception: Exception?): Int = when (exception) {
        is SocketTimeoutException -> R.string.network_error_socket_timeout_exception
        is ConnectTimeoutException -> R.string.network_error_connect_timeout_exception
        else -> R.string.network_error_socket_exception
    }

    private fun getMessageForOperation(operation: RemoteOperation<*>?, res: Resources): String? = when (operation) {
        is UploadFileOperation -> res.getString(R.string.uploader_upload_failed_content_single, operation.fileName)
        is DownloadFileOperation -> res.getString(R.string.downloader_download_failed_content, operation.savedFileName)
        is SynchronizeFolderOperation -> getSyncFolderFailedMessage(operation, res)
        else -> getOperationFailedMessageRes(operation)?.let(res::getString)
    }

    private fun getSyncFolderFailedMessage(operation: SynchronizeFolderOperation, res: Resources): String? =
        operation.folderNameFromPath.getOrNull()?.let { res.getString(R.string.sync_folder_failed_content, it) }

    @StringRes
    private fun getOperationFailedMessageRes(operation: RemoteOperation<*>?): Int? = when (operation) {
        is RemoveFileOperation -> R.string.remove_fail_msg
        is RenameFileOperation -> R.string.rename_server_fail_msg
        is CreateFolderOperation -> R.string.create_dir_fail_msg
        is CreateShareViaLinkOperation, is CreateShareWithShareeOperation -> R.string.share_link_file_error
        is UnshareOperation -> R.string.unshare_link_file_error
        is UpdateShareViaLinkOperation, is UpdateSharePermissionsOperation -> R.string.update_link_file_error
        is MoveFileOperation -> R.string.move_file_error
        is CopyFileOperation -> R.string.copy_file_error
        else -> null
    }

    private fun getCopyToAlbumMessage(result: RemoteOperationResult<*>, res: Resources): String? =
        if (result.code == ResultCode.CONFLICT) res.getString(R.string.album_copy_file_conflict) else null

    private fun getRenameAlbumMessage(result: RemoteOperationResult<*>, res: Resources): String? =
        if (result.code == ResultCode.INVALID_OVERWRITE) res.getString(R.string.album_rename_conflict) else null

    private val RemoteOperationResult<*>.shareApiMessage: String?
        get() = message?.takeIf { it.isNotEmpty() }

    private val DownloadFileOperation.savedFileName: String
        get() = File(savePath).name

    private fun Resources.getForbiddenMessage(@StringRes actionRes: Int): String =
        getString(R.string.forbidden_permissions, getString(actionRes))

    private fun Resources.getServerForbiddenCharactersMessage(): String =
        getString(R.string.filename_forbidden_charaters_from_server)
}
