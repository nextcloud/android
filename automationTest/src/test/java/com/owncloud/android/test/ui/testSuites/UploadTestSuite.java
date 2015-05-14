package com.owncloud.android.test.ui.testSuites;


import static org.junit.Assert.*;
import io.appium.java_client.MobileBy;
import io.appium.java_client.android.AndroidDriver;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.experimental.categories.Category;
import org.junit.rules.TestName;
import org.junit.runners.MethodSorters;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.support.ui.ExpectedConditions;

import com.owncloud.android.test.ui.actions.Actions;
import com.owncloud.android.test.ui.groups.FailingTestCategory;
import com.owncloud.android.test.ui.groups.IgnoreTestCategory;
import com.owncloud.android.test.ui.groups.NoIgnoreTestCategory;
import com.owncloud.android.test.ui.groups.UnfinishedTestCategory;
import com.owncloud.android.test.ui.models.FileDetailsView;
import com.owncloud.android.test.ui.models.ElementMenuOptions;
import com.owncloud.android.test.ui.models.GmailEmailListView;
import com.owncloud.android.test.ui.models.GmailEmailView;
import com.owncloud.android.test.ui.models.ImageView;
import com.owncloud.android.test.ui.models.FileListView;
import com.owncloud.android.test.ui.models.UploadView;


@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@Category({NoIgnoreTestCategory.class})
public class UploadTestSuite{

	AndroidDriver driver;
	Common common;
	String FILE_NAME = Config.fileToTestName;
	String FILE_GMAIL_NAME = Config.fileToTestSendByEmailName;
	private Boolean fileHasBeenUploadedFromGmail = false;
	private Boolean fileHasBeenUploaded = false;

	@Rule public TestName name = new TestName();


	@Before
	public void setUp() throws Exception {
		common=new Common();
		driver=common.setUpCommonDriver();
	}

	@Test
	@Category(NoIgnoreTestCategory.class)
	public void testUploadFile () throws Exception {

		FileListView fileListView = Actions.login(Config.URL, Config.user,Config.password, Config.isTrusted, driver);
		common.assertIsInFileListView();

		//check if the file already exists and if true, delete it
		Actions.deleteElement(FILE_NAME, fileListView, driver);

		FileListView fileListViewAfterUploadFile = Actions.uploadFile(FILE_NAME, fileListView);

		fileListViewAfterUploadFile.scrollTillFindElement(FILE_NAME);
		assertTrue(fileListViewAfterUploadFile.getFileElement().isDisplayed());
		Common.waitTillElementIsNotPresent(fileListViewAfterUploadFile.getProgressCircular(), 1000);
		common.wait.until(ExpectedConditions.visibilityOf(fileListViewAfterUploadFile.getFileElementLayout().findElement(By.id(FileListView.getLocalFileIndicator()))));
		assertTrue(fileListViewAfterUploadFile.getFileElementLayout().findElement(By.id(FileListView.getLocalFileIndicator())).isDisplayed());
		fileListView = new FileListView(driver);
		fileListView.scrollTillFindElement(FILE_NAME);
		assertTrue(fileHasBeenUploaded = fileListView.getFileElement().isDisplayed());
	}

	@Test
	@Category(UnfinishedTestCategory.class)
	public void testUploadFromGmail () throws Exception {
		FileListView fileListView = Actions.login(Config.URL, Config.user,Config.password, Config.isTrusted, driver);
		driver.startActivity("com.google.android.gm", ".ConversationListActivityGmail");
		GmailEmailListView gmailEmailListView = new GmailEmailListView(driver);
		Thread.sleep(3000);
		GmailEmailView gmailEmailView = gmailEmailListView.clickOnEmail();
		ImageView imageView = gmailEmailView.clickOnfileButton();
		imageView.clickOnOptionsButton();
		imageView.clickOnShareButton();
		imageView.clickOnOwnCloudButton();
		//justonce button do not appear always
		try{
			imageView.clickOnJustOnceButton();
		}catch (NoSuchElementException e) {
		}
		UploadView uploadView = new UploadView(driver);
		uploadView.clickOUploadButton();
		driver.sendKeyEvent(android.view.KeyEvent.KEYCODE_HOME);
		driver.startActivity("com.owncloud.android", ".ui.activity.FileDisplayActivity");
		common.wait.until(ExpectedConditions.visibilityOfAllElementsLocatedBy(By.name(FILE_GMAIL_NAME)));
		assertEquals(Config.fileToTestSendByEmailName , driver.findElementByName(FILE_GMAIL_NAME).getText());
		fileListView = new FileListView(driver);
		fileListView.scrollTillFindElement(FILE_GMAIL_NAME);
		assertTrue(fileHasBeenUploadedFromGmail = fileListView.getFileElement().isDisplayed());
		//TODO. correct assert if fileListView is shown in grid mode
	}


	@Test	
	@Category({FailingTestCategory.class})
	public void testKeepFileUpToDate () throws Exception {

		FileListView fileListView = Actions.login(Config.URL, Config.user,Config.password, Config.isTrusted, driver);
		common.assertIsInFileListView();

		Common.waitTillElementIsNotPresent(fileListView.getProgressCircular(), 1000);

		FileListView fileListViewAfterUploadFile = Actions.uploadFile(FILE_NAME, fileListView);
		fileListViewAfterUploadFile.scrollTillFindElement(FILE_NAME);
		assertTrue(fileListViewAfterUploadFile.getFileElement().isDisplayed());

		ElementMenuOptions menuOptions = fileListViewAfterUploadFile.longPressOnElement(FILE_NAME);
		FileDetailsView fileDetailsView = menuOptions.clickOnDetails();
		fileDetailsView.checkKeepFileUpToDateCheckbox();
		Thread.sleep(3000);
		driver.sendKeyEvent(android.view.KeyEvent.KEYCODE_BACK);
		assertTrue(common.isElementPresent(fileListViewAfterUploadFile.getFileElementLayout(), MobileBy.id(FileListView.getFavoriteFileIndicator())));
		assertTrue(fileListViewAfterUploadFile.getFileElementLayout().findElement(By.id(FileListView.getFavoriteFileIndicator())).isDisplayed());
	}


	@After
	public void tearDown() throws Exception {
		common.takeScreenShotOnFailed(name.getMethodName());
		FileListView fileListView = new FileListView(driver);
		if (fileHasBeenUploadedFromGmail) {
			Actions.deleteElement(FILE_GMAIL_NAME,fileListView, driver);
		}
		if(fileHasBeenUploaded){
			Actions.deleteElement(FILE_NAME,fileListView, driver);
		}

		driver.removeApp("com.owncloud.android");
		driver.quit();
	}


}

