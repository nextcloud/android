/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2024 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-FileCopyrightText: 2023 ZetaTom
 * SPDX-FileCopyrightText: 2022 Álvaro Brey <alvaro@alvarobrey.com>
 * SPDX-FileCopyrightText: 2021 TSI-mc
 * SPDX-FileCopyrightText: 2020 Infomaniak Network SA
 * SPDX-FileCopyrightText: 2020 Joris Bodin <joris.bodin@infomaniak.com>
 * SPDX-FileCopyrightText: 2020 Kilian Périsset <kilian.perisset@infomaniak.com>
 * SPDX-FileCopyrightText: 2020 Chris Narkiewicz <hello@ezaquarii.com>
 * SPDX-FileCopyrightText: 2018-2020 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2017 Harikrishnan Rajan <rhari991@gmail.com>
 * SPDX-FileCopyrightText: 2017 Alejandro Morales <aleister09@gmail.com>
 * SPDX-FileCopyrightText: 2016 Andy Scherzinger <info@andy-scherzinger.de>
 * SPDX-FileCopyrightText: 2016 ownCloud Inc.
 * SPDX-FileCopyrightText: 2015 David A. Velasco <dvelasco@solidgear.es>
 * SPDX-FileCopyrightText: 2012 Lennart Rosam <lennart@familie-rosam.de>
 * SPDX-FileCopyrightText: 2011 Bartosz Przybylski <bart.p.pl@gmail.com>
 * SPDX-License-Identifier: GPL-2.0-only AND (AGPL-3.0-or-later OR GPL-2.0-only)
 */
package com.owncloud.android.utils;

import android.accounts.AccountManager;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.text.style.StyleSpan;
import android.util.DisplayMetrics;
import android.view.View;

import com.google.android.material.snackbar.Snackbar;
import com.nextcloud.client.account.User;
import com.owncloud.android.MainApp;
import com.owncloud.android.R;
import com.owncloud.android.datamodel.ArbitraryDataProvider;
import com.owncloud.android.datamodel.ArbitraryDataProviderImpl;
import com.owncloud.android.datamodel.ThumbnailsCacheManager;
import com.owncloud.android.lib.common.OwnCloudAccount;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.ui.TextDrawable;
import com.owncloud.android.ui.dialog.SortingOrderDialogFragment;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.IDN;
import java.nio.charset.Charset;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.widget.AppCompatDrawableManager;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import static com.owncloud.android.ui.dialog.SortingOrderDialogFragment.SORTING_ORDER_FRAGMENT;
import static com.owncloud.android.utils.FileSortOrder.SORT_BIG_TO_SMALL_ID;
import static com.owncloud.android.utils.FileSortOrder.SORT_NEW_TO_OLD_ID;
import static com.owncloud.android.utils.FileSortOrder.SORT_OLD_TO_NEW_ID;
import static com.owncloud.android.utils.FileSortOrder.SORT_SMALL_TO_BIG_ID;
import static com.owncloud.android.utils.FileSortOrder.SORT_Z_TO_A_ID;

/**
 * A helper class for UI/display related operations.
 */
public final class DisplayUtils {
    private static final String TAG = DisplayUtils.class.getSimpleName();

    private static final String[] sizeSuffixes = {"B", "KB", "MB", "GB", "TB", "PB", "EB", "ZB", "YB"};
    private static final int[] sizeScales = {0, 0, 1, 1, 1, 2, 2, 2, 2};

    private static final String HTTP_PROTOCOL = "http://";
    private static final String HTTPS_PROTOCOL = "https://";
    private static final String TWITTER_HANDLE_PREFIX = "@";
    private static final int BYTE_SIZE_DIVIDER = 1024;
    private static final double BYTE_SIZE_DIVIDER_DOUBLE = 1024.0;
    private static final int DATE_TIME_PARTS_SIZE = 2;
    private static final Handler mainLooper = new Handler(Looper.getMainLooper());
    public static final String MONTH_YEAR_PATTERN = "MMMM yyyy";
    public static final String MONTH_PATTERN = "MMMM";
    public static final String YEAR_PATTERN = "yyyy";

