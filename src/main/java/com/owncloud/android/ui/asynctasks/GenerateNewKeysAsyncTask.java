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
import android.content.res.Resources;
import android.os.AsyncTask;

import com.owncloud.android.lib.common.OwnCloudClient;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.lib.resources.users.SendCSROperation;
import com.owncloud.android.lib.resources.users.StorePrivateKeyOperation;
import com.owncloud.android.utils.CsrHelper;
import com.owncloud.android.utils.EncryptionUtils;

import java.security.KeyPair;
import java.security.PrivateKey;
import java.util.ArrayList;

/**
 * Async task for generating new e2e keys
 */

public class GenerateNewKeysAsyncTask extends AsyncTask<Void, Void, String> {
    private static final String TAG = GenerateNewKeysAsyncTask.class.getSimpleName();
    private ArrayList<String> keyWords;

    private OwnCloudClient client;
    private Account account;
    private Resources resources;
    private String packageName;

    public GenerateNewKeysAsyncTask(OwnCloudClient client, Account account, Resources resources, String packageName) {
        this.client = client;
        this.account = account;
        this.resources = resources;
        this.packageName = packageName;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();

//        textView.setText(R.string.end_to_end_encryption_generating_keys);
    }

    @Override
    protected String doInBackground(Void... voids) {
        //  - create CSR, push to server, store returned public key in database
        //  - encrypt private key, push key to server, store unencrypted private key in database

        try {
            String publicKey;

            // Create public/private key pair
            KeyPair keyPair = EncryptionUtils.generateKeyPair();
            PrivateKey privateKey = keyPair.getPrivate();

            // create CSR
            String urlEncoded = CsrHelper.generateCsrPemEncodedString(keyPair, account.name);

            SendCSROperation operation = new SendCSROperation(urlEncoded);
            RemoteOperationResult result = operation.execute(client);

            if (result.isSuccess()) {
                Log_OC.d(TAG, "public key success");

                publicKey = (String) result.getData().get(0);
            } else {
//                keyResult = KEY_FAILED;
                return "";
            }

            keyWords = EncryptionUtils.getRandomWords(12, null);

            StringBuilder stringBuilder = new StringBuilder();
            for (String string : keyWords) {
                stringBuilder.append(string);
            }
            String keyPhrase = stringBuilder.toString();

            String privateKeyString = EncryptionUtils.encodeBytesToBase64String(privateKey.getEncoded());
            String privatePemKeyString = EncryptionUtils.privateKeyToPEM(privateKey);
            String encryptedPrivateKey = EncryptionUtils.encryptPrivateKey(privatePemKeyString, keyPhrase);

            // upload encryptedPrivateKey
            StorePrivateKeyOperation storePrivateKeyOperation = new StorePrivateKeyOperation(encryptedPrivateKey);
            RemoteOperationResult storePrivateKeyResult = storePrivateKeyOperation.execute(client);

            if (storePrivateKeyResult.isSuccess()) {
                Log_OC.d(TAG, "private key success");

//                arbitraryDataProvider.storeOrUpdateKeyValue(account.name, EncryptionUtils.PRIVATE_KEY,
//                        privateKeyString);
//                arbitraryDataProvider.storeOrUpdateKeyValue(account.name, EncryptionUtils.PUBLIC_KEY, publicKey);

//                keyResult = KEY_CREATED;
                return (String) storePrivateKeyResult.getData().get(0);
            }

        } catch (Exception e) {
            Log_OC.e(TAG, e.getMessage());
            e.printStackTrace();
        }

//        keyResult = KEY_FAILED;
        return "";
    }

    @Override
    protected void onPostExecute(String s) {
        super.onPostExecute(s);

//        if (s.isEmpty()) {
//            keyResult = KEY_FAILED;
//
//            getDialog().setTitle(R.string.common_error);
//            textView.setText(R.string.end_to_end_encryption_unsuccessful);
//            positiveButton.setText(R.string.end_to_end_encryption_dialog_close);
//            positiveButton.setVisibility(View.VISIBLE);
//        } else {
//            getDialog().setTitle(R.string.end_to_end_encryption_passphrase_title);
//
//            textView.setText(R.string.end_to_end_encryption_keywords_description);
//
//            StringBuilder stringBuilder = new StringBuilder();
//
//            for (String string: keyWords) {
//                stringBuilder.append(string).append(" ");
//            }
//            String keys = stringBuilder.toString();
//
//            passphraseTextView.setText(keys);
//
//            passphraseTextView.setVisibility(View.VISIBLE);
//            positiveButton.setText(R.string.end_to_end_encryption_confirm_button);
//            positiveButton.setVisibility(View.VISIBLE);
//        }
    }
}