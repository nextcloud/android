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
package com.nextcloud.ui.theming;

import android.accounts.Account;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Color;
import android.text.TextUtils;

import com.nextcloud.client.account.UserAccountManagerImpl;
import com.owncloud.android.R;
import com.owncloud.android.datamodel.FileDataStorageManager;
import com.owncloud.android.lib.resources.status.OCCapability;
import com.owncloud.android.utils.NextcloudServer;

import androidx.annotation.Nullable;
import androidx.core.graphics.ColorUtils;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Utility class with methods for color values used for theming.
 */
public final class ColorsUtils {

    private static final int INDEX_LUMINATION = 2;
    private static final double MAX_LIGHTNESS = 0.92;
    public static final double LUMINATION_THRESHOLD = 0.8;

    private ColorsUtils() {
        // utility class -> private constructor
    }

    /**
     * return the element color defined in the server-side theming respecting Android dark/light theming and edge case
     * scenarios.
     *
     * @param context the context (needed to load client-side colors and capability infos)
     * @return the color
     */
    public static int elementColor(@Nullable Context context) {
        return elementColor(null, context, true);
    }

    /**
     * return the element color defined in the server-side theming respecting Android dark/light theming and edge case
     * scenarios.
     *
     * @param account the Nextcloud user
     * @param context the context (needed to load client-side colors and capability infos)
     * @return the color
     */
    public static int elementColor(@Nullable Account account,
                                   @Nullable Context context) {
        return elementColor(account, context, true);
    }

    /**
     * return the element color defined in the server-side theming respecting Android dark/light theming and edge case
     * scenarios.
     *
     * @param account           the Nextcloud user
     * @param replaceEdgeColors flag if edge case color scenarios should be handled
     * @param context           the context (needed to load client-side colors and capability infos)
     * @return the color
     */
    public static int elementColor(@Nullable Account account,
                                   @Nullable Context context,
                                   boolean replaceEdgeColors) {
        return elementColor(account, context, replaceEdgeColors, false);
    }

    /**
     * return the element color defined in the server-side theming respecting Android dark/light theming and edge case
     * scenarios including drawer menu.
     *
     * @param account                          the Nextcloud user
     * @param replaceEdgeColors                flag if edge case color scenarios should be handled
     * @param replaceEdgeColorsByInvertedColor flag in edge case handling should be done via color inversion
     *                                         (black/white)
     * @param context                          the context (needed to load client-side colors and capability infos)
     * @return the color
     */
    public static int elementColor(@Nullable Account account,
                                   @Nullable Context context,
                                   boolean replaceEdgeColors,
                                   boolean replaceEdgeColorsByInvertedColor) {

        if (context == null) {
            return Color.GRAY;
        }

        if (account == null) {
            return context.getResources().getColor(R.color.primary);
        }

        OCCapability capability = getCapability(account, context);

        if (!TextUtils.isEmpty(capability.getServerElementColorDark())
            && !TextUtils.isEmpty(capability.getServerElementColorBright())) {
            // Dark/Light contrast aware background available
            try {
                if (isDarkModeActive(context)) {
                    return Color.parseColor(capability.getServerElementColorDark());
                } else {
                    return Color.parseColor(capability.getServerElementColorBright());
                }
            } catch (Exception e) {
                return elementColorLegacy(account, context, replaceEdgeColors, replaceEdgeColorsByInvertedColor);
            }
        } else if (!TextUtils.isEmpty(capability.getServerElementColor())) {
            return elementColorLegacy(account, context, replaceEdgeColors, replaceEdgeColorsByInvertedColor);
        } else if (!TextUtils.isEmpty(capability.getServerColor())) {
            return serverColor(account, context);
        }

        return context.getResources().getColor(R.color.primary);
    }

