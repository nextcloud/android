/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-FileCopyrightText: 2019 Chris Narkiewicz <hello@ezaquarii.com>
 * SPDX-FileCopyrightText: 2018 Edvard Holst <edvard.holst@gmail.com>
 * SPDX-FileCopyrightText: 2025 TSI-mc <surinder.kumar@t-systems.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.ui.activities.data.activities;

import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.content.Context;
import android.os.AsyncTask;

import com.nextcloud.client.account.User;
import com.nextcloud.client.account.UserAccountManager;
import com.nextcloud.common.NextcloudClient;
import com.owncloud.android.MainApp;
import com.owncloud.android.R;
import com.owncloud.android.lib.common.OwnCloudAccount;
import com.owncloud.android.lib.common.OwnCloudClientManagerFactory;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.lib.resources.activities.GetActivitiesRemoteOperation;

import org.apache.commons.httpclient.HttpStatus;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Implementation of the Activities Service API that communicates with the NextCloud remote server
 */
public class ActivitiesServiceApiImpl implements ActivitiesServiceApi {

    private static final String TAG = ActivitiesServiceApiImpl.class.getSimpleName();
    private final UserAccountManager accountManager;

    public ActivitiesServiceApiImpl(UserAccountManager accountManager) {
        this.accountManager = accountManager;
    }

    @Override
    public void getAllActivities(long lastGiven, ActivitiesServiceCallback<List<Object>> callback) {
        GetActivityListTask getActivityListTask = new GetActivityListTask(accountManager.getUser(),
                                                                          lastGiven,
                                                                          callback);
        getActivityListTask.execute();
    }

    private static class GetActivityListTask extends AsyncTask<Void, Void, Boolean> {

        private final ActivitiesServiceCallback<List<Object>> callback;
        private List<Object> activities;
        private final User user;
        private long lastGiven;
        private String errorMessage;
        private NextcloudClient client;

        private GetActivityListTask(User user,
                                    long lastGiven,
                                    ActivitiesServiceCallback<List<Object>> callback) {
            this.user = user;
            this.lastGiven = lastGiven;
            this.callback = callback;
            activities = new ArrayList<>();
        }


        @Override
        protected Boolean doInBackground(Void... voids) {
            final Context context = MainApp.getAppContext();
            try {
                final OwnCloudAccount ocAccount = user.toOwnCloudAccount();
                client = OwnCloudClientManagerFactory.getDefaultSingleton().
                    getNextcloudClientFor(ocAccount, MainApp.getAppContext());

                GetActivitiesRemoteOperation getRemoteActivitiesOperation;
                if (lastGiven > 0) {
                    getRemoteActivitiesOperation = new GetActivitiesRemoteOperation(lastGiven);
                } else {
                    getRemoteActivitiesOperation = new GetActivitiesRemoteOperation();
                }

                final RemoteOperationResult result = getRemoteActivitiesOperation.execute(client);

                if (result.isSuccess() && result.getData() != null) {
                    final ArrayList<Object> data = result.getData();
                    activities = (ArrayList) data.get(0);

                    lastGiven = (long) data.get(1);
                    return Boolean.TRUE;
                } else {
                    Log_OC.d(TAG, result.getLogMessage());
                    // show error
                    errorMessage = result.getLogMessage(MainApp.getAppContext());
                    if (result.getHttpCode() == HttpStatus.SC_NOT_MODIFIED) {
                        errorMessage = context.getString(R.string.file_list_empty_headline_server_search);
                    }
                    return Boolean.FALSE;

                }
            } catch (IOException e) {
                Log_OC.e(TAG, "IO error", e);
                errorMessage = "IO error";
            } catch (OperationCanceledException e) {
                Log_OC.e(TAG, "Operation has been canceled", e);
                errorMessage = "Operation has been canceled";
            } catch (AuthenticatorException e) {
                Log_OC.e(TAG, "Authentication Exception", e);
                errorMessage = "Authentication Exception";
            }

            return Boolean.FALSE;
        }

        @Override
        protected void onPostExecute(Boolean success) {
            super.onPostExecute(success);
            if (success) {
                callback.onLoaded(activities, client, lastGiven);
            } else {
                callback.onError(errorMessage);
            }
        }
    }
}
