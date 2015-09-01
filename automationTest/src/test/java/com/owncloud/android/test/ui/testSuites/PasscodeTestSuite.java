/**
 *   ownCloud Android client application
 *
 *   @author purigarcia
 *   Copyright (C) 2015 ownCloud Inc.
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License version 2,
 *   as published by the Free Software Foundation.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package com.owncloud.android.test.ui.testSuites;

import io.appium.java_client.android.AndroidDriver;

import org.junit.After;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.TestName;
import org.junit.runners.MethodSorters;
import org.openqa.selenium.ScreenOrientation;

import com.owncloud.android.test.ui.actions.Actions;
import com.owncloud.android.test.ui.groups.NoIgnoreTestCategory;
import com.owncloud.android.test.ui.models.FileListView;
import com.owncloud.android.test.ui.models.MenuList;
import com.owncloud.android.test.ui.models.PassCodeRequestView;
import com.owncloud.android.test.ui.models.PassCodeView;
import com.owncloud.android.test.ui.models.SettingsView;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class PasscodeTestSuite {
	AndroidDriver driver;
	Common common;
	
	@Rule public TestName name = new TestName();
	
	@Before
	public void setUp() throws Exception {
		common=new Common();
		driver=common.setUpCommonDriver();
	}
	
	@Test
	@Category({NoIgnoreTestCategory.class})
	public void testPincodeEnable () throws Exception {
		driver.rotate(ScreenOrientation.PORTRAIT);
		FileListView fileListView = Actions.login(Config.URL, Config.user,
				Config.password, Config.isTrusted, driver);
		common.assertIsInFileListView();
		
		MenuList menu = fileListView.clickOnMenuButton();
		SettingsView settingsView = menu.clickOnSettingsButton();
		
		PassCodeView passCodeview = settingsView.EnablePassCode();
		PassCodeView passCodeview2 = passCodeview.enterPasscode(
				Config.passcode1, Config.passcode2, Config.passcode3, 
				Config.passcode4);
		passCodeview2.reenterPasscode(Config.passcode1, Config.passcode2, 
				Config.passcode3, Config.passcode4);
		
		driver.sendKeyEvent(android.view.KeyEvent.KEYCODE_HOME);
		//TO DO. Open the app instead of start an activity
		driver.startActivity("com.owncloud.android", 
				".ui.activity.FileDisplayActivity");
		//here we check that we are not in the fileDisplayActivity,
		//because pincode is asked
		common.assertIsNotInFileListView();
		common.assertIsPasscodeRequestView();
		
		PassCodeRequestView passCodeReequestView = new 
				PassCodeRequestView(driver);
		passCodeReequestView.enterPasscode(Config.passcode1, Config.passcode2,
				Config.passcode3, Config.passcode4);
		common.assertIsInFileListView();
	}
	
	
	@After
	public void tearDown() throws Exception {
		common.takeScreenShotOnFailed(name.getMethodName());
		driver.removeApp("com.owncloud.android");
		driver.quit();
	}

}
