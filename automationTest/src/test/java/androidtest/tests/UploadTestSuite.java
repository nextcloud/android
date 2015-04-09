package androidtest.tests;


import io.appium.java_client.MobileBy;

import org.junit.After;
import org.junit.Before;
import org.junit.runners.MethodSorters;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.support.ui.ExpectedConditions;

import androidtest.actions.Actions;
import androidtest.models.AppDetailsView;
import androidtest.models.ElementMenuOptions;
import androidtest.models.MainView;


@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class UploadTestSuite extends Common{

	String FILE_NAME = "test";

	@Before
	public void setUp() throws Exception {
		setUpCommonDriver();
	}

	@Test
	public void test1UploadFile () throws Exception {

		MainView mainView = Actions.login(Config.URL, Config.user,Config.password, Config.isTrusted, driver);
		assertTrue(waitForTextPresent("ownCloud", mainView.getTitleTextElement()));

		//check if the file already exists and if true, delete it
		Actions.deleteElement(FILE_NAME, mainView, driver);

		MainView mainViewAfterUploadFile = Actions.uploadFile(FILE_NAME, mainView);

		mainViewAfterUploadFile.scrollTillFindElement(FILE_NAME);
		assertTrue(mainViewAfterUploadFile.getFileElement().isDisplayed());
		waitTillElementIsNotPresent(mainViewAfterUploadFile.getProgressCircular(), 1000);
		wait.until(ExpectedConditions.visibilityOf(mainViewAfterUploadFile.getFileElementLayout().findElement(By.id(MainView.getLocalFileIndicator()))));


	}

	@Test
	public void test2KeepFileUpToDate () throws Exception {

		MainView mainView = Actions.login(Config.URL, Config.user,Config.password, Config.isTrusted, driver);
		assertTrue(waitForTextPresent("ownCloud", mainView.getTitleTextElement()));

		waitTillElementIsNotPresent(mainView.getProgressCircular(), 1000);

		MainView mainViewAfterUploadFile = Actions.uploadFile(FILE_NAME, mainView);
		mainViewAfterUploadFile.scrollTillFindElement(FILE_NAME);
		assertTrue(mainViewAfterUploadFile.getFileElement().isDisplayed());

		ElementMenuOptions menuOptions = mainViewAfterUploadFile.longPressOnElement(FILE_NAME);
		AppDetailsView appDetailsView = menuOptions.clickOnDetails();
		appDetailsView.checkKeepFileUpToDateCheckbox();
		Thread.sleep(3000);
		driver.sendKeyEvent(android.view.KeyEvent.KEYCODE_BACK);
		assertTrue(isElementPresent(mainViewAfterUploadFile.getFileElementLayout(), MobileBy.id(MainView.getFavoriteFileIndicator())));

	}


	@After
	public void tearDown() throws Exception {
		MainView mainView = new MainView(driver);
		Actions.deleteElement(FILE_NAME,mainView, driver);
		driver.removeApp("com.owncloud.android");
		driver.quit();
	}


}

