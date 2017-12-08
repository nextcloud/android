/*
 * Nextcloud Android client application
 *
 * @author Tobias Kaminsky
 * Copyright (C) 2017 Tobias Kaminsky
 * Copyright (C) 2017 Nextcloud GmbH.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package com.owncloud.android.ui.asynctasks;

import android.accounts.Account;
import android.content.ContentResolver;
import android.content.res.Resources;
import android.os.AsyncTask;

import com.owncloud.android.datamodel.ArbitraryDataProvider;
import com.owncloud.android.lib.common.OwnCloudClient;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.lib.resources.users.GetPrivateKeyOperation;
import com.owncloud.android.lib.resources.users.GetPublicKeyOperation;
import com.owncloud.android.utils.EncryptionUtils;

/**
 * Async task for downloading e2e keys
 */

public class DownloadKeysAsyncTask extends AsyncTask<Void, Void, String> {
    private final static String TAG = DownloadKeysAsyncTask.class.getSimpleName();

    private OwnCloudClient client;
    private Account account;
    private Resources resources;
    private String packageName;
    private ContentResolver contentResolver;

    public DownloadKeysAsyncTask(OwnCloudClient client, Account account, Resources resources, String packageName,
                                 ContentResolver contentResolver) {
        this.client = client;
        this.account = account;
        this.resources = resources;
        this.packageName = packageName;
        this.contentResolver = contentResolver;
    }

    @Override
    protected String doInBackground(Void... voids) {
        // fetch private/public key
        // if available
        //  - store public key
        //  - decrypt private key, store unencrypted private key in database

        GetPublicKeyOperation publicKeyOperation = new GetPublicKeyOperation();
        RemoteOperationResult publicKeyResult = publicKeyOperation.execute(client);

        if (publicKeyResult.isSuccess()) {
            Log_OC.d(TAG, "public key successful downloaded for " + account.name);

            String publicKeyFromServer = (String) publicKeyResult.getData().get(0);
            ArbitraryDataProvider arbitraryDataProvider = new ArbitraryDataProvider(contentResolver);
            arbitraryDataProvider.storeOrUpdateKeyValue(account.name, EncryptionUtils.PUBLIC_KEY, publicKeyFromServer);
        } else {
            return null;
        }

        GetPrivateKeyOperation privateKeyOperation = new GetPrivateKeyOperation();
        RemoteOperationResult privateKeyResult = privateKeyOperation.execute(client);

        if (privateKeyResult.isSuccess()) {
            Log_OC.d(TAG, "private key successful downloaded for " + account.name);

//            keyResult = KEY_EXISTING_USED;
            return (String) privateKeyResult.getData().get(0);
        } else {
            return null;
        }
    }

    @Override
    protected void onPostExecute(String privateKey) {
        super.onPostExecute(privateKey);

        if (privateKey == null) {
            // no public/private key available, generate new
            GenerateNewKeysAsyncTask newKeysTask = new GenerateNewKeysAsyncTask(client, account, resources, packageName);

            newKeysTask.execute();

        } else if (!privateKey.isEmpty()) {
//            textView.setText(R.string.end_to_end_encryption_enter_password);
//            passwordLayout.setVisibility(View.VISIBLE);
//            positiveButton.setVisibility(View.VISIBLE);
        } else {
            Log_OC.e(TAG, "Got empty private key string");
        }
    }
}

