/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2020 Chris Narkiewicz <hello@ezaquarii.com>
 * SPDX-FileCopyrightText: 2017 Mario Danic <mario@lovelyhq.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.services.firebase;

import android.content.Intent;
import android.text.TextUtils;

import com.google.firebase.messaging.Constants.MessageNotificationKeys;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.nextcloud.client.account.UserAccountManager;
import com.nextcloud.client.jobs.BackgroundJobManager;
import com.nextcloud.client.jobs.NotificationWork;
import com.nextcloud.client.preferences.AppPreferences;
import com.owncloud.android.R;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.utils.PushUtils;

import java.util.Map;

import javax.inject.Inject;

import androidx.annotation.NonNull;
import dagger.android.AndroidInjection;

public class NCFirebaseMessagingService extends FirebaseMessagingService {
    @Inject AppPreferences preferences;
    @Inject UserAccountManager accountManager;
    @Inject BackgroundJobManager backgroundJobManager;

    static final String TAG = "NCFirebaseMessagingService";

    // Firebase Messaging may apparently use two intent extras to specify a notification message.
    //
    // See the following fragments in https://github.com/firebase/firebase-android-sdk/blob/releases/m144_1.release/
    //  firebase-messaging/src/main/java/com/google/firebase/messaging/FirebaseMessagingService.java#L223
    //  firebase-messaging/src/main/java/com/google/firebase/messaging/NotificationParams.java#L419
    //  firebase-messaging/src/main/java/com/google/firebase/messaging/Constants.java#L158
    //
    // The "old" key is not exposed in com.google.firebase.messaging.Constants.MessageNotificationKeys,
    // so we need to define it ourselves.
    static final String ENABLE_NOTIFICATION_OLD = MessageNotificationKeys.NOTIFICATION_PREFIX_OLD + "e";
    static final String ENABLE_NOTIFICATION_NEW = MessageNotificationKeys.ENABLE_NOTIFICATION;

    @Override
    public void onCreate() {
        super.onCreate();
        AndroidInjection.inject(this);
    }

    @Override
    public void handleIntent(Intent intent) {
        Log_OC.d(TAG, "handleIntent - extras: " +
            ENABLE_NOTIFICATION_NEW + ": " + intent.getExtras().getString(ENABLE_NOTIFICATION_NEW) + ", " +
            ENABLE_NOTIFICATION_OLD + ": " + intent.getExtras().getString(ENABLE_NOTIFICATION_OLD));

        // When the app is in background and one of the ENABLE_NOTIFICATION or ENABLE_NOTIFICATION_OLD extras is set
        // to "1" in the intent sent from the FCM system code to the FirebaseMessagingService in the application,
        // the FCM library code that handles the intent DOES NOT invoke the onMessageReceived method.
        // It just displays the notification by itself.
        //
        // In our case the original FCM message contains dummy values "NEW_NOTIFICATION" and we need to get the
        // message in onMessageReceived to decrypt it.
        //
        // So we cheat here a little, by telling the FCM library that the notification flag is not set.
        //
        // Code below depends on implementation details of the firebase-messaging library (Firebase Android SDK).
        // https://github.com/firebase/firebase-android-sdk/tree/master/firebase-messaging

        intent.removeExtra(ENABLE_NOTIFICATION_OLD);
        intent.removeExtra(ENABLE_NOTIFICATION_NEW);
        intent.putExtra(ENABLE_NOTIFICATION_NEW, "0");

        super.handleIntent(intent);
    }

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        Log_OC.d(TAG, "onMessageReceived");
        final Map<String, String> data = remoteMessage.getData();
        final String subject = data.get(NotificationWork.KEY_NOTIFICATION_SUBJECT);
        final String signature = data.get(NotificationWork.KEY_NOTIFICATION_SIGNATURE);
        if (subject != null && signature != null) {
            backgroundJobManager.startNotificationJob(subject,
                                                      signature,
                                                      NotificationWork.BACKEND_TYPE_FIREBASE_CLOUD_MESSAGING);
        }
    }

    @Override
    public void onNewToken(@NonNull String newToken) {
        Log_OC.d(TAG, "onNewToken");
        super.onNewToken(newToken);

        if (!TextUtils.isEmpty(getResources().getString(R.string.push_server_url))) {
            preferences.setPushToken(newToken);
            PushUtils.updateRegistrationsWithServer(null, accountManager, preferences.getPushToken());
        }
    }
}
