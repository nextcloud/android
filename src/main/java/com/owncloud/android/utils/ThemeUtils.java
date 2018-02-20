/*
 * Nextcloud Android client application
 *
 * @author Tobias Kaminsky
 * @author Andy Scherzinger
 * Copyright (C) 2017 Tobias Kaminsky
 * Copyright (C) 2017 Andy Scherzinger
 * Copyright (C) 2017 Nextcloud GmbH.
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
package com.owncloud.android.utils;

import android.accounts.Account;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.annotation.ColorInt;
import android.support.annotation.DrawableRes;
import android.support.design.widget.Snackbar;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v4.graphics.ColorUtils;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v4.widget.CompoundButtonCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.AppCompatCheckBox;
import android.support.v7.widget.SwitchCompat;
import android.text.Html;
import android.text.Spanned;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.SeekBar;

import com.getbase.floatingactionbutton.FloatingActionButton;
import com.owncloud.android.MainApp;
import com.owncloud.android.R;
import com.owncloud.android.authentication.AccountUtils;
import com.owncloud.android.datamodel.FileDataStorageManager;
import com.owncloud.android.lib.resources.status.OCCapability;
import com.owncloud.android.ui.activity.ToolbarActivity;

/**
 * Utility class with methods for client side theming.
 */
public class ThemeUtils {

    public static int primaryAccentColor() {
        OCCapability capability = getCapability();

        try {
            float adjust;
            if (darkTheme()) {
                adjust = +0.1f;
            } else {
                adjust = -0.1f;
            }
            return adjustLightness(adjust, Color.parseColor(capability.getServerColor()), 0.35f);
        } catch (Exception e) {
            return MainApp.getAppContext().getResources().getColor(R.color.color_accent);
        }
    }

    public static int primaryDarkColor() {
        return primaryDarkColor(null);
    }

    public static int primaryDarkColor(Account account) {
        OCCapability capability = getCapability(account);

        try {
            return adjustLightness(-0.2f, Color.parseColor(capability.getServerColor()), -1f);
        } catch (Exception e) {
            return MainApp.getAppContext().getResources().getColor(R.color.primary_dark);
        }
    }

    public static int primaryColor() {
        return primaryColor(null);
    }

    public static int primaryColor(Account account) {
        OCCapability capability = getCapability(account);

        try {
            return Color.parseColor(capability.getServerColor());
        } catch (Exception e) {
            return MainApp.getAppContext().getResources().getColor(R.color.primary);
        }
    }

    public static int elementColor() {
        return elementColor(null);
    }

    @NextcloudServer(max = 12)
    public static int elementColor(Account account) {
        OCCapability capability = getCapability(account);

        try {
            return Color.parseColor(capability.getServerElementColor());
        } catch (Exception e) {
            int primaryColor;

            try {
                primaryColor = Color.parseColor(capability.getServerColor());
            } catch (Exception e1) {
                primaryColor = MainApp.getAppContext().getResources().getColor(R.color.primary);
            }

            float[] hsl = colorToHSL(primaryColor);

            if (hsl[2] > 0.8) {
                return MainApp.getAppContext().getResources().getColor(R.color.elementFallbackColor);
            } else {
                return primaryColor;
            }
        }
    }

    public static boolean themingEnabled() {
        return getCapability().getServerColor() != null && !getCapability().getServerColor().isEmpty();
    }

    /**
     * @return int font color to use
     * adapted from https://github.com/nextcloud/server/blob/master/apps/theming/lib/Util.php#L90-L102
     */
    public static int fontColor() {
        try {
            return Color.parseColor(getCapability().getServerTextColor());
        } catch (Exception e) {
            if (darkTheme()) {
                return Color.WHITE;
            } else {
                return Color.BLACK;
            }
        }
    }

