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

package com.owncloud.android.ui.preview;

import android.Manifest;

import com.owncloud.android.AbstractIT;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.ui.activity.FileDisplayActivity;
import com.owncloud.android.utils.MimeTypeUtil;

import org.junit.Rule;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

import androidx.test.espresso.intent.rule.IntentsTestRule;
import androidx.test.rule.GrantPermissionRule;

public class PreviewTextFileFragmentTest extends AbstractIT {
    @Rule public IntentsTestRule<FileDisplayActivity> activityRule = new IntentsTestRule<>(FileDisplayActivity.class,
                                                                                           true,
                                                                                           false);

    @Rule
    public final GrantPermissionRule permissionRule = GrantPermissionRule.grant(
        Manifest.permission.WRITE_EXTERNAL_STORAGE);

    @Test
    // @ScreenshotTest // todo run without real server
    public void displaySimpleTextFile() throws IOException {
        FileDisplayActivity sut = activityRule.launchActivity(null);

        shortSleep();

        File file = getDummyFile("nonEmpty.txt");
        OCFile test = new OCFile("/text.md");
        test.setMimeType(MimeTypeUtil.MIMETYPE_TEXT_MARKDOWN);
        test.setStoragePath(file.getAbsolutePath());
        sut.startTextPreview(test, false);

        shortSleep();

        screenshot(sut);
    }

    @Test
    // @ScreenshotTest // todo run without real server
    public void displayJavaSnippetFile() throws IOException {
        FileDisplayActivity sut = activityRule.launchActivity(null);

        shortSleep();

        File file = getFile("java.md");
        OCFile test = new OCFile("/java.md");
        test.setMimeType(MimeTypeUtil.MIMETYPE_TEXT_MARKDOWN);
        test.setStoragePath(file.getAbsolutePath());
        sut.startTextPreview(test, false);

        shortSleep();

        screenshot(sut);
    }
}
