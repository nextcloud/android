/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2020 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2020 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.ui.preview;

import com.owncloud.android.AbstractIT;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.ui.activity.FileDisplayActivity;
import com.owncloud.android.utils.MimeTypeUtil;
import com.owncloud.android.utils.ScreenshotTest;

import org.junit.Rule;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

import androidx.test.espresso.intent.rule.IntentsTestRule;

public class PreviewTextFileFragmentTest extends AbstractIT {
    @Rule public IntentsTestRule<FileDisplayActivity> activityRule = new IntentsTestRule<>(FileDisplayActivity.class,
                                                                                           true,
                                                                                           false);

    @Test
    @ScreenshotTest
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
    @ScreenshotTest
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
