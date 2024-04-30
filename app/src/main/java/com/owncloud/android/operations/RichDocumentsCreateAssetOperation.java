/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2018 Tobias Kaminsky
 * SPDX-FileCopyrightText: 2018 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.operations;

import com.owncloud.android.lib.common.OwnCloudClient;
import com.owncloud.android.lib.common.operations.RemoteOperation;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.common.utils.Log_OC;

import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.Utf8PostMethod;
import org.json.JSONObject;

/**
 * Create asset for RichDocuments app from file, which is already stored on Nextcloud server
 */

public class RichDocumentsCreateAssetOperation extends RemoteOperation {
    private static final String TAG = RichDocumentsCreateAssetOperation.class.getSimpleName();
    private static final int SYNC_READ_TIMEOUT = 40000;
    private static final int SYNC_CONNECTION_TIMEOUT = 5000;
    private static final String ASSET_URL = "/index.php/apps/richdocuments/assets";

    private static final String NODE_URL = "url";
    private static final String PARAMETER_PATH = "path";
    private static final String PARAMETER_FORMAT = "format";
    private static final String PARAMETER_FORMAT_VALUE = "json";

    private String path;

    public RichDocumentsCreateAssetOperation(String path) {
        this.path = path;
    }

    protected RemoteOperationResult run(OwnCloudClient client) {
        RemoteOperationResult result;
        Utf8PostMethod postMethod = null;

        try {
            postMethod = new Utf8PostMethod(client.getBaseUri() + ASSET_URL);
            postMethod.setParameter(PARAMETER_PATH, path);
            postMethod.setParameter(PARAMETER_FORMAT, PARAMETER_FORMAT_VALUE);

            // remote request
            postMethod.addRequestHeader(OCS_API_HEADER, OCS_API_HEADER_VALUE);

            int status = client.executeMethod(postMethod, SYNC_READ_TIMEOUT, SYNC_CONNECTION_TIMEOUT);

            if (status == HttpStatus.SC_OK) {
                String response = postMethod.getResponseBodyAsString();

                // Parse the response
                JSONObject respJSON = new JSONObject(response);
                String url = respJSON.getString(NODE_URL);

                result = new RemoteOperationResult(true, postMethod);
                result.setSingleData(url);
            } else {
                result = new RemoteOperationResult(false, postMethod);
                client.exhaustResponse(postMethod.getResponseBodyAsStream());
            }
        } catch (Exception e) {
            result = new RemoteOperationResult(e);
            Log_OC.e(TAG, "Create asset for richdocuments with path " + path + " failed: " + result.getLogMessage(),
                    result.getException());
        } finally {
            if (postMethod != null) {
                postMethod.releaseConnection();
            }
        }
        return result;
    }
}