    public static final long SECOND_IN_MS = 1000;
    public static final long MINUTE_IN_MS = 60 * SECOND_IN_MS;
    public static final long HOUR_IN_MS = 60 * MINUTE_IN_MS;
    public static final long DAY_IN_MS = 24 * HOUR_IN_MS;

    private DisplayUtils() {
        // utility class -> private constructor
    }

    /**
     * Converts the file size in bytes to human readable output.
     * <ul>
     *     <li>appends a size suffix, e.g. B, KB, MB etc.</li>
     *     <li>rounds the size based on the suffix to 0,1 or 2 decimals</li>
     * </ul>
     *
     * @param bytes Input file size
     * @return something readable like "12 MB", {@link com.owncloud.android.R.string#common_pending} for negative
     * byte values
     */
    public static String bytesToHumanReadable(long bytes) {
        if (bytes < 0) {
            return MainApp.string(R.string.common_pending);
        } else {
            double result = bytes;
            int suffixIndex = 0;
            while (result > BYTE_SIZE_DIVIDER && suffixIndex < sizeSuffixes.length) {
                result /= BYTE_SIZE_DIVIDER_DOUBLE;
                suffixIndex++;
            }

            return new BigDecimal(String.valueOf(result)).setScale(
                sizeScales[suffixIndex], RoundingMode.HALF_UP) + " " + sizeSuffixes[suffixIndex];
        }
    }

    /**
     * Converts Unix time to human readable format
     *
     * @param milliseconds that have passed since 01/01/1970
     * @return The human readable time for the users locale
     */
    public static String unixTimeToHumanReadable(long milliseconds) {
        Date date = new Date(milliseconds);
        DateFormat df = DateFormat.getDateTimeInstance();
        return df.format(date);
    }

    /**
     * beautifies a given URL by removing any http/https protocol prefix.
     *
     * @param url to be beautified url
     * @return beautified url
     */
    public static String beautifyURL(@Nullable String url) {
        if (TextUtils.isEmpty(url)) {
            return "";
        }

        if (url.length() >= 7 && HTTP_PROTOCOL.equalsIgnoreCase(url.substring(0, 7))) {
            return url.substring(HTTP_PROTOCOL.length()).trim();
        }

        if (url.length() >= 8 && HTTPS_PROTOCOL.equalsIgnoreCase(url.substring(0, 8))) {
            return url.substring(HTTPS_PROTOCOL.length()).trim();
        }

        return url.trim();
    }

    /**
     * beautifies a given twitter handle by prefixing it with an @ in case it is missing.
     *
     * @param handle to be beautified twitter handle
     * @return beautified twitter handle
     */
    public static String beautifyTwitterHandle(@Nullable String handle) {
        if (handle != null) {
            String trimmedHandle = handle.trim();

            if (TextUtils.isEmpty(trimmedHandle)) {
                return "";
            }

            if (trimmedHandle.startsWith(TWITTER_HANDLE_PREFIX)) {
                return trimmedHandle;
            } else {
                return TWITTER_HANDLE_PREFIX + trimmedHandle;
            }
        } else {
            return "";
        }
    }

