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

package com.owncloud.android.ui.whatsnew;

import android.content.Context;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.owncloud.android.R;

/**
 * @author Bartosz Przybylski
 */
public class ProgressIndicator extends FrameLayout {

	protected LinearLayout mDotsContainer;
	protected ImageView mCurrentProgressDot;

	protected int mNumberOfSteps;
	protected int mCurrentStep;

	public ProgressIndicator(Context context) {
		super(context);
		setup();
	}

	public ProgressIndicator(Context context, AttributeSet attrs) {
		super(context, attrs);
		setup();
	}

	public ProgressIndicator(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		setup();
	}

	@Override
	public void onWindowFocusChanged(boolean hasWindowFocus) {
		super.onWindowFocusChanged(hasWindowFocus);
		// This is not the best place to reset steps but I couldn't find a better one
		setStep(mCurrentStep);
	}

	public boolean hasNextStep() {
		return mNumberOfSteps > mCurrentStep;
	}

	public boolean hasPrevStep() {
		return mCurrentStep > 1;
	}

	public void animateToNextStep() {
		animateToStep(mCurrentStep+1);
	}

	public void animateToPrevStep() {
		animateToStep(mCurrentStep-1);
	}

	public void setNumberOfSteps(int steps) {
		mNumberOfSteps = steps;
		mDotsContainer.removeAllViews();
		for (int i = 0; i < steps; ++i) {
			ImageView iv = new ImageView(getContext());
			iv.setImageDrawable(getContext().getResources().getDrawable(R.drawable.indicator_dot_background));
			mDotsContainer.addView(iv);
		}
	}

	private void setStep(int step) {
		if (step < 1 || step > mNumberOfSteps) return;

		View dot = mDotsContainer.getChildAt(step-1);
		FrameLayout.LayoutParams lp = (LayoutParams) mCurrentProgressDot.getLayoutParams();
		lp.leftMargin = dot.getLeft();
		lp.topMargin = dot.getTop();
		mCurrentProgressDot.setLayoutParams(lp);
	}

	public void animateToStep(int step) {
		if (step < 1 || step > mNumberOfSteps) return;
        mCurrentStep = step;
		View dot = mDotsContainer.getChildAt(step-1);
		mCurrentProgressDot
				.animate()
				.x(dot.getLeft())
				.y(dot.getTop());
	}

	private void setup() {
		mCurrentStep = 1;

		mDotsContainer = new LinearLayout(getContext());
		mDotsContainer.setGravity(Gravity.CENTER);
		FrameLayout.LayoutParams params = generateDefaultLayoutParams();
		params.width = ViewGroup.LayoutParams.MATCH_PARENT;
		params.height = ViewGroup.LayoutParams.MATCH_PARENT;
		mDotsContainer.setLayoutParams(params);
		addView(mDotsContainer);

		mCurrentProgressDot = new ImageView(getContext());
		params = generateDefaultLayoutParams();
		params.width = ViewGroup.LayoutParams.WRAP_CONTENT;
		params.height = ViewGroup.LayoutParams.WRAP_CONTENT;
		mCurrentProgressDot.setLayoutParams(params);
		mCurrentProgressDot.setImageDrawable(getContext().getResources().getDrawable(R.drawable.indicator_dot_selected));
		addView(mCurrentProgressDot);
	}

}
