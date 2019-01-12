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
package com.owncloud.android.utils;

import android.accounts.Account;
import android.app.Activity;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.text.Html;
import android.text.Spanned;
import android.view.Window;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.SeekBar;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputLayout;
import com.owncloud.android.MainApp;
import com.owncloud.android.R;
import com.owncloud.android.authentication.AccountUtils;
import com.owncloud.android.datamodel.FileDataStorageManager;
import com.owncloud.android.lib.resources.status.OCCapability;
import com.owncloud.android.ui.activity.ToolbarActivity;

import androidx.annotation.ColorInt;
import androidx.annotation.DrawableRes;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.AppCompatCheckBox;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.graphics.ColorUtils;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.core.widget.CompoundButtonCompat;
import androidx.fragment.app.FragmentActivity;

/**
 * Utility class with methods for client side theming.
 */
public final class ThemeUtils {

    private ThemeUtils() {
        // utility class -> private constructor
    }

    public static int primaryAccentColor(Context context) {
        OCCapability capability = getCapability(context);

        try {
            float adjust;
            if (darkTheme(context)) {
                adjust = +0.1f;
            } else {
                adjust = -0.1f;
            }
            return adjustLightness(adjust, Color.parseColor(capability.getServerColor()), 0.35f);
        } catch (Exception e) {
            return context.getResources().getColor(R.color.color_accent);
        }
    }

    public static int primaryDarkColor(Context context) {
        return primaryDarkColor(null, context);
    }

    public static int primaryDarkColor(Account account, Context context) {
        OCCapability capability = getCapability(account, context);

        try {
            return adjustLightness(-0.2f, Color.parseColor(capability.getServerColor()), -1f);
        } catch (Exception e) {
            return context.getResources().getColor(R.color.primary_dark);
        }
    }

    public static int primaryColor(Context context) {
        return primaryColor(context, false);
    }

    public static int primaryColor(Context context, boolean replaceWhite) {
        return primaryColor(null, replaceWhite, context);
    }

    public static int primaryColor(Account account, boolean replaceWhite, Context context) {
        OCCapability capability = getCapability(account, context);

        try {
            int color = Color.parseColor(capability.getServerColor());
            if (replaceWhite && Color.WHITE == color) {
                return Color.GRAY;
            } else {
                return color;
            }
        } catch (Exception e) {
            return context.getResources().getColor(R.color.primary);
        }
    }

    public static int elementColor(Context context) {
        return elementColor(null, context);
    }

    @NextcloudServer(max = 12)
    public static int elementColor(Account account, Context context) {
        OCCapability capability = getCapability(account, context);

        try {
            return Color.parseColor(capability.getServerElementColor());
        } catch (Exception e) {
            int primaryColor;

            try {
                primaryColor = Color.parseColor(capability.getServerColor());
            } catch (Exception e1) {
                primaryColor = context.getResources().getColor(R.color.primary);
            }

            float[] hsl = colorToHSL(primaryColor);

            if (hsl[2] > 0.8) {
                return context.getResources().getColor(R.color.elementFallbackColor);
            } else {
                return primaryColor;
            }
        }
    }

    public static boolean themingEnabled(Context context) {
        return getCapability(context).getServerColor() != null && !getCapability(context).getServerColor().isEmpty();
    }

