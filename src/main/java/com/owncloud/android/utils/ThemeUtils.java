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
import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.view.View;
import android.view.Window;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputLayout;
import com.nextcloud.client.account.UserAccountManagerImpl;
import com.owncloud.android.MainApp;
import com.owncloud.android.R;
import com.owncloud.android.datamodel.FileDataStorageManager;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.lib.resources.status.OCCapability;
import com.owncloud.android.ui.activity.ToolbarActivity;

import java.lang.reflect.Field;

import androidx.annotation.ColorInt;
import androidx.annotation.DrawableRes;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.AppCompatCheckBox;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.graphics.ColorUtils;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.core.widget.CompoundButtonCompat;
import androidx.fragment.app.FragmentActivity;
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

            if (hsl[INDEX_LUMINATION] > LUMINATION_THRESHOLD) {
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
     * Tests if light color is set
     * @return  true if primaryColor is lighter than MAX_LIGHTNESS
     */
    public static boolean lightTheme(Context context) {
        int primaryColor = primaryColor(context);
        float[] hsl = colorToHSL(primaryColor);

        return hsl[INDEX_LUMINATION] >= MAX_LIGHTNESS;
    }

    /**
     * Tests if dark color is set
     * @return true if dark theme -> e.g.use light font color, darker accent color
     */
    public static boolean darkTheme(Context context) {
        int primaryColor = primaryColor(context);
        float[] hsl = colorToHSL(primaryColor);

        return hsl[INDEX_LUMINATION] <= 0.55;
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
                Spannable text = new SpannableString(title);
                text.setSpan(new ForegroundColorSpan(fontColor(context)),
                             0,
                             text.length(),
                             Spannable.SPAN_INCLUSIVE_INCLUSIVE);
                actionBar.setTitle(text);
            }
        }
    }

    public static Spanned getColoredTitle(String title, int color) {
        Spannable text = new SpannableString(title);
        text.setSpan(new ForegroundColorSpan(color),
                     0,
                     text.length(),
                     Spannable.SPAN_INCLUSIVE_INCLUSIVE);

        return text;
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
                String title = context.getString(titleId);
                Spannable text = new SpannableString(title);
                text.setSpan(new ForegroundColorSpan(fontColor(context)),
                             0,
                             text.length(),
                             Spannable.SPAN_INCLUSIVE_INCLUSIVE);
                actionBar.setTitle(text);
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
        snackbar.setActionTextColor(ContextCompat.getColor(context, R.color.fg_inverse));
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
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                View decor = window.getDecorView();
                if (lightTheme(fragmentActivity.getApplicationContext())) {
                    decor.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
                } else {
                    decor.setSystemUiVisibility(0);
                }
            }
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

    public static void themeDialogActionButton(MaterialButton button) {
        if (button == null ) {
            return;
        }

        Context context = button.getContext();
        int accentColor = ThemeUtils.primaryAccentColor(button.getContext());
        int disabledColor = ContextCompat.getColor(context, R.color.disabled_text);
        button.setTextColor(new ColorStateList(
            new int[][]{
                new int[] { android.R.attr.state_enabled}, // enabled
                new int[] {-android.R.attr.state_enabled}, // disabled
            },
            new int[]{
                accentColor,
                disabledColor
            }
        ));
    }

    public static void themeEditText(Context context, EditText editText, boolean themedBackground) {
        if (editText == null) { return; }

        int color = primaryColor(context);
        // Don't theme the view when it is already on a theme'd background
        if (themedBackground) {
            if (darkTheme(context)) {
                color = ContextCompat.getColor(context, R.color.themed_fg);
            } else {
                color = ContextCompat.getColor(context, R.color.themed_fg_inverse);
            }
        } else {
            if (lightTheme(context)) {
                color = ContextCompat.getColor(context, R.color.fg_default);
            }
        }

        editText.setHighlightColor(context.getResources().getColor(R.color.fg_contrast));
        setEditTextCursorColor(editText, color);
        setTextViewHandlesColor(context, editText, color);
    }

    public static void themeSearchView(Context context, SearchView searchView, boolean themedBackground) {
        if (searchView == null) { return; }

        SearchView.SearchAutoComplete editText = searchView.findViewById(R.id.search_src_text);
        themeEditText(context, editText, themedBackground);
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

    /**
     * Lifted from SO.
     * FindBugs surpressed because of lack of public API to alter the cursor color.
     *
     * @param editText  TextView to be styled
     * @param color     The desired cursor colour
     * @see             <a href="https://stackoverflow.com/a/52564925">StackOverflow url</a>
     */
    @SuppressFBWarnings
    public static void setEditTextCursorColor(EditText editText, int color) {
        try {
            // Get the cursor resource id
            if (Build.VERSION.SDK_INT >= 28) {//set differently in Android P (API 28)
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
     * Set the color of the handles when you select text in a
     * {@link android.widget.EditText} or other view that extends {@link TextView}.
     * FindBugs surpressed because of lack of public API to alter the {@link TextView} handles color.
     *
     * @param view
     *     The {@link TextView} or a {@link View} that extends {@link TextView}.
     * @param color
     *     The color to set for the text handles
     *
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
}