    /**
     * Tests if dark color is set
     * @return true if dark theme -> e.g.use light font color, darker accent color
     */
    public static boolean darkTheme() {
        int primaryColor = primaryColor();
        float[] hsl = colorToHSL(primaryColor);

        return hsl[2] <= 0.55;
    }

    /**
     * Set color of title to white/black depending on background color
     *
     * @param actionBar actionBar to be used
     * @param title     title to be shown
     */
    public static void setColoredTitle(ActionBar actionBar, String title) {
        if (actionBar != null) {
            if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.KITKAT) {
                actionBar.setTitle(title);
            } else {
                String colorHex = colorToHexString(fontColor());
                actionBar.setTitle(Html.fromHtml("<font color='" + colorHex + "'>" + title + "</font>"));
            }
        }
    }

    public static Spanned getColoredTitle(String title, int color) {
        String colorHex = colorToHexString(color);
        return Html.fromHtml("<font color='" + colorHex + "'>" + title + "</font>");
    }

    /**
     * Set color of title to white/black depending on background color
     *
     * @param actionBar actionBar to be used
     * @param titleId   title to be shown
     */
    public static void setColoredTitle(ActionBar actionBar, int titleId, Context context) {
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.KITKAT) {
            actionBar.setTitle(titleId);
        } else {
            String colorHex = colorToHexString(fontColor());
            String title = context.getString(titleId);
            actionBar.setTitle(Html.fromHtml("<font color='" + colorHex + "'>" + title + "</font>"));
        }
    }

    public static String getDefaultDisplayNameForRootFolder() {
        OCCapability capability = getCapability();

        if (capability.getServerName() == null || capability.getServerName().isEmpty()) {
            return MainApp.getAppContext().getResources().getString(R.string.default_display_name_for_root_folder);
        } else {
            return capability.getServerName();
        }
    }

    /**
     * Adjust lightness of given color
     *
     * @param lightnessDelta values -1..+1
     * @param color
     * @param threshold      0..1 as maximum value, -1 to disable
     * @return color adjusted by lightness
     */
    public static int adjustLightness(float lightnessDelta, int color, float threshold) {
        float[] hsl = colorToHSL(color);

        if (threshold == -1f) {
            hsl[2] += lightnessDelta;
        } else {
            hsl[2] = Math.min(hsl[2] + lightnessDelta, threshold);
        }

        return ColorUtils.HSLToColor(hsl);
    }

    private static float[] colorToHSL(int color) {
        float[] hsl = new float[3];
        ColorUtils.RGBToHSL(Color.red(color), Color.green(color), Color.blue(color), hsl);

        return hsl;
    }

    /**
     * sets the tinting of the given ImageButton's icon to color_accent.
     *
     * @param imageButton the image button who's icon should be colored
     */
    public static void colorImageButton(ImageButton imageButton, @ColorInt int color) {
        if (imageButton != null) {
            imageButton.setColorFilter(color, PorterDuff.Mode.SRC_ATOP);
        }
    }

    /**
     * sets the coloring of the given progress bar to color_accent.
     *
     * @param progressBar the progress bar to be colored
     * @param color       the color to be used
     */
    public static void colorHorizontalProgressBar(ProgressBar progressBar, @ColorInt int color) {
        if (progressBar != null) {
            progressBar.getIndeterminateDrawable().setColorFilter(color, PorterDuff.Mode.SRC_IN);
            progressBar.getProgressDrawable().setColorFilter(color, PorterDuff.Mode.SRC_IN);
        }
    }

    /**
     * sets the coloring of the given seek bar to color_accent.
     *
     * @param seekBar the seek bar to be colored
     */
    public static void colorHorizontalSeekBar(SeekBar seekBar) {
        int color = ThemeUtils.primaryAccentColor();
        colorHorizontalProgressBar(seekBar, color);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            seekBar.getThumb().setColorFilter(color, PorterDuff.Mode.SRC_IN);
        }
    }

    /**
     * set the Nextcloud standard colors for the snackbar.
     *
     * @param context  the context relevant for setting the color according to the context's theme
     * @param snackbar the snackbar to be colored
     */
    public static void colorSnackbar(Context context, Snackbar snackbar) {
        // Changing action button text color
        snackbar.setActionTextColor(ContextCompat.getColor(context, R.color.white));
    }

    /**
     * Sets the color of the status bar to {@code color} on devices with OS version lollipop or higher.
     *
     * @param fragmentActivity fragment activity
     * @param color            the color
     */
    public static void colorStatusBar(FragmentActivity fragmentActivity, @ColorInt int color) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            fragmentActivity.getWindow().setStatusBarColor(color);
        }
    }

    /**
     * Sets the color of the progressbar to {@code color} within the given toolbar.
     *
     * @param activity         the toolbar activity instance
     * @param progressBarColor the color to be used for the toolbar's progress bar
     */
    public static void colorToolbarProgressBar(FragmentActivity activity, int progressBarColor) {
        if (activity instanceof ToolbarActivity) {
            ((ToolbarActivity) activity).setProgressBarBackgroundColor(progressBarColor);
        }
    }

    public static void tintCheckbox(AppCompatCheckBox checkBox, int color) {
        CompoundButtonCompat.setButtonTintList(checkBox, new ColorStateList(
                new int[][]{
                        new int[]{-android.R.attr.state_checked},
                        new int[]{android.R.attr.state_checked},
                },
                new int[]{
                        Color.GRAY,
                        color
                }
        ));
    }

    public static void tintSwitch(SwitchCompat switchView, int color) {
        tintSwitch(switchView, color, false);
    }

    public static void tintSwitch(SwitchCompat switchView, int color, boolean colorText) {
        if (colorText) {
            switchView.setTextColor(color);
        }

        int trackColor = Color.argb(77, Color.red(color), Color.green(color), Color.blue(color));

        // setting the thumb color
        DrawableCompat.setTintList(switchView.getThumbDrawable(), new ColorStateList(
                new int[][]{new int[]{android.R.attr.state_checked}, new int[]{}},
                new int[]{color, Color.WHITE}));

        // setting the track color
        DrawableCompat.setTintList(switchView.getTrackDrawable(), new ColorStateList(
                new int[][]{new int[]{android.R.attr.state_checked}, new int[]{}},
                new int[]{trackColor, Color.parseColor("#4D000000")}));
    }

    public static Drawable tintDrawable(@DrawableRes int id, int color) {
        Drawable drawable = ResourcesCompat.getDrawable(MainApp.getAppContext().getResources(), id, null);

        return tintDrawable(drawable, color);
    }

    public static Drawable tintDrawable(Drawable drawable, int color) {
        if (drawable != null) {
            Drawable wrap = DrawableCompat.wrap(drawable);
            wrap.setColorFilter(color, PorterDuff.Mode.SRC_ATOP);

            return wrap;
        } else {
            return drawable;
        }
    }

    public static String colorToHexString(int color) {
        return String.format("#%06X", 0xFFFFFF & color);
    }

    public static void tintFloatingActionButton(FloatingActionButton button, int drawable) {
        button.setColorNormal(ThemeUtils.primaryColor());
        button.setColorPressed(ThemeUtils.primaryDarkColor());
        button.setIconDrawable(ThemeUtils.tintDrawable(drawable, ThemeUtils.fontColor()));
    }

    private static OCCapability getCapability() {
        return getCapability(null);
    }

    private static OCCapability getCapability(Account acc) {
        Account account;

        if (acc != null) {
            account = acc;
        } else {
            account = AccountUtils.getCurrentOwnCloudAccount(MainApp.getAppContext());
        }

        if (account != null) {
            Context context = MainApp.getAppContext();

            FileDataStorageManager storageManager = new FileDataStorageManager(account, context.getContentResolver());
            return storageManager.getCapability(account.name);
        } else {
            return new OCCapability();
        }
    }
}