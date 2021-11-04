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

import com.nextcloud.client.appinfo.AppInfo;
import com.nextcloud.client.di.Injectable;
import com.nextcloud.client.preferences.AppPreferences;
import com.owncloud.android.BuildConfig;
import com.owncloud.android.R;
import com.owncloud.android.ui.adapter.FeaturesViewAdapter;
import com.owncloud.android.ui.adapter.FeaturesWebViewAdapter;
import com.owncloud.android.ui.whatsnew.ProgressIndicator;
import com.owncloud.android.utils.theme.ThemeButtonUtils;
import com.owncloud.android.utils.theme.ThemeUtils;

import javax.inject.Inject;

import androidx.fragment.app.FragmentActivity;
import androidx.viewpager.widget.ViewPager;

/**
 * Activity displaying new features after an update.
 */
public class WhatsNewActivity extends FragmentActivity implements ViewPager.OnPageChangeListener, Injectable {

    private ImageButton mForwardFinishButton;
    private Button mSkipButton;
    private ProgressIndicator mProgress;
    private ViewPager mPager;
    @Inject AppPreferences preferences;
    @Inject AppInfo appInfo;
    @Inject OnboardingService onboarding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.whats_new_activity);

        int fontColor = getResources().getColor(R.color.login_text_color);

        mProgress = findViewById(R.id.progressIndicator);
        mPager = findViewById(R.id.contentPanel);
        String[] urls = getResources().getStringArray(R.array.whatsnew_urls);

        boolean showWebView = urls.length > 0;

        if (showWebView) {
            FeaturesWebViewAdapter featuresWebViewAdapter = new FeaturesWebViewAdapter(getSupportFragmentManager(),
                                                                                       urls);
            mProgress.setNumberOfSteps(featuresWebViewAdapter.getCount());
            mPager.setAdapter(featuresWebViewAdapter);
        } else {
            FeaturesViewAdapter featuresViewAdapter = new FeaturesViewAdapter(getSupportFragmentManager(),
                                                                              onboarding.getWhatsNew());
            mProgress.setNumberOfSteps(featuresViewAdapter.getCount());
            mPager.setAdapter(featuresViewAdapter);
        }

        mPager.addOnPageChangeListener(this);

        mForwardFinishButton = findViewById(R.id.forward);
        ThemeButtonUtils.colorImageButton(mForwardFinishButton, fontColor);

        mForwardFinishButton.setOnClickListener(view -> {
            if (mProgress.hasNextStep()) {
                mPager.setCurrentItem(mPager.getCurrentItem() + 1, true);
                mProgress.animateToStep(mPager.getCurrentItem() + 1);
            } else {
                onFinish();
                finish();
            }
            updateNextButtonIfNeeded();
        });

        mForwardFinishButton.setBackground(null);

        mSkipButton = findViewById(R.id.skip);
        mSkipButton.setTextColor(fontColor);
        mSkipButton.setOnClickListener(view -> {
            onFinish();
            finish();
        });

        TextView tv = findViewById(R.id.welcomeText);

        if (showWebView) {
            tv.setText(R.string.app_name);
        } else {
            tv.setText(String.format(getString(R.string.whats_new_title), appInfo.getFormattedVersionCode()));
        }

        updateNextButtonIfNeeded();
    }

    @Override
    public void onBackPressed() {
        onFinish();
        super.onBackPressed();
    }

    private void updateNextButtonIfNeeded() {
        if (!mProgress.hasNextStep()) {
            mForwardFinishButton.setImageResource(R.drawable.ic_ok);
            mSkipButton.setVisibility(View.INVISIBLE);
        } else {
            mForwardFinishButton.setImageResource(R.drawable.arrow_right);
            mSkipButton.setVisibility(View.VISIBLE);
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
        mProgress.animateToStep(position + 1);
        updateNextButtonIfNeeded();
    }

    @Override
    public void onPageScrollStateChanged(int state) {
        // unused but to be implemented due to abstract parent
    }
}

