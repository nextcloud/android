/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2018 Tobias Kaminsky
 * SPDX-FileCopyrightText: 2018 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.operations;

import com.nextcloud.common.NextcloudClient;
import com.owncloud.android.lib.common.operations.RemoteOperation;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.lib.resources.comments.CommentFileRemoteOperation;

/**
 * Comment file
 */
public class CommentFileOperation extends RemoteOperation<Void> {

    private final String message;
    private final long fileId;

    /**
     * Constructor
     *
     * @param message Comment to store
     */
    public CommentFileOperation(String message, long fileId) {
        this.message = message;
        this.fileId = fileId;
    }

    /**
     * Performs the operation.
     *
     * @param client Client object to communicate with the remote ownCloud server.
     */
    @Override
    public RemoteOperationResult<Void> run(NextcloudClient client) {
        RemoteOperationResult<Void> result = new CommentFileRemoteOperation(message, fileId).execute(client);

        if (!result.isSuccess()) {
            Log_OC.e(this, "File with Id " + fileId + " could not be commented");
        }

        return result;
    }
}
