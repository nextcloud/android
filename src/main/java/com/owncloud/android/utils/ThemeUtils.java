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
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputLayout;
import com.nextcloud.client.account.User;
import com.nextcloud.client.account.UserAccountManagerImpl;
import com.owncloud.android.MainApp;
import com.owncloud.android.R;
import com.owncloud.android.datamodel.FileDataStorageManager;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.lib.resources.shares.ShareType;
import com.owncloud.android.lib.resources.status.OCCapability;

import java.lang.reflect.Field;

import androidx.annotation.ColorInt;
import androidx.annotation.DrawableRes;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.AppCompatCheckBox;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.graphics.ColorUtils;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.core.widget.CompoundButtonCompat;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Utility class with methods for client side theming.
 */
public final class ThemeUtils {

    private static final String TAG = ThemeUtils.class.getSimpleName();

    private static final int INDEX_LUMINATION = 2;
    private static final double MAX_LIGHTNESS = 0.92;
    public static final double LUMINATION_THRESHOLD = 0.8;

    private ThemeUtils() {
        // utility class -> private constructor
    }

    public static int primaryAccentColor(Context context) {
        OCCapability capability = getCapability(context);

        try {
            float adjust;
            if (darkTheme(context)) {
                if (AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_YES) {
                    adjust = +0.5f;
//                    return adjustLightness(adjust, Color.parseColor(capability.getServerColor()), -1);
                } else {
                    adjust = +0.1f;
                }
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
            return calculateDarkColor(Color.parseColor(capability.getServerColor()), context);
        } catch (Exception e) {
            return context.getResources().getColor(R.color.primary_dark);
        }
    }

    public static int calculateDarkColor(int color, Context context){
        try {
            return adjustLightness(-0.2f, color, -1f);
        } catch (Exception e) {
            return context.getResources().getColor(R.color.primary_dark);
        }
    }

    public static int primaryColor(Context context) {
        return primaryColor(context, false);
    }

    public static int primaryColor(Context context, boolean replaceEdgeColors) {
        User nullUser = null;
        return primaryColor(nullUser, replaceEdgeColors, context);
    }

    public static int primaryColor(User user, boolean replaceEdgeColors, Context context) {
        return primaryColor(user != null ? user.toPlatformAccount() : null,
                            replaceEdgeColors,
                            false,
                            context);
    }

    public static int primaryColor(Account account, boolean replaceEdgeColors, Context context) {
        return primaryColor(account, replaceEdgeColors, false, context);
    }

    /**
     * return the primary color defined in the server-side theming respecting Android dark/light theming and edge case
     * scenarios including drawer menu.
     *
     * @param account                          the Nextcloud user
     * @param replaceEdgeColors                flag if edge case color scenarios should be handled
     * @param replaceEdgeColorsByInvertedColor flag in edge case handling should be done via color inversion
     *                                         (black/white)
     * @param context                          the context (needed to load client-side colors)
     * @return the color
     */
    public static int primaryColor(Account account,
                                   boolean replaceEdgeColors,
                                   boolean replaceEdgeColorsByInvertedColor,
                                   Context context) {
        if (context == null) {
            return Color.GRAY;
        }

        try {
            int color = Color.parseColor(getCapability(account, context).getServerColor());
            if (replaceEdgeColors) {
                if (isDarkModeActive(context)) {
                    if (Color.BLACK == color) {
                        if (replaceEdgeColorsByInvertedColor) {
                            return Color.WHITE;
                        } else {
                            return getNeutralGrey(context);
                        }
                    } else {
                        return color;
                    }
                } else {
                    if (Color.WHITE == color) {
                        if (replaceEdgeColorsByInvertedColor) {
                            return Color.BLACK;
                        } else {
                            return getNeutralGrey(context);
                        }
                    } else {
                        return color;
                    }
                }
            } else {
                return color;
            }
        } catch (Exception e) {
            return context.getResources().getColor(R.color.primary);
        }
    }

    public static int getNeutralGrey(Context context) {
        return darkTheme(context) ? context.getResources().getColor(R.color.fg_contrast) : Color.GRAY;
    }

    public static boolean themingEnabled(Context context) {
        return getCapability(context).getServerColor() != null && !getCapability(context).getServerColor().isEmpty();
    }

    /**
     * returns the font color based on the server side theming and uses black/white as a fallback based on
     * replaceWhite.
     *
     * @param context      the context
     * @param replaceWhite FLAG to return white/black if server side color isn't available
     * @return int font color to use
     */
    public static int fontColor(Context context, boolean replaceWhite) {
        if (AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_YES) {
            if (replaceWhite) {
                return Color.BLACK;
            } else {
                return Color.WHITE;
            }
        }

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

    public static int fontColor(Context context) {
        return fontColor(context, false);
    }

    /**
     * Tests if light color is set
     *
     * @param color the color
     * @return true if primaryColor is lighter than MAX_LIGHTNESS
     */
    public static boolean lightTheme(int color) {
        float[] hsl = colorToHSL(color);

        return hsl[INDEX_LUMINATION] >= MAX_LIGHTNESS;
    }

    /**
     * Tests if dark color is set
     *
     * @return true if dark theme -> e.g.use light font color, darker accent color
     */
    public static boolean darkTheme(Context context) {
        int primaryColor = primaryColor(context);
        float[] hsl = colorToHSL(primaryColor);

        return hsl[INDEX_LUMINATION] <= 0.55;
    }

    public static int primaryAppbarColor(Context context) {
        return ContextCompat.getColor(context, R.color.appbar);
    }

    public static int appBarPrimaryFontColor(Context context) {
        return ContextCompat.getColor(context, R.color.fontAppbar);
    }

    public static int appBarSecondaryFontColor(Context context) {
        return ContextCompat.getColor(context, R.color.fontSecondaryAppbar);
    }

    public static int actionModeColor(Context context) {
        return ContextCompat.getColor(context, R.color.action_mode_background);
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
            text.setSpan(new ForegroundColorSpan(appBarPrimaryFontColor(context)),
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
            text.setSpan(new ForegroundColorSpan(appBarSecondaryFontColor(context)),
                         0,
                         text.length(),
                         Spannable.SPAN_INCLUSIVE_INCLUSIVE);
            actionBar.setSubtitle(text);
        }
    }

    /**
     * For activities that do not use drawer, e.g. Settings, this can be used to correctly tint back button based on
     * theme
     *
     * @param supportActionBar
     */
    public static void tintBackButton(@Nullable ActionBar supportActionBar, Context context) {
        tintBackButton(supportActionBar, context, ThemeUtils.appBarPrimaryFontColor(context));
    }

    public static void tintBackButton(@Nullable ActionBar supportActionBar, Context context, @ColorInt int color) {
        if (supportActionBar == null) {
            return;
        }

        Drawable backArrow = ResourcesCompat.getDrawable(context.getResources(), R.drawable.ic_arrow_back, null);
        supportActionBar.setHomeAsUpIndicator(ThemeUtils.tintDrawable(backArrow, color));
    }

    public static Spanned getColoredTitle(String title, int color) {
        Spannable text = new SpannableString(title);
        text.setSpan(new ForegroundColorSpan(color),
                     0,
                     text.length(),
                     Spannable.SPAN_INCLUSIVE_INCLUSIVE);

        return text;
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
     * @param color          original color
     * @param threshold      0..1 as maximum value, -1 to disable
     * @return color adjusted by lightness
     */
    public static int adjustLightness(float lightnessDelta, int color, float threshold) {
        float[] hsl = colorToHSL(color);

        if (threshold == -1f) {
            hsl[INDEX_LUMINATION] += lightnessDelta;
        } else {
            hsl[INDEX_LUMINATION] = Math.min(hsl[INDEX_LUMINATION] + lightnessDelta, threshold);
        }

        return ColorUtils.HSLToColor(hsl);
    }

    private static float[] colorToHSL(int color) {
        float[] hsl = new float[3];
        ColorUtils.RGBToHSL(Color.red(color), Color.green(color), Color.blue(color), hsl);

        return hsl;
    }

    public static void colorPrimaryButton(Button button, Context context) {
        int primaryColor = ThemeUtils.primaryColor(null, true, false, context);
        int fontColor = ThemeUtils.fontColor(context, false);

        button.setBackgroundColor(primaryColor);

        if (Color.BLACK == primaryColor) {
            button.setTextColor(Color.WHITE);
        } else if (Color.WHITE == primaryColor) {
            button.setTextColor(Color.BLACK);
        } else {
            button.setTextColor(fontColor);
        }
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

    public static void colorEditText(EditText editText, int color) {
        if (editText != null) {
            editText.setTextColor(color);
            editText.getBackground().setColorFilter(color, PorterDuff.Mode.SRC_ATOP);
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
        seekBar.getThumb().setColorFilter(color, PorterDuff.Mode.SRC_IN);
    }

    public static void colorSwipeRefreshLayout(Context context, SwipeRefreshLayout swipeRefreshLayout) {
        int primaryColor = ThemeUtils.primaryColor(context);
        int darkColor = ThemeUtils.primaryDarkColor(context);
        int accentColor = ThemeUtils.primaryAccentColor(context);

        swipeRefreshLayout.setColorSchemeColors(accentColor, primaryColor, darkColor);
        swipeRefreshLayout.setProgressBackgroundColorSchemeResource(R.color.bg_elevation_one);
    }

    /**
     * set the Nextcloud standard colors for the snackbar.
     *
     * @param context  the context relevant for setting the color according to the context's theme
     * @param snackbar the snackbar to be colored
     */
    public static void colorSnackbar(Context context, Snackbar snackbar) {
        // Changing action button text color
        snackbar.setActionTextColor(ContextCompat.getColor(context, R.color.fg_inverse));
    }

    /**
     * Sets the color of the status bar to {@code color} on devices with OS version lollipop or higher.
     *
     * @param fragmentActivity fragment activity
     * @param color            the color
     */
    public static void colorStatusBar(Activity fragmentActivity, @ColorInt int color) {
        Window window = fragmentActivity.getWindow();
        boolean isLightTheme = lightTheme(color);
        if (window != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
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
        colorStatusBar(fragmentActivity, primaryAppbarColor(fragmentActivity));
    }

    /**
     * Sets the color of the  TextInputLayout to {@code color} for hint text and box stroke.
     *
     * @param textInputLayout the TextInputLayout instance
     * @param color           the color to be used for the hint text and box stroke
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

    public static void themeDialogActionButton(MaterialButton button) {
        if (button == null) {
            return;
        }

        Context context = button.getContext();
        int accentColor = ThemeUtils.primaryAccentColor(button.getContext());
        int disabledColor = ContextCompat.getColor(context, R.color.disabled_text);
        button.setTextColor(new ColorStateList(
            new int[][]{
                new int[]{android.R.attr.state_enabled}, // enabled
                new int[]{-android.R.attr.state_enabled}, // disabled
            },
            new int[]{
                accentColor,
                disabledColor
            }
        ));
    }

    public static void themeEditText(Context context, EditText editText, boolean themedBackground) {
        if (editText == null) {
            return;
        }

        int color = ContextCompat.getColor(context, R.color.text_color);

        if (themedBackground) {
            if (darkTheme(context)) {
                color = ContextCompat.getColor(context, R.color.themed_fg);
            } else {
                color = ContextCompat.getColor(context, R.color.themed_fg_inverse);
            }
        }

        setEditTextColor(context, editText, color);
    }

    private static void setEditTextColor(Context context, EditText editText, int color) {
        editText.setTextColor(color);
        editText.setHighlightColor(context.getResources().getColor(R.color.fg_contrast));
        setEditTextCursorColor(editText, color);
        setTextViewHandlesColor(context, editText, color);
    }

    /**
     * Theme search view
     *
     * @param searchView searchView to be changed
     * @param context    the app's context
     */
    public static void themeSearchView(SearchView searchView, Context context) {
        // hacky as no default way is provided
        int fontColor = appBarPrimaryFontColor(context);
        SearchView.SearchAutoComplete editText = searchView.findViewById(R.id.search_src_text);
        setEditTextColor(context, editText, fontColor);
        editText.setHintTextColor(appBarSecondaryFontColor(context));

        ImageView closeButton = searchView.findViewById(androidx.appcompat.R.id.search_close_btn);
        closeButton.setColorFilter(fontColor);
        ImageView searchButton = searchView.findViewById(androidx.appcompat.R.id.search_button);
        searchButton.setColorFilter(fontColor);
    }

    public static void themeProgressBar(Context context, ProgressBar progressBar) {
        int color = ThemeUtils.primaryAccentColor(context);
        progressBar.getIndeterminateDrawable().setColorFilter(color, PorterDuff.Mode.SRC_IN);
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
            new int[]{trackColor, MainApp.getAppContext().getResources().getColor(R.color.switch_track_color_unchecked)}));
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

    /**
     * Will change a menu item text tint
     * @param item the menu item object
     * @param color the wanted color (as resource or color)
     */
    public static void tintMenuItemText(MenuItem item, int color) {
        SpannableString newItemTitle = new SpannableString(item.getTitle());
        newItemTitle.setSpan(new ForegroundColorSpan(color), 0, newItemTitle.length(),
                             Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        item.setTitle(newItemTitle);
    }

    public static String colorToHexString(int color) {
        return String.format("#%06X", 0xFFFFFF & color);
    }

    public static void colorFloatingActionButton(FloatingActionButton button, @DrawableRes int drawable,
                                                 Context context) {
        int primaryColor = ThemeUtils.primaryColor(null, true, false, context);

        colorFloatingActionButton(button, context, primaryColor);
        button.setImageDrawable(ThemeUtils.tintDrawable(drawable, getColorForPrimary(primaryColor, context)));
    }

    public static void colorFloatingActionButton(FloatingActionButton button, Context context) {
        colorFloatingActionButton(button, context, ThemeUtils.primaryColor(null, true, false, context));
    }

    public static void colorFloatingActionButton(FloatingActionButton button, Context context, int primaryColor) {
        colorFloatingActionButton(button, primaryColor, calculateDarkColor(primaryColor, context));
    }

    public static void colorFloatingActionButton(FloatingActionButton button, int backgroundColor, int rippleColor) {
        button.setBackgroundTintList(ColorStateList.valueOf(backgroundColor));
        button.setRippleColor(rippleColor);
    }

    public static void colorIconImageViewWithBackground(ImageView imageView, Context context) {
        int primaryColor = ThemeUtils.primaryColor(null, true, false, context);

        imageView.getBackground().setColorFilter(primaryColor, PorterDuff.Mode.SRC_IN);
        imageView.getDrawable().mutate().setColorFilter(getColorForPrimary(primaryColor, context),
                                                        PorterDuff.Mode.SRC_IN);
    }

    /**
     * returns a primary color matching color for texts/icons on top of a primary-colored element (like buttons).
     *
     * @param primaryColor the primary color
     */
    public static int getColorForPrimary(int primaryColor, Context context) {
        if (Color.BLACK == primaryColor) {
            return Color.WHITE;
        } else if (Color.WHITE == primaryColor) {
            return Color.BLACK;
        } else {
            return ThemeUtils.fontColor(context, false);
        }
    }

    private static OCCapability getCapability(Context context) {
        return getCapability(null, context);
    }

    private static OCCapability getCapability(Account acc, Context context) {
        Account account = null;

        if (acc != null) {
            account = acc;
        } else if (context != null) {
            // TODO: refactor when dark theme work is completed
            account = UserAccountManagerImpl.fromContext(context).getCurrentAccount();
        }

        if (account != null) {
            FileDataStorageManager storageManager = new FileDataStorageManager(account, context.getContentResolver());
            return storageManager.getCapability(account.name);
        } else {
            return new OCCapability();
        }
    }

    public static Drawable setIconColor(Drawable drawable) {
        int color;
        if (AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_YES) {
            color = Color.WHITE;
        } else {
            color = Color.BLACK;
        }
        return tintDrawable(drawable, color);
    }

    /**
     * Lifted from SO. FindBugs surpressed because of lack of public API to alter the cursor color.
     *
     * @param editText TextView to be styled
     * @param color    The desired cursor colour
     * @see <a href="https://stackoverflow.com/a/52564925">StackOverflow url</a>
     */
    @SuppressFBWarnings
    public static void setEditTextCursorColor(EditText editText, int color) {
        try {
            // Get the cursor resource id
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {//set differently in Android P (API 28)
                Field field = TextView.class.getDeclaredField("mCursorDrawableRes");
                field.setAccessible(true);
                int drawableResId = field.getInt(editText);

                // Get the editor
                field = TextView.class.getDeclaredField("mEditor");
                field.setAccessible(true);
                Object editor = field.get(editText);

                // Get the drawable and set a color filter
                Drawable drawable = ContextCompat.getDrawable(editText.getContext(), drawableResId);
                drawable.setColorFilter(color, PorterDuff.Mode.SRC_IN);

                // Set the drawables
                field = editor.getClass().getDeclaredField("mDrawableForCursor");
                field.setAccessible(true);
                field.set(editor, drawable);
            } else {
                Field field = TextView.class.getDeclaredField("mCursorDrawableRes");
                field.setAccessible(true);
                int drawableResId = field.getInt(editText);

                // Get the editor
                field = TextView.class.getDeclaredField("mEditor");
                field.setAccessible(true);
                Object editor = field.get(editText);

                // Get the drawable and set a color filter
                Drawable drawable = ContextCompat.getDrawable(editText.getContext(), drawableResId);
                drawable.setColorFilter(color, PorterDuff.Mode.SRC_IN);
                Drawable[] drawables = {drawable, drawable};

                // Set the drawables
                field = editor.getClass().getDeclaredField("mCursorDrawable");
                field.setAccessible(true);
                field.set(editor, drawables);
            }
        } catch (Exception exception) {
            // we do not log this
        }
    }


    /**
     * Set the color of the handles when you select text in a {@link android.widget.EditText} or other view that extends
     * {@link TextView}. FindBugs surpressed because of lack of public API to alter the {@link TextView} handles color.
     *
     * @param view  The {@link TextView} or a {@link View} that extends {@link TextView}.
     * @param color The color to set for the text handles
     * @see <a href="https://gist.github.com/jaredrummler/2317620559d10ac39b8218a1152ec9d4">External reference</a>
     */
    @SuppressFBWarnings
    private static void setTextViewHandlesColor(Context context, TextView view, int color) {
        try {
            Field editorField = TextView.class.getDeclaredField("mEditor");
            if (!editorField.isAccessible()) {
                editorField.setAccessible(true);
            }

            Object editor = editorField.get(view);
            Class<?> editorClass = editor.getClass();

            String[] handleNames = {"mSelectHandleLeft", "mSelectHandleRight", "mSelectHandleCenter"};
            String[] resNames = {"mTextSelectHandleLeftRes", "mTextSelectHandleRightRes", "mTextSelectHandleRes"};

            for (int i = 0; i < handleNames.length; i++) {
                Field handleField = editorClass.getDeclaredField(handleNames[i]);
                if (!handleField.isAccessible()) {
                    handleField.setAccessible(true);
                }

                Drawable handleDrawable = (Drawable) handleField.get(editor);

                if (handleDrawable == null) {
                    Field resField = TextView.class.getDeclaredField(resNames[i]);
                    if (!resField.isAccessible()) {
                        resField.setAccessible(true);
                    }
                    int resId = resField.getInt(view);
                    handleDrawable = ContextCompat.getDrawable(context, resId);
                }

                if (handleDrawable != null) {
                    Drawable drawable = handleDrawable.mutate();
                    drawable.setColorFilter(color, PorterDuff.Mode.SRC_IN);
                    handleField.set(editor, drawable);
                }
            }
        } catch (Exception e) {
            Log_OC.e(TAG, "Error setting TextView handles color", e);
        }
    }

    public static boolean isDarkModeActive(Context context) {
        int nightModeFlag = context.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;

        return Configuration.UI_MODE_NIGHT_YES == nightModeFlag;
    }

    @SuppressFBWarnings(
        value = "SF_SWITCH_NO_DEFAULT",
        justification = "We only create avatars for a subset of share types")
    public static void createAvatar(ShareType type, ImageView avatar, Context context) {
        switch (type) {
            case GROUP:
                avatar.setImageResource(R.drawable.ic_group);
                avatar.setBackground(ResourcesCompat.getDrawable(context.getResources(),
                                                                 R.drawable.round_bgnd,
                                                                 null));
                avatar.setCropToPadding(true);
                avatar.setPadding(4, 4, 4, 4);
                ThemeUtils.colorIconImageViewWithBackground(avatar, context);
                break;

            case ROOM:
                avatar.setImageResource(R.drawable.first_run_talk);
                avatar.setBackground(ResourcesCompat.getDrawable(context.getResources(),
                                                                 R.drawable.round_bgnd,
                                                                 null));
                avatar.setCropToPadding(true);
                avatar.setPadding(8, 8, 8, 8);
                ThemeUtils.colorIconImageViewWithBackground(avatar, context);
                break;

            case CIRCLE:
                avatar.setImageResource(R.drawable.ic_circles);
                avatar.setBackground(ResourcesCompat.getDrawable(context.getResources(),
                                                                 R.drawable.round_bgnd,
                                                                 null));
                avatar.getBackground().setColorFilter(context.getResources().getColor(R.color.nc_grey),
                                                      PorterDuff.Mode.SRC_IN);
                avatar.getDrawable().mutate().setColorFilter(context.getResources().getColor(R.color.icon_on_nc_grey),
                                                             PorterDuff.Mode.SRC_IN);
                avatar.setCropToPadding(true);
                avatar.setPadding(4, 4, 4, 4);
                break;

            case EMAIL:
                avatar.setImageResource(R.drawable.ic_email);
                avatar.setBackground(ResourcesCompat.getDrawable(context.getResources(),
                                                                 R.drawable.round_bgnd,
                                                                 null));
                avatar.setCropToPadding(true);
                avatar.setPadding(8, 8, 8, 8);
                avatar.getBackground().setColorFilter(context.getResources().getColor(R.color.nc_grey),
                                                      PorterDuff.Mode.SRC_IN);
                avatar.getDrawable().mutate().setColorFilter(context.getResources().getColor(R.color.icon_on_nc_grey),
                                                             PorterDuff.Mode.SRC_IN);
                break;
        }
    }
}
