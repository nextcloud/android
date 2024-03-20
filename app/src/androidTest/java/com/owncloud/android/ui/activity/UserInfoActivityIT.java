/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2020 Andy Scherzinger <info@andy-scherzinger.de>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.owncloud.android.ui.activity;

import android.content.Intent;

import com.owncloud.android.AbstractIT;
import com.owncloud.android.lib.common.UserInfo;
import com.owncloud.android.utils.ScreenshotTest;

import org.junit.Test;

import androidx.test.core.app.ActivityScenario;

public class UserInfoActivityIT extends AbstractIT {
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
        intent.putExtra(UserInfoActivity.KEY_USER_DATA, userInfo);
        ActivityScenario<UserInfoActivity> sutScenario = ActivityScenario.launch(intent);


        sutScenario.onActivity(sut -> onIdleSync(() -> {
            shortSleep();
            shortSleep();
            screenshot(sut);
        }));
    }
}
