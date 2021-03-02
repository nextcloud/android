/*
 *   ownCloud Android client application
 *
 *   @author masensio
 *   Copyright (C) 2016 ownCloud GmbH.
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License version 2,
 *   as published by the Free Software Foundation.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package com.owncloud.android.utils;

import android.content.res.Resources;
import android.text.TextUtils;

import com.owncloud.android.R;
import com.owncloud.android.lib.common.operations.RemoteOperation;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.common.operations.RemoteOperationResult.ResultCode;
import com.owncloud.android.operations.CopyFileOperation;
import com.owncloud.android.operations.CreateFolderOperation;
import com.owncloud.android.operations.CreateShareViaLinkOperation;
import com.owncloud.android.operations.CreateShareWithShareeOperation;
import com.owncloud.android.operations.DownloadFileOperation;
import com.owncloud.android.operations.MoveFileOperation;
import com.owncloud.android.operations.RemoveFileOperation;
import com.owncloud.android.operations.RenameFileOperation;
import com.owncloud.android.operations.SynchronizeFileOperation;
import com.owncloud.android.operations.SynchronizeFolderOperation;
import com.owncloud.android.operations.UnshareOperation;
import com.owncloud.android.operations.UpdateSharePermissionsOperation;
import com.owncloud.android.operations.UpdateShareViaLinkOperation;
import com.owncloud.android.operations.UploadFileOperation;

import org.apache.commons.httpclient.ConnectTimeoutException;

import java.io.File;
import java.net.SocketTimeoutException;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Class to choose proper error messages to show to the user depending on the results of operations,
 * always following the same policy
 */
public final class ErrorMessageAdapter {

    private ErrorMessageAdapter() {
        // utility class -> private constructor
    }

    /**
     * Return an internationalized user message corresponding to an operation result
     * and the operation performed.
     *
     * @param result        Result of a {@link RemoteOperation} performed.
     * @param operation     Operation performed.
     * @param res           Reference to app resources, for i18n.
     * @return              User message corresponding to 'result' and 'operation'.
     */
    public static @NonNull
    String getErrorCauseMessage(
        RemoteOperationResult result,
        RemoteOperation operation,
        Resources res) {

        String message = getMessageForResultAndOperation(result, operation, res);

        if (TextUtils.isEmpty(message)) {
            message = getMessageForResult(result, res);
        }

        if (TextUtils.isEmpty(message)) {
            message = getMessageForOperation(operation, res);
        }

        if (message == null) {
            if (result.isSuccess()) {
                message = res.getString(R.string.common_ok);

            } else {
                message = res.getString(R.string.common_error_unknown);
            }
        }

        return message;
    }

    /**
     * Return a user message corresponding to an operation result and specific for the operation
     * performed.
     *
     * @param result        Result of a {@link RemoteOperation} performed.
     * @param operation     Operation performed.
     * @param res           Reference to app resources, for i18n.
     * @return              User message corresponding to 'result' and 'operation', or NULL if there is no
     *                      specific message for both.
     */
    private static @Nullable
    String getMessageForResultAndOperation(
        RemoteOperationResult result,
        RemoteOperation operation,
        Resources res) {

        String message = null;

        if (operation instanceof UploadFileOperation) {
            message = getMessageForUploadFileOperation(result, (UploadFileOperation) operation, res);

        } else if (operation instanceof DownloadFileOperation) {
            message = getMessageForDownloadFileOperation(result, (DownloadFileOperation) operation, res);

        } else if (operation instanceof RemoveFileOperation) {
            message = getMessageForRemoveFileOperation(result, res);

        } else if (operation instanceof RenameFileOperation) {
            message = getMessageForRenameFileOperation(result, res);

        } else if (operation instanceof SynchronizeFileOperation) {
            if (!((SynchronizeFileOperation) operation).transferWasRequested()) {
                message = res.getString(R.string.sync_file_nothing_to_do_msg);
            }

        } else if (operation instanceof CreateFolderOperation) {
            message = getMessageForCreateFolderOperation(result, res);

        } else if (operation instanceof CreateShareViaLinkOperation ||
                operation instanceof CreateShareWithShareeOperation) {

            message = getMessageForCreateShareOperations(result, res);

        } else if (operation instanceof UnshareOperation) {

            message = getMessageForUnshareOperation(result, res);

        } else if (operation instanceof UpdateShareViaLinkOperation ||
                operation instanceof UpdateSharePermissionsOperation) {

            message = getMessageForUpdateShareOperations(result, res);

        } else if (operation instanceof MoveFileOperation) {

            message = getMessageForMoveFileOperation(result, res);

        } else if (operation instanceof SynchronizeFolderOperation) {

            message = getMessageForSynchronizeFolderOperation(result, (SynchronizeFolderOperation) operation, res);

        } else if (operation instanceof CopyFileOperation) {
            message = getMessageForCopyFileOperation(result, res);
        }

        return message;
    }

