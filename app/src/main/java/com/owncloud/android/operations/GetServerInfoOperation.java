/**
 *   ownCloud Android client application
 *
 *   @author David A. Velasco
 *   @author masensio
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

import com.owncloud.android.authentication.AuthenticatorUrlUtils;
import com.owncloud.android.lib.common.OwnCloudClient;
import com.owncloud.android.lib.common.operations.RemoteOperation;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.common.operations.RemoteOperationResult.ResultCode;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.lib.resources.status.GetStatusRemoteOperation;
import com.owncloud.android.lib.resources.status.OwnCloudVersion;
import com.owncloud.android.operations.DetectAuthenticationMethodOperation.AuthenticationMethod;

import java.util.ArrayList;
import java.util.Locale;

/**
 * Get basic information from an ownCloud server given its URL.
 *
 * Checks the existence of a configured ownCloud server in the URL, gets its version
 * and finds out what authentication method is needed to access files in it.
 */

public class GetServerInfoOperation extends RemoteOperation {

    private static final String TAG = GetServerInfoOperation.class.getSimpleName();

    private String mUrl;
    private Context mContext;
    private ServerInfo mResultData;

    /**
     * Constructor.
     *
     * @param url               URL to an ownCloud server.
     * @param context           Android context; needed to check network state
     *                          TODO ugly dependency, get rid of it.
     */
    public GetServerInfoOperation(String url, Context context) {
        mUrl = trimWebdavSuffix(url);
        mContext = context;
        mResultData = new ServerInfo();
    }

    /**
     * Performs the operation
     *
     * @return Result of the operation. If successful, includes an instance of
     *              {@link ServerInfo} with the information retrieved from the server.
     *              Call {@link RemoteOperationResult#getData()}.get(0) to get it.
     */
	@Override
	protected RemoteOperationResult run(OwnCloudClient client) {

	    // first: check the status of the server (including its version)
        GetStatusRemoteOperation getStatus = new GetStatusRemoteOperation(mContext);

	    RemoteOperationResult result = getStatus.execute(client);

        if (result.isSuccess()) {
            // second: get authentication method required by the server
            mResultData.mVersion = (OwnCloudVersion) result.getData().get(0);
            mResultData.hasExtendedSupport = (boolean) result.getData().get(1);
            mResultData.mIsSslConn = result.getCode() == ResultCode.OK_SSL;
            mResultData.mBaseUrl = normalizeProtocolPrefix(mUrl, mResultData.mIsSslConn);
            RemoteOperationResult detectAuthResult = detectAuthorizationMethod(client);

            // third: merge results
            if (detectAuthResult.isSuccess()) {
                mResultData.mAuthMethod = (AuthenticationMethod) detectAuthResult.getData().get(0);
                ArrayList<Object> data = new ArrayList<Object>();
                data.add(mResultData);
                result.setData(data);
            } else {
                result = detectAuthResult;
            }
        }
        return result;
	}


    private RemoteOperationResult detectAuthorizationMethod(OwnCloudClient client) {
        Log_OC.d(TAG, "Trying empty authorization to detect authentication method");
        DetectAuthenticationMethodOperation operation =
                new DetectAuthenticationMethodOperation(mContext);
        return operation.execute(client);
    }


    private String trimWebdavSuffix(String url) {
	    String trimmedUrl = url;
        if (trimmedUrl == null) {
            trimmedUrl = "";
        } else {
            if (trimmedUrl.endsWith("/")) {
                trimmedUrl = trimmedUrl.substring(0, trimmedUrl.length() - 1);
            }
            if (trimmedUrl.toLowerCase(Locale.ROOT).endsWith(AuthenticatorUrlUtils.WEBDAV_PATH_4_0_AND_LATER)) {
                trimmedUrl = trimmedUrl.substring(0,
                        trimmedUrl.length() - AuthenticatorUrlUtils.WEBDAV_PATH_4_0_AND_LATER.length());
            }
        }
        return trimmedUrl;
    }


    private String normalizeProtocolPrefix(String url, boolean isSslConn) {
        if (!url.toLowerCase(Locale.ROOT).startsWith("http://") &&
                !url.toLowerCase(Locale.ROOT).startsWith("https://")) {
            if (isSslConn) {
                return "https://" + url;
            } else {
                return "http://" + url;
            }
        }
        return url;
    }


    public static class ServerInfo {
        public OwnCloudVersion mVersion;
        public boolean hasExtendedSupport;
        public String mBaseUrl = "";
        public AuthenticationMethod mAuthMethod = AuthenticationMethod.UNKNOWN;
        public boolean mIsSslConn;
    }

}
