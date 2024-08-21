/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2018 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2018 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.files;

import com.owncloud.android.lib.common.OwnCloudClient;
import com.owncloud.android.lib.common.operations.RemoteOperation;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.common.utils.Log_OC;

import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.Utf8PostMethod;
import org.json.JSONObject;

public class CreateFileFromTemplateOperation extends RemoteOperation<String> {
    private static final String TAG = CreateFileFromTemplateOperation.class.getSimpleName();
    private static final int SYNC_READ_TIMEOUT = 40000;
    private static final int SYNC_CONNECTION_TIMEOUT = 5000;
    private static final String NEW_FROM_TEMPLATE_URL = "/ocs/v2.php/apps/richdocuments/api/v1/templates/new";

    private String path;
    private long templateId;

    // JSON node names
    private static final String NODE_OCS = "ocs";
    private static final String NODE_DATA = "data";
    private static final String JSON_FORMAT = "?format=json";

    public CreateFileFromTemplateOperation(String path, long templateId) {
        this.path = path;
        this.templateId = templateId;
    }

    protected RemoteOperationResult<String> run(OwnCloudClient client) {
        RemoteOperationResult<String> result;
        Utf8PostMethod postMethod = null;

        try {

            postMethod = new Utf8PostMethod(client.getBaseUri() + NEW_FROM_TEMPLATE_URL + JSON_FORMAT);
            postMethod.setParameter("path", path);
            postMethod.setParameter("template", String.valueOf(templateId));

            // remote request
            postMethod.addRequestHeader(OCS_API_HEADER, OCS_API_HEADER_VALUE);

            int status = client.executeMethod(postMethod, SYNC_READ_TIMEOUT, SYNC_CONNECTION_TIMEOUT);

            if (status == HttpStatus.SC_OK) {
                String response = postMethod.getResponseBodyAsString();

                // Parse the response
                JSONObject respJSON = new JSONObject(response);
                String url = respJSON.getJSONObject(NODE_OCS).getJSONObject(NODE_DATA).getString("url");

                result = new RemoteOperationResult<>(true, postMethod);
                result.setResultData(url);
            } else {
                result = new RemoteOperationResult<>(false, postMethod);
                client.exhaustResponse(postMethod.getResponseBodyAsStream());
            }
        } catch (Exception e) {
            result = new RemoteOperationResult<>(e);
            Log_OC.e(TAG, "Create file from template " + templateId + " failed: " + result.getLogMessage(),
                    result.getException());
        } finally {
            if (postMethod != null) {
                postMethod.releaseConnection();
            }
        }
        return result;
    }
}
