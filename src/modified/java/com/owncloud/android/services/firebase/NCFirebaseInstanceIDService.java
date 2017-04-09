/**
 * Nextcloud Android client application
 *
 * @author Mario Danic
 * Copyright (C) 2017 Mario Danic
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.owncloud.android.services.firebase;

import android.accounts.Account;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.content.Context;
import android.text.TextUtils;

import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.FirebaseInstanceIdService;
import com.owncloud.android.MainApp;
import com.owncloud.android.R;
import com.owncloud.android.authentication.AccountUtils;
import com.owncloud.android.lib.common.OwnCloudAccount;
import com.owncloud.android.lib.common.OwnCloudClient;
import com.owncloud.android.lib.common.OwnCloudClientManagerFactory;
import com.owncloud.android.lib.common.operations.RemoteOperation;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.lib.resources.notifications.RegisterAccountDeviceForNotificationsOperation;
import com.owncloud.android.lib.resources.notifications.RegisterAccountDeviceForProxyOperation;
import com.owncloud.android.lib.resources.notifications.models.PushResponse;
import com.owncloud.android.utils.PushUtils;

import java.io.IOException;
import java.security.PublicKey;

public class NCFirebaseInstanceIDService extends FirebaseInstanceIdService {
    private static final String TAG = "NCFirebaseInstanceID";

    @Override
    public void onTokenRefresh() {
        //You can implement this method to store the token on your server
        if (!TextUtils.isEmpty(getResources().getString(R.string.push_server_url))) {
            PushUtils.generateRsa2048KeyPair();
            sendRegistrationToServer(FirebaseInstanceId.getInstance().getToken());
        }
    }

    private void sendRegistrationToServer(String token) {
        String pushTokenHash = PushUtils.generateSHA512Hash(token);
        PublicKey devicePublicKey = (PublicKey) PushUtils.readKeyFromFile(true);
        if (devicePublicKey != null) {
            String publicKey = devicePublicKey.toString();

            Context context = MainApp.getAppContext();
            for (Account account : AccountUtils.getAccounts(context)) {
                try {
                    OwnCloudAccount ocAccount = new OwnCloudAccount(account, context);
                    OwnCloudClient mClient = OwnCloudClientManagerFactory.getDefaultSingleton().
                            getClientFor(ocAccount, context);

                    RemoteOperation registerAccountDeviceForNotificationsOperation =
                            new RegisterAccountDeviceForNotificationsOperation(pushTokenHash,
                        publicKey, context.getResources().getString(R.string.push_server_url));

                    RemoteOperationResult remoteOperationResult = registerAccountDeviceForNotificationsOperation.
                            execute(mClient);

                    if (remoteOperationResult.isSuccess()) {
                        PushResponse pushResponse = remoteOperationResult.getPushResponseData();

                        RemoteOperation registerAccountDeviceForProxyOperation = new
                                RegisterAccountDeviceForProxyOperation(
                                context.getResources().getString(R.string.push_server_url),
                                token, pushResponse.getDeviceIdentifier(), pushResponse.getSignature(),
                                pushResponse.getPublicKey());

                        remoteOperationResult = registerAccountDeviceForProxyOperation.execute(mClient);
                    }
                } catch (com.owncloud.android.lib.common.accounts.AccountUtils.AccountNotFoundException e) {
                    Log_OC.d(TAG, "Failed to find an account");
                } catch (AuthenticatorException e) {
                    Log_OC.d(TAG, "Failed via AuthenticatorException");
                } catch (IOException e) {
                    Log_OC.d(TAG, "Failed via IOException");
                } catch (OperationCanceledException e) {
                    Log_OC.d(TAG, "Failed via OperationCanceledException");
                }

            }

        }

    }

}

