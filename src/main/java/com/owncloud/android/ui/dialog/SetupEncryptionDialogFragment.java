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

import android.accounts.AccountManager;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.nextcloud.client.account.User;
import com.owncloud.android.R;
import com.owncloud.android.datamodel.ArbitraryDataProvider;
import com.owncloud.android.lib.common.accounts.AccountUtils;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.lib.resources.users.DeletePublicKeyOperation;
import com.owncloud.android.lib.resources.users.GetPrivateKeyOperation;
import com.owncloud.android.lib.resources.users.GetPublicKeyOperation;
import com.owncloud.android.lib.resources.users.SendCSROperation;
import com.owncloud.android.lib.resources.users.StorePrivateKeyOperation;
import com.owncloud.android.utils.CsrHelper;
import com.owncloud.android.utils.EncryptionUtils;
import com.owncloud.android.utils.theme.ThemeButtonUtils;
import com.owncloud.android.utils.theme.ThemeColorUtils;

import java.io.IOException;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import androidx.appcompat.app.AlertDialog;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.fragment.app.DialogFragment;

import static com.owncloud.android.utils.EncryptionUtils.decodeStringToBase64Bytes;
import static com.owncloud.android.utils.EncryptionUtils.decryptStringAsymmetric;
import static com.owncloud.android.utils.EncryptionUtils.encodeBytesToBase64String;
import static com.owncloud.android.utils.EncryptionUtils.generateKey;

/*
 *  Dialog to setup encryption
 */
public class SetupEncryptionDialogFragment extends DialogFragment {

    public static final String SUCCESS = "SUCCESS";
    public static final int SETUP_ENCRYPTION_RESULT_CODE = 101;
    public static final int SETUP_ENCRYPTION_REQUEST_CODE = 100;
    public static final String SETUP_ENCRYPTION_DIALOG_TAG = "SETUP_ENCRYPTION_DIALOG_TAG";
    public static final String ARG_POSITION = "ARG_POSITION";

    private static final String ARG_USER = "ARG_USER";
    private static final String TAG = SetupEncryptionDialogFragment.class.getSimpleName();

    private static final String KEY_CREATED = "KEY_CREATED";
    private static final String KEY_EXISTING_USED = "KEY_EXISTING_USED";
    private static final String KEY_FAILED = "KEY_FAILED";
    private static final String KEY_GENERATE = "KEY_GENERATE";

    private User user;
    private TextView textView;
    private TextView passphraseTextView;
    private ArbitraryDataProvider arbitraryDataProvider;
    private Button positiveButton;
    private Button neutralButton;
    private DownloadKeysAsyncTask task;
    private TextView passwordField;
    private String keyResult;
    private List<String> keyWords;

