package androidtest.tests;


import io.appium.java_client.MobileBy;

import org.junit.After;
import org.junit.Before;
import org.junit.runners.MethodSorters;
import org.junit.FixMethodOrder;
import org.junit.Test;

import androidtest.actions.Actions;
import androidtest.models.AppDetailsView;
import androidtest.models.FilesView;
import androidtest.models.MainView;


@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class UploadTestSuite extends CommonTest{

	@Before
	public void setUp() throws Exception {
			setUpCommonDriver();
	}
	
	@Test
	public void test1UploadFile () throws Exception {
		String FILE_NAME = "test";
		
		MainView mainView = login(Config.URL, Config.user,Config.password);
		assertTrue(waitForTextPresent("ownCloud", mainView.getTitleTextElement()));
		mainView.clickOnUploadButton();
		FilesView filesView = mainView.clickOnFilesElementUploadFile();
		filesView.clickOnFileName(FILE_NAME);
		MainView mainViewAfterUploadFile = filesView.clickOnUploadButton();
		//TO DO. detect when the file is successfully uploaded
		Thread.sleep(3000);
		mainViewAfterUploadFile.scrollTillFindElement(FILE_NAME);
		assertTrue(mainViewAfterUploadFile.getFileElement().isDisplayed());
		mainViewAfterUploadFile.tapOnFileElement(FILE_NAME);
		mainViewAfterUploadFile.clickOnRemoveFileElement();
		mainViewAfterUploadFile.clickOnRemoteAndLocalButton();
		assertTrue(waitForTextPresent("Wait a moment" , mainViewAfterUploadFile.getWaitAMomentTextElement()));
		while(mainViewAfterUploadFile.getWaitAMomentTextElement().isDisplayed()){}
		Actions.deleteAccount(mainViewAfterUploadFile);
		
	}
	
	@Test
	public void test2KeepFileUpToDate () throws Exception {
		String FILE_NAME = "test";
		
		MainView mainView = login(Config.URL, Config.user,Config.password);
		assertTrue(waitForTextPresent("ownCloud", mainView.getTitleTextElement()));
		mainView.clickOnUploadButton();
		FilesView filesView = mainView.clickOnFilesElementUploadFile();
		filesView.clickOnFileName(FILE_NAME);
		MainView mainViewAfterUploadFile = filesView.clickOnUploadButton();
		//TO DO. detect when the file is successfully uploaded
		Thread.sleep(3000);
		mainViewAfterUploadFile.scrollTillFindElement(FILE_NAME);
		assertTrue(mainViewAfterUploadFile.getFileElement().isDisplayed());
		mainViewAfterUploadFile.tapOnFileElement(FILE_NAME);
		AppDetailsView appDetailsView = mainViewAfterUploadFile.clickOnDetailsFileElement();
		appDetailsView.checkKeepFileUpToDateCheckbox();
		//assertTrue(appDetailsView.getProgressBar().isDisplayed());
		Thread.sleep(3000);
		driver.sendKeyEvent(android.view.KeyEvent.KEYCODE_BACK);
		assertTrue(isElementPresent(mainViewAfterUploadFile.getFileElementLayout(), MobileBy.id("com.owncloud.android:id/imageView3")));
		mainViewAfterUploadFile.tapOnFileElement(FILE_NAME);
		mainViewAfterUploadFile.clickOnRemoveFileElement();
		mainViewAfterUploadFile.clickOnRemoteAndLocalButton();
		assertTrue(waitForTextPresent("Wait a moment" , mainViewAfterUploadFile.getWaitAMomentTextElement()));
		while(mainViewAfterUploadFile.getWaitAMomentTextElement().isDisplayed()){}
		Actions.deleteAccount(mainViewAfterUploadFile);
		
	}
	
	
	@After
	public void tearDown() throws Exception {
		driver.removeApp("com.owncloud.android");
		driver.quit();
	}
	

}

