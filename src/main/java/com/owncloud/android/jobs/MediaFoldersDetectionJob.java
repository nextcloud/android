/*
 * Nextcloud Android client application
 *
 * @author Mario Danic
 * Copyright (C) 2018 Mario Danic
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
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.media.RingtoneManager;
import android.support.annotation.NonNull;
import android.support.v4.app.NotificationCompat;
import android.text.TextUtils;

import com.evernote.android.job.Job;
import com.google.gson.Gson;
import com.owncloud.android.R;
import com.owncloud.android.authentication.AccountUtils;
import com.owncloud.android.datamodel.ArbitraryDataProvider;
import com.owncloud.android.datamodel.MediaFolder;
import com.owncloud.android.datamodel.MediaFoldersModel;
import com.owncloud.android.datamodel.MediaProvider;
import com.owncloud.android.datamodel.SyncedFolderProvider;
import com.owncloud.android.ui.activity.ManageAccountsActivity;
import com.owncloud.android.ui.activity.SyncedFoldersActivity;
import com.owncloud.android.ui.notifications.NotificationUtils;
import com.owncloud.android.utils.ThemeUtils;

import java.util.ArrayList;
import java.util.List;

public class MediaFoldersDetectionJob extends Job {
    public static final String TAG = "MediaFoldersDetectionJob";

    public static final String KEY_MEDIA_FOLDER_PATH = "KEY_MEDIA_FOLDER_PATH";
    public static final String KEY_MEDIA_FOLDER_TYPE = "KEY_MEDIA_FOLDER_TYPE";

    @NonNull
    @Override
    protected Result onRunJob(@NonNull Params params) {
        Context context = getContext();
        ContentResolver contentResolver = context.getContentResolver();
        ArbitraryDataProvider arbitraryDataProvider = new ArbitraryDataProvider(contentResolver);
        SyncedFolderProvider syncedFolderProvider = new SyncedFolderProvider(contentResolver);
        Gson gson = new Gson();
        String arbitraryDataString;
        MediaFoldersModel mediaFoldersModel;

        List<MediaFolder> imageMediaFolders = MediaProvider.getImageFolders(contentResolver, 1,
                null, true);
        List<MediaFolder> videoMediaFolders = MediaProvider.getVideoFolders(contentResolver, 1, null,
                true);


        List<String> imageMediaFolderPaths = new ArrayList<>();
        List<String> videoMediaFolderPaths = new ArrayList<>();

        for (int i = 0; i < imageMediaFolders.size(); i++) {
            imageMediaFolderPaths.add(imageMediaFolders.get(i).absolutePath);
        }

        for (int i = 0; i < videoMediaFolders.size(); i++) {
            videoMediaFolderPaths.add(videoMediaFolders.get(i).absolutePath);
        }

        if (!TextUtils.isEmpty(arbitraryDataString = arbitraryDataProvider.getValue("global", "media_folders"))) {
            mediaFoldersModel = gson.fromJson(arbitraryDataString, MediaFoldersModel.class);

            // Store updated values
            arbitraryDataProvider.storeOrUpdateKeyValue("global", "media_folders", gson.toJson(new
                    MediaFoldersModel(imageMediaFolderPaths, videoMediaFolderPaths)));

            imageMediaFolderPaths.removeAll(mediaFoldersModel.getImageMediaFolders());
            videoMediaFolderPaths.removeAll(mediaFoldersModel.getVideoMediaFolders());

            if (imageMediaFolderPaths.size() > 0 || videoMediaFolderPaths.size() > 0) {
                Account[] accounts = AccountUtils.getAccounts(getContext());
                List<Account> accountList = new ArrayList<>();
                for (Account account : accounts) {
                    if (!arbitraryDataProvider.getBooleanValue(account, ManageAccountsActivity.PENDING_FOR_REMOVAL)) {
                        accountList.add(account);
                    }
                }


                for (Account account : accountList) {
                    for (int i = 0; i < imageMediaFolderPaths.size(); i++) {
                        if (syncedFolderProvider.findByLocalPathAndAccount(imageMediaFolderPaths.get(i),
                                account) == null) {
                            String imageMediaFolder = imageMediaFolderPaths.get(i);
                            sendNotification(String.format(context.getString(R.string.new_media_folder_detected),
                                    context.getString(R.string.new_media_folder_photos)),
                                    imageMediaFolder.substring(imageMediaFolder.lastIndexOf("/") + 1,
                                            imageMediaFolder.length()),
                                    account, imageMediaFolder, 1);
                        }
                    }

                    for (int i = 0; i < videoMediaFolderPaths.size(); i++) {
                        if (syncedFolderProvider.findByLocalPathAndAccount(videoMediaFolderPaths.get(i),
                                account) == null) {
                            String videoMediaFolder = videoMediaFolderPaths.get(i);
                            sendNotification(String.format(context.getString(R.string.new_media_folder_detected),
                                    context.getString(R.string.new_media_folder_videos)),
                                    videoMediaFolder.substring(videoMediaFolder.lastIndexOf("/") + 1,
                                            videoMediaFolder.length()),
                                    account, videoMediaFolder, 2);
                        }
                    }
                }
            }


        } else {
            mediaFoldersModel = new MediaFoldersModel(imageMediaFolderPaths, videoMediaFolderPaths);
            arbitraryDataProvider.storeOrUpdateKeyValue("global", "media_folders", gson.toJson(mediaFoldersModel));
        }

        return Result.SUCCESS;
    }

    private void sendNotification(String contentTitle, String subtitle,  Account account,
                                  String path, int type) {
        Context context = getContext();
        Intent intent = new Intent(getContext(), SyncedFoldersActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.putExtra(NotificationJob.KEY_NOTIFICATION_ACCOUNT, account.name);
        intent.putExtra(KEY_MEDIA_FOLDER_PATH, path);
        intent.putExtra(KEY_MEDIA_FOLDER_TYPE, type);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_ONE_SHOT);

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(context)
                .setSmallIcon(R.drawable.notification_icon)
                .setLargeIcon(BitmapFactory.decodeResource(context.getResources(), R.drawable.notification_icon))
                .setColor(ThemeUtils.primaryColor(getContext()))
                .setSubText(account.name)
                .setContentTitle(contentTitle)
                .setContentText(subtitle)
                .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);

        if ((android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O)) {
            notificationBuilder.setChannelId(NotificationUtils.NOTIFICATION_CHANNEL_GENERAL);
        }

        NotificationManager notificationManager = (NotificationManager)
                context.getSystemService(Context.NOTIFICATION_SERVICE);

        if (notificationManager != null) {
            notificationManager.notify(0, notificationBuilder.build());
        }
    }
}
