/*
 * Nextcloud Android client application
 *
 * @author Bartosz Przybylski
 * @author Chris Narkiewicz
 * Copyright (C) 2015 Bartosz Przybylski
 * Copyright (C) 2015 ownCloud Inc.
 * Copyright (C) 2016 Nextcloud.
 * Copyright (C) 2019 Chris Narkiewicz <hello@ezaquarii.com>
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU AFFERO GENERAL PUBLIC LICENSE
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU AFFERO GENERAL PUBLIC LICENSE for more details.
 *
 * You should have received a copy of the GNU Affero General Public
 * License along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.owncloud.android.ui.activity;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.viewpager.widget.ViewPager;

import com.nextcloud.client.account.UserAccountManager;
import com.nextcloud.client.appinfo.AppInfo;
import com.nextcloud.client.di.Injectable;
import com.nextcloud.client.preferences.AppPreferences;
import com.nextcloud.client.whatsnew.WhatsNewService;
import com.owncloud.android.BuildConfig;
import com.owncloud.android.MainApp;
import com.owncloud.android.R;
import com.owncloud.android.authentication.AccountUtils;
import com.owncloud.android.authentication.AuthenticatorActivity;
import com.owncloud.android.features.FeatureItem;
import com.owncloud.android.ui.adapter.FeaturesViewAdapter;
import com.owncloud.android.ui.whatsnew.ProgressIndicator;
import com.owncloud.android.utils.DisplayUtils;

import javax.inject.Inject;

/**
 * Activity displaying general feature after a fresh install.
 */
public class FirstRunActivity extends BaseActivity implements ViewPager.OnPageChangeListener, Injectable {

    public static final String EXTRA_ALLOW_CLOSE = "ALLOW_CLOSE";
    public static final String EXTRA_EXIT = "EXIT";
    public static final int FIRST_RUN_RESULT_CODE = 199;

    private ProgressIndicator progressIndicator;