    private static @Nullable
    String getMessageForSynchronizeFolderOperation(
        RemoteOperationResult result,
        SynchronizeFolderOperation operation,
        Resources res) {

        if (!result.isSuccess() && result.getCode() == ResultCode.FILE_NOT_FOUND) {
            return String.format(
                res.getString(R.string.sync_current_folder_was_removed),
                new File(operation.getFolderPath()).getName()
                                );
        }
        return null;
    }

    private static @Nullable
    String getMessageForMoveFileOperation(RemoteOperationResult result, Resources res) {
        if (result.getCode() == ResultCode.FILE_NOT_FOUND) {
            return res.getString(R.string.move_file_not_found);
        } else if (result.getCode() == ResultCode.INVALID_MOVE_INTO_DESCENDANT) {
            return res.getString(R.string.move_file_invalid_into_descendent);

        } else if (result.getCode() == ResultCode.INVALID_OVERWRITE) {
            return res.getString(R.string.move_file_invalid_overwrite);

        } else if (result.getCode() == ResultCode.FORBIDDEN) {
            return String.format(res.getString(R.string.forbidden_permissions),
                                 res.getString(R.string.forbidden_permissions_move));

        } else if (result.getCode() == ResultCode.INVALID_CHARACTER_DETECT_IN_SERVER) {
            return res.getString(R.string.filename_forbidden_charaters_from_server);
        }
        return null;
    }

    private static @Nullable
    String getMessageForUpdateShareOperations(RemoteOperationResult result, Resources res) {
        if (!TextUtils.isEmpty(result.getMessage())) {
            return result.getMessage();     // share API sends its own error messages
        } else if (result.getCode() == ResultCode.SHARE_NOT_FOUND) {
            return res.getString(R.string.update_link_file_no_exist);
        } else if (result.getCode() == ResultCode.SHARE_FORBIDDEN) {
            // Error --> No permissions
            return String.format(res.getString(R.string.forbidden_permissions),
                                 res.getString(R.string.update_link_forbidden_permissions));
        }
        return null;
    }

    private static @Nullable
    String getMessageForUnshareOperation(RemoteOperationResult result, Resources res) {
        if (!TextUtils.isEmpty(result.getMessage())) {
            return result.getMessage();     // share API sends its own error messages
        } else if (result.getCode() == ResultCode.SHARE_NOT_FOUND) {
            return res.getString(R.string.unshare_link_file_no_exist);
        } else if (result.getCode() == ResultCode.SHARE_FORBIDDEN) {
            // Error --> No permissions
            return String.format(res.getString(R.string.forbidden_permissions),
                                 res.getString(R.string.unshare_link_forbidden_permissions));
        }
        return null;
    }

    private static @Nullable
    String getMessageForCopyFileOperation(RemoteOperationResult result, Resources res) {
        if (result.getCode() == ResultCode.FILE_NOT_FOUND) {
            return res.getString(R.string.copy_file_not_found);

        } else if (result.getCode() == ResultCode.INVALID_COPY_INTO_DESCENDANT) {
            return res.getString(R.string.copy_file_invalid_into_descendent);

        } else if (result.getCode() == ResultCode.INVALID_OVERWRITE) {
            return res.getString(R.string.copy_file_invalid_overwrite);

        } else if (result.getCode() == ResultCode.FORBIDDEN) {
            return String.format(res.getString(R.string.forbidden_permissions),
                                 res.getString(R.string.forbidden_permissions_copy));

        }
        return null;
    }

    private static @Nullable
    String getMessageForCreateShareOperations(RemoteOperationResult result, Resources res) {
        if (!TextUtils.isEmpty(result.getMessage())) {
            return result.getMessage();     // share API sends its own error messages
        } else if (result.getCode() == ResultCode.SHARE_NOT_FOUND) {
            return res.getString(R.string.share_link_file_no_exist);
        } else if (result.getCode() == ResultCode.SHARE_FORBIDDEN) {
            // Error --> No permissions
            return String.format(res.getString(R.string.forbidden_permissions),
                                 res.getString(R.string.share_link_forbidden_permissions));
        }
        return null;
    }

