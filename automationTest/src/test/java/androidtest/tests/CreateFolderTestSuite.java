package androidtest.tests;

import io.appium.java_client.android.AndroidElement;

import org.junit.After;
import org.junit.Before;
import org.junit.runners.MethodSorters;
import org.junit.FixMethodOrder;
import org.junit.Test;


import androidtest.actions.Actions;
import androidtest.models.MainView;


@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class CreateFolderTestSuite extends CommonTest{

	@Before
	public void setUp() throws Exception {
		setUpCommonDriver();
	}


	@Test
	public void test6CreateNewFolder () throws Exception {
		String NEW_FOLDER_NAME = "testCreateFolder";

		MainView mainView = login(Config.URL, Config.user,Config.password);
		assertTrue(waitForTextPresent("ownCloud", mainView.getTitleTextElement()));
		mainView.clickOnNewFolderButton();
		mainView.typeNewFolderName(NEW_FOLDER_NAME);
		mainView.clickOnNewFolderOkButton();
		assertTrue(waitForTextPresent("Wait a moment" , mainView.getWaitAMomentTextElement()));
		while(mainView.getWaitAMomentTextElement().isDisplayed()){}
		AndroidElement newFolderElement = mainView.scrollTillFindElement(NEW_FOLDER_NAME);
		assertTrue(newFolderElement.isDisplayed());
		newFolderElement.tap(1, 1000);
		mainView.clickOnRemoveFileElement();
		mainView.clickOnRemoteAndLocalButton();
		assertTrue(waitForTextPresent("Wait a moment" , mainView.getWaitAMomentTextElement()));
		while(mainView.getWaitAMomentTextElement().isDisplayed()){}
		Actions.deleteAccount(mainView);
	}


	@After
	public void tearDown() throws Exception {
		driver.removeApp("com.owncloud.android");
		driver.quit();
	}

}

