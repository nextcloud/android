/* ownCloud Android Library is available under MIT license
 *   Copyright (C) 2015 ownCloud Inc.
 *
 *   Permission is hereby granted, free of charge, to any person obtaining a copy
 *   of this software and associated documentation files (the "Software"), to deal
 *   in the Software without restriction, including without limitation the rights
 *   to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *   copies of the Software, and to permit persons to whom the Software is
 *   furnished to do so, subject to the following conditions:
 *
 *   The above copyright notice and this permission notice shall be included in
 *   all copies or substantial portions of the Software.
 *
 *   THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 *   EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 *   MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 *   NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS
 *   BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN
 *   ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 *   CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 *   THE SOFTWARE.
 *
 */

package com.owncloud.android;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.text.TextUtils;

import com.owncloud.android.lib.common.OwnCloudClient;
import com.owncloud.android.lib.common.OwnCloudClientManagerFactory;
import com.owncloud.android.lib.common.accounts.AccountUtils;
import com.owncloud.android.lib.common.operations.RemoteOperation;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.lib.resources.status.OwnCloudVersion;

import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.params.HttpMethodParams;
import org.apache.commons.httpclient.params.HttpParams;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

/**
 * Checks if the server is valid and if the server supports the Share API
 *
 * @author David A. Velasco
 * @author masensio
 */
public class BaseUrlRemoteOperation extends RemoteOperation {

    /**
     * Maximum time to wait for a response from the server when the connection is being tested, in MILLISECONDs.
     */
    private static final int TRY_CONNECTION_TIMEOUT = 5000;

    private static final String TAG = BaseUrlRemoteOperation.class.getSimpleName();

    private static final String NODE_INSTALLED = "installed";
    private static final String NODE_VERSION = "version";
    private static final String NODE_EXTENDED_SUPPORT = "extendedSupport";
    private static final String PROTOCOL_HTTPS = "https://";
    private static final String PROTOCOL_HTTP = "http://";
    private static final int UNTRUSTED_DOMAIN_ERROR_CODE = 15;
    public static String initBaseUrl;

    private RemoteOperationResult mLatestResult;
    private Context mContext;
    private OnBaseUrlChange listener;

    public BaseUrlRemoteOperation(Context context,OnBaseUrlChange listener) {
        mContext = context;
        this.listener = listener;
    }

    public interface OnBaseUrlChange{

        void onBaseUrlChange(String baseUrl);
    }

