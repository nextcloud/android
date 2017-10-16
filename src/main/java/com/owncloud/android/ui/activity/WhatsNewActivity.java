/**
 * Nextcloud Android client application
 *
 * @author Bartosz Przybylski
 * Copyright (C) 2015 Bartosz Przybylski
 * Copyright (C) 2015 ownCloud Inc.
 * Copyright (C) 2016 Nextcloud.
 * <p>
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU AFFERO GENERAL PUBLIC LICENSE
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or any later version.
 * <p>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU AFFERO GENERAL PUBLIC LICENSE for more details.
 * <p>
 * You should have received a copy of the GNU Affero General Public
 * License along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.owncloud.android.ui.activity;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.owncloud.android.MainApp;
import com.owncloud.android.R;
import com.owncloud.android.authentication.AccountAuthenticatorActivity;
import com.owncloud.android.authentication.AccountUtils;
import com.owncloud.android.features.FeatureList;
import com.owncloud.android.features.FeatureList.FeatureItem;
import com.owncloud.android.ui.whatsnew.ProgressIndicator;
import com.owncloud.android.utils.AnalyticsUtils;

/**
 * Activity displaying general feature after a fresh install and new features after an update.
 */
public class WhatsNewActivity extends FragmentActivity implements ViewPager.OnPageChangeListener {

    private static final String KEY_LAST_SEEN_VERSION_CODE = "lastSeenVersionCode";

    private static final String SCREEN_NAME = "What's new";

    private static final String TAG = WhatsNewActivity.class.getSimpleName();

