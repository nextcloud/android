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
import com.owncloud.android.authentication.AccountUtils;
import com.owncloud.android.features.FeatureList;
import com.owncloud.android.features.FeatureList.FeatureItem;
import com.owncloud.android.ui.whatsnew.ProgressIndicator;


/**
 * @author Bartosz Przybylski
 */
public class WhatsNewActivity extends Activity {
	private static String TAG = WhatsNewActivity.class.getSimpleName();

	private static final String KEY_LAST_SEEN_VERSION_CODE = "lastSeenVersionCode";

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

		final int listLength = FeatureList.getFiltered(getLastSeenVersionCode(), isFirstRun()).length;

		mProgress.setNumberOfSteps(listLength);

		mForwardFinishButton = (ImageButton) findViewById(R.id.forward);
		mForwardFinishButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				if (mProgress.hasNextStep()) {
					mProgress.animateToNextStep();
					mContentPanel.animate().x(-mContentPanel.getChildAt(++mCurrentStep).getLeft());
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
		ll2.width = getScreenWidth()*listLength;
		mContentPanel.setLayoutParams(ll2);

		fillContentPanelWithFeatureData();
	}

	private void fillContentPanelWithFeatureData() {
		LayoutInflater inflater = (LayoutInflater)getSystemService(LAYOUT_INFLATER_SERVICE);

		for (FeatureItem item : FeatureList.getFiltered(getLastSeenVersionCode(), isFirstRun())) {
			LinearLayout newElement = (LinearLayout)inflater.inflate(R.layout.whats_new_element, null);

			LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(getScreenWidth(), ViewGroup.LayoutParams.MATCH_PARENT);
			newElement.setLayoutParams(params);

			mContentPanel.addView(newElement);
			ImageView iv = (ImageView)newElement.findViewById(R.id.whatsNewImage);
			if (item.getImage() != FeatureItem.DO_NOT_SHOW)
				iv.setImageResource(item.getImage());

			TextView tv2 = (TextView)newElement.findViewById(R.id.whatsNewTitle);
			if (item.getTitleText() != FeatureItem.DO_NOT_SHOW)
				tv2.setText(item.getTitleText());

			tv2 = (TextView)newElement.findViewById(R.id.whatsNewText);
			if (item.getContentText() != FeatureItem.DO_NOT_SHOW)
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
		if (getLastSeenVersionCode() != 0)
			return false;
		return AccountUtils.getCurrentOwnCloudAccount(MainApp.getAppContext()) == null;

	}

	static public void runIfNeeded(Context context) {
		if (context instanceof WhatsNewActivity)
			return;

		if (FeatureList.getFiltered(getLastSeenVersionCode(), isFirstRun()).length > 0)
			context.startActivity(new Intent(context, WhatsNewActivity.class));
	}


}
