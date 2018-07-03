/*
 * Nextcloud Android client application
 *
 * @author Bartosz Przybylski
 * Copyright (C) 2015 Bartosz Przybylski
 * Copyright (C) 2015 ownCloud Inc.
 * Copyright (C) 2016 Nextcloud.
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

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import com.nextcloud.client.preferences.AppPreferences;
import com.owncloud.android.MainApp;
import com.owncloud.android.R;
import com.owncloud.android.authentication.AccountUtils;
import com.nextcloud.client.preferences.PreferenceManager;
import com.owncloud.android.features.FeatureItem;
import com.owncloud.android.ui.adapter.FeaturesViewAdapter;
import com.owncloud.android.ui.adapter.FeaturesWebViewAdapter;
import com.owncloud.android.ui.whatsnew.ProgressIndicator;
import com.owncloud.android.utils.ThemeUtils;

import androidx.fragment.app.FragmentActivity;
import androidx.viewpager.widget.ViewPager;

/**
 * Activity displaying new features after an update.
 */
public class WhatsNewActivity extends FragmentActivity implements ViewPager.OnPageChangeListener {

    private ImageButton mForwardFinishButton;
    private Button mSkipButton;
    private ProgressIndicator mProgress;
    private ViewPager mPager;
    private AppPreferences preferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.whats_new_activity);
        preferences = PreferenceManager.fromContext(this);

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
                    getWhatsNew(this));
            mProgress.setNumberOfSteps(featuresViewAdapter.getCount());
            mPager.setAdapter(featuresViewAdapter);
        }

        mPager.addOnPageChangeListener(this);

        mForwardFinishButton = findViewById(R.id.forward);
        ThemeUtils.colorImageButton(mForwardFinishButton, fontColor);

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

        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            mForwardFinishButton.setBackground(null);
        } else {
            mForwardFinishButton.setBackgroundDrawable(null);
        }

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
            tv.setText(String.format(getString(R.string.whats_new_title), MainApp.getVersionName()));
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
        preferences.setLastSeenVersionCode(MainApp.getVersionCode());
    }

    public static void runIfNeeded(Context context) {
        if (!context.getResources().getBoolean(R.bool.show_whats_new) || context instanceof WhatsNewActivity) {
            return;
        }

        if (shouldShow(context)) {
            context.startActivity(new Intent(context, WhatsNewActivity.class));
        }
    }

    private static boolean shouldShow(Context context) {
        return !(context instanceof PassCodeActivity) && getWhatsNew(context).length > 0;
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

    static private boolean isFirstRun(Context context) {
        return AccountUtils.getCurrentOwnCloudAccount(context) == null;
    }

    private static FeatureItem[] getWhatsNew(Context context) {
        int itemVersionCode = 30030099;
        AppPreferences preferences = PreferenceManager.fromContext(context);

        if (!isFirstRun(context) && MainApp.getVersionCode() >= itemVersionCode
                && preferences.getLastSeenVersionCode() < itemVersionCode) {
            return new FeatureItem[]{new FeatureItem(R.drawable.whats_new_device_credentials,
                    R.string.whats_new_device_credentials_title, R.string.whats_new_device_credentials_content,
                    false, false)};
        } else {
            return new FeatureItem[0];
        }
    }
}
