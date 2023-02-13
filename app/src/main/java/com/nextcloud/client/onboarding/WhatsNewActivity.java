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

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import com.google.android.material.button.MaterialButton;
import com.nextcloud.android.common.ui.theme.utils.ColorRole;
import com.nextcloud.client.appinfo.AppInfo;
import com.nextcloud.client.di.Injectable;
import com.nextcloud.client.preferences.AppPreferences;
import com.owncloud.android.BuildConfig;
import com.owncloud.android.R;
import com.owncloud.android.databinding.WhatsNewActivityBinding;
import com.owncloud.android.ui.adapter.FeaturesViewAdapter;
import com.owncloud.android.ui.adapter.FeaturesWebViewAdapter;
import com.owncloud.android.ui.whatsnew.ProgressIndicator;
import com.owncloud.android.utils.theme.ViewThemeUtils;

import javax.inject.Inject;

import androidx.fragment.app.FragmentActivity;
import androidx.viewpager.widget.ViewPager;

/**
 * Activity displaying new features after an update.
 */
public class WhatsNewActivity extends FragmentActivity implements ViewPager.OnPageChangeListener, Injectable {

    @Inject AppPreferences preferences;
    @Inject AppInfo appInfo;
    @Inject OnboardingService onboarding;
    @Inject ViewThemeUtils.Factory viewThemeUtilsFactory;
    private ViewThemeUtils viewThemeUtils;
    
    private WhatsNewActivityBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = WhatsNewActivityBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        viewThemeUtils = viewThemeUtilsFactory.withPrimaryAsBackground();
        viewThemeUtils.platform.themeStatusBar(this, ColorRole.PRIMARY);

        
        String[] urls = getResources().getStringArray(R.array.whatsnew_urls);

        boolean showWebView = urls.length > 0;

        if (showWebView) {
            FeaturesWebViewAdapter featuresWebViewAdapter = new FeaturesWebViewAdapter(getSupportFragmentManager(),
                                                                                       urls);
            binding.progressIndicator.setNumberOfSteps(featuresWebViewAdapter.getCount());
            binding.contentPanel.setAdapter(featuresWebViewAdapter);
        } else {
            FeaturesViewAdapter featuresViewAdapter = new FeaturesViewAdapter(getSupportFragmentManager(),
                                                                              onboarding.getWhatsNew());
            binding.progressIndicator.setNumberOfSteps(featuresViewAdapter.getCount());
            binding.contentPanel.setAdapter(featuresViewAdapter);
        }

        binding.contentPanel.addOnPageChangeListener(this);

        viewThemeUtils.platform.colorImageView(binding.forward, ColorRole.ON_PRIMARY);

        binding.forward.setOnClickListener(view -> {
            if (binding.progressIndicator.hasNextStep()) {
                binding.contentPanel.setCurrentItem(binding.contentPanel.getCurrentItem() + 1, true);
                binding.progressIndicator.animateToStep(binding.contentPanel.getCurrentItem() + 1);
            } else {
                onFinish();
                finish();
            }
            updateNextButtonIfNeeded();
        });

        binding.forward.setBackground(null);

        viewThemeUtils.platform.colorTextView(binding.skip, ColorRole.ON_PRIMARY);
        binding.skip.setOnClickListener(view -> {
            onFinish();
            finish();
        });

        viewThemeUtils.platform.colorTextView(binding.welcomeText, ColorRole.ON_PRIMARY);

        if (showWebView) {
            binding.welcomeText.setText(R.string.app_name);
        } else {
            binding.welcomeText.setText(String.format(getString(R.string.whats_new_title), appInfo.getVersionName()));
        }

        updateNextButtonIfNeeded();
    }

    @Override
    public void onBackPressed() {
        onFinish();
        super.onBackPressed();
    }

    private void updateNextButtonIfNeeded() {
        if (!binding.progressIndicator.hasNextStep()) {
            binding.forward.setImageResource(R.drawable.ic_ok);
            binding.skip.setVisibility(View.INVISIBLE);
        } else {
            binding.forward.setImageResource(R.drawable.arrow_right);
            binding.skip.setVisibility(View.VISIBLE);
        }
    }

    private void onFinish() {
        preferences.setLastSeenVersionCode(BuildConfig.VERSION_CODE);
    }

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
        // unused but to be implemented due to abstract parent
    }

    @Override
    public void onPageSelected(int position) {
        binding.progressIndicator.animateToStep(position + 1);
        updateNextButtonIfNeeded();
    }

    @Override
    public void onPageScrollStateChanged(int state) {
        // unused but to be implemented due to abstract parent
    }
}