    /**
     * Public factory method to create new SetupEncryptionDialogFragment instance
     *
     * @return Dialog ready to show.
     */
    public static SetupEncryptionDialogFragment newInstance(User user, int position) {
        SetupEncryptionDialogFragment fragment = new SetupEncryptionDialogFragment();
        Bundle args = new Bundle();
        args.putParcelable(ARG_USER, user);
        args.putInt(ARG_POSITION, position);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onStart() {
        super.onStart();

        AlertDialog alertDialog = (AlertDialog) getDialog();

        positiveButton = alertDialog.getButton(AlertDialog.BUTTON_POSITIVE);
        neutralButton = alertDialog.getButton(AlertDialog.BUTTON_NEUTRAL);
        ThemeButtonUtils.themeBorderlessButton(positiveButton,
                                               neutralButton);

        task = new DownloadKeysAsyncTask();
        task.execute();
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        int primaryColor = ThemeColorUtils.primaryColor(getContext());
        user = getArguments().getParcelable(ARG_USER);

        arbitraryDataProvider = new ArbitraryDataProvider(getContext().getContentResolver());

        // Inflate the layout for the dialog
        LayoutInflater inflater = getActivity().getLayoutInflater();

        // Setup layout
        View v = inflater.inflate(R.layout.setup_encryption_dialog, null);
        textView = v.findViewById(R.id.encryption_status);
        passphraseTextView = v.findViewById(R.id.encryption_passphrase);
        passwordField = v.findViewById(R.id.encryption_passwordInput);
        passwordField.getBackground().setColorFilter(primaryColor, PorterDuff.Mode.SRC_ATOP);

        Drawable wrappedDrawable = DrawableCompat.wrap(passwordField.getBackground());
        DrawableCompat.setTint(wrappedDrawable, primaryColor);
        passwordField.setBackgroundDrawable(wrappedDrawable);

        return createDialog(v);
    }

    @NonNull
    private Dialog createDialog(View v) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setView(v).setPositiveButton(R.string.common_ok, null)
                .setNeutralButton(R.string.common_cancel, null)
                .setTitle(R.string.end_to_end_encryption_title);

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
                                    String mnemonicUnchanged = passwordField.getText().toString();
                                    String mnemonic = passwordField.getText().toString().replaceAll("\\s", "")
                                        .toLowerCase(Locale.ROOT);
                                    String decryptedPrivateKey = EncryptionUtils.decryptPrivateKey(privateKey,
                                                                                                   mnemonic);

                                    arbitraryDataProvider.storeOrUpdateKeyValue(user.getAccountName(),
                                                                                EncryptionUtils.PRIVATE_KEY, decryptedPrivateKey);

                                    dialog.dismiss();
                                    Log_OC.d(TAG, "Private key successfully decrypted and stored");

                                    arbitraryDataProvider.storeOrUpdateKeyValue(user.getAccountName(),
                                                                                EncryptionUtils.MNEMONIC,
                                                                                mnemonicUnchanged);

                                    // check if private key and public key match
                                    String publicKey = arbitraryDataProvider.getValue(user.getAccountName(),
                                                                                      EncryptionUtils.PUBLIC_KEY);

                                    byte[] key1 = generateKey();
                                    String base64encodedKey = encodeBytesToBase64String(key1);

                                    String encryptedString = EncryptionUtils.encryptStringAsymmetric(base64encodedKey,
                                                                                                     publicKey);
                                    String decryptedString = decryptStringAsymmetric(encryptedString,
                                                                                     decryptedPrivateKey);

                                    byte[] key2 = decodeStringToBase64Bytes(decryptedString);

                                    if (!Arrays.equals(key1, key2)) {
                                        throw new Exception("Keys do not match");
                                    }

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
                                neutralButton.setVisibility(View.GONE);
                                getDialog().setTitle(R.string.end_to_end_encryption_storing_keys);

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

    public class DownloadKeysAsyncTask extends AsyncTask<Void, Void, String> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            textView.setText(R.string.end_to_end_encryption_retrieving_keys);
            positiveButton.setVisibility(View.INVISIBLE);
            neutralButton.setVisibility(View.INVISIBLE);
        }

