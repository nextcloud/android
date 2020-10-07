/*
 * Nextcloud Android client application
 *
 * @author Andy Scherzinger
 * Copyright (C) 2020 Andy Scherzinger
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

import android.content.Intent;

import com.owncloud.android.AbstractIT;
import com.owncloud.android.lib.common.UserInfo;
import com.owncloud.android.utils.ScreenshotTest;

import org.junit.Rule;
import org.junit.Test;
import org.parceler.Parcels;

import androidx.test.espresso.intent.rule.IntentsTestRule;

public class UserInfoActivityIT extends AbstractIT {
    @Rule
    public IntentsTestRule<UserInfoActivity> activityRule = new IntentsTestRule<>(UserInfoActivity.class,
                                                                                  true,
                                                                                  false);

    @Test
    @ScreenshotTest
    public void fullUserInfoDetail() {
        final Intent intent = new Intent(targetContext, UserInfoActivity.class);
        intent.putExtra(UserInfoActivity.KEY_ACCOUNT, user);
        UserInfo userInfo = new UserInfo("test",
                                         true,
                                         "Firstname Familyname",
                                         "oss@rocks.com",
                                         "+49 7613 672 255",
                                         "Awesome Place Av.",
                                         "https://www.nextcloud.com",
                                         "nextclouders",
                                         null,
                                         null
        );
        intent.putExtra(UserInfoActivity.KEY_USER_DATA, Parcels.wrap(userInfo));
        UserInfoActivity sut = activityRule.launchActivity(intent);

        shortSleep();
        shortSleep();

        screenshot(sut);
    }
}
