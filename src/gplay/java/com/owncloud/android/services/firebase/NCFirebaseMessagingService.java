/**
 * Nextcloud Android client application
 *
 * @author Mario Danic
 * Copyright (C) 2017 Mario Danic
 * Copyright (C) 2020 Chris Narkiewicz <hello@ezaquarii.com>
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
package com.owncloud.android.services.firebase;

import android.text.TextUtils;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.nextcloud.client.account.UserAccountManager;
import com.nextcloud.client.jobs.BackgroundJobManager;
import com.nextcloud.client.jobs.NotificationWork;
import com.nextcloud.client.preferences.AppPreferences;
import com.owncloud.android.R;
import com.owncloud.android.utils.PushUtils;

import java.util.Map;

import javax.inject.Inject;

import androidx.annotation.NonNull;
import dagger.android.AndroidInjection;

public class NCFirebaseMessagingService extends FirebaseMessagingService {
    @Inject AppPreferences preferences;
    @Inject UserAccountManager accountManager;
    @Inject BackgroundJobManager backgroundJobManager;

    @Override
    public void onCreate() {
        super.onCreate();
        AndroidInjection.inject(this);
    }

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        final Map<String, String> data = remoteMessage.getData();
        final String subject = data.get(NotificationWork.KEY_NOTIFICATION_SUBJECT);
        final String signature = data.get(NotificationWork.KEY_NOTIFICATION_SIGNATURE);
        if (subject != null && signature != null) {
            backgroundJobManager.startNotificationJob(subject, signature);
        }
    }

    @Override
    public void onNewToken(@NonNull String newToken) {
        super.onNewToken(newToken);

        if (!TextUtils.isEmpty(getResources().getString(R.string.push_server_url))) {
            preferences.setPushToken(newToken);
            PushUtils.pushRegistrationToServer(accountManager, preferences.getPushToken());
        }
    }
}
