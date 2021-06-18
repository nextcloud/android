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

import android.accounts.Account;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Color;

import com.nextcloud.client.account.User;
import com.nextcloud.client.account.UserAccountManagerImpl;
import com.owncloud.android.R;
import com.owncloud.android.datamodel.FileDataStorageManager;
import com.owncloud.android.lib.resources.status.OCCapability;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.ColorUtils;

/**
 * Utility class with methods for theming related .
 */
public final class ThemeColorUtils {

    private static final int INDEX_LUMINATION = 2;
    private static final double MAX_LIGHTNESS = 0.92;
    public static final double LUMINATION_THRESHOLD = 0.8;

    private ThemeColorUtils() {
        // utility class -> private constructor
    }

    public static int primaryAccentColor(Context context) {
        OCCapability capability = getCapability(context);

        try {
            float adjust;
            if (isDarkModeActive(context)) {
                adjust = +0.5f;
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

    public static int calculateDarkColor(int color, Context context) {
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

    public static String colorToHexString(int color) {
        return String.format("#%06X", 0xFFFFFF & color);
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
            return ThemeColorUtils.fontColor(context, false);
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

    public static boolean isDarkModeActive(Context context) {
        int nightModeFlag = context.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;

        return Configuration.UI_MODE_NIGHT_YES == nightModeFlag;
    }

    public static String primaryColorToHexString(Context context) {
        return String.format("#%06X", 0xFFFFFF & primaryColor(context, true));
    }

    public static int unchangedPrimaryColor(Account account, Context context) {
        try {
            return Color.parseColor(getCapability(account, context).getServerColor());
        } catch (Exception e) {
            return context.getResources().getColor(R.color.primary);
        }
    }

    public static int unchangedFontColor(Context context) {
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
}