        @Override
        protected String doInBackground(Void... voids) {
            // fetch private/public key
            // if available
            //  - store public key
            //  - decrypt private key, store unencrypted private key in database

            GetPublicKeyOperation publicKeyOperation = new GetPublicKeyOperation();
            RemoteOperationResult publicKeyResult = publicKeyOperation.execute(user.toPlatformAccount(), getContext());

            if (publicKeyResult.isSuccess()) {
                Log_OC.d(TAG, "public key successful downloaded for " + user.getAccountName());

                String publicKeyFromServer = (String) publicKeyResult.getData().get(0);
                arbitraryDataProvider.storeOrUpdateKeyValue(user.getAccountName(),
                                                            EncryptionUtils.PUBLIC_KEY,
                                                            publicKeyFromServer);
            } else {
                return null;
            }

            RemoteOperationResult<com.owncloud.android.lib.ocs.responses.PrivateKey> privateKeyResult =
                new GetPrivateKeyOperation().execute(user.toPlatformAccount(), getContext());

            if (privateKeyResult.isSuccess()) {
                Log_OC.d(TAG, "private key successful downloaded for " + user.getAccountName());

                keyResult = KEY_EXISTING_USED;
                return privateKeyResult.getResultData().getKey();
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
                    keyWords = EncryptionUtils.getRandomWords(12, requireContext());
                    showMnemonicInfo();
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

    public class GenerateNewKeysAsyncTask extends AsyncTask<Void, Void, String> {
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

                // create CSR
                AccountManager accountManager = AccountManager.get(getContext());
                String userId = accountManager.getUserData(user.toPlatformAccount(), AccountUtils.Constants.KEY_USER_ID);
                String urlEncoded = CsrHelper.generateCsrPemEncodedString(keyPair, userId);

                SendCSROperation operation = new SendCSROperation(urlEncoded);
                RemoteOperationResult result = operation.execute(user.toPlatformAccount(), getContext());

                if (result.isSuccess()) {
                    Log_OC.d(TAG, "public key success");
                    publicKey = (String) result.getData().get(0);
                } else {
                    keyResult = KEY_FAILED;
                    return "";
                }

                PrivateKey privateKey = keyPair.getPrivate();
                String privateKeyString = EncryptionUtils.encodeBytesToBase64String(privateKey.getEncoded());
                String privatePemKeyString = EncryptionUtils.privateKeyToPEM(privateKey);
                String encryptedPrivateKey = EncryptionUtils.encryptPrivateKey(privatePemKeyString,
                        generateMnemonicString(false));

                // upload encryptedPrivateKey
                StorePrivateKeyOperation storePrivateKeyOperation = new StorePrivateKeyOperation(encryptedPrivateKey);
                RemoteOperationResult storePrivateKeyResult = storePrivateKeyOperation.execute(user.toPlatformAccount(),
                                                                                               getContext());

                if (storePrivateKeyResult.isSuccess()) {
                    Log_OC.d(TAG, "private key success");

                    arbitraryDataProvider.storeOrUpdateKeyValue(user.getAccountName(), EncryptionUtils.PRIVATE_KEY,
                            privateKeyString);
                    arbitraryDataProvider.storeOrUpdateKeyValue(user.getAccountName(), EncryptionUtils.PUBLIC_KEY, publicKey);
                    arbitraryDataProvider.storeOrUpdateKeyValue(user.getAccountName(), EncryptionUtils.MNEMONIC,
                            generateMnemonicString(true));

                    keyResult = KEY_CREATED;
                    return (String) storePrivateKeyResult.getData().get(0);
                } else {
                    DeletePublicKeyOperation deletePublicKeyOperation = new DeletePublicKeyOperation();
                    deletePublicKeyOperation.execute(user.toPlatformAccount(), getContext());
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
                errorSavingKeys();
            } else {
                requireDialog().dismiss();

                Intent intentExisting = new Intent();
                intentExisting.putExtra(SUCCESS, true);
                intentExisting.putExtra(ARG_POSITION, requireArguments().getInt(ARG_POSITION));
                getTargetFragment().onActivityResult(getTargetRequestCode(),
                                                     SETUP_ENCRYPTION_RESULT_CODE, intentExisting);
            }
        }
    }

    private String generateMnemonicString(boolean withWhitespace) {
        StringBuilder stringBuilder = new StringBuilder();

        for (String string : keyWords) {
            stringBuilder.append(string);
            if (withWhitespace) {
                stringBuilder.append(' ');
            }
        }

        return stringBuilder.toString();
    }

    @VisibleForTesting
    public void showMnemonicInfo() {
        requireDialog().setTitle(R.string.end_to_end_encryption_passphrase_title);

        textView.setText(R.string.end_to_end_encryption_keywords_description);

        passphraseTextView.setText(generateMnemonicString(true));

        passphraseTextView.setVisibility(View.VISIBLE);
        positiveButton.setText(R.string.end_to_end_encryption_confirm_button);
        positiveButton.setVisibility(View.VISIBLE);

        neutralButton.setVisibility(View.VISIBLE);
        ThemeButtonUtils.themeBorderlessButton(positiveButton, neutralButton);

        keyResult = KEY_GENERATE;
    }

    @VisibleForTesting
    public void errorSavingKeys() {
        keyResult = KEY_FAILED;

        requireDialog().setTitle(R.string.common_error);
        textView.setText(R.string.end_to_end_encryption_unsuccessful);
        positiveButton.setText(R.string.end_to_end_encryption_dialog_close);
        positiveButton.setVisibility(View.VISIBLE);
        positiveButton.setTextColor(ThemeColorUtils.primaryAccentColor(getContext()));
    }

    @VisibleForTesting
    public void setMnemonic(List<String> keyWords) {
        this.keyWords = keyWords;
    }
}
