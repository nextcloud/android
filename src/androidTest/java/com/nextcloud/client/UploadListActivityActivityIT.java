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
import com.owncloud.android.AbstractIT;
import com.owncloud.android.R;
import com.owncloud.android.ui.activity.UploadListActivity;

import org.junit.Rule;
import org.junit.Test;

import androidx.test.espresso.contrib.DrawerActions;
import androidx.test.espresso.intent.rule.IntentsTestRule;
import androidx.test.rule.GrantPermissionRule;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.matcher.ViewMatchers.withId;


public class UploadListActivityActivityIT extends AbstractIT {
    @Rule public IntentsTestRule<UploadListActivity> activityRule = new IntentsTestRule<>(UploadListActivity.class,
                                                                                          true,
                                                                                          false);


    @Rule
    public final GrantPermissionRule permissionRule = GrantPermissionRule.grant(
        Manifest.permission.WRITE_EXTERNAL_STORAGE);

    @Test
    public void openDrawer() throws InterruptedException {
        Activity sut = activityRule.launchActivity(null);

        Thread.sleep(3000);

        onView(withId(R.id.drawer_layout)).perform(DrawerActions.open());

        waitForIdleSync();

        Screenshot.snapActivity(sut).record();
    }
}