    /**
     * Converts an internationalized domain name (IDN) in an URL to and from ASCII/Unicode.
     *
     * @param url the URL where the domain name should be converted
     * @param toASCII if true converts from Unicode to ASCII, if false converts from ASCII to Unicode
     * @return the URL containing the converted domain name
     */
    public static String convertIdn(String url, boolean toASCII) {

        String urlNoDots = url;
        StringBuilder dots = new StringBuilder();
        while (urlNoDots.length() > 0 && urlNoDots.charAt(0) == '.') {
            urlNoDots = url.substring(1);
            dots.append(".");
        }

        // Find host name after '//' or '@'
        int hostStart = 0;
        if (urlNoDots.contains("//")) {
            hostStart = url.indexOf("//") + "//".length();
        } else if (url.contains("@")) {
            hostStart = url.indexOf('@') + "@".length();
        }

        int hostEnd = url.substring(hostStart).indexOf('/');
        // Handle URL which doesn't have a path (path is implicitly '/')
        hostEnd = hostEnd == -1 ? urlNoDots.length() : hostStart + hostEnd;

        String host = urlNoDots.substring(hostStart, hostEnd);
        host = toASCII ? IDN.toASCII(host) : IDN.toUnicode(host);

        return dots.toString() + urlNoDots.substring(0, hostStart) + host + urlNoDots.substring(hostEnd);
    }

    /**
     * Creates the display string for a user.
     *
     * @return the display string for the given account data
     */
    public static String getAccountNameDisplayText(User user) {
        final OwnCloudAccount ocs = user.toOwnCloudAccount();
        final String accountName = user.getAccountName();
        return ocs.getDisplayName()
                + "@"
                + convertIdn(accountName.substring(accountName.lastIndexOf('@') + 1), false);
    }


    /**
     * calculates the relative time string based on the given modification timestamp.
     *
     * @param context the app's context
     * @param modificationTimestamp the UNIX timestamp of the file modification time in milliseconds.
     * @return a relative time string
     */
    public static CharSequence getRelativeTimestamp(Context context, long modificationTimestamp) {
        return getRelativeDateTimeString(context, modificationTimestamp, DateUtils.SECOND_IN_MILLIS,
                                         DateUtils.WEEK_IN_MILLIS, 0);
    }

    public static CharSequence getRelativeTimestamp(Context context, long modificationTimestamp, boolean showFuture) {
        return getRelativeDateTimeString(context,
                                         modificationTimestamp,
                                         DateUtils.SECOND_IN_MILLIS,
                                         DateUtils.WEEK_IN_MILLIS,
                                         0,
                                         showFuture);
    }

    public static CharSequence getRelativeDateTimeString(Context c, long time, long minResolution,
                                                         long transitionResolution, int flags) {
        return getRelativeDateTimeString(c, time, minResolution, transitionResolution, flags, false);
    }

    public static CharSequence getRelativeDateTimeString(Context c,
                                                         long time,
                                                         long minResolution,
                                                         long transitionResolution,
                                                         int flags,
                                                         boolean showFuture) {


        // in Future
        if (!showFuture && time > System.currentTimeMillis()) {
            return DisplayUtils.unixTimeToHumanReadable(time);
        }
        // < 60 seconds -> seconds ago
        long diff = System.currentTimeMillis() - time;
        if (diff > 0 && diff < 60 * 1000 && minResolution == DateUtils.MINUTE_IN_MILLIS) {
            return c.getString(R.string.file_list_seconds_ago);
        } else {
            CharSequence dateString = DateUtils.getRelativeDateTimeString(c, time, minResolution, transitionResolution, flags);

            String[] parts = dateString.toString().split(",");
            if (parts.length == DATE_TIME_PARTS_SIZE) {
                if (parts[1].contains(":") && !parts[0].contains(":")) {
                    return parts[0];
                } else if (parts[0].contains(":") && !parts[1].contains(":")) {
                    return parts[1];
                }
            }
            // dateString contains unexpected format. fallback: use relative date time string from android api as is.
            return dateString.toString();
        }
    }

    /**
     * Gets the screen size in pixels.
     *
     * @param caller Activity calling; needed to get access to the {@link android.view.WindowManager}
     * @return Size in pixels of the screen, or default {@link Point} if caller is null
     */
    public static Point getScreenSize(Activity caller) {
        Point size = new Point();
        if (caller != null) {
            caller.getWindowManager().getDefaultDisplay().getSize(size);
        }
        return size;
    }

