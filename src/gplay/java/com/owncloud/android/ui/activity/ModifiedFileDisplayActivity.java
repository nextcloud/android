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

package com.owncloud.android.ui.activity;

import android.os.Bundle;

import com.owncloud.android.ui.events.TokenPushEvent;
import com.owncloud.android.utils.PushUtils;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

public class ModifiedFileDisplayActivity extends FileDisplayActivity {

    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    public void onMessageEvent(TokenPushEvent event) {
        PushUtils.pushRegistrationToServer();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // see if there's stuff to push every time we start the app
        EventBus.getDefault().post(new TokenPushEvent());
    }

}