    private static @Nullable
    String getMessageForCreateFolderOperation(RemoteOperationResult result, Resources res) {
        if (result.getCode() == ResultCode.INVALID_CHARACTER_IN_NAME) {
            return res.getString(R.string.filename_forbidden_characters);

        } else if (result.getCode().equals(ResultCode.FORBIDDEN)) {
            return String.format(res.getString(R.string.forbidden_permissions),
                                 res.getString(R.string.forbidden_permissions_create));

        } else if (result.getCode() == ResultCode.INVALID_CHARACTER_DETECT_IN_SERVER) {
            return res.getString(R.string.filename_forbidden_charaters_from_server);

        }
        return null;
    }

    private static @Nullable
    String getMessageForRenameFileOperation(RemoteOperationResult result, Resources res) {
        if (result.getCode().equals(ResultCode.INVALID_LOCAL_FILE_NAME)) {
            return res.getString(R.string.rename_local_fail_msg);

        } else if (result.getCode().equals(ResultCode.FORBIDDEN)) {
            // Error --> No permissions
            return String.format(res.getString(R.string.forbidden_permissions),
                                 res.getString(R.string.forbidden_permissions_rename));

        } else if (result.getCode().equals(ResultCode.INVALID_CHARACTER_IN_NAME)) {
            return res.getString(R.string.filename_forbidden_characters);

        } else if (result.getCode() == ResultCode.INVALID_CHARACTER_DETECT_IN_SERVER) {
            return res.getString(R.string.filename_forbidden_charaters_from_server);

        }

        return null;
    }

    private static @Nullable
    String getMessageForRemoveFileOperation(RemoteOperationResult result, Resources res) {
        if (result.isSuccess()) {
            return res.getString(R.string.remove_success_msg);

        } else {
            if (result.getCode().equals(ResultCode.FORBIDDEN)) {
                // Error --> No permissions
                return String.format(res.getString(R.string.forbidden_permissions),
                                     res.getString(R.string.forbidden_permissions_delete));
            }
        }

        return null;
    }

    private static @Nullable
    String getMessageForDownloadFileOperation(
        RemoteOperationResult result,
        DownloadFileOperation operation,
        Resources res) {

        if (result.isSuccess()) {
            return String.format(
                res.getString(R.string.downloader_download_succeeded_content),
                new File(operation.getSavePath()).getName());

        } else {
            if (result.getCode() == ResultCode.FILE_NOT_FOUND) {
                return res.getString(R.string.downloader_download_file_not_found);

            }
        }
        return null;
    }

    private static @Nullable
    String getMessageForUploadFileOperation(
        RemoteOperationResult result,
        UploadFileOperation operation,
        Resources res) {

        if (result.isSuccess()) {
            return String.format(
                res.getString(R.string.uploader_upload_succeeded_content_single),
                operation.getFileName());
        } else {

            if (result.getCode() == ResultCode.LOCAL_STORAGE_FULL
                    || result.getCode() == ResultCode.LOCAL_STORAGE_NOT_COPIED) {
                return String.format(
                        res.getString(R.string.error__upload__local_file_not_copied),
                        operation.getFileName(),
                        res.getString(R.string.app_name));

            } else if (result.getCode() == ResultCode.FORBIDDEN) {
                return String.format(res.getString(R.string.forbidden_permissions),
                        res.getString(R.string.uploader_upload_forbidden_permissions));

            } else if (result.getCode() == ResultCode.INVALID_CHARACTER_DETECT_IN_SERVER) {
                return res.getString(R.string.filename_forbidden_charaters_from_server);

            } else if(result.getCode() == ResultCode.SYNC_CONFLICT) {
                return String.format(res.getString(R.string.uploader_upload_failed_sync_conflict_error_content),
                                        operation.getFileName());
            }
        }

        return null;
    }


