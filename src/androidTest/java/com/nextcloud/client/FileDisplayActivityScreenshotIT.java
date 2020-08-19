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

import com.owncloud.android.AbstractIT;
import com.owncloud.android.R;
import com.owncloud.android.ui.activity.FileDisplayActivity;
import com.owncloud.android.utils.ScreenshotTest;

import org.junit.Rule;
import org.junit.Test;

import androidx.test.espresso.contrib.DrawerActions;
import androidx.test.espresso.intent.rule.IntentsTestRule;
import androidx.test.rule.GrantPermissionRule;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

public class FileDisplayActivityScreenshotIT extends AbstractIT {
    @Rule public IntentsTestRule<FileDisplayActivity> activityRule = new IntentsTestRule<>(FileDisplayActivity.class,
                                                                                           true,
                                                                                           false);

    @Rule
    public final GrantPermissionRule permissionRule = GrantPermissionRule.grant(
        Manifest.permission.WRITE_EXTERNAL_STORAGE);

    @Test
    @ScreenshotTest
    public void open() {
        FileDisplayActivity sut = activityRule.launchActivity(null);

        sut.getListOfFilesFragment().setFabEnabled(false);
        sut.getListOfFilesFragment().setEmptyListLoadingMessage(false);
        sut.getListOfFilesFragment().setLoading(false);
        waitForIdleSync();

        screenshot(sut);
    }

    @Test
    @ScreenshotTest
    public void drawer() {
        FileDisplayActivity sut = activityRule.launchActivity(null);

        onView(withId(R.id.drawer_layout)).perform(DrawerActions.open());

        sut.getListOfFilesFragment().setFabEnabled(false);
        sut.getListOfFilesFragment().setEmptyListLoadingMessage(false);
        sut.getListOfFilesFragment().setLoading(false);
        waitForIdleSync();

        screenshot(sut);
    }
}
