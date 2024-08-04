/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2019 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2019 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.nextcloud.client;

import android.app.Activity;

import com.nextcloud.client.onboarding.FirstRunActivity;
import com.owncloud.android.AbstractIT;
import com.owncloud.android.utils.ScreenshotTest;

import org.junit.Rule;
import org.junit.Test;

import androidx.test.espresso.intent.rule.IntentsTestRule;

public class FirstRunActivityIT extends AbstractIT {
    @Rule public IntentsTestRule<FirstRunActivity> activityRule = new IntentsTestRule<>(FirstRunActivity.class,
                                                                                        true,
                                                                                        false);

    @Test
    @ScreenshotTest
    public void open() {
        Activity sut = activityRule.launchActivity(null);

        screenshot(sut);
    }

}
