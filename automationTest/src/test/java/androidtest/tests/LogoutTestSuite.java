package androidtest.tests;


import static org.junit.Assert.*;
import io.appium.java_client.android.AndroidDriver;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.TestName;

import androidtest.actions.Actions;
import androidtest.groups.NoIgnoreTestCategory;
import androidtest.groups.SmokeTestCategory;
import androidtest.models.LoginForm;
import androidtest.models.MainView;
import androidtest.models.MenuList;
import androidtest.models.SettingsView;

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
		MainView mainView = Actions.login(Config.URL, Config.user,Config.password, Config.isTrusted, driver);
		common.assertIsInMainView();
		MenuList menulist = mainView.clickOnMenuButton();
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
