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
import android.content.Intent;
import android.os.Looper;

import com.owncloud.android.AbstractIT;
import com.owncloud.android.datamodel.ArbitraryDataProvider;
import com.owncloud.android.ui.activity.RequestCredentialsActivity;
import com.owncloud.android.ui.activity.SettingsActivity;
import com.owncloud.android.utils.EncryptionUtils;
import com.owncloud.android.utils.ScreenshotTest;

import org.junit.Rule;
import org.junit.Test;

import androidx.test.espresso.intent.rule.IntentsTestRule;
import androidx.test.rule.GrantPermissionRule;

import static org.junit.Assert.assertTrue;


public class SettingsActivityIT extends AbstractIT {
    @Rule public IntentsTestRule<SettingsActivity> activityRule = new IntentsTestRule<>(SettingsActivity.class,
                                                                                        true,
                                                                                        false);

    @Rule
    public final GrantPermissionRule permissionRule = GrantPermissionRule.grant(
        Manifest.permission.WRITE_EXTERNAL_STORAGE);

    @Test
    @ScreenshotTest
    public void open() {
        Activity sut = activityRule.launchActivity(null);

        screenshot(sut);
    }

    @Test
    @ScreenshotTest
    public void showMnemonic_Error() {
        SettingsActivity sut = activityRule.launchActivity(null);
        sut.handleMnemonicRequest(null);
        shortSleep();
        waitForIdleSync();

        screenshot(sut);
    }

    @Test
    public void showMnemonic() {
        if (Looper.myLooper() == null) {
            Looper.prepare();
        }

        Intent intent = new Intent();
        intent.putExtra(RequestCredentialsActivity.KEY_CHECK_RESULT, RequestCredentialsActivity.KEY_CHECK_RESULT_TRUE);

        ArbitraryDataProvider arbitraryDataProvider = new ArbitraryDataProvider(targetContext.getContentResolver());
        arbitraryDataProvider.storeOrUpdateKeyValue(user.getAccountName(), EncryptionUtils.MNEMONIC, "Secret mnemonic");

        SettingsActivity sut = activityRule.launchActivity(null);
        sut.handleMnemonicRequest(intent);

        Looper.myLooper().quitSafely();
        assertTrue(true); // if we reach this, everything is ok
    }
}
