/*
 * Nextcloud Android client application
 *
 * @author Andy Scherzinger
 * Copyright (C) 2021 Andy Scherzinger
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

import com.owncloud.android.AbstractIT;
import com.owncloud.android.R;
import com.owncloud.android.authentication.AuthenticatorActivity;
import com.owncloud.android.utils.ScreenshotTest;

import org.junit.Rule;
import org.junit.Test;

import androidx.test.espresso.intent.rule.IntentsTestRule;
import androidx.test.rule.GrantPermissionRule;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.matcher.ViewMatchers.withId;


public class AuthenticatorActivityIT extends AbstractIT {
    private static final String URL = "cloud.nextcloud.com";
    @Rule public IntentsTestRule<AuthenticatorActivity> activityRule = new IntentsTestRule<>(AuthenticatorActivity.class,
                                                                                             true,
                                                                                             false);

    @Rule
    public final GrantPermissionRule permissionRule = GrantPermissionRule.grant(
        Manifest.permission.WRITE_EXTERNAL_STORAGE);

    @Test
    @ScreenshotTest
    public void login() {
        AuthenticatorActivity sut = activityRule.launchActivity(null);
        onView(withId(R.id.host_url_input)).perform(typeText(URL));
        sut.runOnUiThread(() -> sut.getAccountSetupBinding().hostUrlInput.clearFocus());
        screenshot(sut);
    }
}
