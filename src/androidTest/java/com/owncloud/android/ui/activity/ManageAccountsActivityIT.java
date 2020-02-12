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

import android.app.Activity;

import com.facebook.testing.screenshot.Screenshot;
import com.nextcloud.client.account.User;
import com.owncloud.android.AbstractIT;

import org.junit.Rule;
import org.junit.Test;

import androidx.test.espresso.intent.rule.IntentsTestRule;

public class ManageAccountsActivityIT extends AbstractIT {
    @Rule
    public IntentsTestRule<ManageAccountsActivity> activityRule = new IntentsTestRule<>(ManageAccountsActivity.class,
                                                                                        true,
                                                                                        false);

    @Test
    public void open() throws InterruptedException {
        Activity sut = activityRule.launchActivity(null);

        Thread.sleep(2000);

        Screenshot.snapActivity(sut).record();
    }

    @Test
    public void userInfoDetail() throws InterruptedException {
        ManageAccountsActivity sut = activityRule.launchActivity(null);

        User user = sut.accountManager.getUser();
        sut.onClick(user);

        Thread.sleep(2000);

        Screenshot.snapActivity(getCurrentActivity()).record();
    }
}