    /**
     * styling of given spanText within a given text.
     *
     * @param text     the non styled complete text
     * @param spanText the to be styled text
     * @param style    the style to be applied
     */
    public static SpannableStringBuilder createTextWithSpan(String text, String spanText, StyleSpan style) {
        if (text == null) {
            return null;
        }

        SpannableStringBuilder sb = new SpannableStringBuilder(text);
        if (spanText == null) {
            return sb;
        }

        int start = text.lastIndexOf(spanText);

        if (start < 0) {
            return sb;
        }

        int end = start + spanText.length();
        sb.setSpan(style, start, end, Spannable.SPAN_INCLUSIVE_INCLUSIVE);
        return sb;
    }

    public interface AvatarGenerationListener {
        void avatarGenerated(Drawable avatarDrawable, Object callContext);

        boolean shouldCallGeneratedCallback(String tag, Object callContext);
    }

    /**
     * fetches and sets the avatar of the given account in the passed callContext
     *
     * @param user        the account to be used to connect to server
     * @param avatarRadius   the avatar radius
     * @param resources      reference for density information
     * @param callContext    which context is called to set the generated avatar
     */
    public static void setAvatar(@NonNull User user, AvatarGenerationListener listener,
                                 float avatarRadius, Resources resources, Object callContext, Context context) {

        AccountManager accountManager = AccountManager.get(context);
        String userId = accountManager.getUserData(user.toPlatformAccount(),
                com.owncloud.android.lib.common.accounts.AccountUtils.Constants.KEY_USER_ID);

        if (userId == null) {
            Log_OC.e(TAG, "user id is null, cannot set avatar");
            return;
        }

        setAvatar(user, userId, listener, avatarRadius, resources, callContext, context);
    }

    /**
     * fetches and sets the avatar of the given account in the passed callContext
     *
     * @param user        the account to be used to connect to server
     * @param userId         the userId which avatar should be set
     * @param avatarRadius   the avatar radius
     * @param resources      reference for density information
     * @param callContext    which context is called to set the generated avatar
     */
    public static void setAvatar(@NonNull User user, @NonNull String userId, AvatarGenerationListener listener,
                                 float avatarRadius, Resources resources, Object callContext, Context context) {
        setAvatar(user, userId, userId, listener, avatarRadius, resources, callContext, context);
    }

    /**
     * fetches and sets the avatar of the given account in the passed callContext
     *
     * @param user         the account to be used to connect to server
     * @param userId       the userId which avatar should be set
     * @param displayName  displayName used to generate avatar with first char, only used as fallback
     * @param avatarRadius the avatar radius
     * @param resources    reference for density information
     * @param callContext  which context is called to set the generated avatar
     * @param context      general context
     */
    public static void setAvatar(@NonNull User user,
                                 @NonNull String userId,
                                 String displayName,
                                 AvatarGenerationListener listener,
                                 float avatarRadius,
                                 Resources resources,
                                 Object callContext,
                                 Context context) {
        setAvatar(user, userId, displayName, listener, avatarRadius, resources, callContext, context, 0);
    }

