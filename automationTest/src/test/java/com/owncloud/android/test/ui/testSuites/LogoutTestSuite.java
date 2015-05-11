package com.owncloud.android.test.ui.testSuites;


import static org.junit.Assert.*;
import io.appium.java_client.android.AndroidDriver;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.TestName;

import com.owncloud.android.test.ui.actions.Actions;
import com.owncloud.android.test.ui.groups.NoIgnoreTestCategory;
import com.owncloud.android.test.ui.groups.SmokeTestCategory;
import com.owncloud.android.test.ui.models.LoginForm;
import com.owncloud.android.test.ui.models.FileListView;
import com.owncloud.android.test.ui.models.MenuList;
import com.owncloud.android.test.ui.models.SettingsView;

public class LogoutTestSuite{
	
	AndroidDriver driver;
	Common common;

	@Rule public TestName name = new TestName();
	
	@Before
	public void setUp() throws Exception {
		common=new Common();
		driver=common.setUpCommonDriver();
	}

	@Test
	@Category({NoIgnoreTestCategory.class, SmokeTestCategory.class})
	public void testLogout () throws Exception {
		FileListView fileListView = Actions.login(Config.URL, Config.user,Config.password, Config.isTrusted, driver);
		common.assertIsInFileListView();
		MenuList menulist = fileListView.clickOnMenuButton();
		SettingsView settingsView = menulist.clickOnSettingsButton();
		settingsView.tapOnAccountElement(1, 1000);
		LoginForm loginForm = settingsView.clickOnDeleteAccountElement();
		assertEquals("Server address https://…", loginForm.gethostUrlInput().getText());
		assertEquals("Username", loginForm.getUserNameInput().getText());
		assertEquals("", loginForm.getPasswordInput().getText());
	}

	@After
	public void tearDown() throws Exception {
		common.takeScreenShotOnFailed(name.getMethodName());
		//driver.removeApp("com.owncloud.android");
		driver.quit();
	}
}
