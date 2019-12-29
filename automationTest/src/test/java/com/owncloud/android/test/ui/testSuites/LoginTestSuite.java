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
import io.appium.java_client.android.AndroidElement;
import io.appium.java_client.android.AndroidStartScreenRecordingOptions;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
<<<<<<< HEAD
import org.junit.jupiter.api.TestInfo;
=======
>>>>>>>   implement login testcase1
import org.openqa.selenium.ScreenOrientation;

import java.time.Duration;

import com.owncloud.android.test.ui.actions.Actions;

public class LoginTestSuite{
	AndroidDriver<AndroidElement> driver;
	Common common;
	
	
	@BeforeEach
	public void setUp() throws Exception {
		common=new Common();
		driver=common.setUpCommonDriver();
		driver.startRecordingScreen(
			new AndroidStartScreenRecordingOptions().withTimeLimit(Duration.ofSeconds(200)));
	}
	
	@Test
	public void test1LoginPortrait () throws Exception {
		driver.rotate(ScreenOrientation.PORTRAIT);
		
		Actions.login(Config.URL, Config.user,
			Config.password, Config.isTrusted, driver);
		
		common.assertIsInFileListView();
	}

	@AfterEach
	public void tearDown(TestInfo testInfo) throws Exception {
		common.takeScreenShotOnFailed(testInfo.getTestMethod().toString());
		driver.removeApp("com.nextcloud.android.qa");
		driver.quit();
	}
	
	
}
