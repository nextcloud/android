/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2021 Andy Scherzinger <info@andy-scherzinger.de>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.nextcloud.client;

import android.content.Intent;
import android.widget.TextView;

import com.nextcloud.test.GrantStoragePermissionRule;
import com.owncloud.android.AbstractIT;
import com.owncloud.android.R;
import com.owncloud.android.authentication.AuthenticatorActivity;
import com.owncloud.android.utils.ScreenshotTest;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;

public class AuthenticatorActivityIT extends AbstractIT {
    private static final String URL = "cloud.nextcloud.com";

    private ActivityScenario<AuthenticatorActivity> scenario;

    @Before
    public void setUp() {
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), AuthenticatorActivity.class);
        scenario = ActivityScenario.launch(intent);
    }

    @After
    public void tearDown() {
        scenario.close();
    }

    @Rule
    public final TestRule permissionRule = GrantStoragePermissionRule.grant();

    @Test
    @ScreenshotTest
    public void login() {
        scenario.onActivity(sut -> {
            ((TextView) sut.findViewById(R.id.host_url_input)).setText(URL);
            onIdleSync(() -> {
                sut.runOnUiThread(() -> sut.getAccountSetupBinding().hostUrlInput.clearFocus());
                screenshot(sut);
            });
        });
    }
}
