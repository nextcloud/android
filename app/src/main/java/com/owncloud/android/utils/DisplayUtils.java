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
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.text.style.StyleSpan;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Toast;

import com.bumptech.glide.GenericRequestBuilder;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.model.StreamEncoder;
import com.bumptech.glide.load.resource.file.FileToStreamDecoder;
import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.request.target.Target;
import com.caverock.androidsvg.SVG;
import com.elyeproj.loaderviewlibrary.LoaderImageView;
import com.google.android.material.snackbar.Snackbar;
import com.nextcloud.client.account.CurrentAccountProvider;
import com.nextcloud.client.account.User;
import com.nextcloud.client.network.ClientFactory;
import com.nextcloud.client.preferences.AppPreferences;
import com.owncloud.android.MainApp;
import com.owncloud.android.R;
import com.owncloud.android.datamodel.ArbitraryDataProvider;
import com.owncloud.android.datamodel.ArbitraryDataProviderImpl;
import com.owncloud.android.datamodel.FileDataStorageManager;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.datamodel.SyncedFolderProvider;
import com.owncloud.android.datamodel.ThumbnailsCacheManager;
import com.owncloud.android.lib.common.OwnCloudAccount;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.ui.TextDrawable;
import com.owncloud.android.ui.activity.FileDisplayActivity;
import com.owncloud.android.ui.dialog.SortingOrderDialogFragment;
import com.owncloud.android.ui.events.SearchEvent;
import com.owncloud.android.ui.fragment.OCFileListFragment;
import com.owncloud.android.utils.glide.CustomGlideUriLoader;
import com.owncloud.android.utils.svg.SvgDecoder;
import com.owncloud.android.utils.svg.SvgDrawableTranscoder;
import com.owncloud.android.utils.theme.ViewThemeUtils;