    /**
     * fetches and sets the avatar of the given account in the passed callContext
     *
     * @param user           the account to be used to connect to server
     * @param userId         the userId which avatar should be set
     * @param displayName    displayName used to generate avatar with first char, only used as fallback
     * @param avatarRadius   the avatar radius
     * @param resources      reference for density information
     * @param callContext    which context is called to set the generated avatar
     * @param context        general context
     * @param avatarBorder  value in case the avatar has a border, like in the case of the AvatarGroupLayout
     */
    public static void setAvatar(@NonNull User user,
                                 @NonNull String userId,
                                 String displayName,
                                 AvatarGenerationListener listener,
                                 float avatarRadius,
                                 Resources resources,
                                 Object callContext,
                                 Context context,
                                 int avatarBorder) {
        if (callContext instanceof View v) {
            v.setContentDescription(String.valueOf(user.toPlatformAccount().hashCode()));
        }

        final String accountName = user.getAccountName();
        String serverName = accountName.substring(accountName.lastIndexOf('@') + 1);
        Drawable avatar;

        if (userId.isEmpty()) {
            avatar = ContextCompat.getDrawable(context, R.drawable.ic_link);
            if (avatar != null) {
                int tintColor = ContextCompat.getColor(context, R.color.icon_on_nc_grey);
                avatar.setTint(tintColor);
            }
        } else {
            ArbitraryDataProvider arbitraryDataProvider = new ArbitraryDataProviderImpl(context);
            String eTag = arbitraryDataProvider.getValue(userId + "@" + serverName, ThumbnailsCacheManager.AVATAR);
            String avatarKey = "a_" + userId + "_" + serverName + "_" + eTag;

            // first show old one
            avatar = BitmapUtils.bitmapToCircularBitmapDrawable(resources,
                                                                ThumbnailsCacheManager.getBitmapFromDiskCache(avatarKey));

            // if no one exists, show colored icon with initial char
            if (avatar == null) {
                try {
                    avatar = TextDrawable.createAvatarByUserId(displayName,
                                                               (avatarRadius - avatarBorder));
                } catch (Exception e) {
                    Log_OC.e(TAG, "Error calculating RGB value for active account icon.", e);
                    avatar = ResourcesCompat.getDrawable(resources,
                                                         R.drawable.account_circle_white,
                                                         null);
                }
            }
        }

        listener.avatarGenerated(avatar, callContext);

        // check for new avatar, eTag is compared, so only new one is downloaded
        final ThumbnailsCacheManager.AvatarGenerationTask task =
            new ThumbnailsCacheManager.AvatarGenerationTask(listener,
                                                            callContext,
                                                            user,
                                                            resources,
                                                            avatarRadius,
                                                            userId,
                                                            displayName,
                                                            serverName,
                                                            context);

        task.execute(userId);
    }

    /**
     * Get String data from a InputStream
     *
     * @param inputStream        The File InputStream
     */
    public static String getData(InputStream inputStream) {

        BufferedReader buffreader = new BufferedReader(new InputStreamReader(inputStream, Charset.defaultCharset()));
        String line;
        StringBuilder text = new StringBuilder();
        try {
            while ((line = buffreader.readLine()) != null) {
                text.append(line);
                text.append('\n');
            }
        } catch (IOException e) {
            Log_OC.e(TAG, e.getMessage());
        }
        return text.toString();
    }

    // region snackbar
    public static Snackbar createAndShowSnackMessage(Fragment fragment, @StringRes int messageResource) {
        if (fragment == null) {
            Log_OC.e(TAG, "snackbar cannot be shown fragment is null");
            return null;
        }

        final var activity = fragment.getActivity();
        if (activity == null) {
            Log_OC.e(TAG, "snackbar cannot be shown activity is null");
            return null;
        }

        final var snackbar = Snackbar.make(
            activity.findViewById(android.R.id.content),
            messageResource,
            Snackbar.LENGTH_INDEFINITE);

        var fab = findFABView(activity);
        if (fab != null && fab.getVisibility() == View.VISIBLE) {
            snackbar.setAnchorView(fab);
        }

        mainLooper.post(snackbar::show);
        return snackbar;
    }

    public static void dismissSnackMessage(@Nullable Snackbar snackbar) {
        if (snackbar == null) {
            return;
        }
        mainLooper.post(snackbar::dismiss);
    }

    public static void showSnackMessage(Fragment fragment, @StringRes int messageResource) {
        if (fragment == null) {
            Log_OC.e(TAG, "snackbar cannot be shown fragment is null");
            return;
        }

        final var activity = fragment.getActivity();
        if (activity == null) {
            Log_OC.e(TAG, "snackbar cannot be shown activity is null");
            return;
        }

        showSnackMessage(activity, messageResource);
    }

