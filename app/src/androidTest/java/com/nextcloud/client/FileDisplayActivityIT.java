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

import com.owncloud.android.AbstractOnServerIT;
import com.owncloud.android.R;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.lib.resources.files.CreateFolderRemoteOperation;
import com.owncloud.android.lib.resources.files.ExistenceCheckRemoteOperation;
import com.owncloud.android.lib.resources.files.SearchRemoteOperation;
import com.owncloud.android.lib.resources.shares.CreateShareRemoteOperation;
import com.owncloud.android.lib.resources.shares.OCShare;
import com.owncloud.android.lib.resources.shares.ShareType;
import com.owncloud.android.operations.CreateFolderOperation;
import com.owncloud.android.ui.activity.FileDisplayActivity;
import com.owncloud.android.ui.events.SearchEvent;

import org.greenrobot.eventbus.EventBus;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;

import androidx.test.espresso.Espresso;
import androidx.test.espresso.contrib.DrawerActions;
import androidx.test.espresso.contrib.NavigationViewActions;
import androidx.test.espresso.intent.rule.IntentsTestRule;
import androidx.test.rule.GrantPermissionRule;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.platform.app.InstrumentationRegistry.getInstrumentation;
import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertTrue;

public class FileDisplayActivityIT extends AbstractOnServerIT {
    @Rule public IntentsTestRule<FileDisplayActivity> activityRule = new IntentsTestRule<>(FileDisplayActivity.class,
                                                                                           true,
                                                                                           false);

    @Rule
    public final GrantPermissionRule permissionRule = GrantPermissionRule.grant(
        Manifest.permission.WRITE_EXTERNAL_STORAGE);

    @Test
    // @ScreenshotTest // todo run without real server
    public void showShares() {
        assertTrue(new ExistenceCheckRemoteOperation("/shareToAdmin/", true).execute(client).isSuccess());
        assertTrue(new CreateFolderRemoteOperation("/shareToAdmin/", true).execute(client).isSuccess());
        assertTrue(new CreateFolderRemoteOperation("/shareToGroup/", true).execute(client).isSuccess());
        assertTrue(new CreateFolderRemoteOperation("/shareViaLink/", true).execute(client).isSuccess());
        assertTrue(new CreateFolderRemoteOperation("/noShare/", true).execute(client).isSuccess());
        // assertTrue(new CreateFolderRemoteOperation("/shareToCircle/", true).execute(client).isSuccess());

        // share folder to user "admin"
        assertTrue(new CreateShareRemoteOperation("/shareToAdmin/",
                                                  ShareType.USER,
                                                  "admin",
                                                  false,
                                                  "",
                                                  OCShare.MAXIMUM_PERMISSIONS_FOR_FOLDER)
                       .execute(client).isSuccess());

        // share folder via public link
        assertTrue(new CreateShareRemoteOperation("/shareViaLink/",
                                                  ShareType.PUBLIC_LINK,
                                                  "",
                                                  true,
                                                  "",
                                                  OCShare.READ_PERMISSION_FLAG)
                       .execute(client).isSuccess());

        // share folder to group
        Assert.assertTrue(new CreateShareRemoteOperation("/shareToGroup/",
                                                         ShareType.GROUP,
                                                         "users",
                                                         false,
                                                         "",
                                                         OCShare.NO_PERMISSION)
                              .execute(client).isSuccess());

        // share folder to circle
        // get share
//        RemoteOperationResult searchResult = new GetShareesRemoteOperation("publicCircle", 1, 50).execute(client);
//        assertTrue(searchResult.getLogMessage(), searchResult.isSuccess());
//
//        JSONObject resultJson = (JSONObject) searchResult.getData().get(0);
//        String circleId = resultJson.getJSONObject("value").getString("shareWith");
//
//        assertTrue(new CreateShareRemoteOperation("/shareToCircle/",
//                                                  ShareType.CIRCLE,
//                                                  circleId,
//                                                  false,
//                                                  "",
//                                                  OCShare.DEFAULT_PERMISSION)
//                       .execute(client).isSuccess());

        Activity sut = activityRule.launchActivity(null);

        getInstrumentation().waitForIdleSync();

        EventBus.getDefault().post(new SearchEvent("", SearchRemoteOperation.SearchType.SHARED_FILTER));

        shortSleep();
        shortSleep();

        screenshot(sut);
    }

    @Test
    public void allFiles() {
        FileDisplayActivity sut = activityRule.launchActivity(null);

        // given test folder
        assertTrue(new CreateFolderOperation("/test/", user, targetContext)
                       .execute(client, getStorageManager())
                       .isSuccess());

        // navigate into it
        OCFile test = getStorageManager().getFileByPath("/test/");
        sut.setFile(test);
        sut.startSyncFolderOperation(test, false);

        assertEquals(getStorageManager().getFileByPath("/test/"), sut.getCurrentDir());

        // open drawer
        onView(withId(R.id.drawer_layout)).perform(DrawerActions.open());

        // click "all files"
        onView(withId(R.id.nav_view))
            .perform(NavigationViewActions.navigateTo(R.id.nav_all_files));

        // then should be in root again
        shortSleep();
        assertEquals(getStorageManager().getFileByPath("/"), sut.getCurrentDir());
    }

    @Test
    public void switchToGridView() {
        activityRule.launchActivity(null);

        assertTrue(new CreateFolderOperation("/test/", user, targetContext)
                       .execute(client, getStorageManager())
                       .isSuccess());

        Espresso.onView(withId(R.id.switch_grid_view_button)).perform(click());
    }

    @Test
    public void openAccountSwitcher() {
        activityRule.launchActivity(null);

        Espresso.onView(withId(R.id.switch_account_button)).perform(click());
    }
}
