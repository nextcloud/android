/*
 *   ownCloud Android client application
 *
 *   Copyright (C) 2015 ownCloud Inc.
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License version 2,
 *   as published by the Free Software Foundation.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package com.owncloud.android.ui.notifications;

import android.app.NotificationManager;
import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Process;

import com.owncloud.android.utils.theme.ThemeColorUtils;

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
     * @return              An instance of the regular {@link NotificationCompat.Builder}.
     */
    public static NotificationCompat.Builder newNotificationBuilder(Context context) {
        return new NotificationCompat.Builder(context).setColor(ThemeColorUtils.primaryColor(context));
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
}