    @NextcloudServer(max = 19)
    private static int elementColorLegacy(@Nullable Account account,
                                          @Nullable Context context,
                                          boolean replaceEdgeColors,
                                          boolean replaceEdgeColorsByInvertedColor) {
        if (context == null) {
            return Color.GRAY;
        }

        if (account == null) {
            return context.getResources().getColor(R.color.primary);
        }

        OCCapability capability = getCapability(account, context);

        try {
            return Color.parseColor(capability.getServerElementColor());
        } catch (Exception e) {
            if (replaceEdgeColors) {
                return replaceEdgeColor(context, serverColor(account, context), replaceEdgeColorsByInvertedColor);
            } else {
                return luminationAwareColor(context, serverColor(account, context));
            }
        }
    }

    @NextcloudServer(max = 19)
    private static int serverColor(@Nullable Account account, @Nullable Context context) {
        try {
            return Color.parseColor(getCapability(account, context).getServerColor());
        } catch (Exception e) {
            if (context != null) {
                return context.getResources().getColor(R.color.primary);
            } else {
                return Color.GRAY;
            }
        }
    }

    private static int replaceEdgeColor(Context context, int color, boolean replaceEdgeColorsByInvertedColor) {
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
    }

    public static String colorToHexString(int color) {
        return String.format("#%06X", 0xFFFFFF & color);
    }

    public static int elementTextColor(Context context) {
        try {
            return Color.parseColor(getCapability(null, context).getServerTextColor());
        } catch (Exception e) {
            if (ColorsUtils.darkColor(context)) {
                return Color.WHITE;
            } else {
                return Color.BLACK;
            }
        }
    }

    public static int calculateDarkColor(int color, Context context) {
        try {
            return adjustLightness(-0.2f, color, -1f);
        } catch (Exception e) {
            if (context != null) {
                return context.getResources().getColor(R.color.primary_dark);
            } else {
                return Color.DKGRAY;
            }
        }
    }

    @SuppressFBWarnings("CLI_CONSTANT_LIST_INDEX")
    private static int luminationAwareColor(Context context, int color) {
        float[] hsl = colorToHSL(color);

        if (hsl[INDEX_LUMINATION] > LUMINATION_THRESHOLD) {
            return getNeutralGrey(context);
        } else {
            return color;
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
    @SuppressFBWarnings("CLI_CONSTANT_LIST_INDEX")
    public static int adjustLightness(float lightnessDelta, int color, float threshold) {
        float[] hsl = colorToHSL(color);

        if (threshold == -1f) {
            hsl[INDEX_LUMINATION] += lightnessDelta;
        } else {
            hsl[INDEX_LUMINATION] = Math.min(hsl[INDEX_LUMINATION] + lightnessDelta, threshold);
        }

        return ColorUtils.HSLToColor(hsl);
    }

    /**
     * Tests if dark color is set
     *
     * @return true if dark theme -> e.g.use light font color, darker accent color
     */
    @SuppressFBWarnings("CLI_CONSTANT_LIST_INDEX")
    public static boolean darkColor(Context context) {
        int elementColor = ColorsUtils.elementColor(context);
        float[] hsl = ColorsUtils.colorToHSL(elementColor);

        return hsl[INDEX_LUMINATION] <= 0.55;
    }

    public static float[] colorToHSL(int color) {
        float[] hsl = new float[3];
        ColorUtils.RGBToHSL(Color.red(color), Color.green(color), Color.blue(color), hsl);

        return hsl;
    }

    /**
     * Tests if color is light
     *
     * @param color the color
     * @return true if color is lighter than MAX_LIGHTNESS
     */
    @SuppressFBWarnings("CLI_CONSTANT_LIST_INDEX")
    public static boolean isLightColor(int color) {
        float[] hsl = colorToHSL(color);

        return hsl[INDEX_LUMINATION] >= MAX_LIGHTNESS;
    }

    private static boolean isDarkModeActive(Context context) {
        int nightModeFlag = context.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;

        return Configuration.UI_MODE_NIGHT_YES == nightModeFlag;
    }

    public static int getNeutralGrey(Context context) {
        return ColorsUtils.darkColor(context) ? context.getResources().getColor(R.color.fg_contrast) : Color.GRAY;
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
}
