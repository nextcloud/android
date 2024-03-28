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

package com.owncloud.android.ui.activity;

import android.content.Intent;

import com.nextcloud.client.account.User;
import com.owncloud.android.AbstractIT;
import com.owncloud.android.lib.common.Quota;
import com.owncloud.android.lib.common.UserInfo;
import com.owncloud.android.utils.ScreenshotTest;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;

public class ManageAccountsActivityIT extends AbstractIT {
    private ActivityScenario<ManageAccountsActivity> scenario;

    @Before
    public void setUp() {
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), ManageAccountsActivity.class);
        scenario = ActivityScenario.launch(intent);
    }

    @After
    public void tearDown() {
        scenario.close();
    }

    @Test
    @ScreenshotTest
    public void open() {
        scenario.onActivity(sut -> {
            onIdleSync(() -> {
                shortSleep();
                screenshot(sut);
            });
        });
    }

    @Test
    @ScreenshotTest
    public void userInfoDetail() {
        scenario.onActivity(sut -> {
            User user = sut.accountManager.getUser();

            UserInfo userInfo = new UserInfo("test",
                                             true,
                                             "Test User",
                                             "test@nextcloud.com",
                                             "+49 123 456",
                                             "Address 123, Berlin",
                                             "https://www.nextcloud.com",
                                             "https://twitter.com/Nextclouders",
                                             new Quota(),
                                             new ArrayList<>());

            onIdleSync(() -> {
                sut.showUser(user, userInfo);

                shortSleep();
                shortSleep();

                screenshot(getCurrentActivity());
            });
        });
    }
}
