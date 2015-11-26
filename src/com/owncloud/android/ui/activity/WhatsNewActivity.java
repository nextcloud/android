/**
 *   ownCloud Android client application
 *
 *   @author Bartosz Przybylski
 *   Copyright (C) 2015 Bartosz Przybylski
 *   Copyright (C) 2015 ownCloud Inc.
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License version 2,
 *   as published by the Free Software Foundation.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package com.owncloud.android.ui.activity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.owncloud.android.BuildConfig;
import com.owncloud.android.MainApp;
import com.owncloud.android.R;
import com.owncloud.android.ui.whatsnew.ProgressIndicator;
import com.owncloud.android.lib.common.utils.Log_OC;

import java.util.LinkedList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * @author Bartosz Przybylski
 */
public class WhatsNewActivity extends Activity {
	private static String TAG = WhatsNewActivity.class.getSimpleName();

	private static final String KEY_LAST_SEEN_VERSION_CODE = "lastSeenVersionCode";

	private FeatureItem[] mFeaturesToShow;

	private ImageButton mForwardFinishButton;
	private ProgressIndicator mProgress;
	private LinearLayout mContentPanel;

	private int mCurrentStep;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.whats_new_activity);

		mCurrentStep = 0;
		mProgress = (ProgressIndicator) findViewById(R.id.progressIndicator);
		mFeaturesToShow = filterFeaturesToShow();

		mProgress.setNumberOfSteps(mFeaturesToShow.length);

		mForwardFinishButton = (ImageButton) findViewById(R.id.forward);
		mForwardFinishButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				if (mProgress.hasNextStep()) {
					mProgress.animateToNextStep();
					mContentPanel.animate().x(-mContentPanel.getChildAt(++mCurrentStep).getLeft());
				} else if (mProgress.hasPrevStep()) {
					mProgress.animateToPrevStep();
					mContentPanel.animate().x(-mContentPanel.getChildAt(--mCurrentStep).getLeft());
				} else {
					onFinish();
					finish();
				}
				if (!mProgress.hasNextStep()) {
					mForwardFinishButton.setImageResource(R.drawable.ic_ok);
				} else {
					mForwardFinishButton.setImageResource(R.drawable.ic_menu_forward);
				}
			}
		});
		Button skipButton = (Button) findViewById(R.id.skip);
		skipButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				onFinish();
				finish();
			}
		});

		TextView tv = (TextView)findViewById(R.id.welcomeText);
		tv.setText(isFirstRun() ? R.string.welcome_to_oc_title : R.string.whats_new_title);


		mContentPanel = (LinearLayout)findViewById(R.id.contentPanel);
		LinearLayout.LayoutParams ll2 = (LinearLayout.LayoutParams) mContentPanel.getLayoutParams();
		ll2.width = getScreenWidth()*mFeaturesToShow.length;
		mContentPanel.setLayoutParams(ll2);

		fillContentPanelWithFeatureData();
	}

	private void fillContentPanelWithFeatureData() {
		LayoutInflater inflater = (LayoutInflater)getSystemService(LAYOUT_INFLATER_SERVICE);

		for (int i = 0; i < mFeaturesToShow.length; ++i) {
			FeatureItem item = mFeaturesToShow[i];
			LinearLayout newElement = (LinearLayout)inflater.inflate(R.layout.whats_new_element, null);

			LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(getScreenWidth(), ViewGroup.LayoutParams.MATCH_PARENT);
			newElement.setLayoutParams(params);

			mContentPanel.addView(newElement);
			ImageView iv = (ImageView)newElement.findViewById(R.id.whatsNewImage);
			if (item.getImage() != FeatureItem.doNotShow)
				iv.setImageResource(item.getImage());

			TextView tv2 = (TextView)newElement.findViewById(R.id.whatsNewTitle);
			if (item.getTitleText() != FeatureItem.doNotShow)
				tv2.setText(item.getTitleText());

			tv2 = (TextView)newElement.findViewById(R.id.whatsNewText);
			if (item.getContentText() != FeatureItem.doNotShow)
				tv2.setText(item.getContentText());
		}
	}

	private void onFinish() {
		SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
		SharedPreferences.Editor editor = pref.edit();
		editor.putInt(KEY_LAST_SEEN_VERSION_CODE, BuildConfig.VERSION_CODE);
		editor.apply();
	}

	private int getScreenWidth() {
		DisplayMetrics dm = new DisplayMetrics();
		getWindowManager().getDefaultDisplay().getMetrics(dm);
		return dm.widthPixels;
	}

	static private int getLastSeenVersionCode() {
		SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(MainApp.getAppContext());
		return pref.getInt(KEY_LAST_SEEN_VERSION_CODE, 0);
	}

	static private boolean isFirstRun() {
		return getLastSeenVersionCode() == 0;
	}


	static public void runIfNeeded(Context context) {
		if (context instanceof WhatsNewActivity)
			return;

		if (filterFeaturesToShow().length > 0)
			context.startActivity(new Intent(context, WhatsNewActivity.class));
	}

	static private FeatureItem[] filterFeaturesToShow() {
		List<FeatureItem> features = new LinkedList<>();

		SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(MainApp.getAppContext());
		final int lastSeenVersionCode = getLastSeenVersionCode();
		final boolean isFirstRun = isFirstRun();

		for (FeatureItem item : featuresToShow) {
			if (isFirstRun && item.shouldShowOnFirstRun()) {
				features.add(item);
			} else if (!isFirstRun && !item.shouldShowOnFirstRun() &&
					BuildConfig.VERSION_CODE >= item.getVersionNumber() &&
					lastSeenVersionCode < item.getVersionNumber()) {
				features.add(item);
			}
		}
		return features.toArray(new FeatureItem[features.size()]);
	}

	static FeatureItem featuresToShow[] = {
			new FeatureItem(R.drawable.logo, R.string.welcome_feature_1_title, R.string.welcome_feature_1_text, "1.0.0", true),
			new FeatureItem(R.drawable.logo, R.string.welcome_feature_2_title,  R.string.welcome_feature_2_text, "1.0.0", true),
			new FeatureItem(R.drawable.logo, R.string.welcome_feature_3_title,  R.string.welcome_feature_3_text, "1.0.0", true),
			new FeatureItem(R.drawable.logo, R.string.welcome_feature_4_title,  R.string.welcome_feature_4_text, "1.0.0", true),
			new FeatureItem(R.drawable.logo, R.string.welcome_feature_5_title,  FeatureItem.doNotShow, "1.0.0", true),
	};

	static private class FeatureItem {
		static final int doNotShow = -1;
		private int image;
		private int titleText;
		private int contentText;
		private int versionNumber;
		private boolean showOnInitialRun;

		public FeatureItem(int image, int titleText, int contentText, String version) {
			this(image, titleText, contentText, version, false);
		}

		public FeatureItem(int image, int titleText, int contentText, String version, boolean showOnInitialRun) {
			this.image = image;
			this.titleText = titleText;
			this.contentText = contentText;
			this.versionNumber = versionCodeFromString(version);
			this.showOnInitialRun = showOnInitialRun;
		}

		public int getImage() { return image; }
		public int getTitleText() { return titleText; }
		public int getContentText() { return contentText; }
		public int getVersionNumber() { return versionNumber; }
		public boolean shouldShowOnFirstRun() { return showOnInitialRun; }
	}

	static int versionCodeFromString(String version) {
		String v[] = version.split(Pattern.quote("."));
		if (v.length != 3) {
			Log_OC.wtf(TAG, "Version string is incorrect " + version);
			return 0;
		}
		int result = Integer.parseInt(v[0])*(int)(10e6) +
				Integer.parseInt(v[1])*(int)(10e4) +
				Integer.parseInt(v[2]);

		return result;
	}
}
