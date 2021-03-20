/*
 * Nextcloud Android client application
 *
 * @author Tobias Kaminsky
 * @author Andy Scherzinger
 * Copyright (C) 2017 Tobias Kaminsky
 * Copyright (C) 2017 Nextcloud GmbH
 * Copyright (C) 2018 Andy Scherzinger
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.owncloud.android.utils.theme;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.view.View;
import android.view.Window;
import android.widget.ImageView;

import com.owncloud.android.R;

import androidx.annotation.ColorInt;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.SearchView;
import androidx.core.content.res.ResourcesCompat;

/**
 * Utility class with methods for client side action/toolbar theming.
 */
public final class ThemeToolbarUtils {
    /**
     * For activities that do not use drawer, e.g. Settings, this can be used to correctly tint back button based on
     * theme
     *
     * @param supportActionBar the back button's action bar
     */
    public static void tintBackButton(@Nullable ActionBar supportActionBar, Context context) {
        tintBackButton(supportActionBar, context, ThemeColorUtils.appBarPrimaryFontColor(context));
    }

    public static void tintBackButton(@Nullable ActionBar supportActionBar, Context context, @ColorInt int color) {
        if (supportActionBar == null) {
            return;
        }

        Drawable backArrow = ResourcesCompat.getDrawable(context.getResources(), R.drawable.ic_arrow_back, null);
        supportActionBar.setHomeAsUpIndicator(ThemeDrawableUtils.tintDrawable(backArrow, color));
    }

    /**
     * Set color of title to white/black depending on background color
     *
     * @param actionBar actionBar to be used
     * @param title     title to be shown
     */
    public static void setColoredTitle(@Nullable ActionBar actionBar, String title, Context context) {
        if (actionBar != null) {
            Spannable text = new SpannableString(title);
            text.setSpan(new ForegroundColorSpan(ThemeColorUtils.appBarPrimaryFontColor(context)),
                         0,
                         text.length(),
                         Spannable.SPAN_INCLUSIVE_INCLUSIVE);
            actionBar.setTitle(text);
        }
    }

    public static void setColoredTitle(@Nullable ActionBar actionBar, int titleId, Context context) {
        setColoredTitle(actionBar, context.getString(titleId), context);
    }

    /**
     * Set color of subtitle to white/black depending on background color
     *
     * @param actionBar actionBar to be used
     * @param title     title to be shown
     */
    public static void setColoredSubtitle(@Nullable ActionBar actionBar, String title, Context context) {
        if (actionBar != null) {
            Spannable text = new SpannableString(title);
            text.setSpan(new ForegroundColorSpan(ThemeColorUtils.appBarSecondaryFontColor(context)),
                         0,
                         text.length(),
                         Spannable.SPAN_INCLUSIVE_INCLUSIVE);
            actionBar.setSubtitle(text);
        }
    }

    /**
     * Theme search view
     *
     * @param searchView searchView to be changed
     * @param context    the app's context
     */
    public static void themeSearchView(SearchView searchView, Context context) {
        // hacky as no default way is provided
        int fontColor = ThemeColorUtils.appBarPrimaryFontColor(context);
        SearchView.SearchAutoComplete editText = searchView.findViewById(R.id.search_src_text);
        editText.setTextSize(16);
        ThemeTextInputUtils.setEditTextColor(context, editText, fontColor);
        editText.setHintTextColor(ThemeColorUtils.appBarSecondaryFontColor(context));

        ImageView closeButton = searchView.findViewById(androidx.appcompat.R.id.search_close_btn);
        closeButton.setColorFilter(fontColor);
        ImageView searchButton = searchView.findViewById(androidx.appcompat.R.id.search_button);
        searchButton.setColorFilter(fontColor);
    }

    /**
     * Sets the color of the status bar to {@code color}.
     *
     * @param fragmentActivity fragment activity
     * @param color            the color
     */
    public static void colorStatusBar(Activity fragmentActivity, @ColorInt int color) {
        Window window = fragmentActivity.getWindow();
        boolean isLightTheme = ThemeColorUtils.lightTheme(color);
        if (window != null) {
            window.setStatusBarColor(color);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                View decor = window.getDecorView();
                if (isLightTheme) {
                    int systemUiFlagLightStatusBar;
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        systemUiFlagLightStatusBar = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
                    } else {
                        systemUiFlagLightStatusBar = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
                    }
                    decor.setSystemUiVisibility(systemUiFlagLightStatusBar);
                } else {
                    decor.setSystemUiVisibility(0);
                }
            } else if (isLightTheme) {
                window.setStatusBarColor(Color.BLACK);
            }
        }
    }

    public static void colorStatusBar(Activity fragmentActivity) {
        colorStatusBar(fragmentActivity, ThemeColorUtils.primaryAppbarColor(fragmentActivity));
    }
}
