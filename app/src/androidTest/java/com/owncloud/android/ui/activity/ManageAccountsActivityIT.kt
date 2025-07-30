/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2020 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2020 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
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

import java.util.ArrayList;

import androidx.test.espresso.intent.rule.IntentsTestRule;

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
