/**
 * ownCloud Android client application
 *
 *   @author David A. Velasco
 *   Copyright (C) 2015 ownCloud Inc.
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

package com.owncloud.android.operations;

import android.content.Context;
import android.net.Uri;
import android.text.TextUtils;

import com.owncloud.android.lib.common.OwnCloudClient;
import com.owncloud.android.lib.common.operations.RemoteOperation;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.lib.resources.files.ExistenceCheckRemoteOperation;

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpStatus;

import java.util.ArrayList;
import java.util.Locale;

/**
 * Operation to find out what authentication method requires the server to access files.
 *
 * Basically, tries to access to the root folder without authorization and analyzes the response.
 *
 * When successful, the instance of {@link RemoteOperationResult} passed through
 * {@link com.owncloud.android.lib.common.operations.OnRemoteOperationListener
 * #onRemoteOperationFinish(RemoteOperation, RemoteOperationResult)} returns in
 * {@link RemoteOperationResult#getData()} a value of {@link AuthenticationMethod}.
 */
public class DetectAuthenticationMethodOperation extends RemoteOperation {

    private static final String TAG = DetectAuthenticationMethodOperation.class.getSimpleName();

    public enum AuthenticationMethod {
        UNKNOWN,
        NONE,
        BASIC_HTTP_AUTH,
        SAML_WEB_SSO,
        BEARER_TOKEN
    }

    private Context mContext;

    /**
     * Constructor
     *
     * @param context Android context of the caller.
     */
    public DetectAuthenticationMethodOperation(Context context) {
        mContext = context;
    }


    /**
     *  Performs the operation.
     *
     *  Triggers a check of existence on the root folder of the server, granting
     *  that the request is not authenticated.
     *
     *  Analyzes the result of check to find out what authentication method, if
     *  any, is requested by the server.
     */
    @Override
    protected RemoteOperationResult run(OwnCloudClient client) {
        RemoteOperationResult result = null;
        AuthenticationMethod authMethod = AuthenticationMethod.UNKNOWN;

        RemoteOperation operation = new ExistenceCheckRemoteOperation("", mContext, false);
        client.clearCredentials();
        client.setFollowRedirects(false);

        // try to access the root folder, following redirections but not SAML SSO redirections
        result = operation.execute(client);
        String redirectedLocation = result.getRedirectedLocation();
        while (!TextUtils.isEmpty(redirectedLocation) && !result.isIdPRedirection()) {
            client.setBaseUri(Uri.parse(result.getRedirectedLocation()));
            result = operation.execute(client);
            redirectedLocation = result.getRedirectedLocation();
        }

        // analyze response
        if (result.getHttpCode() == HttpStatus.SC_UNAUTHORIZED || result.getHttpCode() == HttpStatus.SC_FORBIDDEN) {
            ArrayList<String> authHeaders = result.getAuthenticateHeaders();

            for (String header : authHeaders) {
                // currently we only support basic auth
                if (header.toLowerCase(Locale.ROOT).contains("basic")) {
                    authMethod = AuthenticationMethod.BASIC_HTTP_AUTH;
                    break;
                }
            }
            // else - fall back to UNKNOWN

        } else if (result.isSuccess()) {
            authMethod = AuthenticationMethod.NONE;

        } else if (result.isIdPRedirection()) {
            authMethod = AuthenticationMethod.SAML_WEB_SSO;
        }
        // else - fall back to UNKNOWN
        Log_OC.d(TAG, "Authentication method found: " + authenticationMethodToString(authMethod));

        if (authMethod != AuthenticationMethod.UNKNOWN) {
            result = new RemoteOperationResult(true, result.getHttpCode(), result.getHttpPhrase(), new Header[0]);
        }
        ArrayList<Object> data = new ArrayList<>();
        data.add(authMethod);
        result.setData(data);
        return result;  // same result instance, so that other errors
        // can be handled by the caller transparently
    }

    private String authenticationMethodToString(AuthenticationMethod value) {
        switch (value) {
            case NONE:
                return "NONE";
            case BASIC_HTTP_AUTH:
                return "BASIC_HTTP_AUTH";
            case BEARER_TOKEN:
                return "BEARER_TOKEN";
            case SAML_WEB_SSO:
                return "SAML_WEB_SSO";
            default:
                return "UNKNOWN";
        }
    }

}
