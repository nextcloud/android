package com.owncloud.android.ui.activities.data;

import android.accounts.Account;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.content.Context;
import android.os.AsyncTask;
import android.view.View;

import com.owncloud.android.MainApp;
import com.owncloud.android.R;
import com.owncloud.android.authentication.AccountUtils;
import com.owncloud.android.lib.common.OwnCloudAccount;
import com.owncloud.android.lib.common.OwnCloudClient;
import com.owncloud.android.lib.common.OwnCloudClientManagerFactory;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.lib.resources.activities.GetRemoteActivitiesOperation;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import butterknife.BindString;

/**
 * Implementation of the Activities Service API that communicates with the NextCloud remote server
 */
public class ActivitiesServiceApiImpl implements ActivitiesServiceApi {

    private static final String TAG = ActivitiesServiceApiImpl.class.getSimpleName();

    @Override
    public void getAllActivities(String pageUrl, ActivitiesServiceCallback<List<Object>> callback) {
        GetActivityListTask getActivityListTask = new GetActivityListTask(pageUrl, callback);
        getActivityListTask.execute();
    }

    private class GetActivityListTask extends AsyncTask<Void, Void, Boolean> {

        private final ActivitiesServiceCallback<List<Object>> mCallback;
        private List<Object> mActivities;
        private String mPageUrl;
        String noResultsMessage = "no results";
        private String errorMessage;
        private OwnCloudClient ownCloudClient;

        private GetActivityListTask(String pageUrl, ActivitiesServiceCallback<List<Object>> callback) {
            mPageUrl = pageUrl;
            mCallback = callback;
            mActivities = new ArrayList<>();
        }


        @Override
        protected Boolean doInBackground(Void... voids) {
            final Account currentAccount = AccountUtils.getCurrentOwnCloudAccount(MainApp.getAppContext());
            final Context context = MainApp.getAppContext();
            OwnCloudAccount ocAccount;
            try {
                ocAccount = new OwnCloudAccount(currentAccount, context);
                ownCloudClient = OwnCloudClientManagerFactory.getDefaultSingleton().
                        getClientFor(ocAccount, MainApp.getAppContext());
                ownCloudClient.setOwnCloudVersion(AccountUtils.getServerVersion(currentAccount));

                GetRemoteActivitiesOperation getRemoteNotificationOperation = new GetRemoteActivitiesOperation();
                if (mPageUrl != null) {
                    getRemoteNotificationOperation.setNextUrl(mPageUrl);
                }

                final RemoteOperationResult result = getRemoteNotificationOperation.execute(ownCloudClient);

                if (result.isSuccess() && result.getData() != null) {
                    final ArrayList<Object> data = result.getData();
                    mActivities = (ArrayList) data.get(0);

                    mPageUrl = (String) data.get(1);
                    return true;
                } else {
                    Log_OC.d(TAG, result.getLogMessage());
                    // show error
                    errorMessage = result.getLogMessage();
                    if (result.getHttpCode() == 304) {
                        errorMessage = noResultsMessage;
                    }
                    return false;

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

            return false;
        }

        @Override
        protected void onPostExecute(Boolean success) {
            super.onPostExecute(success);
            if (success) {
                mCallback.onLoaded(mActivities, ownCloudClient, mPageUrl == null);
            }
            mCallback.onError(errorMessage);
        }
    }
}
