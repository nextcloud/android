/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2023 Jonas Mayer <jonas.a.mayer@gmx.net>
 * SPDX-FileCopyrightText: 2018 Andy Scherzinger <info@andy-scherzinger.de>
 * SPDX-FileCopyrightText: 2017 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2015 ownCloud Inc.
 * SPDX-FileCopyrightText: 2014 David A. Velasco <dvelasco@solidgear.es>
 * SPDX-License-Identifier: GPL-2.0-only AND (AGPL-3.0-or-later OR GPL-2.0-only)
 */
package com.owncloud.android.ui.notifications;

import android.app.NotificationManager;
import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Process;

import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.utils.theme.ViewThemeUtils;

import java.security.SecureRandom;

import androidx.core.app.NotificationCompat;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

public final class NotificationUtils {

    public static final String NOTIFICATION_CHANNEL_GENERAL = "NOTIFICATION_CHANNEL_GENERAL";
    public static final String NOTIFICATION_CHANNEL_DOWNLOAD = "NOTIFICATION_CHANNEL_DOWNLOAD";
    public static final String NOTIFICATION_CHANNEL_UPLOAD = "NOTIFICATION_CHANNEL_UPLOAD";
    public static final String NOTIFICATION_CHANNEL_MEDIA = "NOTIFICATION_CHANNEL_MEDIA";
    public static final String NOTIFICATION_CHANNEL_FILE_SYNC = "NOTIFICATION_CHANNEL_FILE_SYNC";
    public static final String NOTIFICATION_CHANNEL_FILE_OBSERVER = "NOTIFICATION_CHANNEL_FILE_OBSERVER";
    public static final String NOTIFICATION_CHANNEL_PUSH = "NOTIFICATION_CHANNEL_PUSH";
    public static final String NOTIFICATION_CHANNEL_BACKGROUND_OPERATIONS = "NOTIFICATION_CHANNEL_BACKGROUND_OPERATIONS";

    private NotificationUtils() {
        // utility class -> private constructor
    }

    /**
     * Factory method for {@link androidx.core.app.NotificationCompat.Builder} instances.
     *
     * Not strictly needed from the moment when the minimum API level supported by the app
     * was raised to 14 (Android 4.0).
     *
     * Formerly, returned a customized implementation of {@link androidx.core.app.NotificationCompat.Builder}
     * for Android API levels >= 8 and < 14.
     *
     * Kept in place for the extra abstraction level; notifications in the app need a review, and they
     * change a lot in different Android versions.
     *
     * @param context       Context that will use the builder to create notifications
     * @return An instance of the regular {@link NotificationCompat.Builder}.
     */
    public static NotificationCompat.Builder newNotificationBuilder(Context context, String channelId, final ViewThemeUtils viewThemeUtils) {
        final NotificationCompat.Builder builder = new NotificationCompat.Builder(context, channelId);
        viewThemeUtils.androidx.themeNotificationCompatBuilder(context, builder);
        return builder;
    }

    @SuppressFBWarnings("DMI")
    public static void cancelWithDelay(final NotificationManager notificationManager, final int notificationId,
                                       long delayInMillis) {

        HandlerThread thread = new HandlerThread(
            "NotificationDelayerThread_" + new SecureRandom().nextInt(), Process.THREAD_PRIORITY_BACKGROUND);
        thread.start();

        Handler handler = new Handler(thread.getLooper());
        handler.postDelayed(() -> {
            notificationManager.cancel(notificationId);
            ((HandlerThread) Thread.currentThread()).getLooper().quit();
        }, delayInMillis);
    }

    public static String createUploadNotificationTag(OCFile file){
        return createUploadNotificationTag(file.getRemotePath(), file.getStoragePath());
    }
    public static String createUploadNotificationTag(String remotePath, String localPath){
        return remotePath + localPath;
    }
}
