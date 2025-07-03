/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2020 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2015 ownCloud Inc.
 * SPDX-FileCopyrightText: 2015 María Asensio Valverde <masensio@solidgear.es>
 * SPDX-FileCopyrightText: 2012 David A. Velasco <dvelasco@solidgear.es>
 * SPDX-License-Identifier: GPL-2.0-only AND (AGPL-3.0-or-later OR GPL-2.0-only)
 */
package com.owncloud.android.operations;

import android.content.Context;

import com.nextcloud.client.account.User;
import com.owncloud.android.datamodel.FileDataStorageManager;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.datamodel.ThumbnailsCacheManager;
import com.owncloud.android.lib.common.OwnCloudClient;
import com.owncloud.android.lib.common.operations.RemoteOperation;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.common.operations.RemoteOperationResult.ResultCode;
import com.owncloud.android.lib.resources.files.RemoveFileRemoteOperation;
import com.owncloud.android.operations.common.SyncOperation;
import com.owncloud.android.utils.MimeTypeUtil;

/**
 * Remote operation performing the removal of a remote file or folder in the ownCloud server.
 */
public class RemoveFileOperation extends SyncOperation {

    private final OCFile fileToRemove;
    private final boolean onlyLocalCopy;
    private final User user;
    private final boolean inBackground;
    private final Context context;

    /**
     * Constructor
     *
     * @param fileToRemove  OCFile instance describing the remote file or folder to remove from the server
     * @param onlyLocalCopy When 'true', and a local copy of the file exists, only this is removed.
     */
    public RemoveFileOperation(OCFile fileToRemove,
                               boolean onlyLocalCopy,
                               User user,
                               boolean inBackground,
                               Context context,
                               FileDataStorageManager storageManager) {
        super(storageManager);

        this.fileToRemove = fileToRemove;
        this.onlyLocalCopy = onlyLocalCopy;
        this.user = user;
        this.inBackground = inBackground;
        this.context = context;
    }

    /**
     * Getter for the file to remove (or removed, if the operation was successfully performed).
     *
     * @return      File to remove or already removed.
     */
    public OCFile getFile() {
        return fileToRemove;
    }

    public boolean isInBackground() {
        return inBackground;
    }

    /**
     * Performs the remove operation
     *
     * @param   client      Client object to communicate with the remote ownCloud server.
     */
    @Override
    protected RemoteOperationResult run(OwnCloudClient client) {
        RemoteOperationResult result = null;
        RemoteOperation operation;

        if (MimeTypeUtil.isImage(fileToRemove.getMimeType())) {
            // store resized image
            ThumbnailsCacheManager.generateResizedImage(fileToRemove);
        }

        boolean localRemovalFailed = false;
        if (!onlyLocalCopy) {
            if (fileToRemove.isEncrypted()) {
                OCFile parent = getStorageManager().getFileById(fileToRemove.getParentId());
                if (parent == null) {
                    return new RemoteOperationResult(ResultCode.LOCAL_FILE_NOT_FOUND);
                }

                operation = new RemoveRemoteEncryptedFileOperation(fileToRemove.getRemotePath(),
                                                                   user,
                                                                   context,
                                                                   fileToRemove.getEncryptedFileName(),
                                                                   parent,
                                                                   fileToRemove.isFolder());
            } else {
                operation = new RemoveFileRemoteOperation(fileToRemove.getRemotePath());
            }
            result = operation.execute(client);
            if (result.isSuccess() || result.getCode() == ResultCode.FILE_NOT_FOUND) {
                if (fileToRemove.isFolder()) {
                    localRemovalFailed = !(getStorageManager().removeFolder(fileToRemove, true, true));
                } else {
                    localRemovalFailed = !(getStorageManager().removeFile(fileToRemove, true, true));
                }
            }
        } else {
            if (fileToRemove.isFolder()) {
                localRemovalFailed = !(getStorageManager().removeFolder(fileToRemove, false, true));
            } else {
                localRemovalFailed = !(getStorageManager().removeFile(fileToRemove, false, true));
            }
            if (!localRemovalFailed) {
                result = new RemoteOperationResult(ResultCode.OK);
            }
        }

        if (localRemovalFailed) {
            result = new RemoteOperationResult(ResultCode.LOCAL_STORAGE_NOT_REMOVED);
        }

        return result;
    }
}
