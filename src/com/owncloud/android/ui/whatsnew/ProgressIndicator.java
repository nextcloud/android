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
import android.graphics.drawable.TransitionDrawable;
import android.util.AttributeSet;
import android.view.Gravity;
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

	protected int mNumberOfSteps = -1;
	protected int mCurrentStep = -1;

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

	public boolean hasNextStep() {
		return mNumberOfSteps > mCurrentStep;
	}

	public void setNumberOfSteps(int steps) {
		mNumberOfSteps = steps;
		mDotsContainer.removeAllViews();
		for (int i = 0; i < steps; ++i) {
			ImageView iv = new ImageView(getContext());
			iv.setImageDrawable(getContext().getResources().getDrawable(R.drawable.whats_new_progress_transition));
			mDotsContainer.addView(iv);
		}
        animateToStep(1);
	}

	public void animateToStep(int step) {
		if (step < 1 || step > mNumberOfSteps) return;

        if (mCurrentStep != -1) {
            ImageView prevDot = (ImageView) mDotsContainer.getChildAt(mCurrentStep-1);
            TransitionDrawable transition = (TransitionDrawable)prevDot.getDrawable();
            transition.resetTransition();
        }

        mCurrentStep = step;
		ImageView dot = (ImageView)mDotsContainer.getChildAt(step-1);
        TransitionDrawable transition = (TransitionDrawable)dot.getDrawable();
        transition.startTransition(500);
	}

	private void setup() {

		mDotsContainer = new LinearLayout(getContext());
		mDotsContainer.setGravity(Gravity.CENTER);
		FrameLayout.LayoutParams params = generateDefaultLayoutParams();
		params.width = ViewGroup.LayoutParams.MATCH_PARENT;
		params.height = ViewGroup.LayoutParams.MATCH_PARENT;
		mDotsContainer.setLayoutParams(params);
		addView(mDotsContainer);
	}

}
