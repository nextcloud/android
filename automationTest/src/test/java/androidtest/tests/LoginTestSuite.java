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
import androidtest.models.SettingsView;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class LoginTestSuite extends CommonTest{

	@Before
	public void setUp() throws Exception {
			setUpCommonDriver();
	}
	
	@Test
	public void test1LoginPortrait () throws Exception {
		
		System.out.println("Hello" + Config.server);
		String testName = "loginPortrait";
		driver.rotate(ScreenOrientation.PORTRAIT);

		MainView mainView = login(Config.URL, Config.user,Config.password);
		assertTrue(waitForTextPresent("ownCloud", mainView.getTitleTextElement()));
		//TO DO. detect in which view is. it can be files view or settings view
		/*if(mainView.getTitleTextElement().equals("ownCloud") || mainView.getTitleTextElement().equals("Settings")){
			if(mainView.getTitleTextElement().getText().equals("ownCloud")){
				assertTrue(waitForTextPresent("ownCloud", mainView.getTitleTextElement()));
			}else{
				assertTrue(waitForTextPresent("Settings", mainView.getTitleTextElement()));
			}
			fail(testName);
		}*/
		Actions.deleteAccount(mainView);
	}
	
	@Test
	public void test2LoginLandscape () throws Exception {
		
		String testName = "loginLandscape";
		driver.rotate(ScreenOrientation.LANDSCAPE);
		MainView mainView = login(Config.URL, Config.user,Config.password);
		assertTrue(waitForTextPresent("ownCloud", mainView.getTitleTextElement()));
		//TO DO. detect in which view is. it can be files view or settings view
		Actions.deleteAccount(mainView);
	}
	
	
	@Test
	public void test3MultiAccountRotate () throws Exception {
		
		String testName = "MultiAccountRotate";
		driver.rotate(ScreenOrientation.LANDSCAPE);
		MainView mainView = login(Config.URL, Config.user,Config.password);
		assertTrue(waitForTextPresent("ownCloud", mainView.getTitleTextElement()));
		
		driver.rotate(ScreenOrientation.PORTRAIT);
		mainView.clickOnMenuButton();
		SettingsView settingsView = mainView.clickOnSettingsButton();
		settingsView.tapOnAddAccount(1, 1000);
		mainView = login(Config.URL, Config.user2,Config.password2);
		
		assertTrue(waitForTextPresent("Settings", mainView.getTitleTextElement()));
		//TO DO. detect in which view is. it can be files view or settings view
		//Actions.deleteAccount(mainView);
		Actions.deleteAccount(settingsView);
		//TO DO. Delete the second user
	}
	
	@Test
	public void test4ExistingAccountRotate () throws Exception {
		
		String testName = "ExistingAccountRotate";
		driver.rotate(ScreenOrientation.PORTRAIT);
		MainView mainView = login(Config.URL, Config.user,Config.password);
		assertTrue(waitForTextPresent("ownCloud", mainView.getTitleTextElement()));
		
		driver.rotate(ScreenOrientation.LANDSCAPE);
		mainView.clickOnMenuButton();
		SettingsView settingsView = mainView.clickOnSettingsButton();
		settingsView.tapOnAddAccount(1, 1000);
		LoginForm loginForm = new LoginForm(driver);
		loginForm.typeHostUrl(Config.URL);
		loginForm.clickOnUserName();
		waitForTextPresent("Secure connection established", loginForm.getServerStatusTextElement());
		assertTrue(waitForTextPresent("Secure connection established", loginForm.getServerStatusTextElement()));	
		loginForm.typeUserName(Config.user);
		loginForm.typePassword(Config.password);
		mainView = loginForm.clickOnConnectButton();
		
		assertTrue(waitForTextPresent("An account for the same user and server already exists in the device", loginForm.getAuthStatusText()));
		driver.sendKeyEvent(android.view.KeyEvent.KEYCODE_BACK);
		Actions.deleteAccount(settingsView);
	}
	

	public void test5ChangePasswordWrong () throws Exception {

		MainView mainView = login(Config.URL, Config.user,Config.password);
		assertTrue(waitForTextPresent("ownCloud", mainView.getTitleTextElement()));
		mainView.clickOnMenuButton();
		SettingsView settingView = mainView.clickOnSettingsButton();
		settingView.tapOnAccountElement(1, 1000);
		LoginForm changePasswordForm = settingView.clickOnChangePasswordElement();
		changePasswordForm.typePassword("WrongPassword");
		changePasswordForm.clickOnConnectButton();
		assertTrue(waitForTextPresent("Wrong username or password", changePasswordForm.getAuthStatusText()));
		driver.sendKeyEvent(android.view.KeyEvent.KEYCODE_BACK);
		Actions.deleteAccount(settingView);
	}
	

	@After
	public void tearDown() throws Exception {
		driver.removeApp("com.owncloud.android");
		driver.quit();
	}
	
	
}
