package androidtest.tests;

import static org.junit.Assert.*;
import io.appium.java_client.android.AndroidDriver;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.experimental.categories.Category;
import org.junit.rules.TestName;
import org.junit.runners.MethodSorters;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.openqa.selenium.ScreenOrientation;
import androidtest.actions.Actions;
import androidtest.groups.*;
import androidtest.models.LoginForm;
import androidtest.models.MainView;
import androidtest.models.MenuList;
import androidtest.models.SettingsView;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class LoginTestSuite{
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
	public void test1LoginPortrait () throws Exception {
		driver.rotate(ScreenOrientation.PORTRAIT);
		
		MainView mainView = Actions.login(Config.URL, Config.user,Config.password, Config.isTrusted, driver);
		common.assertIsInMainView();
	}
	
	@Test
	@Category({NoIgnoreTestCategory.class})
	public void test2LoginLandscape () throws Exception {
		driver.rotate(ScreenOrientation.LANDSCAPE);
		MainView mainView = Actions.login(Config.URL, Config.user,Config.password, Config.isTrusted, driver);
		common.assertIsInMainView();
	}
	
	
	@Test
	@Category({NoIgnoreTestCategory.class, SmokeTestCategory.class})
	public void test3MultiAccountRotate () throws Exception {
		driver.rotate(ScreenOrientation.LANDSCAPE);
		MainView mainView = Actions.login(Config.URL, Config.user,Config.password, Config.isTrusted, driver);
		common.assertIsInMainView();
		
		driver.rotate(ScreenOrientation.PORTRAIT);
		MenuList menu = mainView.clickOnMenuButton();
		SettingsView settingsView = menu.clickOnSettingsButton();
		
		settingsView.tapOnAddAccount(1, 1000);
		mainView = Actions.login(Config.URL2, Config.user2,Config.password2, Config.isTrusted2, driver);
		common.assertIsInSettingsView();
	}
	
	@Test
	@Category({NoIgnoreTestCategory.class})
	public void test4ExistingAccountRotate () throws Exception {
		driver.rotate(ScreenOrientation.PORTRAIT);
		MainView mainView = Actions.login(Config.URL, Config.user,Config.password, Config.isTrusted, driver);
		common.assertIsInMainView();
		
		driver.rotate(ScreenOrientation.LANDSCAPE);
		MenuList menu = mainView.clickOnMenuButton();
		SettingsView settingsView = menu.clickOnSettingsButton();
		settingsView.tapOnAddAccount(1, 1000);
		
		LoginForm loginForm = new LoginForm(driver);
		mainView = Actions.login(Config.URL, Config.user,Config.password, Config.isTrusted, driver);	
		assertTrue(common.waitForTextPresent("An account for the same user and server already exists in the device", loginForm.getAuthStatusText()));
	}
	
	@Test
	@Category({NoIgnoreTestCategory.class})
	public void test5ChangePasswordWrong () throws Exception {
		driver.rotate(ScreenOrientation.PORTRAIT);
		MainView mainView = Actions.login(Config.URL, Config.user,Config.password, Config.isTrusted, driver);
		common.assertIsInMainView();
		MenuList menu = mainView.clickOnMenuButton();
		SettingsView settingsView = menu.clickOnSettingsButton();
		settingsView.tapOnAccountElement(1, 1000);
		LoginForm changePasswordForm = settingsView.clickOnChangePasswordElement();
		changePasswordForm.typePassword("WrongPassword");
		changePasswordForm.clickOnConnectButton();
		assertTrue(common.waitForTextPresent("Wrong username or password", changePasswordForm.getAuthStatusText()));
	}
	

	@After
	public void tearDown() throws Exception {
		common.takeScreenShotOnFailed(name.getMethodName());
		driver.removeApp("com.owncloud.android");
		driver.quit();
	}
	
	
}
