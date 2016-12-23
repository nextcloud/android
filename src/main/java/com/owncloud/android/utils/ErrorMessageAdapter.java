/**
 * ownCloud Android client application
 *
 * @author masensio
 * Copyright (C) 2014 ownCloud Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */


package com.owncloud.android.utils;

import android.content.res.Resources;

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

/**
 * Class to choose proper error messages to show to the user depending on the results of operations,
 * always following the same policy.
 */
public class ErrorMessageAdapter {

    public static String getErrorCauseMessage(RemoteOperationResult result,
                                              RemoteOperation operation, Resources res) {

        String message = null;

        if (!result.isSuccess() && isNetworkError(result.getCode())) {
            message = getErrorMessage(result, res);

        } else if (operation instanceof UploadFileOperation) {

            if (result.isSuccess()) {
                message = String.format(
                        res.getString(R.string.uploader_upload_succeeded_content_single),
                        ((UploadFileOperation) operation).getFileName());
            } else {
                if (result.getCode() == ResultCode.LOCAL_STORAGE_FULL
                        || result.getCode() == ResultCode.LOCAL_STORAGE_NOT_COPIED) {
                    message = String.format(
                            res.getString(R.string.error__upload__local_file_not_copied),
                            ((UploadFileOperation) operation).getFileName(),
                            res.getString(R.string.app_name));
                /*
                } else if (result.getCode() == ResultCode.QUOTA_EXCEEDED) {
                    message = res.getString(R.string.failed_upload_quota_exceeded_text);
                    */

                } else if (result.getCode() == ResultCode.FORBIDDEN) {
                    message = String.format(res.getString(R.string.forbidden_permissions),
                            res.getString(R.string.uploader_upload_forbidden_permissions));

                } else if (result.getCode() == ResultCode.INVALID_CHARACTER_DETECT_IN_SERVER) {
                    message = res.getString(R.string.filename_forbidden_charaters_from_server);

                } else if (result.getCode() == ResultCode.MAINTENANCE_MODE) {
                    message = res.getString(R.string.maintenance_mode);

                } else {
                    message = String.format(
                            res.getString(R.string.uploader_upload_failed_content_single),
                            ((UploadFileOperation) operation).getFileName());
                }
            }

        } else if (operation instanceof DownloadFileOperation) {

            if (result.isSuccess()) {
                message = String.format(
                        res.getString(R.string.downloader_download_succeeded_content),
                        new File(((DownloadFileOperation) operation).getSavePath()).getName());

            } else {
                if (result.getCode() == ResultCode.FILE_NOT_FOUND) {
                    message = res.getString(R.string.downloader_download_file_not_found);

                }  else if (result.getCode() == ResultCode.MAINTENANCE_MODE) {
                    message = res.getString(R.string.maintenance_mode);

                } else {
                    message = String.format(
                            res.getString(R.string.downloader_download_failed_content), new File(
                                    ((DownloadFileOperation) operation).getSavePath()).getName());
                }
            }

        } else if (operation instanceof RemoveFileOperation) {
            if (result.isSuccess()) {
                message = res.getString(R.string.remove_success_msg);

            } else {
                if (result.getCode().equals(ResultCode.FORBIDDEN)) {
                    // Error --> No permissions
                    message = String.format(res.getString(R.string.forbidden_permissions),
                            res.getString(R.string.forbidden_permissions_delete));

                } else if (result.getCode() == ResultCode.MAINTENANCE_MODE) {
                    message = res.getString(R.string.maintenance_mode);

                } else {
                    message = res.getString(R.string.remove_fail_msg);
                }
            }

        } else if (operation instanceof RenameFileOperation) {
            if (result.getCode().equals(ResultCode.INVALID_LOCAL_FILE_NAME)) {
                message = res.getString(R.string.rename_local_fail_msg);

            } else if (result.getCode().equals(ResultCode.FORBIDDEN)) {
                // Error --> No permissions
                message = String.format(res.getString(R.string.forbidden_permissions),
                        res.getString(R.string.forbidden_permissions_rename));

            } else if (result.getCode().equals(ResultCode.INVALID_CHARACTER_IN_NAME)) {
                message = res.getString(R.string.filename_forbidden_characters);

            } else if (result.getCode() == ResultCode.INVALID_CHARACTER_DETECT_IN_SERVER) {
                message = res.getString(R.string.filename_forbidden_charaters_from_server);

            } else if (result.getCode() == ResultCode.MAINTENANCE_MODE) {
                message = res.getString(R.string.maintenance_mode);

            } else {
                message = res.getString(R.string.rename_server_fail_msg);
            }

        } else if (operation instanceof SynchronizeFileOperation) {
            if (!((SynchronizeFileOperation) operation).transferWasRequested()) {
                message = res.getString(R.string.sync_file_nothing_to_do_msg);
            }

        } else if (operation instanceof CreateFolderOperation) {
            if (result.getCode() == ResultCode.INVALID_CHARACTER_IN_NAME) {
                message = res.getString(R.string.filename_forbidden_characters);

            } else if (result.getCode().equals(ResultCode.FORBIDDEN)) {
                message = String.format(res.getString(R.string.forbidden_permissions),
                        res.getString(R.string.forbidden_permissions_create));

            } else if (result.getCode() == ResultCode.INVALID_CHARACTER_DETECT_IN_SERVER) {
                message = res.getString(R.string.filename_forbidden_charaters_from_server);

            } else if (result.getCode() == ResultCode.MAINTENANCE_MODE) {
                message = res.getString(R.string.maintenance_mode);

            } else {
                message = res.getString(R.string.create_dir_fail_msg);
            }

        } else if (operation instanceof CreateShareViaLinkOperation ||
                operation instanceof CreateShareWithShareeOperation) {

            if (result.getData() != null && result.getData().size() > 0) {
                message = (String) result.getData().get(0);     // share API sends its own error messages

            } else if (result.getCode() == ResultCode.SHARE_NOT_FOUND) {
                message = res.getString(R.string.share_link_file_no_exist);

            } else if (result.getCode() == ResultCode.SHARE_FORBIDDEN) {
                // Error --> No permissions
                message = String.format(res.getString(R.string.forbidden_permissions),
                        res.getString(R.string.share_link_forbidden_permissions));

            } else if (result.getCode() == ResultCode.MAINTENANCE_MODE) {
                message = res.getString(R.string.maintenance_mode);

            } else {    // Generic error
                // Show a Message, operation finished without success
                message = res.getString(R.string.share_link_file_error);
            }

        } else if (operation instanceof UnshareOperation) {

            if (result.getData() != null && result.getData().size() > 0) {
                message = (String) result.getData().get(0);     // share API sends its own error messages

            } else if (result.getCode() == ResultCode.SHARE_NOT_FOUND) {
                message = res.getString(R.string.unshare_link_file_no_exist);

            } else if (result.getCode() == ResultCode.SHARE_FORBIDDEN) {
                // Error --> No permissions
                message = String.format(res.getString(R.string.forbidden_permissions),
                        res.getString(R.string.unshare_link_forbidden_permissions));

            } else if (result.getCode() == ResultCode.MAINTENANCE_MODE) {
                message = res.getString(R.string.maintenance_mode);

            } else {    // Generic error
                // Show a Message, operation finished without success
                message = res.getString(R.string.unshare_link_file_error);
            }

        } else if (operation instanceof UpdateShareViaLinkOperation ||
                operation instanceof UpdateSharePermissionsOperation) {

            if (result.getData() != null && result.getData().size() > 0) {
                message = (String) result.getData().get(0);     // share API sends its own error messages

            } else if (result.getCode() == ResultCode.SHARE_NOT_FOUND) {
                message = res.getString(R.string.update_link_file_no_exist);

            } else if (result.getCode() == ResultCode.SHARE_FORBIDDEN) {
                // Error --> No permissions
                message = String.format(res.getString(R.string.forbidden_permissions),
                        res.getString(R.string.update_link_forbidden_permissions));

            } else if (result.getCode() == ResultCode.MAINTENANCE_MODE) {
                message = res.getString(R.string.maintenance_mode);

            } else {    // Generic error
                // Show a Message, operation finished without success
                message = res.getString(R.string.update_link_file_error);
            }

        } else if (operation instanceof MoveFileOperation) {

            if (result.getCode() == ResultCode.FILE_NOT_FOUND) {
                message = res.getString(R.string.move_file_not_found);
            } else if (result.getCode() == ResultCode.INVALID_MOVE_INTO_DESCENDANT) {
                message = res.getString(R.string.move_file_invalid_into_descendent);

            } else if (result.getCode() == ResultCode.INVALID_OVERWRITE) {
                message = res.getString(R.string.move_file_invalid_overwrite);

            } else if (result.getCode() == ResultCode.FORBIDDEN) {
                message = String.format(res.getString(R.string.forbidden_permissions),
                        res.getString(R.string.forbidden_permissions_move));

            } else if (result.getCode() == ResultCode.INVALID_CHARACTER_DETECT_IN_SERVER) {
                message = res.getString(R.string.filename_forbidden_charaters_from_server);

            } else if (result.getCode() == ResultCode.MAINTENANCE_MODE) {
                message = res.getString(R.string.maintenance_mode);

            } else {    // Generic error
                // Show a Message, operation finished without success
                message = res.getString(R.string.move_file_error);
            }

        } else if (operation instanceof SynchronizeFolderOperation) {

            if (!result.isSuccess()) {
                String folderPathName = new File(
                        ((SynchronizeFolderOperation) operation).getFolderPath()).getName();
                if (result.getCode() == ResultCode.FILE_NOT_FOUND) {
                    message = String.format(res.getString(R.string.sync_current_folder_was_removed),
                            folderPathName);

                } else if (result.getCode() == ResultCode.MAINTENANCE_MODE) {
                    message = res.getString(R.string.maintenance_mode);

                } else {    // Generic error
                    // Show a Message, operation finished without success
                    message = String.format(res.getString(R.string.sync_folder_failed_content),
                            folderPathName);
                }
            }

        } else if (operation instanceof CopyFileOperation) {
            if (result.getCode() == ResultCode.FILE_NOT_FOUND) {
                message = res.getString(R.string.copy_file_not_found);

            } else if (result.getCode() == ResultCode.INVALID_COPY_INTO_DESCENDANT) {
                message = res.getString(R.string.copy_file_invalid_into_descendent);

            } else if (result.getCode() == ResultCode.INVALID_OVERWRITE) {
                message = res.getString(R.string.copy_file_invalid_overwrite);

            } else if (result.getCode() == ResultCode.FORBIDDEN) {
                message = String.format(res.getString(R.string.forbidden_permissions),
                        res.getString(R.string.forbidden_permissions_copy));

            } else if (result.getCode() == ResultCode.MAINTENANCE_MODE) {
                message = res.getString(R.string.maintenance_mode);

            } else {    // Generic error
                // Show a Message, operation finished without success
                message = res.getString(R.string.copy_file_error);
            }
        }

        return message;
    }

    private static String getErrorMessage(RemoteOperationResult result, Resources res) {

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
            }
        }

        return message;
    }

    private static boolean isNetworkError(RemoteOperationResult.ResultCode code) {
        return code == ResultCode.WRONG_CONNECTION ||
                code == ResultCode.TIMEOUT ||
                code == ResultCode.HOST_NOT_AVAILABLE;
    }
}
