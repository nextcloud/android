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
package com.owncloud.android.ui.dialog;

import android.accounts.Account;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.owncloud.android.R;
import com.owncloud.android.datamodel.ArbitraryDataProvider;
import com.owncloud.android.lib.common.UserInfo;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.lib.resources.users.DeletePublicKeyOperation;
import com.owncloud.android.lib.resources.users.GetPrivateKeyOperation;
import com.owncloud.android.lib.resources.users.GetPublicKeyOperation;
import com.owncloud.android.lib.resources.users.GetRemoteUserInfoOperation;
import com.owncloud.android.lib.resources.users.SendCSROperation;
import com.owncloud.android.lib.resources.users.StorePrivateKeyOperation;
import com.owncloud.android.utils.CsrHelper;
import com.owncloud.android.utils.EncryptionUtils;
import com.owncloud.android.utils.ThemeUtils;

import java.io.IOException;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.util.ArrayList;
import java.util.Locale;

/*
 *  Dialog to setup encryption
 */

public class SetupEncryptionDialogFragment extends DialogFragment {

    public static final String SUCCESS = "SUCCESS";
    public static final int SETUP_ENCRYPTION_RESULT_CODE = 101;
    public static final int SETUP_ENCRYPTION_REQUEST_CODE = 100;
    public static String SETUP_ENCRYPTION_DIALOG_TAG = "SETUP_ENCRYPTION_DIALOG_TAG";
    public static final String ARG_POSITION = "ARG_POSITION";

    private static String ARG_ACCOUNT = "ARG_ACCOUNT";
    private static String TAG = SetupEncryptionDialogFragment.class.getSimpleName();

    private static final String KEY_CREATED = "KEY_CREATED";
    private static final String KEY_EXISTING_USED = "KEY_EXISTING_USED";
    private static final String KEY_FAILED = "KEY_FAILED";
    private static final String KEY_GENERATE = "KEY_GENERATE";

    private Account account;
    private TextView textView;
    private TextView passphraseTextView;
    private ArbitraryDataProvider arbitraryDataProvider;
    private Button positiveButton;
    private Button negativeButton;
    private DownloadKeysAsyncTask task;
    private TextView passwordField;
    private String keyResult;
    private ArrayList<String> keyWords;

