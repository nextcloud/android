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

package com.owncloud.android.ui.dialog;

import android.Manifest;

import com.facebook.testing.screenshot.Screenshot;
import com.owncloud.android.AbstractIT;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.ui.activity.FileDisplayActivity;

import org.junit.Rule;
import org.junit.Test;

import androidx.test.espresso.intent.rule.IntentsTestRule;
import androidx.test.rule.GrantPermissionRule;

import static androidx.test.platform.app.InstrumentationRegistry.getInstrumentation;

public class SyncFileNotEnoughSpaceDialogFragmentTest extends AbstractIT {
    @Rule public IntentsTestRule<FileDisplayActivity> activityRule = new IntentsTestRule<>(FileDisplayActivity.class,
                                                                                           true,
                                                                                           false);

    @Rule
    public final GrantPermissionRule permissionRule = GrantPermissionRule.grant(
        Manifest.permission.WRITE_EXTERNAL_STORAGE);


    @Test
    public void showNotEnoughSpaceDialogForFolder() {
        FileDisplayActivity test = activityRule.launchActivity(null);
        OCFile ocFile = new OCFile("/Document/");
        ocFile.setFileLength(5000000);

        SyncFileNotEnoughSpaceDialogFragment dialog = SyncFileNotEnoughSpaceDialogFragment.newInstance(ocFile, 1000);
        dialog.show(test.getListOfFilesFragment().getFragmentManager(), "1");

        getInstrumentation().waitForIdleSync();

        Screenshot.snap(dialog.getDialog().getWindow().getDecorView()).record();
    }

    @Test
    public void showNotEnoughSpaceDialogForFile() {
        FileDisplayActivity test = activityRule.launchActivity(null);
        OCFile ocFile = new OCFile("/Video.mp4");
        ocFile.setFileLength(1000000);

        SyncFileNotEnoughSpaceDialogFragment dialog = SyncFileNotEnoughSpaceDialogFragment.newInstance(ocFile, 2000);
        dialog.show(test.getListOfFilesFragment().getFragmentManager(), "2");

        getInstrumentation().waitForIdleSync();

        Screenshot.snap(dialog.getDialog().getWindow().getDecorView()).record();
    }
}
