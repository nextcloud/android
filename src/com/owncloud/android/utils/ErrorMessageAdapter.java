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

import android.content.res.Resources;

import com.owncloud.android.R;
import com.owncloud.android.lib.common.operations.RemoteOperation;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.common.operations.RemoteOperationResult.ResultCode;
import com.owncloud.android.operations.DownloadFileOperation;
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
                            ((UploadFileOperation) operation).getFileName());
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
        }
        
        return message;
    }
}