    /**
     * Public factory method to create new SetupEncryptionDialogFragment instance
     *
     * @return Dialog ready to show.
     */
    public static SetupEncryptionDialogFragment newInstance(Account account, int position) {
        SetupEncryptionDialogFragment fragment = new SetupEncryptionDialogFragment();
        Bundle args = new Bundle();
        args.putParcelable(ARG_ACCOUNT, account);
        args.putInt(ARG_POSITION, position);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onStart() {
        super.onStart();

        int color = ThemeUtils.primaryAccentColor(getContext());

        AlertDialog alertDialog = (AlertDialog) getDialog();

        positiveButton = alertDialog.getButton(AlertDialog.BUTTON_POSITIVE);
        positiveButton.setTextColor(color);

        negativeButton = alertDialog.getButton(AlertDialog.BUTTON_NEGATIVE);
        negativeButton.setTextColor(color);

        task = new DownloadKeysAsyncTask();
        task.execute();
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        int accentColor = ThemeUtils.primaryAccentColor(getContext());
        account = getArguments().getParcelable(ARG_ACCOUNT);

        arbitraryDataProvider = new ArbitraryDataProvider(getContext().getContentResolver());

        // Inflate the layout for the dialog
        LayoutInflater inflater = getActivity().getLayoutInflater();

        // Setup layout
        View v = inflater.inflate(R.layout.setup_encryption_dialog, null);
        textView = v.findViewById(R.id.encryption_status);
        passphraseTextView = v.findViewById(R.id.encryption_passphrase);
        passwordField = v.findViewById(R.id.encryption_passwordInput);
        passwordField.getBackground().setColorFilter(accentColor, PorterDuff.Mode.SRC_ATOP);

        Drawable wrappedDrawable = DrawableCompat.wrap(passwordField.getBackground());
        DrawableCompat.setTint(wrappedDrawable, accentColor);
        passwordField.setBackgroundDrawable(wrappedDrawable);

        // Build the dialog  
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setView(v).setPositiveButton(R.string.common_ok, null)
                .setNegativeButton(R.string.common_cancel, null)
                .setTitle(ThemeUtils.getColoredTitle(getString(R.string.end_to_end_encryption_title), accentColor));

        Dialog dialog = builder.create();
        dialog.setCanceledOnTouchOutside(false);

        dialog.setOnShowListener(new DialogInterface.OnShowListener() {

            @Override
            public void onShow(final DialogInterface dialog) {

                Button button = ((AlertDialog) dialog).getButton(AlertDialog.BUTTON_POSITIVE);
                button.setOnClickListener(new View.OnClickListener() {

                    @Override
                    public void onClick(View view) {
                        switch (keyResult) {
                            case KEY_CREATED:
                                Log_OC.d(TAG, "New keys generated and stored.");

                                dialog.dismiss();

                                Intent intentCreated = new Intent();
                                intentCreated.putExtra(SUCCESS, true);
                                intentCreated.putExtra(ARG_POSITION, getArguments().getInt(ARG_POSITION));
                                getTargetFragment().onActivityResult(getTargetRequestCode(),
                                        SETUP_ENCRYPTION_RESULT_CODE, intentCreated);
                                break;

                            case KEY_EXISTING_USED:
                                Log_OC.d(TAG, "Decrypt private key");

                                textView.setText(R.string.end_to_end_encryption_decrypting);

                                try {
                                    String privateKey = task.get();
                                    String decryptedPrivateKey = EncryptionUtils.decryptPrivateKey(privateKey,
                                            passwordField.getText().toString().replaceAll("\\s", "")
                                                    .toLowerCase(Locale.ROOT));

                                    arbitraryDataProvider.storeOrUpdateKeyValue(account.name,
                                            EncryptionUtils.PRIVATE_KEY, decryptedPrivateKey);

                                    dialog.dismiss();
                                    Log_OC.d(TAG, "Private key successfully decrypted and stored");

                                    Intent intentExisting = new Intent();
                                    intentExisting.putExtra(SUCCESS, true);
                                    intentExisting.putExtra(ARG_POSITION, getArguments().getInt(ARG_POSITION));
                                    getTargetFragment().onActivityResult(getTargetRequestCode(),
                                            SETUP_ENCRYPTION_RESULT_CODE, intentExisting);

                                } catch (Exception e) {
                                    textView.setText(R.string.end_to_end_encryption_wrong_password);
                                    Log_OC.d(TAG, "Error while decrypting private key: " + e.getMessage());
                                }
                                break;

                            case KEY_GENERATE:
                                passphraseTextView.setVisibility(View.GONE);
                                positiveButton.setVisibility(View.GONE);
                                negativeButton.setVisibility(View.GONE);
                                getDialog().setTitle(ThemeUtils.getColoredTitle(getString(
                                        R.string.end_to_end_encryption_storing_keys),
                                        ThemeUtils.primaryColor(getContext())));

                                GenerateNewKeysAsyncTask newKeysTask = new GenerateNewKeysAsyncTask();
                                newKeysTask.execute();
                                break;

                            default:
                                dialog.dismiss();
                                break;
                        }
                    }
                });
            }
        });

        return dialog;
    }

    private class DownloadKeysAsyncTask extends AsyncTask<Void, Void, String> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            textView.setText(R.string.end_to_end_encryption_retrieving_keys);
            positiveButton.setVisibility(View.INVISIBLE);
            negativeButton.setVisibility(View.INVISIBLE);
        }

        @Override
        protected String doInBackground(Void... voids) {
            // fetch private/public key
            // if available
            //  - store public key
            //  - decrypt private key, store unencrypted private key in database

            // get user id
            String userID;
            GetRemoteUserInfoOperation remoteUserNameOperation = new GetRemoteUserInfoOperation();
            RemoteOperationResult remoteUserNameOperationResult = remoteUserNameOperation.execute(account,
                    getContext(), true);

            if (remoteUserNameOperationResult.isSuccess() && remoteUserNameOperationResult.getData() != null) {
                UserInfo userInfo = (UserInfo) remoteUserNameOperationResult.getData().get(0);
                userID = userInfo.getId();
            } else {
                userID = account.name;
            }

            GetPublicKeyOperation publicKeyOperation = new GetPublicKeyOperation(userID);
            RemoteOperationResult publicKeyResult = publicKeyOperation.execute(account, getContext(), true);

            if (publicKeyResult.isSuccess()) {
                Log_OC.d(TAG, "public key successful downloaded for " + account.name);

                String publicKeyFromServer = (String) publicKeyResult.getData().get(0);
                arbitraryDataProvider.storeOrUpdateKeyValue(account.name, EncryptionUtils.PUBLIC_KEY,
                        publicKeyFromServer);
            } else {
                return null;
            }

            GetPrivateKeyOperation privateKeyOperation = new GetPrivateKeyOperation();
            RemoteOperationResult privateKeyResult = privateKeyOperation.execute(account, getContext(), true);

            if (privateKeyResult.isSuccess()) {
                Log_OC.d(TAG, "private key successful downloaded for " + account.name);

                keyResult = KEY_EXISTING_USED;
                return (String) privateKeyResult.getData().get(0);
            } else {
                return null;
            }
        }

