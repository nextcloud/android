/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2021 Andy Scherzinger <info@andy-scherzinger.de>
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.nextcloud.client;

import android.widget.TextView;

import com.nextcloud.test.GrantStoragePermissionRule;
import com.owncloud.android.AbstractIT;
import com.owncloud.android.R;
import com.owncloud.android.authentication.AuthenticatorActivity;
import com.owncloud.android.utils.ScreenshotTest;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;

import androidx.test.espresso.intent.rule.IntentsTestRule;


public class AuthenticatorActivityIT extends AbstractIT {
    private static final String URL = "cloud.nextcloud.com";
    @Rule public IntentsTestRule<AuthenticatorActivity> activityRule = new IntentsTestRule<>(AuthenticatorActivity.class,
                                                                                             true,
                                                                                             false);

    @Rule
    public final TestRule permissionRule = GrantStoragePermissionRule.grant();

    @Test
    @ScreenshotTest
    public void login() {
        AuthenticatorActivity sut = activityRule.launchActivity(null);
        ((TextView) sut.findViewById(R.id.host_url_input)).setText(URL);
        sut.runOnUiThread(() -> sut.getAccountSetupBinding().hostUrlInput.clearFocus());
        screenshot(sut);
    }
}