    @Inject UserAccountManager userAccountManager;
    @Inject AppPreferences preferences;
    @Inject AppInfo appInfo;
    @Inject WhatsNewService whatsNew;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.first_run_activity);

        boolean isProviderOrOwnInstallationVisible = getResources().getBoolean(R.bool.show_provider_or_own_installation);

        setSlideshowSize(getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE);

        Button loginButton = findViewById(R.id.login);
        loginButton.setBackgroundColor(Color.WHITE);
        loginButton.setTextColor(getResources().getColor(R.color.primary));

        loginButton.setOnClickListener(v -> {
            if (getIntent().getBooleanExtra(EXTRA_ALLOW_CLOSE, false)) {
                Intent authenticatorActivityIntent = new Intent(this, AuthenticatorActivity.class);
                authenticatorActivityIntent.putExtra(AuthenticatorActivity.EXTRA_USE_PROVIDER_AS_WEBLOGIN, false);
                startActivityForResult(authenticatorActivityIntent, FIRST_RUN_RESULT_CODE);
            } else {
                finish();
            }
        });

        Button providerButton = findViewById(R.id.signup);
        providerButton.setBackgroundColor(getResources().getColor(R.color.primary_dark));
        providerButton.setTextColor(getResources().getColor(R.color.login_text_color));
        providerButton.setVisibility(isProviderOrOwnInstallationVisible ? View.VISIBLE : View.GONE);
        providerButton.setOnClickListener(v -> {
            Intent authenticatorActivityIntent = new Intent(this, AuthenticatorActivity.class);
            authenticatorActivityIntent.putExtra(AuthenticatorActivity.EXTRA_USE_PROVIDER_AS_WEBLOGIN, true);

            if (getIntent().getBooleanExtra(EXTRA_ALLOW_CLOSE, false)) {
                startActivityForResult(authenticatorActivityIntent, FIRST_RUN_RESULT_CODE);
            } else {
                authenticatorActivityIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(authenticatorActivityIntent);
            }
        });

        TextView hostOwnServerTextView = findViewById(R.id.host_own_server);
        hostOwnServerTextView.setTextColor(getResources().getColor(R.color.login_text_color));
        hostOwnServerTextView.setVisibility(isProviderOrOwnInstallationVisible ? View.VISIBLE : View.GONE);

        progressIndicator = findViewById(R.id.progressIndicator);
        ViewPager viewPager = findViewById(R.id.contentPanel);

        // Sometimes, accounts are not deleted when you uninstall the application so we'll do it now
        if (whatsNew.isFirstRun()) {
            AccountManager am = (AccountManager) getSystemService(ACCOUNT_SERVICE);
            if (am != null) {
                for (Account account : userAccountManager.getAccounts()) {
                    am.removeAccount(account, null, null);
                }
            }
        }

        FeaturesViewAdapter featuresViewAdapter = new FeaturesViewAdapter(getSupportFragmentManager(), getFirstRun());
        progressIndicator.setNumberOfSteps(featuresViewAdapter.getCount());
        viewPager.setAdapter(featuresViewAdapter);

        viewPager.addOnPageChangeListener(this);
    }

    private void setSlideshowSize(boolean isLandscape) {
        boolean isProviderOrOwnInstallationVisible = getResources().getBoolean(R.bool.show_provider_or_own_installation);
        LinearLayout buttonLayout = findViewById(R.id.buttonLayout);
        LinearLayout.LayoutParams layoutParams;

        buttonLayout.setOrientation(isLandscape ? LinearLayout.HORIZONTAL : LinearLayout.VERTICAL);

        LinearLayout bottomLayout = findViewById(R.id.bottomLayout);
        if (isProviderOrOwnInstallationVisible) {
            layoutParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
        } else {
            layoutParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                    DisplayUtils.convertDpToPixel(isLandscape ? 100f : 150f, this));
        }

        bottomLayout.setLayoutParams(layoutParams);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            setSlideshowSize(true);
        } else {
            setSlideshowSize(false);
        }
    }

    @Override
    public void onBackPressed() {
        onFinish();

        if (getIntent().getBooleanExtra(EXTRA_ALLOW_CLOSE, false)) {
            super.onBackPressed();
        } else {
            Intent intent = new Intent(getApplicationContext(), AuthenticatorActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            intent.putExtra(EXTRA_EXIT, true);
            startActivity(intent);
            finish();
        }
    }

    private void onFinish() {
        preferences.setLastSeenVersionCode(BuildConfig.VERSION_CODE);
    }

    @Override
    protected void onStop() {
        onFinish();

        super.onStop();
    }

    private static boolean isFirstRun(Context context) {
        return AccountUtils.getCurrentOwnCloudAccount(context) == null;
    }

    public static boolean runIfNeeded(Context context) {
        boolean isProviderOrOwnInstallationVisible = context.getResources()
                .getBoolean(R.bool.show_provider_or_own_installation);

        if (!isProviderOrOwnInstallationVisible) {
            return false;
        }

        if (context instanceof FirstRunActivity) {
            return false;
        }

        if (isFirstRun(context) && context instanceof AuthenticatorActivity) {
            ((AuthenticatorActivity) context).startActivityForResult(new Intent(context, FirstRunActivity.class),
                                                                     AuthenticatorActivity.REQUEST_CODE_FIRST_RUN);
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
        // unused but to be implemented due to abstract parent
    }

    @Override
    public void onPageSelected(int position) {
        progressIndicator.animateToStep(position + 1);
    }

    @Override
    public void onPageScrollStateChanged(int state) {
        // unused but to be implemented due to abstract parent
    }

    public void onHostYourOwnServerClick(View view) {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.url_server_install)));
        DisplayUtils.startIntentIfAppAvailable(intent, this, R.string.no_browser_available);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (FIRST_RUN_RESULT_CODE == requestCode && RESULT_OK == resultCode) {

            String accountName = data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
            Account account = AccountUtils.getOwnCloudAccountByName(this, accountName);

            if (account == null) {
                DisplayUtils.showSnackMessage(this, R.string.account_creation_failed);
                return;
            }

            setAccount(account);
            AccountUtils.setCurrentOwnCloudAccount(this, account.name);
            onAccountSet(false);

            Intent i = new Intent(this, FileDisplayActivity.class);
            i.setAction(FileDisplayActivity.RESTART);
            i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(i);
        }
    }

    public static FeatureItem[] getFirstRun() {
        return new FeatureItem[]{
                new FeatureItem(R.drawable.logo, R.string.first_run_1_text, R.string.empty, true, false),
                new FeatureItem(R.drawable.first_run_files, R.string.first_run_2_text, R.string.empty, true, false),
                new FeatureItem(R.drawable.first_run_groupware, R.string.first_run_3_text, R.string.empty, true, false),
                new FeatureItem(R.drawable.first_run_talk, R.string.first_run_4_text, R.string.empty, true, false)};
    }
}