        @Override
        protected void onPostExecute(String privateKey) {
            super.onPostExecute(privateKey);

            if (privateKey == null) {
                // first show info
                try {
                    keyWords = EncryptionUtils.getRandomWords(12, getContext());

                    getDialog().setTitle(ThemeUtils.getColoredTitle(
                            getString(R.string.end_to_end_encryption_passphrase_title),
                            ThemeUtils.primaryColor(getContext())));

                    textView.setText(R.string.end_to_end_encryption_keywords_description);

                    StringBuilder stringBuilder = new StringBuilder();

                    for (String string : keyWords) {
                        stringBuilder.append(string).append(" ");
                    }
                    String keys = stringBuilder.toString();

                    passphraseTextView.setText(keys);

                    passphraseTextView.setVisibility(View.VISIBLE);
                    positiveButton.setText(R.string.end_to_end_encryption_confirm_button);
                    positiveButton.setVisibility(View.VISIBLE);

                    negativeButton.setVisibility(View.VISIBLE);

                    keyResult = KEY_GENERATE;
                } catch (IOException e) {
                    textView.setText(R.string.common_error);
                }
            } else if (!privateKey.isEmpty()) {
                textView.setText(R.string.end_to_end_encryption_enter_password);
                passwordField.setVisibility(View.VISIBLE);
                positiveButton.setVisibility(View.VISIBLE);
            } else {
                Log_OC.e(TAG, "Got empty private key string");
            }
        }
    }

    private class GenerateNewKeysAsyncTask extends AsyncTask<Void, Void, String> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            textView.setText(R.string.end_to_end_encryption_generating_keys);
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

                // get user id
                String userID;
                GetRemoteUserInfoOperation remoteUserNameOperation = new GetRemoteUserInfoOperation();
                RemoteOperationResult remoteUserNameOperationResult = remoteUserNameOperation
                        .execute(account, getContext(), true);

                if (remoteUserNameOperationResult.isSuccess() &&
                        remoteUserNameOperationResult.getData() != null) {
                    UserInfo userInfo = (UserInfo) remoteUserNameOperationResult.getData().get(0);
                    userID = userInfo.getId();
                } else {
                    userID = account.name;
                }

                // create CSR
                String urlEncoded = CsrHelper.generateCsrPemEncodedString(keyPair, userID);

                SendCSROperation operation = new SendCSROperation(urlEncoded);
                RemoteOperationResult result = operation.execute(account, getContext(), true);

                if (result.isSuccess()) {
                    Log_OC.d(TAG, "public key success");

                    publicKey = (String) result.getData().get(0);
                } else {
                    keyResult = KEY_FAILED;
                    return "";
                }

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
                RemoteOperationResult storePrivateKeyResult = storePrivateKeyOperation.execute(account, getContext(),
                        true);

                if (storePrivateKeyResult.isSuccess()) {
                    Log_OC.d(TAG, "private key success");

                    arbitraryDataProvider.storeOrUpdateKeyValue(account.name, EncryptionUtils.PRIVATE_KEY,
                            privateKeyString);
                    arbitraryDataProvider.storeOrUpdateKeyValue(account.name, EncryptionUtils.PUBLIC_KEY, publicKey);

                    keyResult = KEY_CREATED;
                    return (String) storePrivateKeyResult.getData().get(0);
                } else {
                    DeletePublicKeyOperation deletePublicKeyOperation = new DeletePublicKeyOperation();
                    deletePublicKeyOperation.execute(account, getContext(), true);
                }
            } catch (Exception e) {
                Log_OC.e(TAG, e.getMessage());
            }

            keyResult = KEY_FAILED;
            return "";
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);

            if (s.isEmpty()) {
                keyResult = KEY_FAILED;

                getDialog().setTitle(ThemeUtils.getColoredTitle(
                        getString(R.string.common_error), ThemeUtils.primaryColor(getContext())));
                textView.setText(R.string.end_to_end_encryption_unsuccessful);
                positiveButton.setText(R.string.end_to_end_encryption_dialog_close);
                positiveButton.setVisibility(View.VISIBLE);
            } else {
                getDialog().dismiss();

                Intent intentExisting = new Intent();
                intentExisting.putExtra(SUCCESS, true);
                intentExisting.putExtra(ARG_POSITION, getArguments().getInt(ARG_POSITION));
                getTargetFragment().onActivityResult(getTargetRequestCode(),
                        SETUP_ENCRYPTION_RESULT_CODE, intentExisting);
            }
        }
    }
}
