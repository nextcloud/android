/*
 * Nextcloud Android client application
 *
 * @author David Luhmer
 * @author Andy Scherzinger
 * Copyright (C) 2018 David Luhmer
 * Copyright (C) 2018 Andy Scherzinger
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

package com.owncloud.android.ui.activity;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.nextcloud.android.sso.Constants;
import com.owncloud.android.MainApp;
import com.owncloud.android.R;
import com.owncloud.android.databinding.DialogSsoGrantPermissionBinding;
import com.owncloud.android.lib.common.OwnCloudAccount;
import com.owncloud.android.lib.common.accounts.AccountUtils;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.utils.EncryptionUtils;
import com.owncloud.android.utils.theme.ViewThemeUtils;

import java.util.UUID;

import javax.inject.Inject;

import androidx.appcompat.app.AlertDialog;

import static com.nextcloud.android.sso.Constants.DELIMITER;
import static com.nextcloud.android.sso.Constants.EXCEPTION_ACCOUNT_ACCESS_DECLINED;
import static com.nextcloud.android.sso.Constants.EXCEPTION_ACCOUNT_NOT_FOUND;
import static com.nextcloud.android.sso.Constants.NEXTCLOUD_FILES_ACCOUNT;
import static com.nextcloud.android.sso.Constants.NEXTCLOUD_SSO;
import static com.nextcloud.android.sso.Constants.NEXTCLOUD_SSO_EXCEPTION;
import static com.nextcloud.android.sso.Constants.SSO_SHARED_PREFERENCE;


/**
 * Activity for granting access rights to a Nextcloud account, used for SSO.
 */
public class SsoGrantPermissionActivity extends BaseActivity {

    private static final String TAG = SsoGrantPermissionActivity.class.getCanonicalName();

    private String packageName;
    private Account account;

    @Inject ViewThemeUtils.Factory themeUtilsFactory;
    private ViewThemeUtils viewThemeUtils;

    private AlertDialog dialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        viewThemeUtils = themeUtilsFactory.withDefaultSchemes();

        DialogSsoGrantPermissionBinding binding = DialogSsoGrantPermissionBinding.inflate(getLayoutInflater());

        ComponentName callingActivity = getCallingActivity();

        if (callingActivity != null) {
            packageName = callingActivity.getPackageName();
            final String appName = getAppNameForPackage(packageName);
            account = getIntent().getParcelableExtra(NEXTCLOUD_FILES_ACCOUNT);

            final SpannableStringBuilder dialogText = makeSpecialPartsBold(
                getString(R.string.single_sign_on_request_token, appName, account.name),
                appName,
                account.name);
            binding.permissionText.setText(dialogText);
            try {
                if (packageName != null) {
                    Drawable appIcon = getPackageManager().getApplicationIcon(packageName);
                    binding.appIcon.setImageDrawable(appIcon);
                }
            } catch (PackageManager.NameNotFoundException e) {
                Log_OC.e(TAG, "Error retrieving app icon", e);
            }

            final MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this)
                .setView(binding.getRoot())
                .setCancelable(false)
                .setPositiveButton(R.string.permission_allow, (dialog, which) -> grantPermission())
                .setNegativeButton(R.string.permission_deny, (dialog, which) -> exitFailed());

            viewThemeUtils.dialog.colorMaterialAlertDialogBackground(this, builder);

            dialog = builder.create();
            dialog.show();

            Log_OC.v(TAG, "TOKEN-REQUEST: Calling Package: " + packageName);
            Log_OC.v(TAG, "TOKEN-REQUEST: App Name: " + appName);
        } else {
            // Activity was not started using startActivityForResult!
            Log_OC.e(TAG, "Calling Package is null");
            setResultAndExit("Request was not executed properly. Use startActivityForResult()");
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        viewThemeUtils.platform.colorTextButtons(dialog.getButton(AlertDialog.BUTTON_POSITIVE),
                                                 dialog.getButton(AlertDialog.BUTTON_NEGATIVE));
    }

    private SpannableStringBuilder makeSpecialPartsBold(String text, String... toBeStyledText) {
        SpannableStringBuilder ssb = new SpannableStringBuilder(text);
        for (String textBlock : toBeStyledText) {
            int start = text.indexOf(textBlock);
            int end = start + textBlock.length();
            ssb.setSpan(new StyleSpan(Typeface.BOLD), start, end, 0);
            ssb.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.text_color)), start, end,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        return ssb;
    }

    private void setResultAndExit(String exception) {
        Intent data = new Intent();
        data.putExtra(NEXTCLOUD_SSO_EXCEPTION, exception);
        setResult(RESULT_CANCELED, data);
        finish();
    }

    private String getAppNameForPackage(String pkg) {
        final PackageManager pm = getApplicationContext().getPackageManager();
        ApplicationInfo ai = null;
        try {
            ai = pm.getApplicationInfo(pkg, 0);
        } catch (final PackageManager.NameNotFoundException e) {
            Log_OC.e(TAG, "Error fetching app name for package", e);
        }
        return (String) (ai != null ? pm.getApplicationLabel(ai) : "(unknown)");
    }

    private void exitFailed() {
        setResultAndExit(EXCEPTION_ACCOUNT_ACCESS_DECLINED);
    }

    private void grantPermission() {
        // create token
        SharedPreferences sharedPreferences = getSharedPreferences(SSO_SHARED_PREFERENCE, Context.MODE_PRIVATE);
        String token = UUID.randomUUID().toString().replaceAll("-", "");

        String hashedTokenWithSalt = EncryptionUtils.generateSHA512(token);

        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(packageName + DELIMITER + account.name, hashedTokenWithSalt);
        editor.apply();

        String serverUrl;
        String userId;
        try {
            OwnCloudAccount ocAccount = new OwnCloudAccount(account, this);
            serverUrl = ocAccount.getBaseUri().toString();
            AccountManager accountManager = AccountManager.get(this);
            userId = accountManager.getUserData(account,
                    com.owncloud.android.lib.common.accounts.AccountUtils.Constants.KEY_USER_ID);
        } catch (AccountUtils.AccountNotFoundException e) {
            Log_OC.e(TAG, "Account not found");
            setResultAndExit(EXCEPTION_ACCOUNT_NOT_FOUND);
            return;
        }

        final Bundle result = new Bundle();
        result.putString(AccountManager.KEY_ACCOUNT_NAME, account.name);
        result.putString(AccountManager.KEY_ACCOUNT_TYPE, MainApp.getAccountType(this));
        result.putString(AccountManager.KEY_AUTHTOKEN, NEXTCLOUD_SSO);
        result.putString(Constants.SSO_USER_ID, userId);
        result.putString(Constants.SSO_TOKEN, token);
        result.putString(Constants.SSO_SERVER_URL, serverUrl);

        Intent data = new Intent();
        data.putExtra(NEXTCLOUD_SSO, result);
        setResult(RESULT_OK, data);
        finish();
    }
}
