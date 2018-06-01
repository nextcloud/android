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

import com.google.gson.GsonBuilder;
import com.owncloud.android.lib.common.OwnCloudClient;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.operations.common.SyncOperation;

import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;


/**
 * Restore a {@link com.owncloud.android.lib.resources.files.FileVersion}.
 */
public class CommentFileOperation extends SyncOperation {

    private static final String TAG = CommentFileOperation.class.getSimpleName();
    private static final int POST_READ_TIMEOUT = 30000;
    private static final int POST_CONNECTION_TIMEOUT = 5000;

    private static final String ACTOR_ID = "actorId";
    private static final String ACTOR_TYPE = "actorType";
    private static final String ACTOR_TYPE_VALUE = "users";
    private static final String VERB = "verb";
    private static final String VERB_VALUE = "comment";
    private static final String MESSAGE = "message";

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

        RemoteOperationResult result;
        try {
            String url = client.getNewWebdavUri(false) + "/comments/files/" + fileId;
            PostMethod postMethod = new PostMethod(url);
            postMethod.addRequestHeader("Content-type", "application/json");

            Map<String, String> values = new HashMap<>();
            values.put(ACTOR_ID, userId);
            values.put(ACTOR_TYPE, ACTOR_TYPE_VALUE);
            values.put(VERB, VERB_VALUE);
            values.put(MESSAGE, message);

            String json = new GsonBuilder().create().toJson(values, Map.class);

            postMethod.setRequestEntity(new StringRequestEntity(json));

            int status = client.executeMethod(postMethod, POST_READ_TIMEOUT, POST_CONNECTION_TIMEOUT);

            result = new RemoteOperationResult(isSuccess(status), postMethod);

            client.exhaustResponse(postMethod.getResponseBodyAsStream());
        } catch (IOException e) {
            result = new RemoteOperationResult(e);
            Log.e(TAG, "Post comment to file with id " + fileId + " failed: " + result.getLogMessage(), e);
        }

        return result;
    }

    private boolean isSuccess(int status) {
        return status == HttpStatus.SC_CREATED;
    }
}