    /**
     * Return a user message corresponding to an operation result with no knowledge about the operation
     * performed.
     *
     * @param result        Result of a {@link RemoteOperation} performed.
     * @param res           Reference to app resources, for i18n.
     * @return              User message corresponding to 'result'.
     */
    private static @Nullable
    String getMessageForResult(RemoteOperationResult result, Resources res) {
        String message = null;

        if (!result.isSuccess()) {
            if (result.getCode() == ResultCode.WRONG_CONNECTION) {
                message = res.getString(R.string.network_error_socket_exception);

            } else if (result.getCode() == ResultCode.TIMEOUT) {
                message = res.getString(R.string.network_error_socket_exception);

                if (result.getException() instanceof SocketTimeoutException) {
                    message = res.getString(R.string.network_error_socket_timeout_exception);

                } else if (result.getException() instanceof ConnectTimeoutException) {
                    message = res.getString(R.string.network_error_connect_timeout_exception);
                }

            } else if (result.getCode() == ResultCode.HOST_NOT_AVAILABLE) {
                message = res.getString(R.string.network_host_not_available);

            } else if (result.getCode() == ResultCode.MAINTENANCE_MODE) {
                message = res.getString(R.string.maintenance_mode);

            } else if (result.getCode() == ResultCode.SSL_RECOVERABLE_PEER_UNVERIFIED) {
                message = res.getString(R.string.uploads_view_upload_status_failed_ssl_certificate_not_trusted);

            } else if (result.getCode() == ResultCode.BAD_OC_VERSION) {
                message = res.getString(R.string.auth_bad_oc_version_title);

            } else if (result.getCode() == ResultCode.INCORRECT_ADDRESS) {
                message = res.getString(R.string.auth_incorrect_address_title);

            } else if (result.getCode() == ResultCode.SSL_ERROR) {
                message = res.getString(R.string.auth_ssl_general_error_title);

            } else if (result.getCode() == ResultCode.UNAUTHORIZED) {
                message = res.getString(R.string.auth_unauthorized);

            } else if (result.getCode() == ResultCode.INSTANCE_NOT_CONFIGURED) {
                message = res.getString(R.string.auth_not_configured_title);

            } else if (result.getCode() == ResultCode.FILE_NOT_FOUND) {
                message = res.getString(R.string.auth_incorrect_path_title);

            } else if (result.getCode() == ResultCode.OAUTH2_ERROR) {
                message = res.getString(R.string.auth_oauth_error);

            } else if (result.getCode() == ResultCode.OAUTH2_ERROR_ACCESS_DENIED) {
                message = res.getString(R.string.auth_oauth_error_access_denied);

            } else if (result.getCode() == ResultCode.ACCOUNT_NOT_NEW) {
                message = res.getString(R.string.auth_account_not_new);

            } else if (result.getCode() == ResultCode.ACCOUNT_NOT_THE_SAME) {
                message = res.getString(R.string.auth_account_not_the_same);

            }

            else if (!TextUtils.isEmpty(result.getHttpPhrase())) {
                // last chance: error message from server
                message = result.getHttpPhrase();
            }
        }

        return message;
    }

    /**
     * Return a user message corresponding to a generic error for a given operation.
     *
     * @param operation     Operation performed.
     * @param res           Reference to app resources, for i18n.
     * @return              User message corresponding to a generic error of 'operation'.
     */
    private static @Nullable
    String getMessageForOperation(RemoteOperation operation, Resources res) {
        String message = null;

        if (operation instanceof UploadFileOperation) {
            message = String.format(
                    res.getString(R.string.uploader_upload_failed_content_single),
                    ((UploadFileOperation) operation).getFileName());

        } else if (operation instanceof DownloadFileOperation) {
            message = String.format(
                    res.getString(R.string.downloader_download_failed_content),
                    new File(((DownloadFileOperation) operation).getSavePath()).getName()
            );

        } else if (operation instanceof RemoveFileOperation) {
            message = res.getString(R.string.remove_fail_msg);

        } else if (operation instanceof RenameFileOperation) {
            message = res.getString(R.string.rename_server_fail_msg);

        } else if (operation instanceof CreateFolderOperation) {
            message = res.getString(R.string.create_dir_fail_msg);

        } else if (operation instanceof CreateShareViaLinkOperation ||
                operation instanceof CreateShareWithShareeOperation
                ) {
            message = res.getString(R.string.share_link_file_error);

        } else if (operation instanceof UnshareOperation) {
            message = res.getString(R.string.unshare_link_file_error);

        } else if (operation instanceof UpdateShareViaLinkOperation ||
                operation instanceof UpdateSharePermissionsOperation
                ) {
            message = res.getString(R.string.update_link_file_error);

        } else if (operation instanceof MoveFileOperation) {
            message = res.getString(R.string.move_file_error);

        } else if (operation instanceof SynchronizeFolderOperation) {
            String folderPathName = new File(
                    ((SynchronizeFolderOperation) operation).getFolderPath()
            ).getName();
            message = String.format(res.getString(R.string.sync_folder_failed_content), folderPathName);

        } else if (operation instanceof CopyFileOperation) {
            message = res.getString(R.string.copy_file_error);
        }

        return message;
    }
}
