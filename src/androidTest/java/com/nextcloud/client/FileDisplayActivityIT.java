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
import com.owncloud.android.AbstractIT;
import com.owncloud.android.R;
import com.owncloud.android.ui.activity.FileDisplayActivity;

import org.junit.Rule;
import org.junit.Test;

import androidx.test.espresso.contrib.DrawerActions;
import androidx.test.espresso.intent.rule.IntentsTestRule;
import androidx.test.rule.GrantPermissionRule;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.matcher.ViewMatchers.withId;


public class FileDisplayActivityIT extends AbstractIT {
    @Rule public IntentsTestRule<FileDisplayActivity> activityRule = new IntentsTestRule<>(FileDisplayActivity.class,
                                                                                           true,
                                                                                           false);

    @Rule
    public final GrantPermissionRule permissionRule = GrantPermissionRule.grant(
        Manifest.permission.WRITE_EXTERNAL_STORAGE);

    @Test
    public void open() throws InterruptedException {
        Activity sut = activityRule.launchActivity(null);

        Thread.sleep(3000);

        Screenshot.snapActivity(sut).record();
    }

    @Test
    public void drawer() {
        Activity sut = activityRule.launchActivity(null);

        onView(withId(R.id.drawer_layout)).perform(DrawerActions.open());

        Screenshot.snapActivity(sut).record();
    }

    @Test
    public void showAccounts() {
        Activity sut = activityRule.launchActivity(null);

        onView(withId(R.id.drawer_layout)).perform(DrawerActions.open());
        onView(withId(R.id.drawer_active_user)).perform(click());

        Screenshot.snapActivity(sut).record();
    }
}
