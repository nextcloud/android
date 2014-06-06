/* ownCloud Android client application
 *   Copyright (C) 2014 ownCloud Inc.
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

import java.io.File;
import java.net.SocketTimeoutException;
import org.apache.commons.httpclient.ConnectTimeoutException;
import android.content.res.Resources;

import com.owncloud.android.R;
import com.owncloud.android.lib.common.operations.RemoteOperation;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.common.operations.RemoteOperationResult.ResultCode;
import com.owncloud.android.operations.CreateFolderOperation;
import com.owncloud.android.operations.CreateShareOperation;
import com.owncloud.android.operations.DownloadFileOperation;
import com.owncloud.android.operations.RemoveFileOperation;
import com.owncloud.android.operations.RenameFileOperation;
import com.owncloud.android.operations.SynchronizeFileOperation;
import com.owncloud.android.operations.UnshareLinkOperation;
import com.owncloud.android.operations.UploadFileOperation;

/**
 * Class to choose proper error messages to show to the user depending on the results of operations, always following the same policy
 * 
 * @author masensio
 *
 */

public class ErrorMessageAdapter {

    public ErrorMessageAdapter() {
        
    }

    public static String getErrorCauseMessage(RemoteOperationResult result, RemoteOperation operation, Resources res) {
        
        String message = null;
        
        if (operation instanceof UploadFileOperation) {
            
            if (result.isSuccess()) {
                message = String.format(res.getString(R.string.uploader_upload_succeeded_content_single), 
                        ((UploadFileOperation) operation).getFileName());
            } else {
                if (result.getCode() == ResultCode.LOCAL_STORAGE_FULL
                        || result.getCode() == ResultCode.LOCAL_STORAGE_NOT_COPIED) {
                    message = String.format(res.getString(R.string.error__upload__local_file_not_copied), 
                            ((UploadFileOperation) operation).getFileName(), 
                            res.getString(R.string.app_name));
                    
                } else if (result.getCode() == ResultCode.QUOTA_EXCEEDED) {
                    message = res.getString(R.string.failed_upload_quota_exceeded_text);
                    
                } else {
                    message = String.format(res.getString(R.string.uploader_upload_failed_content_single), 
                            ((UploadFileOperation) operation).getFileName());
                }
            }
            
        } else if (operation instanceof DownloadFileOperation) {
            
            if (result.isSuccess()) {
                message = String.format(res.getString(R.string.downloader_download_succeeded_content), 
                        new File(((DownloadFileOperation) operation).getSavePath()).getName());
                
            } else {
                message = String.format(res.getString(R.string.downloader_download_failed_content), 
                        new File(((DownloadFileOperation) operation).getSavePath()).getName());
            }
            
        } else if (operation instanceof RemoveFileOperation) {
            if (result.isSuccess()) {
                message = res.getString(R.string.remove_success_msg);
                
            } else {
                if (isNetworkError(result.getCode())) {
                    message = getErrorMessage(result, res);
                    
                } else {
                    message = res.getString(R.string.remove_fail_msg);
                }
            }

        } else if (operation instanceof RenameFileOperation) {
            if (result.getCode().equals(ResultCode.INVALID_LOCAL_FILE_NAME)) {
                message = res.getString(R.string.rename_local_fail_msg);
                
            } if (result.getCode().equals(ResultCode.INVALID_CHARACTER_IN_NAME)) {
                message = res.getString(R.string.filename_forbidden_characters);
                
            } else if (isNetworkError(result.getCode())) {
                message = getErrorMessage(result, res);
                
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
                
            } else if (isNetworkError(result.getCode())) {
                message = getErrorMessage(result, res);
                
            } else {
                message = res.getString(R.string.create_dir_fail_msg);
            }
        } else if (operation instanceof CreateShareOperation) {        
            if (result.getCode() == ResultCode.SHARE_NOT_FOUND)  {        // Error --> SHARE_NOT_FOUND
                message = res.getString(R.string.share_link_file_no_exist);
                
            } else if (isNetworkError(result.getCode())) {
                message = getErrorMessage(result, res);
                
            } else {    // Generic error
                // Show a Message, operation finished without success
                message = res.getString(R.string.share_link_file_error);
            }
            
        } else if (operation instanceof UnshareLinkOperation) {
        
            if (result.getCode() == ResultCode.SHARE_NOT_FOUND)  {        // Error --> SHARE_NOT_FOUND
                message = res.getString(R.string.unshare_link_file_no_exist);
                
            } else if (isNetworkError(result.getCode())) {
                message = getErrorMessage(result, res);
                
            } else {    // Generic error
                // Show a Message, operation finished without success
                message = res.getString(R.string.unshare_link_file_error);
            }
        }
        
        return message;
    }
    
    private static String getErrorMessage(RemoteOperationResult result , Resources res) {
        
        String message = null;
        
        if (!result.isSuccess()) {
            
            if (result.getCode() == ResultCode.WRONG_CONNECTION) {
                message = res.getString(R.string.network_error_socket_exception);
                
            } else if (result.getCode() == ResultCode.TIMEOUT) {
                message = res.getString(R.string.network_error_socket_exception);
                
                if (result.getException() instanceof SocketTimeoutException) {
                    message = res.getString(R.string.network_error_socket_timeout_exception);
                } else if(result.getException() instanceof ConnectTimeoutException) {
                    message = res.getString(R.string.network_error_connect_timeout_exception);
                } 
                
            } else if (result.getCode() == ResultCode.HOST_NOT_AVAILABLE) {
                message = res.getString(R.string.network_host_not_available);
            }
        }
        
        return message;
    }
    
    private static boolean isNetworkError(RemoteOperationResult.ResultCode code) {
        if (code == ResultCode.WRONG_CONNECTION || 
                code == ResultCode.TIMEOUT || 
                code == ResultCode.HOST_NOT_AVAILABLE) {
            return true;
        }
        else
            return false;
    }
}
