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
