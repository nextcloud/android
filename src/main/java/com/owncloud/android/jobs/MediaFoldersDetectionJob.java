/*
 * Nextcloud Android client application
 *
 * @author Mario Danic
 * @author Andy Scherzinger
 * Copyright (C) 2018 Mario Danic
 * Copyright (C) 2018 Andy Scherzinger
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU AFFERO GENERAL PUBLIC LICENSE
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU AFFERO GENERAL PUBLIC LICENSE for more details.
 *
 * You should have received a copy of the GNU Affero General Public
 * License along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.owncloud.android.jobs;


import android.accounts.Account;
import android.app.Activity;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.media.RingtoneManager;
import android.text.TextUtils;

import com.evernote.android.job.Job;
import com.google.gson.Gson;
import com.nextcloud.client.preferences.AppPreferences;
import com.nextcloud.client.preferences.AppPreferencesImpl;
import com.owncloud.android.R;
import com.owncloud.android.authentication.AccountUtils;
import com.owncloud.android.datamodel.ArbitraryDataProvider;
import com.owncloud.android.datamodel.MediaFolder;
import com.owncloud.android.datamodel.MediaFoldersModel;
import com.owncloud.android.datamodel.MediaProvider;
import com.owncloud.android.datamodel.SyncedFolderProvider;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.ui.activity.ManageAccountsActivity;
import com.owncloud.android.ui.activity.SyncedFoldersActivity;
import com.owncloud.android.ui.notifications.NotificationUtils;
import com.owncloud.android.utils.ThemeUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

@SuppressFBWarnings(value = "PREDICTABLE_RANDOM", justification = "Only used for notification id.")
public class MediaFoldersDetectionJob extends Job {
    public static final String TAG = "MediaFoldersDetectionJob";

    public static final String KEY_MEDIA_FOLDER_PATH = "KEY_MEDIA_FOLDER_PATH";
    public static final String KEY_MEDIA_FOLDER_TYPE = "KEY_MEDIA_FOLDER_TYPE";

    private static final String ACCOUNT_NAME_GLOBAL = "global";
    private static final String KEY_MEDIA_FOLDERS = "media_folders";
    public static final String NOTIFICATION_ID = "NOTIFICATION_ID";

    private static final String DISABLE_DETECTION_CLICK = "DISABLE_DETECTION_CLICK";

    private Random randomId = new Random();

    @NonNull
    @Override
    protected Result onRunJob(@NonNull Params params) {
        Context context = getContext();
        ContentResolver contentResolver = context.getContentResolver();
        ArbitraryDataProvider arbitraryDataProvider = new ArbitraryDataProvider(contentResolver);
        SyncedFolderProvider syncedFolderProvider = new SyncedFolderProvider(contentResolver,
                                                                             AppPreferencesImpl.fromContext(context));
        Gson gson = new Gson();
        String arbitraryDataString;
        MediaFoldersModel mediaFoldersModel;

        List<MediaFolder> imageMediaFolders = MediaProvider.getImageFolders(contentResolver, 1, null, true);
        List<MediaFolder> videoMediaFolders = MediaProvider.getVideoFolders(contentResolver, 1, null, true);

        List<String> imageMediaFolderPaths = new ArrayList<>();
        List<String> videoMediaFolderPaths = new ArrayList<>();

        for (MediaFolder imageMediaFolder : imageMediaFolders) {
            imageMediaFolderPaths.add(imageMediaFolder.absolutePath);
        }

        for (MediaFolder videoMediaFolder : videoMediaFolders) {
            imageMediaFolderPaths.add(videoMediaFolder.absolutePath);
        }

        arbitraryDataString = arbitraryDataProvider.getValue(ACCOUNT_NAME_GLOBAL, KEY_MEDIA_FOLDERS);
        if (!TextUtils.isEmpty(arbitraryDataString)) {
            mediaFoldersModel = gson.fromJson(arbitraryDataString, MediaFoldersModel.class);

            // Store updated values
            arbitraryDataProvider.storeOrUpdateKeyValue(ACCOUNT_NAME_GLOBAL, KEY_MEDIA_FOLDERS, gson.toJson(new
                MediaFoldersModel(imageMediaFolderPaths, videoMediaFolderPaths)));

            final AppPreferences preferences = AppPreferencesImpl.fromContext(getContext());
            if (preferences.isShowMediaScanNotifications()) {
                imageMediaFolderPaths.removeAll(mediaFoldersModel.getImageMediaFolders());
                videoMediaFolderPaths.removeAll(mediaFoldersModel.getVideoMediaFolders());

                if (!imageMediaFolderPaths.isEmpty() || !videoMediaFolderPaths.isEmpty()) {
                    Account[] accounts = AccountUtils.getAccounts(getContext());
                    List<Account> accountList = new ArrayList<>();
                    for (Account account : accounts) {
                        if (!arbitraryDataProvider.getBooleanValue(account, ManageAccountsActivity.PENDING_FOR_REMOVAL)) {
                            accountList.add(account);
                        }
                    }

                    for (Account account : accountList) {
                        for (String imageMediaFolder : imageMediaFolderPaths) {
                            if (syncedFolderProvider.findByLocalPathAndAccount(imageMediaFolder, account) == null) {
                                sendNotification(String.format(context.getString(R.string.new_media_folder_detected),
                                    context.getString(R.string.new_media_folder_photos)),
                                    imageMediaFolder.substring(imageMediaFolder.lastIndexOf('/') + 1
                                    ),
                                    account, imageMediaFolder, 1);
                            }
                        }

                        for (String videoMediaFolder : videoMediaFolderPaths) {
                            if (syncedFolderProvider.findByLocalPathAndAccount(videoMediaFolder, account) == null) {
                                sendNotification(String.format(context.getString(R.string.new_media_folder_detected),
                                    context.getString(R.string.new_media_folder_videos)),
                                    videoMediaFolder.substring(videoMediaFolder.lastIndexOf('/') + 1
                                    ),
                                    account, videoMediaFolder, 2);
                            }
                        }
                    }
                }
            }

        } else {
            mediaFoldersModel = new MediaFoldersModel(imageMediaFolderPaths, videoMediaFolderPaths);
            arbitraryDataProvider.storeOrUpdateKeyValue(ACCOUNT_NAME_GLOBAL, KEY_MEDIA_FOLDERS,
                gson.toJson(mediaFoldersModel));
        }

        return Result.SUCCESS;
    }

    private void sendNotification(String contentTitle, String subtitle, Account account, String path, int type) {
        int notificationId = randomId.nextInt();

        Context context = getContext();
        Intent intent = new Intent(getContext(), SyncedFoldersActivity.class);
        intent.putExtra(NOTIFICATION_ID, notificationId);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.putExtra(NotificationJob.KEY_NOTIFICATION_ACCOUNT, account.name);
        intent.putExtra(KEY_MEDIA_FOLDER_PATH, path);
        intent.putExtra(KEY_MEDIA_FOLDER_TYPE, type);
        intent.putExtra(SyncedFoldersActivity.EXTRA_SHOW_SIDEBAR, true);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_ONE_SHOT);

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(
            context, NotificationUtils.NOTIFICATION_CHANNEL_GENERAL)
            .setSmallIcon(R.drawable.notification_icon)
            .setLargeIcon(BitmapFactory.decodeResource(context.getResources(), R.drawable.notification_icon))
            .setColor(ThemeUtils.primaryColor(getContext()))
            .setSubText(account.name)
            .setContentTitle(contentTitle)
            .setContentText(subtitle)
            .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
            .setAutoCancel(true)
            .setContentIntent(pendingIntent);

        Intent disableDetection = new Intent(context, NotificationReceiver.class);
        disableDetection.putExtra(NOTIFICATION_ID, notificationId);
        disableDetection.setAction(DISABLE_DETECTION_CLICK);

        PendingIntent disableIntent = PendingIntent.getBroadcast(
            context,
            notificationId,
            disableDetection,
            PendingIntent.FLAG_CANCEL_CURRENT
        );
        notificationBuilder.addAction(
            new NotificationCompat.Action(
                R.drawable.ic_close,
                context.getString(R.string.disable_new_media_folder_detection_notifications),
                disableIntent
            )
        );

        PendingIntent configureIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_CANCEL_CURRENT
        );
        notificationBuilder.addAction(
            new NotificationCompat.Action(
                R.drawable.ic_settings,
                context.getString(R.string.configure_new_media_folder_detection_notifications),
                configureIntent
            )
        );

        NotificationManager notificationManager = (NotificationManager)
            context.getSystemService(Context.NOTIFICATION_SERVICE);

        if (notificationManager != null) {
            notificationManager.notify(notificationId, notificationBuilder.build());
        }
    }


    public static class NotificationReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            int notificationId = intent.getIntExtra(NOTIFICATION_ID, 0);
            final AppPreferences preferences = AppPreferencesImpl.fromContext(context);

            if (DISABLE_DETECTION_CLICK.equals(action)) {
                Log_OC.d(this, "Disable media scan notifications");
                preferences.setShowMediaScanNotifications(false);
                cancel(context, notificationId);
            }
        }

        private void cancel(Context context, int notificationId) {
            NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(Activity.NOTIFICATION_SERVICE);
            notificationManager.cancel(notificationId);
        }
    }
}
