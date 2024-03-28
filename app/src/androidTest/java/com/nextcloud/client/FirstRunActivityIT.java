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

import android.content.Intent;

import com.nextcloud.client.onboarding.FirstRunActivity;
import com.owncloud.android.AbstractIT;
import com.owncloud.android.utils.ScreenshotTest;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;


public class FirstRunActivityIT extends AbstractIT {
    private ActivityScenario<FirstRunActivity> scenario;

    @Before
    public void setUp() {
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), FirstRunActivity.class);
        scenario = ActivityScenario.launch(intent);
    }

    @After
    public void tearDown() {
        scenario.close();
    }

    @Test
    @ScreenshotTest
    public void open() {
        scenario.onActivity(sut -> onIdleSync(() -> screenshot(sut)));
    }

}