    private ImageButton mForwardFinishButton;
    private Button mSkipButton;
    private ProgressIndicator mProgress;
    private ViewPager mPager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.whats_new_activity);

        mProgress = (ProgressIndicator) findViewById(R.id.progressIndicator);
        mPager = (ViewPager) findViewById(R.id.contentPanel);
        final boolean isBeta = getResources().getBoolean(R.bool.is_beta);
        String[] urls = getResources().getStringArray(R.array.whatsnew_urls);

        // Sometimes, accounts are not deleted when you uninstall the application so we'll do it now
        if (isFirstRun()) {
            AccountManager am = (AccountManager) getSystemService(ACCOUNT_SERVICE);
            for (Account account : AccountUtils.getAccounts(this)) {
                am.removeAccount(account, null, null);
            }
        }

        boolean showWebView = urls.length > 0;

        if (showWebView) {
            FeaturesWebViewAdapter featuresWebViewAdapter = new FeaturesWebViewAdapter(getSupportFragmentManager(),
                    urls);
            mProgress.setNumberOfSteps(featuresWebViewAdapter.getCount());
            mPager.setAdapter(featuresWebViewAdapter);
        } else {
            FeaturesViewAdapter featuresViewAdapter = new FeaturesViewAdapter(getSupportFragmentManager(),
                    FeatureList.getFiltered(getLastSeenVersionCode(), isFirstRun(), isBeta));
            mProgress.setNumberOfSteps(featuresViewAdapter.getCount());
            mPager.setAdapter(featuresViewAdapter);
        }

        mPager.addOnPageChangeListener(this);


        mForwardFinishButton = (ImageButton) findViewById(R.id.forward);
        mForwardFinishButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mProgress.hasNextStep()) {
                    mPager.setCurrentItem(mPager.getCurrentItem() + 1, true);
                    mProgress.animateToStep(mPager.getCurrentItem() + 1);
                } else {
                    onFinish();
                    finish();
                }
                updateNextButtonIfNeeded();
            }
        });

        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            mForwardFinishButton.setBackground(null);
        } else {
            mForwardFinishButton.setBackgroundDrawable(null);
        }

        mSkipButton = (Button) findViewById(R.id.skip);
        mSkipButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onFinish();
                finish();
            }
        });

        TextView tv = (TextView) findViewById(R.id.welcomeText);

        if (showWebView) {
            tv.setText(R.string.app_name);
        } else if (isFirstRun()) {
            tv.setText(R.string.empty);
        } else {
            tv.setText(String.format(getString(R.string.whats_new_title), MainApp.getVersionName()));
        }

        updateNextButtonIfNeeded();
    }

    @Override
    protected void onResume() {
        super.onResume();
        AnalyticsUtils.setCurrentScreenName(this, SCREEN_NAME, TAG);
    }

    @Override
    public void onBackPressed() {
        onFinish();
        super.onBackPressed();
    }


    private void updateNextButtonIfNeeded() {
        if (!mProgress.hasNextStep()) {
            mForwardFinishButton.setImageResource(R.drawable.ic_done_white);
            mSkipButton.setVisibility(View.INVISIBLE);
        } else {
            mForwardFinishButton.setImageResource(R.drawable.arrow_right);
            mSkipButton.setVisibility(View.VISIBLE);
        }
    }

    private void onFinish() {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = pref.edit();
        editor.putInt(KEY_LAST_SEEN_VERSION_CODE, MainApp.getVersionCode());
        editor.apply();
    }

    static private int getLastSeenVersionCode() {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(MainApp.getAppContext());
        return pref.getInt(KEY_LAST_SEEN_VERSION_CODE, 0);
    }

    static private boolean isFirstRun() {
        return getLastSeenVersionCode() == 0 && AccountUtils.getCurrentOwnCloudAccount(MainApp.getAppContext()) == null;
    }

    static public void runIfNeeded(Context context) {
        if (!context.getResources().getBoolean(R.bool.show_whats_new)) {
            return;
        }

        if (context instanceof WhatsNewActivity) {
            return;
        }

        if (shouldShow(context)) {
            context.startActivity(new Intent(context, WhatsNewActivity.class));
        }
    }

    static private boolean shouldShow(Context context) {
        final boolean isBeta = context.getResources().getBoolean(R.bool.is_beta);
        return (isFirstRun() && context instanceof AccountAuthenticatorActivity) ||
                (
                        !(isFirstRun() && (context instanceof FileDisplayActivity)) &&
                                !(context instanceof PassCodeActivity) &&
                                (FeatureList.getFiltered(getLastSeenVersionCode(), isFirstRun(), isBeta).length > 0)
                );
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

    private final class FeaturesWebViewAdapter extends FragmentPagerAdapter {
        private String[] mWebUrls;

        public FeaturesWebViewAdapter(FragmentManager fm, String[] webUrls) {
            super(fm);
            mWebUrls = webUrls;
        }

        @Override
        public Fragment getItem(int position) {
            return FeatureWebFragment.newInstance(mWebUrls[position]);
        }

        @Override
        public int getCount() {
            return mWebUrls.length;
        }
    }

    public static class FeatureWebFragment extends Fragment {
        private String mWebUrl;

        static public FeatureWebFragment newInstance(String webUrl) {
            FeatureWebFragment f = new FeatureWebFragment();
            Bundle args = new Bundle();
            args.putString("url", webUrl);
            f.setArguments(args);
            return f;
        }

        @Override
        public void onCreate(@Nullable Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            mWebUrl = getArguments() != null ? getArguments().getString("url") : null;
        }

        @Nullable
        @Override
        public View onCreateView(LayoutInflater inflater,
                                 @Nullable ViewGroup container,
                                 @Nullable Bundle savedInstanceState) {
            View v = inflater.inflate(R.layout.whats_new_webview_element, container, false);

            WebView webView = (WebView) v.findViewById(R.id.whatsNewWebView);
            webView.getSettings().setJavaScriptEnabled(true);
            webView.getSettings().setDomStorageEnabled(true);
            webView.getSettings().setAllowFileAccess(false);
            webView.setWebViewClient(new WebViewClient());
            webView.loadUrl(mWebUrl);

            return v;
        }
    }

    private final class FeaturesViewAdapter extends FragmentPagerAdapter {

        private FeatureItem[] mFeatures;

        public FeaturesViewAdapter(FragmentManager fm, FeatureItem[] features) {
            super(fm);
            mFeatures = features;
        }

        @Override
        public Fragment getItem(int position) {
            return FeatureFragment.newInstance(mFeatures[position]);
        }

        @Override
        public int getCount() {
            return mFeatures.length;
        }
    }

    public static class FeatureFragment extends Fragment {
        private FeatureItem mItem;

        static public FeatureFragment newInstance(FeatureItem item) {
            FeatureFragment f = new FeatureFragment();
            Bundle args = new Bundle();
            args.putParcelable("feature", item);
            f.setArguments(args);
            return f;
        }

        @Override
        public void onCreate(@Nullable Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            mItem = getArguments() != null ? (FeatureItem) getArguments().getParcelable("feature") : null;
        }

        @Nullable
        @Override
        public View onCreateView(LayoutInflater inflater,
                                 @Nullable ViewGroup container,
                                 @Nullable Bundle savedInstanceState) {
            View v = inflater.inflate(R.layout.whats_new_element, container, false);

            ImageView iv = (ImageView) v.findViewById(R.id.whatsNewImage);
            if (mItem.shouldShowImage()) {
                iv.setImageResource(mItem.getImage());
            }

            TextView tv2 = (TextView) v.findViewById(R.id.whatsNewTitle);
            if (mItem.shouldShowTitleText()) {
                tv2.setText(mItem.getTitleText());
            }

            tv2 = (TextView) v.findViewById(R.id.whatsNewText);
            if (mItem.shouldShowContentText()) {
                tv2.setText(mItem.getContentText());

                if (!mItem.shouldContentCentered()) {
                    tv2.setGravity(Gravity.START);
                }
            }

            return v;
        }
    }
}
