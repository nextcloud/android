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
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.nextcloud.client.account.User;
import com.nextcloud.client.di.Injectable;
import com.owncloud.android.R;
import com.owncloud.android.databinding.SetupEncryptionDialogBinding;
import com.owncloud.android.datamodel.ArbitraryDataProvider;
import com.owncloud.android.datamodel.ArbitraryDataProviderImpl;
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
import com.owncloud.android.utils.theme.ViewThemeUtils;

import java.io.IOException;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import javax.inject.Inject;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;

import static com.owncloud.android.utils.EncryptionUtils.decodeStringToBase64Bytes;
import static com.owncloud.android.utils.EncryptionUtils.decryptStringAsymmetric;
import static com.owncloud.android.utils.EncryptionUtils.encodeBytesToBase64String;
import static com.owncloud.android.utils.EncryptionUtils.generateKey;

/*
 *  Dialog to setup encryption
 */
public class SetupEncryptionDialogFragment extends DialogFragment implements Injectable {

    public static final String SUCCESS = "SUCCESS";
    public static final int SETUP_ENCRYPTION_RESULT_CODE = 101;
    public static final int SETUP_ENCRYPTION_REQUEST_CODE = 100;
    public static final String SETUP_ENCRYPTION_DIALOG_TAG = "SETUP_ENCRYPTION_DIALOG_TAG";
    public static final String ARG_POSITION = "ARG_POSITION";

    public static final String RESULT_REQUEST_KEY = "RESULT_REQUEST";
    public static final String RESULT_KEY_CANCELLED = "IS_CANCELLED";

    private static final String ARG_USER = "ARG_USER";
    private static final String TAG = SetupEncryptionDialogFragment.class.getSimpleName();

    private static final String KEY_CREATED = "KEY_CREATED";
    private static final String KEY_EXISTING_USED = "KEY_EXISTING_USED";
    private static final String KEY_FAILED = "KEY_FAILED";
    private static final String KEY_GENERATE = "KEY_GENERATE";

    @Inject ViewThemeUtils viewThemeUtils;

    private User user;
    private ArbitraryDataProvider arbitraryDataProvider;
    private Button positiveButton;
    private Button neutralButton;
    private DownloadKeysAsyncTask task;
    private String keyResult;
    private List<String> keyWords;
    private SetupEncryptionDialogBinding binding;

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

        if (alertDialog != null) {
            positiveButton = alertDialog.getButton(AlertDialog.BUTTON_POSITIVE);
            neutralButton = alertDialog.getButton(AlertDialog.BUTTON_NEUTRAL);
            viewThemeUtils.platform.colorTextButtons(positiveButton, neutralButton);
        }

        task = new DownloadKeysAsyncTask();
        task.execute();
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        user = getArguments().getParcelable(ARG_USER);

        arbitraryDataProvider = new ArbitraryDataProviderImpl(getContext());

        // Inflate the layout for the dialog
        LayoutInflater inflater = getActivity().getLayoutInflater();
        binding = SetupEncryptionDialogBinding.inflate(inflater, null, false);

        // Setup layout
        viewThemeUtils.material.colorTextInputLayout(binding.encryptionPasswordInputContainer);

