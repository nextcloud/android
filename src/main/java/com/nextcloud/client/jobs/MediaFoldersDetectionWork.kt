/*
 * Nextcloud Android client application
 *
 * @author Mario Danic
 * @author Andy Scherzinger
 * @author Chris Narkiewicz
 * Copyright (C) 2018 Mario Danic
 * Copyright (C) 2018 Andy Scherzinger
 * Copyright (C) 2020 Chris Narkiewicz <hello@ezaquarii.com>
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
package com.nextcloud.client.jobs

import android.app.Activity
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.graphics.BitmapFactory
import android.media.RingtoneManager
import android.text.TextUtils
import androidx.core.app.NotificationCompat
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.google.gson.Gson
import com.nextcloud.client.account.User
import com.nextcloud.client.account.UserAccountManager
import com.nextcloud.client.core.Clock
import com.nextcloud.client.preferences.AppPreferences
import com.nextcloud.client.preferences.AppPreferencesImpl
import com.owncloud.android.MainApp
import com.owncloud.android.R
import com.owncloud.android.datamodel.ArbitraryDataProvider
import com.owncloud.android.datamodel.MediaFolderType
import com.owncloud.android.datamodel.MediaFoldersModel
import com.owncloud.android.datamodel.MediaProvider
import com.owncloud.android.datamodel.SyncedFolderProvider
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.ui.activity.ManageAccountsActivity.PENDING_FOR_REMOVAL
import com.owncloud.android.ui.activity.SyncedFoldersActivity
import com.owncloud.android.ui.notifications.NotificationUtils
import com.owncloud.android.utils.SyncedFolderUtils
import com.owncloud.android.utils.theme.ThemeColorUtils
import java.util.ArrayList
import java.util.Random

@Suppress("LongParameterList") // dependencies injection
class MediaFoldersDetectionWork constructor(
    private val context: Context,
    params: WorkerParameters,
    private val resources: Resources,
    private val contentResolver: ContentResolver,
    private val userAccountManager: UserAccountManager,
    private val preferences: AppPreferences,
    private val clock: Clock
) : Worker(context, params) {

    companion object {
        const val TAG = "MediaFoldersDetectionJob"
        const val KEY_MEDIA_FOLDER_PATH = "KEY_MEDIA_FOLDER_PATH"
        const val KEY_MEDIA_FOLDER_TYPE = "KEY_MEDIA_FOLDER_TYPE"
        private const val ACCOUNT_NAME_GLOBAL = "global"
        private const val KEY_MEDIA_FOLDERS = "media_folders"
        const val NOTIFICATION_ID = "NOTIFICATION_ID"
        private val DISABLE_DETECTION_CLICK = MainApp.getAuthority() + "_DISABLE_DETECTION_CLICK"
    }

    private val randomIdGenerator = Random(clock.currentTime)

    @Suppress("LongMethod", "ComplexMethod", "NestedBlockDepth") // legacy code
    override fun doWork(): Result {
        val arbitraryDataProvider = ArbitraryDataProvider(contentResolver)
        val syncedFolderProvider = SyncedFolderProvider(contentResolver, preferences, clock)
        val gson = Gson()
        val mediaFoldersModel: MediaFoldersModel
        val imageMediaFolders = MediaProvider.getImageFolders(contentResolver, 1, null, true)
        val videoMediaFolders = MediaProvider.getVideoFolders(contentResolver, 1, null, true)
        val imageMediaFolderPaths: MutableList<String> = ArrayList()
        val videoMediaFolderPaths: MutableList<String> = ArrayList()
        for (imageMediaFolder in imageMediaFolders) {
            imageMediaFolderPaths.add(imageMediaFolder.absolutePath)
        }
        for (videoMediaFolder in videoMediaFolders) {
            imageMediaFolderPaths.add(videoMediaFolder.absolutePath)
        }
        val arbitraryDataString = arbitraryDataProvider.getValue(ACCOUNT_NAME_GLOBAL, KEY_MEDIA_FOLDERS)
        if (!TextUtils.isEmpty(arbitraryDataString)) {
            mediaFoldersModel = gson.fromJson(arbitraryDataString, MediaFoldersModel::class.java)
            // merge new detected paths with already notified ones
            for (existingImageFolderPath in mediaFoldersModel.imageMediaFolders) {
                if (!imageMediaFolderPaths.contains(existingImageFolderPath)) {
                    imageMediaFolderPaths.add(existingImageFolderPath)
                }
            }
            for (existingVideoFolderPath in mediaFoldersModel.videoMediaFolders) {
                if (!videoMediaFolderPaths.contains(existingVideoFolderPath)) {
                    videoMediaFolderPaths.add(existingVideoFolderPath)
                }
            }
            // Store updated values
            arbitraryDataProvider.storeOrUpdateKeyValue(
                ACCOUNT_NAME_GLOBAL,
                KEY_MEDIA_FOLDERS,
                gson.toJson(MediaFoldersModel(imageMediaFolderPaths, videoMediaFolderPaths))
            )
            if (preferences.isShowMediaScanNotifications) {
                imageMediaFolderPaths.removeAll(mediaFoldersModel.imageMediaFolders)
                videoMediaFolderPaths.removeAll(mediaFoldersModel.videoMediaFolders)
                if (imageMediaFolderPaths.isNotEmpty() || videoMediaFolderPaths.isNotEmpty()) {
                    val allUsers = userAccountManager.allUsers
                    val activeUsers: MutableList<User> = ArrayList()
                    for (user in allUsers) {
                        if (!arbitraryDataProvider.getBooleanValue(user, PENDING_FOR_REMOVAL)) {
                            activeUsers.add(user)
                        }
                    }
                    for (user in activeUsers) {
                        for (imageMediaFolder in imageMediaFolderPaths) {
                            val folder = syncedFolderProvider.findByLocalPathAndAccount(
                                imageMediaFolder,
                                user.toPlatformAccount()
                            )
                            if (folder == null &&
                                SyncedFolderUtils.isQualifyingMediaFolder(imageMediaFolder, MediaFolderType.IMAGE)
                            ) {
                                val contentTitle = String.format(
                                    resources.getString(R.string.new_media_folder_detected),
                                    resources.getString(R.string.new_media_folder_photos)
                                )
                                sendNotification(
                                    contentTitle,
                                    imageMediaFolder.substring(imageMediaFolder.lastIndexOf('/') + 1),
                                    user,
                                    imageMediaFolder,
                                    MediaFolderType.IMAGE.id
                                )
                            }
                        }
                        for (videoMediaFolder in videoMediaFolderPaths) {
                            val folder = syncedFolderProvider.findByLocalPathAndAccount(
                                videoMediaFolder,
                                user.toPlatformAccount()
                            )
                            if (folder == null) {
                                val contentTitle = String.format(
                                    context.getString(R.string.new_media_folder_detected),
                                    context.getString(R.string.new_media_folder_videos)
                                )
                                sendNotification(
                                    contentTitle,
                                    videoMediaFolder.substring(videoMediaFolder.lastIndexOf('/') + 1),
                                    user,
                                    videoMediaFolder,
                                    MediaFolderType.VIDEO.id
                                )
                            }
                        }
                    }
                }
            }
        } else {
            mediaFoldersModel = MediaFoldersModel(imageMediaFolderPaths, videoMediaFolderPaths)
            arbitraryDataProvider.storeOrUpdateKeyValue(
                ACCOUNT_NAME_GLOBAL,
                KEY_MEDIA_FOLDERS,
                gson.toJson(mediaFoldersModel)
            )
        }
        return Result.success()
    }

    private fun sendNotification(contentTitle: String, subtitle: String, user: User, path: String, type: Int) {
        val notificationId = randomIdGenerator.nextInt()
        val context = context
        val intent = Intent(context, SyncedFoldersActivity::class.java)
        intent.putExtra(NOTIFICATION_ID, notificationId)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        intent.putExtra(NotificationWork.KEY_NOTIFICATION_ACCOUNT, user.accountName)
        intent.putExtra(KEY_MEDIA_FOLDER_PATH, path)
        intent.putExtra(KEY_MEDIA_FOLDER_TYPE, type)
        val pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_ONE_SHOT)
        val notificationBuilder = NotificationCompat.Builder(
            context,
            NotificationUtils.NOTIFICATION_CHANNEL_GENERAL
        )
            .setSmallIcon(R.drawable.notification_icon)
            .setLargeIcon(BitmapFactory.decodeResource(context.resources, R.drawable.notification_icon))
            .setColor(ThemeColorUtils.primaryColor(context))
            .setSubText(user.accountName)
            .setContentTitle(contentTitle)
            .setContentText(subtitle)
            .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
        val disableDetection = Intent(context, NotificationReceiver::class.java)
        disableDetection.putExtra(NOTIFICATION_ID, notificationId)
        disableDetection.action = DISABLE_DETECTION_CLICK
        val disableIntent = PendingIntent.getBroadcast(
            context,
            notificationId,
            disableDetection,
            PendingIntent.FLAG_CANCEL_CURRENT
        )
        notificationBuilder.addAction(
            NotificationCompat.Action(
                R.drawable.ic_close,
                context.getString(R.string.disable_new_media_folder_detection_notifications),
                disableIntent
            )
        )
        val configureIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_CANCEL_CURRENT
        )
        notificationBuilder.addAction(
            NotificationCompat.Action(
                R.drawable.ic_settings,
                context.getString(R.string.configure_new_media_folder_detection_notifications),
                configureIntent
            )
        )
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(notificationId, notificationBuilder.build())
    }

    class NotificationReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            val notificationId = intent.getIntExtra(NOTIFICATION_ID, 0)
            val preferences = AppPreferencesImpl.fromContext(context)
            if (DISABLE_DETECTION_CLICK == action) {
                Log_OC.d(this, "Disable media scan notifications")
                preferences.isShowMediaScanNotifications = false
                cancel(context, notificationId)
            }
        }

        private fun cancel(context: Context, notificationId: Int) {
            val notificationManager = context.getSystemService(Activity.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.cancel(notificationId)
        }
    }
}