    public static void showSnackMessage(Activity activity, @StringRes int messageResource) {
        if (activity == null) {
            Log_OC.e(TAG, "snackbar cannot be shown activity is null");
            return;
        }

        showSnackMessage(activity.findViewById(android.R.id.content), messageResource);
    }

    public static void showSnackMessage(Activity activity, @StringRes int messageResource, Object... formatArgs) {
        if (activity == null) {
            Log_OC.e(TAG, "snackbar cannot be shown activity is null");
            return;
        }

        showSnackMessage(activity, activity.findViewById(android.R.id.content), messageResource, formatArgs);
    }

    public static void showSnackMessage(Context context, View view, @StringRes int messageResource, Object... formatArgs) {
        if (context == null || view == null) {
            Log_OC.e(TAG, "snackbar cannot be shown view is null");
            return;
        }

        final var snackbar = Snackbar.make(view, String.format(context.getString(messageResource, formatArgs)), Snackbar.LENGTH_LONG);
        snackbar.show();
    }

    public static void showSnackMessage(Activity activity, String message) {
        if (activity == null) {
            Log_OC.e(TAG, "snackbar cannot be shown activity is null");
            return;
        }

        activity.runOnUiThread(() -> {
            final var snackbar = Snackbar.make(activity.findViewById(android.R.id.content), message, Snackbar.LENGTH_LONG);
            var fab = findFABView(activity);
            if (fab != null && fab.getVisibility() == View.VISIBLE) {
                snackbar.setAnchorView(fab);
            }
            snackbar.show();
        });
    }

    public static void showSnackMessage(View view, @StringRes int messageResource) {
        if (view == null) {
            Log_OC.e(TAG, "snackbar cannot be shown view is null");
            return;
        }

        mainLooper.post(() -> {
            final var snackbar = Snackbar.make(view, messageResource, Snackbar.LENGTH_LONG);
            var fab = findFABView(view.getRootView());
            if (fab != null && fab.getVisibility() == View.VISIBLE) {
                snackbar.setAnchorView(fab);
            }
            snackbar.show();
        });
    }

    public static void showSnackMessage(View view, String message) {
        if (view == null) {
            Log_OC.e(TAG, "snackbar cannot be shown view is null");
            return;
        }

        mainLooper.post(() -> {
            final Snackbar snackbar = Snackbar.make(view, message, Snackbar.LENGTH_LONG);
            snackbar.show();
        });
    }
    // endregion

    private static View findFABView(Activity activity) {
        return activity.findViewById(R.id.fab_main);
    }

    private static View findFABView(View view) {
        return view.findViewById(R.id.fab_main);
    }


    /**
     * create a temporary message in a {@link Snackbar} bound to the given view.
     *
     * @param view            The view the {@link Snackbar} is bound to.
     * @param messageResource The resource id of the string resource to use. Can be formatted text.
     * @return The created {@link Snackbar}
     */
    public static Snackbar createSnackbar(View view, @StringRes int messageResource, int length) {
        return Snackbar.make(view, messageResource, length);
    }

    // Solution inspired by https://stackoverflow.com/questions/34936590/why-isnt-my-vector-drawable-scaling-as-expected
    // Copied from https://raw.githubusercontent.com/nextcloud/talk-android/8ec8606bc61878e87e3ac8ad32c8b72d4680013c/app/src/main/java/com/nextcloud/talk/utils/DisplayUtils.java
    // under GPL3
    public static void useCompatVectorIfNeeded() {
        try {
            @SuppressLint("RestrictedApi") AppCompatDrawableManager drawableManager = AppCompatDrawableManager.get();
            Class<?> inflateDelegateClass = Class.forName("android.support.v7.widget.AppCompatDrawableManager$InflateDelegate");
            Class<?> vdcInflateDelegateClass = Class.forName("android.support.v7.widget.AppCompatDrawableManager$VdcInflateDelegate");

            Constructor<?> constructor = vdcInflateDelegateClass.getDeclaredConstructor();
            constructor.setAccessible(true);
            Object vdcInflateDelegate = constructor.newInstance();

            Class<?>[] args = {String.class, inflateDelegateClass};
            Method addDelegate = AppCompatDrawableManager.class.getDeclaredMethod("addDelegate", args);
            addDelegate.setAccessible(true);
            addDelegate.invoke(drawableManager, "vector", vdcInflateDelegate);
        } catch (Exception e) {
            Log_OC.e(TAG, "Failed to use reflection to enable proper vector scaling");
        }
    }