    /**
     * @return int font color to use
     * adapted from https://github.com/nextcloud/server/blob/master/apps/theming/lib/Util.php#L90-L102
     */
    public static int fontColor(Context context) {
        try {
            return Color.parseColor(getCapability(context).getServerTextColor());
        } catch (Exception e) {
            if (darkTheme(context)) {
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
    public static boolean darkTheme(Context context) {
        int primaryColor = primaryColor(context);
        float[] hsl = colorToHSL(primaryColor);

        return hsl[2] <= 0.55;
    }

    /**
     * Set color of title to white/black depending on background color
     *
     * @param actionBar actionBar to be used
     * @param title     title to be shown
     */
    public static void setColoredTitle(@Nullable ActionBar actionBar, String title, Context context) {
        if (actionBar != null) {
            if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.KITKAT) {
                actionBar.setTitle(title);
            } else {
                String colorHex = colorToHexString(fontColor(context));
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
    public static void setColoredTitle(@Nullable ActionBar actionBar, int titleId, Context context) {
        if (actionBar != null) {
            if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.KITKAT) {
                actionBar.setTitle(titleId);
            } else {
                String colorHex = colorToHexString(fontColor(context));
                String title = context.getString(titleId);
                actionBar.setTitle(Html.fromHtml("<font color='" + colorHex + "'>" + title + "</font>"));
            }
        }
    }

    public static String getDefaultDisplayNameForRootFolder(Context context) {
        OCCapability capability = getCapability(context);

        if (MainApp.isOnlyOnDevice()) {
            return MainApp.getAppContext().getString(R.string.drawer_item_on_device);
        } else {
            if (capability.getServerName() == null || capability.getServerName().isEmpty()) {
                return MainApp.getAppContext().getResources().getString(R.string.default_display_name_for_root_folder);
            } else {
                return capability.getServerName();
            }
        }

    }

    public static void setStatusBarColor(Activity activity, @ColorInt int color) {
        if (activity != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            activity.getWindow().setStatusBarColor(color);
        }
    }

    /**
     * Adjust lightness of given color
     *
     * @param lightnessDelta values -1..+1
     * @param color original color
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

    public static void colorEditText(EditText editText, int elementColor) {
        if (editText != null) {
            editText.setTextColor(elementColor);
            editText.getBackground().setColorFilter(elementColor, PorterDuff.Mode.SRC_ATOP);
        }
    }

    /**
     * sets the coloring of the given progress bar to given color.
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
     * sets the coloring of the given progress bar's progress to given color.
     *
     * @param progressBar the progress bar to be colored
     * @param color       the color to be used
     */
    public static void colorProgressBar(ProgressBar progressBar, @ColorInt int color) {
        if (progressBar != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                progressBar.setProgressTintList(ColorStateList.valueOf(color));
            } else {
                ThemeUtils.colorHorizontalProgressBar(progressBar, color);
            }
        }
    }

    /**
     * sets the coloring of the given seek bar to color_accent.
     *
     * @param seekBar the seek bar to be colored
     */
    public static void colorHorizontalSeekBar(SeekBar seekBar, Context context) {
        int color = ThemeUtils.primaryAccentColor(context);
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
        Window window = fragmentActivity.getWindow();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && window != null) {
            window.setStatusBarColor(color);
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

    /**
     * Sets the color of the  TextInputLayout to {@code color} for hint text and box stroke.
     *
     * @param textInputLayout the TextInputLayout instance
     * @param color the color to be used for the hint text and box stroke
     */
    public static void colorTextInputLayout(TextInputLayout textInputLayout, int color) {
        textInputLayout.setBoxStrokeColor(color);
        textInputLayout.setDefaultHintTextColor(new ColorStateList(
            new int[][]{
                new int[]{-android.R.attr.state_focused},
                new int[]{android.R.attr.state_focused},
            },
            new int[]{
                Color.GRAY,
                color
            }
        ));
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

    @Nullable
    public static Drawable tintDrawable(Drawable drawable, int color) {
        if (drawable != null) {
            Drawable wrap = DrawableCompat.wrap(drawable);
            wrap.setColorFilter(color, PorterDuff.Mode.SRC_ATOP);

            return wrap;
        }

        return null;
    }

    public static String colorToHexString(int color) {
        return String.format("#%06X", 0xFFFFFF & color);
    }

    public static void tintFloatingActionButton(FloatingActionButton button, @DrawableRes int
            drawable, Context context) {
        button.setBackgroundTintList(ColorStateList.valueOf(ThemeUtils.primaryColor(context)));
        button.setRippleColor(ThemeUtils.primaryDarkColor(context));
        button.setImageDrawable(ThemeUtils.tintDrawable(drawable, ThemeUtils.fontColor(context)));
    }

    private static OCCapability getCapability(Context context) {
        return getCapability(null, context);
    }

    private static OCCapability getCapability(Account acc, Context context) {
        Account account = null;

        if (acc != null) {
            account = acc;
        } else if (context != null) {
            account = AccountUtils.getCurrentOwnCloudAccount(context);
        }

        if (account != null) {
            FileDataStorageManager storageManager = new FileDataStorageManager(account, context.getContentResolver());
            return storageManager.getCapability(account.name);
        } else {
            return new OCCapability();
        }
    }
}
