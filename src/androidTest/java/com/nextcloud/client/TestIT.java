/*
 *
 * Nextcloud Android client application
 *
 * @author Tobias Kaminsky
 * Copyright (C) 2019 Tobias Kaminsky
 * Copyright (C) 2019 Nextcloud GmbH
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
import com.nextcloud.client.onboarding.FirstRunActivity;
import com.owncloud.android.AbstractIT;
import com.owncloud.android.ui.activity.FileDisplayActivity;

import org.junit.Rule;
import org.junit.Test;

import androidx.test.core.app.ActivityScenario;
import androidx.test.espresso.Espresso;
import androidx.test.espresso.NoActivityResumedException;
import androidx.test.espresso.intent.rule.IntentsTestRule;
import androidx.test.rule.GrantPermissionRule;


public class TestIT extends AbstractIT {
    @Rule public IntentsTestRule<FirstRunActivity> activityRule =
        new IntentsTestRule<>(FirstRunActivity.class, true, false);

    @Rule
    public final GrantPermissionRule permissionRule = GrantPermissionRule.grant(
        Manifest.permission.WRITE_EXTERNAL_STORAGE);

    @Test
    public void test() throws InterruptedException {
        Activity test = activityRule.launchActivity(null);

        Screenshot.snapActivity(test).record();
    }

    private void openOverflowMenu() throws InterruptedException {
        try {
            Espresso.openContextualActionModeOverflowMenu();
        } catch (NoActivityResumedException e) {
            ActivityScenario.launch(FileDisplayActivity.class);
            Thread.sleep(1000);
            Espresso.openContextualActionModeOverflowMenu();
        }
    }

}
