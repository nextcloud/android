/**
 * Nextcloud Android client application
 *
 * @author Mario Danic
 * Copyright (C) 2017 Mario Danic
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

import com.evernote.android.job.JobRequest;
import com.evernote.android.job.util.support.PersistableBundleCompat;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.owncloud.android.jobs.NotificationJob;

public class NCFirebaseMessagingService extends FirebaseMessagingService {

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        if (remoteMessage.getData() != null) {
            PersistableBundleCompat persistableBundleCompat = new PersistableBundleCompat();
            persistableBundleCompat.putString(NotificationJob.KEY_NOTIFICATION_SUBJECT, remoteMessage.getData().get
                    (NotificationJob.KEY_NOTIFICATION_SUBJECT));
            persistableBundleCompat.putString(NotificationJob.KEY_NOTIFICATION_SIGNATURE, remoteMessage.getData().get
                    (NotificationJob.KEY_NOTIFICATION_SIGNATURE));
            new JobRequest.Builder(NotificationJob.TAG)
                    .addExtras(persistableBundleCompat)
                    .setUpdateCurrent(false)
                    .startNow()
                    .build()
                    .schedule();
        }
    }
}
