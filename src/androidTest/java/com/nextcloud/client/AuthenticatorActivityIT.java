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

package com.nextcloud.client;

import android.Manifest;
import android.app.Activity;

import com.facebook.testing.screenshot.Screenshot;
import com.owncloud.android.authentication.AuthenticatorActivity;

import org.junit.Rule;
import org.junit.Test;

import androidx.test.espresso.intent.rule.IntentsTestRule;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.GrantPermissionRule;


public class AuthenticatorActivityIT {
    @Rule public IntentsTestRule<AuthenticatorActivity> activityRule = new IntentsTestRule<>(AuthenticatorActivity.class,
                                                                                             true,
                                                                                             false);

    @Rule
    public final GrantPermissionRule permissionRule = GrantPermissionRule.grant(
        Manifest.permission.WRITE_EXTERNAL_STORAGE);

    @Test
    public void open() {
        Activity sut = activityRule.launchActivity(null);

        InstrumentationRegistry.getInstrumentation().waitForIdleSync();

        Screenshot.snapActivity(sut).record();
    }
}
