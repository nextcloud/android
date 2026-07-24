/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2018 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2018 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.files;

import android.net.Uri;

import com.owncloud.android.datamodel.Template;
import com.owncloud.android.lib.common.OwnCloudClient;
import com.owncloud.android.lib.common.operations.RemoteOperation;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.ui.dialog.ChooseRichDocumentsTemplateDialogFragment;

import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.GetMethod;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Locale;

public class FetchTemplateOperation extends RemoteOperation {
    private static final String TAG = FetchTemplateOperation.class.getSimpleName();
    private static final int SYNC_READ_TIMEOUT = 40000;
    private static final int SYNC_CONNECTION_TIMEOUT = 5000;
    private static final String TEMPLATE_ROUTE = "ocs/v2.php/apps/richdocuments/api/v1/templates";
    private static final String PARAM_FORMAT = "format";
    private static final String VALUE_FORMAT_JSON = "json";

    private ChooseRichDocumentsTemplateDialogFragment.Type type;

    // JSON node names
    private static final String NODE_OCS = "ocs";
    private static final String NODE_DATA = "data";

    public FetchTemplateOperation(ChooseRichDocumentsTemplateDialogFragment.Type type) {
        this.type = type;
    }

    protected RemoteOperationResult run(OwnCloudClient client) {
        RemoteOperationResult result;
        GetMethod getMethod = null;

        try {
            Uri uri = client.getBaseUri()
                .buildUpon()
                .appendEncodedPath(TEMPLATE_ROUTE)
                .appendEncodedPath(type.toString().toLowerCase(Locale.ENGLISH))
                .appendQueryParameter(PARAM_FORMAT, VALUE_FORMAT_JSON)
                .build();

            getMethod = new GetMethod(uri.toString());

            // remote request
            getMethod.addRequestHeader(OCS_API_HEADER, OCS_API_HEADER_VALUE);

            int status = client.executeMethod(getMethod, SYNC_READ_TIMEOUT, SYNC_CONNECTION_TIMEOUT);

            if (status == HttpStatus.SC_OK) {
                String response = getMethod.getResponseBodyAsString();

                // Parse the response
                JSONObject respJSON = new JSONObject(response);
                JSONArray templates = respJSON.getJSONObject(NODE_OCS).getJSONArray(NODE_DATA);

                ArrayList<Object> templateArray = new ArrayList<>();

                for (int i = 0; i < templates.length(); i++) {
                    JSONObject templateObject = templates.getJSONObject(i);

                    templateArray.add(new Template(templateObject.getLong("id"),
                                                   templateObject.getString("name"),
                                                   templateObject.optString("preview"),
                                                   Template.Type.parse(templateObject.getString("type")
                                                                           .toUpperCase(Locale.ROOT)),
                                                   templateObject.getString("extension")));
                }

                result = new RemoteOperationResult(true, getMethod);
                result.setData(templateArray);
            } else {
                String errorBody = getMethod.getResponseBodyAsString();
                result = new RemoteOperationResult(false, getMethod);
                Log_OC.e(TAG, "Get templates for type " + type + " failed with status " + status + ": " + errorBody);
            }
        } catch (Exception e) {
            result = new RemoteOperationResult(e);
            Log_OC.e(TAG, "Get templates for typ " + type + " failed: " + result.getLogMessage(),
                result.getException());
        } finally {
            if (getMethod != null) {
                getMethod.releaseConnection();
            }
        }
        return result;
    }
}
