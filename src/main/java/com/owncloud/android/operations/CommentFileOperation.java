/*
 * Nextcloud Android client application
 *
 * @author Tobias Kaminsky
 * Copyright (C) 2018 Tobias Kaminsky
 * Copyright (C) 2018 Nextcloud GmbH.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package com.owncloud.android.operations;

import com.owncloud.android.lib.common.OwnCloudClient;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.lib.resources.comments.CommentFileRemoteOperation;
import com.owncloud.android.operations.common.SyncOperation;


/**
 * Comment file
 */
public class CommentFileOperation extends SyncOperation {

    private String message;
    private String fileId;
    private String userId;

    /**
     * Constructor
     *
     * @param message Comment to store
     * @param userId  userId to access correct dav endpoint
     */
    public CommentFileOperation(String message, String fileId, String userId) {
        this.message = message;
        this.fileId = fileId;
        this.userId = userId;
    }

    /**
     * Performs the operation.
     *
     * @param client Client object to communicate with the remote ownCloud server.
     */
    @Override
    protected RemoteOperationResult run(OwnCloudClient client) {
        RemoteOperationResult result = new CommentFileRemoteOperation(message, fileId, userId).execute(client, true);

        if (!result.isSuccess()) {
            Log_OC.e(this, "File with Id " + fileId + " could not be commented");
        }

        return result;
    }
}