    public static int convertDpToPixel(float dp, Context context) {
        Resources resources = context.getResources();
        DisplayMetrics metrics = resources.getDisplayMetrics();

        return (int) (dp * ((float) metrics.densityDpi / DisplayMetrics.DENSITY_DEFAULT));
    }

    public static float convertPixelToDp(int px, Context context) {
        Resources resources = context.getResources();
        DisplayMetrics metrics = resources.getDisplayMetrics();

        return px * (DisplayMetrics.DENSITY_DEFAULT / (float) metrics.densityDpi);
    }

    public static boolean isRTL() {
        return TextUtils.getLayoutDirectionFromLocale(Locale.getDefault()) == View.LAYOUT_DIRECTION_RTL;
    }

    static public void showServerOutdatedSnackbar(Activity activity, int length) {
        Snackbar.make(activity.findViewById(android.R.id.content),
                      R.string.outdated_server, length)
            .setAction(R.string.dismiss, v -> {
            })
            .show();
    }

    static public void startLinkIntent(Activity activity, @StringRes int link) {
        startLinkIntent(activity, activity.getString(link));
    }

    static public void startLinkIntent(Activity activity, String url) {
        if (!TextUtils.isEmpty(url)) {
            startLinkIntent(activity, Uri.parse(url));
        }
    }

    static public void startLinkIntent(Activity activity, Uri uri) {
        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
        DisplayUtils.startIntentIfAppAvailable(intent, activity, R.string.no_browser_available);
    }

    static public void startIntentIfAppAvailable(Intent intent, Activity activity, @StringRes int error) {
        if (intent.resolveActivity(activity.getPackageManager()) != null) {
            activity.startActivity(intent);
        } else {
            DisplayUtils.showSnackMessage(activity, error);
        }
    }

    static public void openSortingOrderDialogFragment(FragmentManager supportFragmentManager, FileSortOrder sortOrder) {
        FragmentTransaction fragmentTransaction = supportFragmentManager.beginTransaction();
        fragmentTransaction.addToBackStack(null);

        SortingOrderDialogFragment.newInstance(sortOrder).show(fragmentTransaction, SORTING_ORDER_FRAGMENT);
    }

    public static @StringRes int getSortOrderStringId(FileSortOrder sortOrder) {
        return switch (sortOrder.name) {
            case SORT_Z_TO_A_ID -> R.string.menu_item_sort_by_name_z_a;
            case SORT_NEW_TO_OLD_ID -> R.string.menu_item_sort_by_date_newest_first;
            case SORT_OLD_TO_NEW_ID -> R.string.menu_item_sort_by_date_oldest_first;
            case SORT_BIG_TO_SMALL_ID -> R.string.menu_item_sort_by_size_biggest_first;
            case SORT_SMALL_TO_BIG_ID -> R.string.menu_item_sort_by_size_smallest_first;
            default -> R.string.menu_item_sort_by_name_a_z;
        };
    }

    public static String getDateByPattern(long timestamp, String pattern) {
        return getDateByPattern(timestamp, null, pattern);
    }

    public static String getDateByPattern(long timestamp, @Nullable Context context, String pattern) {
        DateFormat df;
        if (context == null) {
            context = MainApp.getAppContext();
        }
        df = new SimpleDateFormat(pattern, context.getResources().getConfiguration().locale);
        df.setTimeZone(TimeZone.getTimeZone(TimeZone.getDefault().getID()));

        return df.format(timestamp);
    }
}
