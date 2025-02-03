/*
 * IONOS HiDrive Next - Android Client
 *
 * SPDX-FileCopyrightText: 2025 STRATO AG.
 * SPDX-License-Identifier: GPL-2.0
 */

package com.owncloud.android.ui.activity;

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.view.View;

import com.owncloud.android.R;
import com.owncloud.android.utils.theme.ViewThemeUtils;

import androidx.annotation.StringRes;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.drawerlayout.widget.DrawerLayout;

class AnimatedDrawerListener extends ActionBarDrawerToggle {
    private static final float CHANGE_GAIN = 0.1f;

    private final Activity activity;
    private final ViewThemeUtils viewThemeUtils;
    private final ValueAnimator valueAnimator;

    AnimatedDrawerListener(Activity activity,
                           DrawerLayout drawerLayout,
                           @StringRes int openDrawerContentDescRes,
                           @StringRes int closeDrawerContentDescRes,
                           ViewThemeUtils viewThemeUtils
                          ) {
        super(activity, drawerLayout, openDrawerContentDescRes, closeDrawerContentDescRes);

        this.activity = activity;
        this.viewThemeUtils = viewThemeUtils;
        this.valueAnimator = createValueAnimator();
    }

    private ValueAnimator createValueAnimator() {
        int colorFrom = this.activity.getResources().getColor(R.color.bg_default);
        int colorTo = getOpenedDrawerColor();
        return ValueAnimator.ofObject(new ArgbEvaluator(), colorFrom, colorTo);
    }

    @Override
    public void onDrawerSlide(View drawerView, float slideOffset) {
        super.onDrawerSlide(drawerView, slideOffset);

        if (shouldUpdateSystemBarColor(slideOffset)) {
            this.valueAnimator.setCurrentFraction(slideOffset);
            this.viewThemeUtils.ionos.platform.themeSystemBars(this.activity, (int) this.valueAnimator.getAnimatedValue());
        }
    }

    @Override
    public void onDrawerOpened(View drawerView) {
        super.onDrawerOpened(drawerView);

        this.viewThemeUtils.ionos.platform.themeSystemBars(this.activity, getOpenedDrawerColor());
    }

    @Override
    public void onDrawerClosed(View drawerView) {
        super.onDrawerClosed(drawerView);

        this.viewThemeUtils.ionos.platform.themeSystemBars(this.activity);
    }

    private boolean shouldUpdateSystemBarColor(float slideOffset) {
        float delta = Math.abs(slideOffset - this.valueAnimator.getAnimatedFraction());
        return delta > CHANGE_GAIN;
    }

    private int getOpenedDrawerColor() {
        return this.activity.getResources().getColor(R.color.drawer_header_background);
    }
}
