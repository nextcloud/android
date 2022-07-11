/*
 *
 * Nextcloud Android client application
 *
 * @author Tobias Kaminsky
 * Copyright (C) 2020 Tobias Kaminsky
 * Copyright (C) 2020 Nextcloud GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package com.owncloud.android.ui.activity;

import android.app.Activity;

import com.nextcloud.client.account.User;
import com.owncloud.android.AbstractIT;
import com.owncloud.android.lib.common.Quota;
import com.owncloud.android.lib.common.UserInfo;
import com.owncloud.android.utils.ScreenshotTest;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;

import androidx.test.espresso.intent.rule.IntentsTestRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;

@RunWith(AndroidJUnit4.class)
public class ManageAccountsActivityIT extends AbstractIT {
    @Rule
    public IntentsTestRule<ManageAccountsActivity> activityRule = new IntentsTestRule<>(ManageAccountsActivity.class,
                                                                                        true,
                                                                                        false);

    @Test
    @ScreenshotTest
    public void open() {
        Activity sut = activityRule.launchActivity(null);

        shortSleep();

        screenshot(sut);
    }

    @Test
    @ScreenshotTest
    public void userInfoDetail() {
        ManageAccountsActivity sut = activityRule.launchActivity(null);

        User user = sut.accountManager.getUser();

        UserInfo userInfo = new UserInfo("test",
                                         true,
                                         "Test User",
                                         "test@nextcloud.com",
                                         "+49 123 456",
                                         "Address 123, Berlin",
                                         "https://www.nextcloud.com",
                                         "https://twitter.com/Nextclouders",
                                         new Quota(),
                                         new ArrayList<>());

        sut.showUser(user, userInfo);

        shortSleep();
        shortSleep();

        screenshot(getCurrentActivity());
    }
}
