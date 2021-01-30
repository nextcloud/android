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

import android.text.TextUtils;

import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.FirebaseInstanceIdService;
import com.nextcloud.client.account.UserAccountManager;
import com.nextcloud.client.preferences.AppPreferences;
import com.owncloud.android.R;
import com.owncloud.android.utils.PushUtils;

import javax.inject.Inject;

import dagger.android.AndroidInjection;

public class NCFirebaseInstanceIDService extends FirebaseInstanceIdService {


    @Inject AppPreferences preferences;
    @Inject UserAccountManager accountManager;

    @Override
    public void onCreate() {
        super.onCreate();
        AndroidInjection.inject(this);
    }

    @Override
    public void onTokenRefresh() {
        //You can implement this method to store the token on your server
        if (!TextUtils.isEmpty(getResources().getString(R.string.push_server_url))) {
            preferences.setPushToken(FirebaseInstanceId.getInstance().getToken());
            PushUtils.pushRegistrationToServer(accountManager, preferences.getPushToken());
        }
    }
}
