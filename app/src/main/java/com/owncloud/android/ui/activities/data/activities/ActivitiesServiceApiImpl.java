/*
 *   Nextcloud Android client application
 *
 *   @author Edvard Holst
 *   @author Chris Narkiewicz
 *
 *   Copyright (C) 2018 Edvard Holst
 *   Copyright (C) 2019 Chris Narkiewicz <hello@ezaquarii.com>
 *
 *   This program is free software; you can redistribute it and/or
 *   modify it under the terms of the GNU AFFERO GENERAL PUBLIC LICENSE
 *   License as published by the Free Software Foundation; either
 *   version 3 of the License, or any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU AFFERO GENERAL PUBLIC LICENSE for more details.
 *
 *   You should have received a copy of the GNU Affero General Public
 *   License along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.owncloud.android.ui.activities.data.activities;

import android.accounts.Account;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.content.Context;
import android.os.AsyncTask;

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
    private UserAccountManager accountManager;

    public ActivitiesServiceApiImpl(UserAccountManager accountManager) {
        this.accountManager = accountManager;
    }

    @Override
    public void getAllActivities(int lastGiven, ActivitiesServiceCallback<List<Object>> callback) {
        GetActivityListTask getActivityListTask = new GetActivityListTask(accountManager.getUser().toPlatformAccount(),
                                                                          lastGiven,
                                                                          callback);
        getActivityListTask.execute();
    }

    private static class GetActivityListTask extends AsyncTask<Void, Void, Boolean> {

        private final ActivitiesServiceCallback<List<Object>> callback;
        private List<Object> activities;
        private Account account;
        private int lastGiven;
        private String errorMessage;
        private NextcloudClient client;

        private GetActivityListTask(Account account,
                                    int lastGiven,
                                    ActivitiesServiceCallback<List<Object>> callback) {
            this.account = account;
            this.lastGiven = lastGiven;
            this.callback = callback;
            activities = new ArrayList<>();
        }


        @Override
        protected Boolean doInBackground(Void... voids) {
            final Context context = MainApp.getAppContext();
            OwnCloudAccount ocAccount;
            try {
                ocAccount = new OwnCloudAccount(account, context);
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

                    lastGiven = (int) data.get(1);
                    return Boolean.TRUE;
                } else {
                    Log_OC.d(TAG, result.getLogMessage());
                    // show error
                    errorMessage = result.getLogMessage();
                    if (result.getHttpCode() == HttpStatus.SC_NOT_MODIFIED) {
                        errorMessage = context.getString(R.string.file_list_empty_headline_server_search);
                    }
                    return Boolean.FALSE;

                }
            } catch (com.owncloud.android.lib.common.accounts.AccountUtils.AccountNotFoundException e) {
                Log_OC.e(TAG, "Account not found", e);
                errorMessage = "Account not found";
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
