package androidtest.tests;

import org.junit.After;
import org.junit.Before;
import org.junit.runners.MethodSorters;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.openqa.selenium.ScreenOrientation;
import androidtest.actions.Actions;
import androidtest.models.LoginForm;
import androidtest.models.MainView;
import androidtest.models.MenuList;
import androidtest.models.SettingsView;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class LoginTestSuite extends Common{

	@Before
	public void setUp() throws Exception {
			setUpCommonDriver();
	}
	
	@Test
	public void test1LoginPortrait () throws Exception {
		driver.rotate(ScreenOrientation.PORTRAIT);
		
		MainView mainView = Actions.login(Config.URL, Config.user,Config.password, Config.isTrusted, driver);
		assertTrue(waitForTextPresent("ownCloud", mainView.getTitleTextElement()));
	}
	
	@Test
	public void test2LoginLandscape () throws Exception {
		driver.rotate(ScreenOrientation.LANDSCAPE);
		MainView mainView = Actions.login(Config.URL, Config.user,Config.password, Config.isTrusted, driver);
		assertTrue(waitForTextPresent("ownCloud", mainView.getTitleTextElement()));
		//TO DO. detect in which view is. it can be files view or settings view
	}
	
	
	@Test
	public void test3MultiAccountRotate () throws Exception {
		driver.rotate(ScreenOrientation.LANDSCAPE);
		MainView mainView = Actions.login(Config.URL, Config.user,Config.password, Config.isTrusted, driver);
		assertTrue(waitForTextPresent("ownCloud", mainView.getTitleTextElement()));
		
		driver.rotate(ScreenOrientation.PORTRAIT);
		MenuList menu = mainView.clickOnMenuButton();
		SettingsView settingsView = menu.clickOnSettingsButton();
		settingsView.tapOnAddAccount(1, 1000);
		mainView = Actions.login(Config.URL2, Config.user2,Config.password2, Config.isTrusted2, driver);
		
		assertTrue(waitForTextPresent("Settings", mainView.getTitleTextElement()));
		//TO DO. detect in which view is. it can be files view or settings view
		//Actions.deleteAccount(mainView);
		//TO DO. Delete the second user
	}
	
	@Test
	public void test4ExistingAccountRotate () throws Exception {
		driver.rotate(ScreenOrientation.PORTRAIT);
		MainView mainView = Actions.login(Config.URL, Config.user,Config.password, Config.isTrusted, driver);
		assertTrue(waitForTextPresent("ownCloud", mainView.getTitleTextElement()));
		
		driver.rotate(ScreenOrientation.LANDSCAPE);
		MenuList menu = mainView.clickOnMenuButton();
		SettingsView settingsView = menu.clickOnSettingsButton();
		settingsView.tapOnAddAccount(1, 1000);
		
		LoginForm loginForm = new LoginForm(driver);
		mainView = Actions.login(Config.URL, Config.user,Config.password, Config.isTrusted, driver);	
		assertTrue(waitForTextPresent("An account for the same user and server already exists in the device", loginForm.getAuthStatusText()));
	}
	

	public void test5ChangePasswordWrong () throws Exception {

		MainView mainView = Actions.login(Config.URL, Config.user,Config.password, Config.isTrusted, driver);
		assertTrue(waitForTextPresent("ownCloud", mainView.getTitleTextElement()));
		MenuList menu = mainView.clickOnMenuButton();
		SettingsView settingsView = menu.clickOnSettingsButton();
		settingsView.tapOnAccountElement(1, 1000);
		LoginForm changePasswordForm = settingsView.clickOnChangePasswordElement();
		changePasswordForm.typePassword("WrongPassword");
		changePasswordForm.clickOnConnectButton();
		assertTrue(waitForTextPresent("Wrong username or password", changePasswordForm.getAuthStatusText()));
	}
	

	@After
	public void tearDown() throws Exception {
		takeScreenShotOnFailed(getName());
		driver.removeApp("com.owncloud.android");
		driver.quit();
	}
	
	
}