import org.greenrobot.eventbus.EventBus;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.net.IDN;
import java.nio.charset.Charset;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.widget.AppCompatDrawableManager;
import androidx.core.content.res.ResourcesCompat;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import static com.owncloud.android.ui.dialog.SortingOrderDialogFragment.SORTING_ORDER_FRAGMENT;
import static com.owncloud.android.utils.FileSortOrder.SORT_A_TO_Z_ID;
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
    private static final String MIME_TYPE_UNKNOWN = "Unknown type";

    private static final String HTTP_PROTOCOL = "http://";
    private static final String HTTPS_PROTOCOL = "https://";
    private static final String TWITTER_HANDLE_PREFIX = "@";
    private static final int MIMETYPE_PARTS_COUNT = 2;
    private static final int BYTE_SIZE_DIVIDER = 1024;
    private static final double BYTE_SIZE_DIVIDER_DOUBLE = 1024.0;
    private static final int DATE_TIME_PARTS_SIZE = 2;

    public static final String MONTH_YEAR_PATTERN = "MMMM yyyy";
    public static final String MONTH_PATTERN = "MMMM";
    public static final String YEAR_PATTERN = "yyyy";
    public static final int SVG_SIZE = 512;

    private static Map<String, String> mimeType2HumanReadable;

    static {
        mimeType2HumanReadable = new HashMap<>();
        // images
        mimeType2HumanReadable.put("image/jpeg", "JPEG image");
        mimeType2HumanReadable.put("image/jpg", "JPEG image");
        mimeType2HumanReadable.put("image/png", "PNG image");
        mimeType2HumanReadable.put("image/bmp", "Bitmap image");
        mimeType2HumanReadable.put("image/gif", "GIF image");
        mimeType2HumanReadable.put("image/svg+xml", "JPEG image");
        mimeType2HumanReadable.put("image/tiff", "TIFF image");
        // music
        mimeType2HumanReadable.put("audio/mpeg", "MP3 music file");
        mimeType2HumanReadable.put("application/ogg", "OGG music file");
    }

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
                sizeScales[suffixIndex], BigDecimal.ROUND_HALF_UP) + " " + sizeSuffixes[suffixIndex];
        }
    }

    /**
     * Converts MIME types like "image/jpg" to more end user friendly output
     * like "JPG image".
     *
     * @param mimetype MIME type to convert
     * @return A human friendly version of the MIME type, {@link #MIME_TYPE_UNKNOWN} if it can't be converted
     */
    public static String convertMIMEtoPrettyPrint(String mimetype) {
        final String humanReadableMime = mimeType2HumanReadable.get(mimetype);
        if (humanReadableMime != null) {
            return humanReadableMime;
        }
        if (mimetype.split("/").length >= MIMETYPE_PARTS_COUNT) {
            return mimetype.split("/")[1].toUpperCase(Locale.getDefault()) + " file";
        }
        return MIME_TYPE_UNKNOWN;
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
        String dots = "";
        while (urlNoDots.length() > 0 && urlNoDots.charAt(0) == '.') {
            urlNoDots = url.substring(1);
            dots = dots + ".";
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

        return dots + urlNoDots.substring(0, hostStart) + host + urlNoDots.substring(hostEnd);
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

        CharSequence dateString = "";

        // in Future
        if (!showFuture && time > System.currentTimeMillis()) {
            return DisplayUtils.unixTimeToHumanReadable(time);
        }
        // < 60 seconds -> seconds ago
        long diff = System.currentTimeMillis() - time;
        if (diff > 0 && diff < 60 * 1000 && minResolution == DateUtils.SECOND_IN_MILLIS) {
            return c.getString(R.string.file_list_seconds_ago);
        } else {
            dateString = DateUtils.getRelativeDateTimeString(c, time, minResolution, transitionResolution, flags);
        }

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

    /**
     * Update the passed path removing the last "/" if it is not the root folder.
     *
     * @param path the path to be trimmed
     */
    public static String getPathWithoutLastSlash(String path) {

        // Remove last slash from path
        if (path.length() > 1 && path.charAt(path.length() - 1) == OCFile.PATH_SEPARATOR.charAt(0)) {
            return path.substring(0, path.length() - 1);
        }

        return path;
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
     */
    public static void setAvatar(@NonNull User user,
                                 @NonNull String userId,
                                 String displayName,
                                 AvatarGenerationListener listener,
                                 float avatarRadius,
                                 Resources resources,
                                 Object callContext,
                                 Context context) {
        if (callContext instanceof View) {
            ((View) callContext).setContentDescription(String.valueOf(user.toPlatformAccount().hashCode()));
        }

        ArbitraryDataProvider arbitraryDataProvider = new ArbitraryDataProviderImpl(context);

        final String accountName = user.getAccountName();
        String serverName = accountName.substring(accountName.lastIndexOf('@') + 1);
        String eTag = arbitraryDataProvider.getValue(userId + "@" + serverName, ThumbnailsCacheManager.AVATAR);
        String avatarKey = "a_" + userId + "_" + serverName + "_" + eTag;

        // first show old one
        Drawable avatar = BitmapUtils.bitmapToCircularBitmapDrawable(resources,
                                                                     ThumbnailsCacheManager.getBitmapFromDiskCache(avatarKey));

        // if no one exists, show colored icon with initial char
        if (avatar == null) {
            try {
                avatar = TextDrawable.createAvatarByUserId(displayName, avatarRadius);
            } catch (Exception e) {
                Log_OC.e(TAG, "Error calculating RGB value for active account icon.", e);
                avatar = ResourcesCompat.getDrawable(resources,
                                                     R.drawable.account_circle_white,
                                                     null);
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

    public static void downloadIcon(CurrentAccountProvider currentAccountProvider,
                                    ClientFactory clientFactory,
                                    Context context,
                                    String iconUrl,
                                    SimpleTarget imageView,
                                    int placeholder) {
        try {
            if (Uri.parse(iconUrl).getEncodedPath().endsWith(".svg")) {
                downloadSVGIcon(currentAccountProvider, clientFactory, context, iconUrl, imageView, placeholder);
            } else {
                downloadPNGIcon(context, iconUrl, imageView, placeholder);
            }
        } catch (Exception e) {
            Log_OC.d(TAG, "not setting image as activity is destroyed");
        }
    }

    private static void downloadPNGIcon(Context context, String iconUrl, SimpleTarget imageView, int placeholder) {
        Glide
            .with(context)
            .load(iconUrl)
            .centerCrop()
            .placeholder(placeholder)
            .error(placeholder)
            .crossFade()
            .into(imageView);
    }

    private static void downloadSVGIcon(CurrentAccountProvider currentAccountProvider,
                                        ClientFactory clientFactory,
                                        Context context,
                                        String iconUrl,
                                        SimpleTarget imageView,
                                        int placeholder) {
        GenericRequestBuilder<Uri, InputStream, SVG, Drawable> requestBuilder = Glide.with(context)
            .using(new CustomGlideUriLoader(currentAccountProvider.getUser(), clientFactory), InputStream.class)
            .from(Uri.class)
            .as(SVG.class)
            .transcode(new SvgDrawableTranscoder(context), Drawable.class)
            .sourceEncoder(new StreamEncoder())
            .cacheDecoder(new FileToStreamDecoder<>(new SvgDecoder()))
            .decoder(new SvgDecoder())
            .placeholder(placeholder)
            .error(placeholder)
            .animate(android.R.anim.fade_in);


        Uri uri = Uri.parse(iconUrl);
        requestBuilder
            .diskCacheStrategy(DiskCacheStrategy.SOURCE)
            .load(uri)
            .into(imageView);
    }

    public static Bitmap downloadImageSynchronous(Context context, String imageUrl) {
        try {
            return Glide.with(context)
                .load(imageUrl)
                .asBitmap()
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .skipMemoryCache(true)
                .into(Target.SIZE_ORIGINAL, Target.SIZE_ORIGINAL)
                .get();
        } catch (Exception e) {
            Log_OC.e(TAG, "Could not download image " + imageUrl);
            return null;
        }
    }

    private static void switchToSearchFragment(Activity activity, SearchEvent event) {
        if (activity instanceof FileDisplayActivity) {
            EventBus.getDefault().post(event);
        } else {
            Intent recentlyAddedIntent = new Intent(activity.getBaseContext(), FileDisplayActivity.class);
            recentlyAddedIntent.putExtra(OCFileListFragment.SEARCH_EVENT, event);
            recentlyAddedIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            activity.startActivity(recentlyAddedIntent);
        }
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

    /**
     * Show a temporary message in a {@link Snackbar} bound to the content view.
     *
     * @param activity        The {@link Activity} to which's content view the {@link Snackbar} is bound.
     * @param messageResource The resource id of the string resource to use. Can be formatted text.
     * @return The created {@link Snackbar}
     */
    public static Snackbar showSnackMessage(Activity activity, @StringRes int messageResource) {
        return showSnackMessage(activity.findViewById(android.R.id.content), messageResource);
    }

    /**
     * Show a temporary message in a {@link Snackbar} bound to the content view.
     *
     * @param activity The {@link Activity} to which's content view the {@link Snackbar} is bound.
     * @param message  Message to show.
     * @return The created {@link Snackbar}
     */
    public static Snackbar showSnackMessage(Activity activity, String message) {
        final Snackbar snackbar = Snackbar.make(activity.findViewById(android.R.id.content), message, Snackbar.LENGTH_LONG);
        var fab = findFABView(activity);
        if (fab != null && fab.getVisibility() == View.VISIBLE) {
            snackbar.setAnchorView(fab);
        }
        snackbar.show();
        return snackbar;
    }

    private static View findFABView(Activity activity) {
        return activity.findViewById(R.id.fab_main);
    }

    private static View findFABView(View view) {
        return view.findViewById(R.id.fab_main);
    }

    /**
     * Show a temporary message in a {@link Snackbar} bound to the given view.
     *
     * @param view            The view the {@link Snackbar} is bound to.
     * @param messageResource The resource id of the string resource to use. Can be formatted text.
     * @return The created {@link Snackbar}
     */
    public static Snackbar showSnackMessage(View view, @StringRes int messageResource) {
        final Snackbar snackbar = Snackbar.make(view, messageResource, Snackbar.LENGTH_LONG);
        var fab = findFABView(view.getRootView());
        if (fab != null && fab.getVisibility() == View.VISIBLE) {
            snackbar.setAnchorView(fab);
        }
        snackbar.show();
        return snackbar;
    }

    /**
     * Show a temporary message in a {@link Snackbar} bound to the given view.
     *
     * @param view    The view the {@link Snackbar} is bound to.
     * @param message The message.
     * @return The created {@link Snackbar}
     */
    public static Snackbar showSnackMessage(View view, String message) {
        final Snackbar snackbar = Snackbar.make(view, message, Snackbar.LENGTH_LONG);
        snackbar.show();
        return snackbar;
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

    /**
     * Show a temporary message in a {@link Snackbar} bound to the content view.
     *
     * @param activity        The {@link Activity} to which's content view the {@link Snackbar} is bound.
     * @param messageResource The resource id of the string resource to use. Can be formatted text.
     * @param formatArgs      The format arguments that will be used for substitution.
     * @return The created {@link Snackbar}
     */
    public static Snackbar showSnackMessage(Activity activity, @StringRes int messageResource, Object... formatArgs) {
        return showSnackMessage(activity, activity.findViewById(android.R.id.content), messageResource, formatArgs);
    }

    /**
     * Show a temporary message in a {@link Snackbar} bound to the content view.
     *
     * @param context         to load resources.
     * @param view            The content view the {@link Snackbar} is bound to.
     * @param messageResource The resource id of the string resource to use. Can be formatted text.
     * @param formatArgs      The format arguments that will be used for substitution.
     * @return The created {@link Snackbar}
     */
    public static Snackbar showSnackMessage(Context context, View view, @StringRes int messageResource, Object... formatArgs) {
        final Snackbar snackbar = Snackbar.make(
            view,
            String.format(context.getString(messageResource, formatArgs)),
            Snackbar.LENGTH_LONG);
        snackbar
            .show();
        return snackbar;
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

            Class<?> args[] = {String.class, inflateDelegateClass};
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

    static public void showErrorAndFinishActivity(Activity activity, String errorMessage) {
        Toast.makeText(activity, errorMessage, Toast.LENGTH_LONG).show();
        activity.finish();
    }

    static public void openSortingOrderDialogFragment(FragmentManager supportFragmentManager, FileSortOrder sortOrder) {
        FragmentTransaction fragmentTransaction = supportFragmentManager.beginTransaction();
        fragmentTransaction.addToBackStack(null);

        SortingOrderDialogFragment.newInstance(sortOrder).show(fragmentTransaction, SORTING_ORDER_FRAGMENT);
    }

    public static @StringRes int getSortOrderStringId(FileSortOrder sortOrder) {
        switch (sortOrder.name) {
            case SORT_Z_TO_A_ID:
                return R.string.menu_item_sort_by_name_z_a;
            case SORT_NEW_TO_OLD_ID:
                return R.string.menu_item_sort_by_date_newest_first;
            case SORT_OLD_TO_NEW_ID:
                return R.string.menu_item_sort_by_date_oldest_first;
            case SORT_BIG_TO_SMALL_ID:
                return R.string.menu_item_sort_by_size_biggest_first;
            case SORT_SMALL_TO_BIG_ID:
                return R.string.menu_item_sort_by_size_smallest_first;
            case SORT_A_TO_Z_ID:
            default:
                return R.string.menu_item_sort_by_name_a_z;
        }
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

    public static void setThumbnail(OCFile file,
                                    ImageView thumbnailView,
                                    User user,
                                    FileDataStorageManager storageManager,
                                    List<ThumbnailsCacheManager.ThumbnailGenerationTask> asyncTasks,
                                    boolean gridView,
                                    Context context,
                                    LoaderImageView shimmerThumbnail,
                                    AppPreferences preferences,
                                    ViewThemeUtils viewThemeUtils,
                                    SyncedFolderProvider syncedFolderProvider) {
        if (file.isFolder()) {
            stopShimmer(shimmerThumbnail, thumbnailView);

            boolean isAutoUploadFolder = SyncedFolderProvider.isAutoUploadFolder(syncedFolderProvider, file, user);
            boolean isDarkModeActive = preferences.isDarkModeEnabled();

            Integer overlayIconId = file.getFileOverlayIconId(isAutoUploadFolder);
            LayerDrawable fileIcon = MimeTypeUtil.getFileIcon(isDarkModeActive, overlayIconId, context, viewThemeUtils);
            thumbnailView.setImageDrawable(fileIcon);
        } else {
            if (file.getRemoteId() != null && file.isPreviewAvailable()) {
                // Thumbnail in cache?
                Bitmap thumbnail = ThumbnailsCacheManager.getBitmapFromDiskCache(
                    ThumbnailsCacheManager.PREFIX_THUMBNAIL + file.getRemoteId());

                if (thumbnail != null && !file.isUpdateThumbnailNeeded()) {
                    stopShimmer(shimmerThumbnail, thumbnailView);

                    if (MimeTypeUtil.isVideo(file)) {
                        Bitmap withOverlay = ThumbnailsCacheManager.addVideoOverlay(thumbnail, context);
                        thumbnailView.setImageBitmap(withOverlay);
                    } else {
                        if (gridView) {
                            BitmapUtils.setRoundedBitmapForGridMode(thumbnail, thumbnailView);
                        } else {
                            BitmapUtils.setRoundedBitmap(thumbnail, thumbnailView);
                        }
                    }
                } else {
                    generateNewThumbnail(file, thumbnailView, user, storageManager, asyncTasks, gridView, context, shimmerThumbnail, preferences, viewThemeUtils);
                }

                if ("image/png".equalsIgnoreCase(file.getMimeType())) {
                    thumbnailView.setBackgroundColor(context.getResources().getColor(R.color.bg_default));
                }
            } else {
                if (file.getRemoteId() != null) {
                    generateNewThumbnail(file, thumbnailView, user, storageManager, asyncTasks, gridView, context, shimmerThumbnail, preferences, viewThemeUtils);
                } else {
                    stopShimmer(shimmerThumbnail, thumbnailView);
                    thumbnailView.setImageDrawable(MimeTypeUtil.getFileTypeIcon(file.getMimeType(),
                                                                                file.getFileName(),
                                                                                context,
                                                                                viewThemeUtils));
                }
            }
        }
    }

    private static void generateNewThumbnail(OCFile file,
                                             ImageView thumbnailView,
                                             User user,
                                             FileDataStorageManager storageManager,
                                             List<ThumbnailsCacheManager.ThumbnailGenerationTask> asyncTasks,
                                             boolean gridView,
                                             Context context,
                                             LoaderImageView shimmerThumbnail,
                                             AppPreferences preferences,
                                             ViewThemeUtils viewThemeUtils) {
        if (!ThumbnailsCacheManager.cancelPotentialThumbnailWork(file, thumbnailView)) {
            return;
        }

        Bitmap thumbnail = ThumbnailsCacheManager.getBitmapFromDiskCache(
            ThumbnailsCacheManager.PREFIX_THUMBNAIL + file.getRemoteId());

        for (ThumbnailsCacheManager.ThumbnailGenerationTask task : asyncTasks) {
            if (file.getRemoteId() != null && task.getImageKey() != null &&
                file.getRemoteId().equals(task.getImageKey())) {
                return;
            }
        }
        try {
            final ThumbnailsCacheManager.ThumbnailGenerationTask task =
                new ThumbnailsCacheManager.ThumbnailGenerationTask(thumbnailView,
                                                                   storageManager,
                                                                   user,
                                                                   asyncTasks,
                                                                   gridView,
                                                                   file.getRemoteId());
            if (thumbnail == null) {
                Drawable drawable = MimeTypeUtil.getFileTypeIcon(file.getMimeType(),
                                                                 file.getFileName(),
                                                                 context,
                                                                 viewThemeUtils);
                if (drawable == null) {
                    drawable = ResourcesCompat.getDrawable(context.getResources(),
                                                           R.drawable.file_image,
                                                           null);
                }
                if (drawable == null) {
                    drawable = new ColorDrawable(Color.GRAY);
                }

                int px = ThumbnailsCacheManager.getThumbnailDimension();
                thumbnail = BitmapUtils.drawableToBitmap(drawable, px, px);
            }
            final ThumbnailsCacheManager.AsyncThumbnailDrawable asyncDrawable =
                new ThumbnailsCacheManager.AsyncThumbnailDrawable(context.getResources(),
                                                                  thumbnail, task);

            if (shimmerThumbnail != null && shimmerThumbnail.getVisibility() == View.GONE) {
                if (gridView) {
                    configShimmerGridImageSize(shimmerThumbnail, preferences.getGridColumns());
                }
                startShimmer(shimmerThumbnail, thumbnailView);
            }

            task.setListener(new ThumbnailsCacheManager.ThumbnailGenerationTask.Listener() {
                @Override
                public void onSuccess() {
                    stopShimmer(shimmerThumbnail, thumbnailView);
                }

                @Override
                public void onError() {
                    stopShimmer(shimmerThumbnail, thumbnailView);
                }
            });

            thumbnailView.setImageDrawable(asyncDrawable);
            asyncTasks.add(task);
            task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,
                                   new ThumbnailsCacheManager.ThumbnailGenerationTaskObject(file,
                                                                                            file.getRemoteId()));
        } catch (IllegalArgumentException e) {
            Log_OC.d(TAG, "ThumbnailGenerationTask : " + e.getMessage());
        }
    }

    public static void startShimmer(LoaderImageView thumbnailShimmer, ImageView thumbnailView) {
        thumbnailShimmer.setImageResource(R.drawable.background);
        thumbnailShimmer.resetLoader();
        thumbnailView.setVisibility(View.GONE);
        thumbnailShimmer.setVisibility(View.VISIBLE);
    }

    public static void stopShimmer(@Nullable LoaderImageView thumbnailShimmer, ImageView thumbnailView) {
        if (thumbnailShimmer != null) {
            thumbnailShimmer.setVisibility(View.GONE);
        }

        thumbnailView.setVisibility(View.VISIBLE);
    }

    private static void configShimmerGridImageSize(LoaderImageView thumbnailShimmer, float gridColumns) {
        try {
            FrameLayout.LayoutParams targetLayoutParams = (FrameLayout.LayoutParams) thumbnailShimmer.getLayoutParams();

            final Point screenSize = getScreenSize(thumbnailShimmer.getContext());
            final int marginLeftAndRight = targetLayoutParams.leftMargin + targetLayoutParams.rightMargin;
            final int size = Math.round(screenSize.x / gridColumns - marginLeftAndRight);

            FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(size, size);
            params.setMargins(targetLayoutParams.leftMargin,
                              targetLayoutParams.topMargin,
                              targetLayoutParams.rightMargin,
                              targetLayoutParams.bottomMargin);
            thumbnailShimmer.setLayoutParams(params);
        } catch (Exception exception) {
            Log_OC.e("ConfigShimmer", exception.getMessage());
        }
    }

    private static Point getScreenSize(Context context) throws Exception {
        final WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        if (windowManager != null) {
            final Point displaySize = new Point();
            windowManager.getDefaultDisplay().getSize(displaySize);
            return displaySize;
        } else {
            throw new Exception("WindowManager not found");
        }
    }
}
