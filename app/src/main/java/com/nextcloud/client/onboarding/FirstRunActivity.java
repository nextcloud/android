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

package com.nextcloud.client.onboarding;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.Intent;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.nextcloud.android.common.ui.theme.utils.ColorRole;
import com.nextcloud.client.account.UserAccountManager;
import com.nextcloud.client.appinfo.AppInfo;
import com.nextcloud.client.di.Injectable;
import com.nextcloud.client.preferences.AppPreferences;
import com.owncloud.android.BuildConfig;
import com.owncloud.android.R;
import com.owncloud.android.authentication.AuthenticatorActivity;
import com.owncloud.android.databinding.FirstRunActivityBinding;
import com.owncloud.android.features.FeatureItem;
import com.owncloud.android.ui.activity.BaseActivity;
import com.owncloud.android.ui.activity.FileDisplayActivity;
import com.owncloud.android.ui.adapter.FeaturesViewAdapter;
import com.owncloud.android.utils.DisplayUtils;
import com.owncloud.android.utils.theme.ViewThemeUtils;

import javax.inject.Inject;

import androidx.viewpager.widget.ViewPager;

/**
 * Activity displaying general feature after a fresh install.
 */
public class FirstRunActivity extends BaseActivity implements ViewPager.OnPageChangeListener, Injectable {

    public static final String EXTRA_ALLOW_CLOSE = "ALLOW_CLOSE";
    public static final String EXTRA_EXIT = "EXIT";
    public static final int FIRST_RUN_RESULT_CODE = 199;

    @Inject UserAccountManager userAccountManager;
    @Inject AppPreferences preferences;
    @Inject AppInfo appInfo;
    @Inject OnboardingService onboarding;

    @Inject ViewThemeUtils.Factory viewThemeUtilsFactory;

    private FirstRunActivityBinding binding;
    private ViewThemeUtils defaultViewThemeUtils;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        enableAccountHandling = false;

        super.onCreate(savedInstanceState);
        defaultViewThemeUtils = viewThemeUtilsFactory.withPrimaryAsBackground();
        defaultViewThemeUtils.platform.themeStatusBar(this, ColorRole.PRIMARY);
        this.binding = FirstRunActivityBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        boolean isProviderOrOwnInstallationVisible = getResources().getBoolean(R.bool.show_provider_or_own_installation);

        setSlideshowSize(getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE);


        defaultViewThemeUtils.material.colorMaterialButtonFilledOnPrimary(binding.login);
        binding.login.setOnClickListener(v -> {
            if (getIntent().getBooleanExtra(EXTRA_ALLOW_CLOSE, false)) {
                Intent authenticatorActivityIntent = new Intent(this, AuthenticatorActivity.class);
                authenticatorActivityIntent.putExtra(AuthenticatorActivity.EXTRA_USE_PROVIDER_AS_WEBLOGIN, false);
                startActivityForResult(authenticatorActivityIntent, FIRST_RUN_RESULT_CODE);
            } else {
                finish();
            }
        });


        defaultViewThemeUtils.material.colorMaterialButtonOutlinedOnPrimary(binding.signup);
        binding.signup.setVisibility(isProviderOrOwnInstallationVisible ? View.VISIBLE : View.GONE);
        binding.signup.setOnClickListener(v -> {
            Intent authenticatorActivityIntent = new Intent(this, AuthenticatorActivity.class);
            authenticatorActivityIntent.putExtra(AuthenticatorActivity.EXTRA_USE_PROVIDER_AS_WEBLOGIN, true);

            if (getIntent().getBooleanExtra(EXTRA_ALLOW_CLOSE, false)) {
                startActivityForResult(authenticatorActivityIntent, FIRST_RUN_RESULT_CODE);
            } else {
                authenticatorActivityIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(authenticatorActivityIntent);
            }
        });

        defaultViewThemeUtils.platform.colorTextView(binding.hostOwnServer, ColorRole.ON_PRIMARY);
        binding.hostOwnServer.setVisibility(isProviderOrOwnInstallationVisible ? View.VISIBLE : View.GONE);

        if (!isProviderOrOwnInstallationVisible) {
            binding.hostOwnServer.setOnClickListener(v -> onHostYourOwnServerClick());
        }


        // Sometimes, accounts are not deleted when you uninstall the application so we'll do it now
        if (onboarding.isFirstRun()) {
            userAccountManager.removeAllAccounts();
        }

        FeaturesViewAdapter featuresViewAdapter = new FeaturesViewAdapter(getSupportFragmentManager(), getFirstRun());
        binding.progressIndicator.setNumberOfSteps(featuresViewAdapter.getCount());
        binding.contentPanel.setAdapter(featuresViewAdapter);

        binding.contentPanel.addOnPageChangeListener(this);
    }

    private void setSlideshowSize(boolean isLandscape) {
        boolean isProviderOrOwnInstallationVisible = getResources().getBoolean(R.bool.show_provider_or_own_installation);

        LinearLayout.LayoutParams layoutParams;

        binding.buttonLayout.setOrientation(isLandscape ? LinearLayout.HORIZONTAL : LinearLayout.VERTICAL);

        if (isProviderOrOwnInstallationVisible) {
            layoutParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        } else {
            layoutParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, DisplayUtils.convertDpToPixel(isLandscape ? 100f : 150f, this));
        }

        binding.bottomLayout.setLayoutParams(layoutParams);
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

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
        // unused but to be implemented due to abstract parent
    }

    @Override
    public void onPageSelected(int position) {
        binding.progressIndicator.animateToStep(position + 1);
    }

    @Override
    public void onPageScrollStateChanged(int state) {
        // unused but to be implemented due to abstract parent
    }

    public void onHostYourOwnServerClick() {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.url_server_install)));
        DisplayUtils.startIntentIfAppAvailable(intent, this, R.string.no_browser_available);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (FIRST_RUN_RESULT_CODE == requestCode && RESULT_OK == resultCode) {

            String accountName = data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
            Account account = userAccountManager.getAccountByName(accountName);


            if (account == null) {
                DisplayUtils.showSnackMessage(this, R.string.account_creation_failed);
                return;
            }

            userAccountManager.setCurrentOwnCloudAccount(account.name);

            Intent i = new Intent(this, FileDisplayActivity.class);
            i.setAction(FileDisplayActivity.RESTART);
            i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(i);

            finish();
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
