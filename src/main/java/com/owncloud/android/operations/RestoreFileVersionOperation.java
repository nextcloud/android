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

import android.util.Log;

import com.owncloud.android.lib.common.OwnCloudClient;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.operations.common.SyncOperation;

import org.apache.commons.httpclient.HttpStatus;
import org.apache.jackrabbit.webdav.client.methods.MoveMethod;

import java.io.IOException;


/**
 * Restore a {@link com.owncloud.android.lib.resources.files.FileVersion}.
 */
public class RestoreFileVersionOperation extends SyncOperation {

    private static final String TAG = RestoreFileVersionOperation.class.getSimpleName();
    private static final int RESTORE_READ_TIMEOUT = 30000;
    private static final int RESTORE_CONNECTION_TIMEOUT = 5000;

    private String fileId;
    private String fileName;
    private String userId;

    /**
     * Constructor
     *
     * @param fileId fileId
     * @param fileName version date in unixtime
     * @param userId userId to access correct dav endpoint
     */
    public RestoreFileVersionOperation(String fileId, String fileName, String userId) {
        this.fileId = fileId;
        this.fileName = fileName;
        this.userId = userId;
    }

    /**
     * Performs the operation.
     *
     * @param client Client object to communicate with the remote ownCloud server.
     */
    @Override
    protected RemoteOperationResult run(OwnCloudClient client) {

        RemoteOperationResult result;
        try {
            String source = client.getNewWebdavUri() + "/versions/" + userId + "/versions/" + fileId + "/" + fileName;
            String target = client.getNewWebdavUri() + "/versions/" + userId + "/restore/" + fileId;

            MoveMethod move = new MoveMethod(source, target, true);
            int status = client.executeMethod(move, RESTORE_READ_TIMEOUT, RESTORE_CONNECTION_TIMEOUT);

            result = new RemoteOperationResult(isSuccess(status), move);

            client.exhaustResponse(move.getResponseBodyAsStream());
        } catch (IOException e) {
            result = new RemoteOperationResult(e);
            Log.e(TAG, "Restore file version with id " + fileId + " failed: " + result.getLogMessage(), e);
        }

        return result;
    }

    private boolean isSuccess(int status) {
        return status == HttpStatus.SC_CREATED || status == HttpStatus.SC_NO_CONTENT;
    }
}