    private boolean tryConnection(OwnCloudClient client) {
        boolean retval = false;
        GetMethod get = null;
        HttpParams params;
        boolean isRedirectToNonSecureConnection;
        int status;
        String redirectedLocation;
        String response;
        String baseUrlSt = client.getBaseUri().toString();
        try {
            get = new GetMethod(baseUrlSt + "/apps/registration/get_inner_ip");

            params = HttpMethodParams.getDefaultParams();
            params.setParameter(HttpMethodParams.USER_AGENT, OwnCloudClientManagerFactory.getUserAgent());
            get.getParams().setDefaults(params);

            client.setFollowRedirects(false);
            isRedirectToNonSecureConnection = false;
            status = client.executeMethod(get, TRY_CONNECTION_TIMEOUT, TRY_CONNECTION_TIMEOUT);
            mLatestResult = new RemoteOperationResult((status == HttpStatus.SC_OK), get);

            redirectedLocation = mLatestResult.getRedirectedLocation();
            while (redirectedLocation != null && redirectedLocation.length() > 0
                && !mLatestResult.isSuccess()) {

                isRedirectToNonSecureConnection |= (
                    baseUrlSt.startsWith(PROTOCOL_HTTPS) &&
                        redirectedLocation.startsWith(PROTOCOL_HTTP)
                );
                get.releaseConnection();
                get = new GetMethod(redirectedLocation);
                status = client.executeMethod(get, TRY_CONNECTION_TIMEOUT, TRY_CONNECTION_TIMEOUT);
                mLatestResult = new RemoteOperationResult((status == HttpStatus.SC_OK), get);
                redirectedLocation = mLatestResult.getRedirectedLocation();
            }

            response = get.getResponseBodyAsString();

            if (status == HttpStatus.SC_OK) {
                JSONObject json = new JSONObject(response);
                String newBaseUrl = "http://" + json.getString("result");
                get = new GetMethod(newBaseUrl + "/apps/registration/get_status");
                params = HttpMethodParams.getDefaultParams();
                params.setParameter(HttpMethodParams.USER_AGENT, OwnCloudClientManagerFactory.getUserAgent());
                get.getParams().setDefaults(params);

                client.setFollowRedirects(false);
                isRedirectToNonSecureConnection = false;
                status = client.executeMethod(get, TRY_CONNECTION_TIMEOUT, TRY_CONNECTION_TIMEOUT);
                mLatestResult = new RemoteOperationResult((status == HttpStatus.SC_OK), get);

                redirectedLocation = mLatestResult.getRedirectedLocation();
                while (redirectedLocation != null && redirectedLocation.length() > 0
                    && !mLatestResult.isSuccess()) {

                    isRedirectToNonSecureConnection |= (
                        baseUrlSt.startsWith(PROTOCOL_HTTPS) &&
                            redirectedLocation.startsWith(PROTOCOL_HTTP)
                    );
                    get.releaseConnection();
                    get = new GetMethod(redirectedLocation);
                    status = client.executeMethod(get, TRY_CONNECTION_TIMEOUT, TRY_CONNECTION_TIMEOUT);
                    mLatestResult = new RemoteOperationResult((status == HttpStatus.SC_OK), get);
                    redirectedLocation = mLatestResult.getRedirectedLocation();
                }
                if (status == HttpStatus.SC_OK) {
                    if (TextUtils.isEmpty(initBaseUrl)) {
                        initBaseUrl = baseUrlSt;
                    }
                    client.setBaseUri(Uri.parse(newBaseUrl));
                    listener.onBaseUrlChange(newBaseUrl);
                }
            } else if (status == HttpStatus.SC_BAD_REQUEST) {
                try {
                    JSONObject json = new JSONObject(response);

                    if (json.getInt("code") == UNTRUSTED_DOMAIN_ERROR_CODE) {
                        mLatestResult = new RemoteOperationResult(RemoteOperationResult.ResultCode.UNTRUSTED_DOMAIN);
                    } else {
                        mLatestResult = new RemoteOperationResult(false, status, get.getResponseHeaders());
                    }
                } catch (JSONException e) {
                    mLatestResult = new RemoteOperationResult(false, status, get.getResponseHeaders());
                }
            } else {
                mLatestResult = new RemoteOperationResult(false, status, get.getResponseHeaders());
            }

        } catch (Exception e) {
            mLatestResult = new RemoteOperationResult(e);

        } finally {
            if (get != null) {
                get.releaseConnection();
            }
        }

        if (mLatestResult.isSuccess()) {
            Log_OC.i(TAG, "Connection check at " + baseUrlSt + ": " + mLatestResult.getLogMessage());

        } else if (mLatestResult.getException() != null) {
            Log_OC.e(TAG, "Connection check at " + baseUrlSt + ": " + mLatestResult.getLogMessage(),
                     mLatestResult.getException());

        } else {
            Log_OC.e(TAG, "Connection check at " + baseUrlSt + ": " + mLatestResult.getLogMessage());
        }

        return retval;
    }

    private boolean isOnline() {
        ConnectivityManager cm = (ConnectivityManager) mContext
            .getSystemService(Context.CONNECTIVITY_SERVICE);
        return cm != null && cm.getActiveNetworkInfo() != null
            && cm.getActiveNetworkInfo().isConnectedOrConnecting();
    }

    @Override
    protected RemoteOperationResult run(OwnCloudClient client) {
        if (!isOnline()) {
            return new RemoteOperationResult(RemoteOperationResult.ResultCode.NO_NETWORK_CONNECTION);
        }
        String baseUriStr = client.getBaseUri().toString();
        if (baseUriStr.startsWith(PROTOCOL_HTTP) || baseUriStr.startsWith(PROTOCOL_HTTPS)) {
            tryConnection(client);

        } else {
            client.setBaseUri(Uri.parse(PROTOCOL_HTTPS + baseUriStr));
            boolean httpsSuccess = tryConnection(client);
            if (!httpsSuccess && !mLatestResult.isSslRecoverableException()) {
                Log_OC.d(TAG, "establishing secure connection failed, trying non secure connection");
                client.setBaseUri(Uri.parse(PROTOCOL_HTTP + baseUriStr));
                tryConnection(client);
            }
        }
        return mLatestResult;
    }

}
