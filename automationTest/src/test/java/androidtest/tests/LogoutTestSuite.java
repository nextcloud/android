package androidtest.tests;


import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import androidtest.actions.Actions;
import androidtest.models.LoginForm;
import androidtest.models.MainView;
import androidtest.models.MenuList;
import androidtest.models.SettingsView;

public class LogoutTestSuite extends Common{

	@Before
	public void setUp() throws Exception {
		setUpCommonDriver();
	}

	@Test
	public void testLogout () throws Exception {
		MainView mainView = Actions.login(Config.URL, Config.user,Config.password, Config.isTrusted, driver);
		waitForTextPresent("ownCloud", mainView.getTitleTextElement());
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
		takeScreenShotOnFailed(getName());
		driver.removeApp("com.owncloud.android");
		driver.quit();
	}
}
