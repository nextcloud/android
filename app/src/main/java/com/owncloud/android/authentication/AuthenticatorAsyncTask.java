/*
 *   ownCloud Android client application
 *
 *   @author masensio on 09/02/2015.
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
package com.owncloud.android.authentication;

import android.app.Activity;
import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;

import com.nextcloud.common.NextcloudClient;
import com.owncloud.android.lib.common.OwnCloudClient;
import com.owncloud.android.lib.common.OwnCloudClientFactory;
import com.owncloud.android.lib.common.OwnCloudCredentials;
import com.owncloud.android.lib.common.UserInfo;
import com.owncloud.android.lib.common.network.RedirectionPath;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.resources.files.ExistenceCheckRemoteOperation;
import com.owncloud.android.lib.resources.users.GetUserInfoRemoteOperation;

import java.lang.ref.WeakReference;

import static com.owncloud.android.datamodel.OCFile.ROOT_PATH;


/**
 * Async Task to verify the credentials of a user
 */
public class AuthenticatorAsyncTask extends AsyncTask<Object, Void, RemoteOperationResult<UserInfo>> {

    private static final boolean SUCCESS_IF_ABSENT = false;

    private final WeakReference<Context> mWeakContext;
    private final WeakReference<OnAuthenticatorTaskListener> mListener;

    public AuthenticatorAsyncTask(Activity activity) {
        mWeakContext = new WeakReference<>(activity.getApplicationContext());
        mListener = new WeakReference<>((OnAuthenticatorTaskListener) activity);
    }

    @Override
    protected RemoteOperationResult<UserInfo> doInBackground(Object... params) {

        RemoteOperationResult<UserInfo> result;
        if (params != null && params.length == 2 && mWeakContext.get() != null) {
            String url = (String) params[0];
            Context context = mWeakContext.get();
            OwnCloudCredentials credentials = (OwnCloudCredentials) params[1];

            // Client
            Uri uri = Uri.parse(url);
            NextcloudClient nextcloudClient = OwnCloudClientFactory.createNextcloudClient(uri,
                                                                                          credentials.getUsername(),
                                                                                          credentials.toOkHttpCredentials(),
                                                                                          context,
                                                                                          true);


            // Operation - get display name
            RemoteOperationResult<UserInfo> userInfoResult = new GetUserInfoRemoteOperation().execute(nextcloudClient);

            // Operation - try credentials
            if (userInfoResult.isSuccess()) {
                OwnCloudClient client = OwnCloudClientFactory.createOwnCloudClient(uri, context, true);
                client.setUserId(userInfoResult.getResultData().getId());
                client.setCredentials(credentials);

                ExistenceCheckRemoteOperation operation = new ExistenceCheckRemoteOperation(ROOT_PATH, SUCCESS_IF_ABSENT);
                result = operation.execute(client);

                if (operation.wasRedirected()) {
                    RedirectionPath redirectionPath = operation.getRedirectionPath();
                    String permanentLocation = redirectionPath.getLastPermanentLocation();
                    result.setLastPermanentLocation(permanentLocation);
                }

                result.setResultData(userInfoResult.getResultData());
            } else {
                result = userInfoResult;
            }
        } else {
            result = new RemoteOperationResult(RemoteOperationResult.ResultCode.UNKNOWN_ERROR);
        }

        return result;
    }

    @Override
    protected void onPostExecute(RemoteOperationResult<UserInfo> result) {

        if (result != null) {
            OnAuthenticatorTaskListener listener = mListener.get();
            if (listener != null) {
                listener.onAuthenticatorTaskCallback(result);
            }
        }
    }
    /*
     * Interface to retrieve data from recognition task
     */
    public interface OnAuthenticatorTaskListener{

        void onAuthenticatorTaskCallback(RemoteOperationResult<UserInfo> result);
    }
}