        return createDialog(binding.getRoot());
    }

    @NonNull
    private Dialog createDialog(View v) {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(v.getContext());
        builder.setView(v).setPositiveButton(R.string.common_ok, null)
            .setNeutralButton(R.string.common_cancel, (dialog, which) -> {
                dialog.cancel();
            })
            .setTitle(R.string.end_to_end_encryption_title);

        viewThemeUtils.dialog.colorMaterialAlertDialogBackground(v.getContext(), builder);

        Dialog dialog = builder.create();
        dialog.setCanceledOnTouchOutside(false);

        dialog.setOnShowListener(dialog1 -> {

            Button button = ((AlertDialog) dialog1).getButton(AlertDialog.BUTTON_POSITIVE);
            button.setOnClickListener(view -> {
                switch (keyResult) {
                    case KEY_CREATED:
                        Log_OC.d(TAG, "New keys generated and stored.");

                        dialog1.dismiss();

                        notifyResult();
                        break;

                    case KEY_EXISTING_USED:
                        Log_OC.d(TAG, "Decrypt private key");

                        binding.encryptionStatus.setText(R.string.end_to_end_encryption_decrypting);

                        try {
                            String privateKey = task.get();
                            String mnemonicUnchanged = binding.encryptionPasswordInput.getText().toString();
                            String mnemonic = binding.encryptionPasswordInput.getText().toString().replaceAll("\\s", "")
                                .toLowerCase(Locale.ROOT);
                            String decryptedPrivateKey = EncryptionUtils.decryptPrivateKey(privateKey,
                                                                                           mnemonic);

                            arbitraryDataProvider.storeOrUpdateKeyValue(user.getAccountName(),
                                                                        EncryptionUtils.PRIVATE_KEY, decryptedPrivateKey);

                            dialog1.dismiss();
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

                            notifyResult();

                        } catch (Exception e) {
                            binding.encryptionStatus.setText(R.string.end_to_end_encryption_wrong_password);
                            Log_OC.d(TAG, "Error while decrypting private key: " + e.getMessage());
                        }
                        break;

                    case KEY_GENERATE:
                        binding.encryptionPassphrase.setVisibility(View.GONE);
                        positiveButton.setVisibility(View.GONE);
                        neutralButton.setVisibility(View.GONE);
                        getDialog().setTitle(R.string.end_to_end_encryption_storing_keys);

                        GenerateNewKeysAsyncTask newKeysTask = new GenerateNewKeysAsyncTask();
                        newKeysTask.execute();
                        break;

                    default:
                        dialog1.dismiss();
                        break;
                }
            });
        });
        return dialog;
    }

    private void notifyResult() {
        final Fragment targetFragment = getTargetFragment();
        if (targetFragment != null) {
            targetFragment.onActivityResult(getTargetRequestCode(),
                                            SETUP_ENCRYPTION_RESULT_CODE, getResultIntent());
        }
        getParentFragmentManager().setFragmentResult(RESULT_REQUEST_KEY, getResultBundle());
    }

    @NonNull
    private Intent getResultIntent() {
        Intent intentCreated = new Intent();
        intentCreated.putExtra(SUCCESS, true);
        intentCreated.putExtra(ARG_POSITION, getArguments().getInt(ARG_POSITION));
        return intentCreated;
    }

    @NonNull
    private Bundle getResultBundle() {
        final Bundle bundle = new Bundle();
        bundle.putBoolean(SUCCESS, true);
        bundle.putInt(ARG_POSITION, getArguments().getInt(ARG_POSITION));
        return bundle;
    }


    @Override
    public void onCancel(@NonNull DialogInterface dialog) {
        super.onCancel(dialog);
        final Bundle bundle = new Bundle();
        bundle.putBoolean(RESULT_KEY_CANCELLED, true);
        getParentFragmentManager().setFragmentResult(RESULT_REQUEST_KEY, bundle);
    }

    public class DownloadKeysAsyncTask extends AsyncTask<Void, Void, String> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            binding.encryptionStatus.setText(R.string.end_to_end_encryption_retrieving_keys);
            positiveButton.setVisibility(View.INVISIBLE);
        }

        @Override
        protected String doInBackground(Void... voids) {
            // fetch private/public key
            // if available
            //  - store public key
            //  - decrypt private key, store unencrypted private key in database

            GetPublicKeyOperation publicKeyOperation = new GetPublicKeyOperation();
            RemoteOperationResult<String> publicKeyResult = publicKeyOperation.execute(user, getContext());

            if (publicKeyResult.isSuccess()) {
                Log_OC.d(TAG, "public key successful downloaded for " + user.getAccountName());

                String publicKeyFromServer = publicKeyResult.getResultData();
                arbitraryDataProvider.storeOrUpdateKeyValue(user.getAccountName(),
                                                            EncryptionUtils.PUBLIC_KEY,
                                                            publicKeyFromServer);
            } else {
                return null;
            }

            RemoteOperationResult<com.owncloud.android.lib.ocs.responses.PrivateKey> privateKeyResult =
                new GetPrivateKeyOperation().execute(user, getContext());

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
                    binding.encryptionStatus.setText(R.string.common_error);
                }
            } else if (!privateKey.isEmpty()) {
                binding.encryptionStatus.setText(R.string.end_to_end_encryption_enter_password);
                binding.encryptionPasswordInputContainer.setVisibility(View.VISIBLE);
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

            binding.encryptionStatus.setText(R.string.end_to_end_encryption_generating_keys);
        }

        @Override
        protected String doInBackground(Void... voids) {
            //  - create CSR, push to server, store returned public key in database
            //  - encrypt private key, push key to server, store unencrypted private key in database

            try {
                String publicKeyString;

                // Create public/private key pair
                KeyPair keyPair = EncryptionUtils.generateKeyPair();

                // create CSR
                AccountManager accountManager = AccountManager.get(getContext());
                String userId = accountManager.getUserData(user.toPlatformAccount(), AccountUtils.Constants.KEY_USER_ID);
                String urlEncoded = CsrHelper.generateCsrPemEncodedString(keyPair, userId);

                SendCSROperation operation = new SendCSROperation(urlEncoded);
                RemoteOperationResult result = operation.execute(user, getContext());

                if (result.isSuccess()) {
                    publicKeyString = (String) result.getData().get(0);

                    if (!EncryptionUtils.isMatchingKeys(keyPair, publicKeyString)) {
                        throw new RuntimeException("Wrong CSR returned");
                    }

                    Log_OC.d(TAG, "public key success");
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
                RemoteOperationResult storePrivateKeyResult = storePrivateKeyOperation.execute(user, getContext());

                if (storePrivateKeyResult.isSuccess()) {
                    Log_OC.d(TAG, "private key success");

                    arbitraryDataProvider.storeOrUpdateKeyValue(user.getAccountName(),
                                                                EncryptionUtils.PRIVATE_KEY,
                                                                privateKeyString);
                    arbitraryDataProvider.storeOrUpdateKeyValue(user.getAccountName(),
                                                                EncryptionUtils.PUBLIC_KEY,
                                                                publicKeyString);
                    arbitraryDataProvider.storeOrUpdateKeyValue(user.getAccountName(),
                                                                EncryptionUtils.MNEMONIC,
                                                                generateMnemonicString(true));

                    keyResult = KEY_CREATED;
                    return (String) storePrivateKeyResult.getData().get(0);
                } else {
                    DeletePublicKeyOperation deletePublicKeyOperation = new DeletePublicKeyOperation();
                    deletePublicKeyOperation.execute(user, getContext());
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
                notifyResult();
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

        binding.encryptionStatus.setText(R.string.end_to_end_encryption_keywords_description);
        viewThemeUtils.material.colorTextInputLayout(binding.encryptionPasswordInputContainer);

        binding.encryptionPassphrase.setText(generateMnemonicString(true));

        binding.encryptionPassphrase.setVisibility(View.VISIBLE);
        positiveButton.setText(R.string.end_to_end_encryption_confirm_button);
        positiveButton.setVisibility(View.VISIBLE);

        neutralButton.setVisibility(View.VISIBLE);
        viewThemeUtils.platform.colorTextButtons(positiveButton, neutralButton);

        keyResult = KEY_GENERATE;
    }

    @VisibleForTesting
    public void errorSavingKeys() {
        keyResult = KEY_FAILED;

        requireDialog().setTitle(R.string.common_error);
        binding.encryptionStatus.setText(R.string.end_to_end_encryption_unsuccessful);
        binding.encryptionPassphrase.setVisibility(View.GONE);
        positiveButton.setText(R.string.end_to_end_encryption_dialog_close);
        positiveButton.setVisibility(View.VISIBLE);
        viewThemeUtils.platform.colorTextButtons(positiveButton);
    }

    @VisibleForTesting
    public void setMnemonic(List<String> keyWords) {
        this.keyWords = keyWords;
    }
}
