/**
 *   ownCloud Android client application
 *
 *   @author purigarcia
 *   Copyright (C) 2015 ownCloud Inc.
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License version 2,
 *   as published by the Free Software Foundation.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

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
import com.owncloud.android.test.ui.groups.InProgressCategory;
import com.owncloud.android.test.ui.groups.NoIgnoreTestCategory;
import com.owncloud.android.test.ui.groups.SmokeTestCategory;
import com.owncloud.android.test.ui.groups.UnfinishedTestCategory;
import com.owncloud.android.test.ui.models.FileDetailsView;
import com.owncloud.android.test.ui.models.ElementMenuOptions;
import com.owncloud.android.test.ui.models.GmailEmailListView;
import com.owncloud.android.test.ui.models.GmailEmailView;
import com.owncloud.android.test.ui.models.ImageView;
import com.owncloud.android.test.ui.models.FileListView;
import com.owncloud.android.test.ui.models.NotificationView;
import com.owncloud.android.test.ui.models.SettingsView;
import com.owncloud.android.test.ui.models.UploadView;


@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@Category({NoIgnoreTestCategory.class})
public class UploadTestSuite{

	AndroidDriver driver;
	Common common;
	String FILE_NAME = Config.fileToTestName;
	String BIG_FILE_NAME = Config.bigFileToTestName;
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
	@Category({NoIgnoreTestCategory.class, SmokeTestCategory.class})
	public void testUploadFile () throws Exception {

		FileListView fileListView = Actions.login(Config.URL, Config.user,
				Config.password, Config.isTrusted, driver);
		common.assertIsInFileListView();

		//check if the file already exists and if true, delete it
		Actions.deleteElement(FILE_NAME, fileListView, driver);

		FileListView fileListViewAfterUploadFile = Actions
				.uploadFile(FILE_NAME, fileListView);

		fileListViewAfterUploadFile.scrollTillFindElement(FILE_NAME);
		assertTrue(fileListViewAfterUploadFile.getFileElement().isDisplayed());
		Common.waitTillElementIsNotPresentWithoutTimeout(
				fileListViewAfterUploadFile.getProgressCircular(), 1000);
		common.wait.until(ExpectedConditions.visibilityOf(
				fileListViewAfterUploadFile.getFileElementLayout()
				.findElement(By.id(FileListView.getLocalFileIndicator()))));
		assertTrue(fileListViewAfterUploadFile.getFileElementLayout()
				.findElement(By.id(FileListView.getLocalFileIndicator()))
				.isDisplayed());
		fileListView = new FileListView(driver);
		fileListView.scrollTillFindElement(FILE_NAME);
		assertTrue(
				fileHasBeenUploaded = fileListView.getFileElement().isDisplayed());
	}

	
	
	@Test
	@Category({UnfinishedTestCategory.class})
	public void testUploadBigFile () throws Exception {

		FileListView fileListView = Actions.login(Config.URL, Config.user,
				Config.password, Config.isTrusted, driver);
		common.assertIsInFileListView();

		//check if the file already exists and if true, delete it
		Actions.deleteElement(BIG_FILE_NAME, fileListView, driver);

		FileListView fileListViewAfterUploadFile = Actions
				.uploadFile(BIG_FILE_NAME, fileListView);

		
		driver.openNotifications();
		NotificationView notificationView = new NotificationView(driver);

		try{
			if(notificationView.getUploadingNotification().isDisplayed()){
				Common.waitTillElementIsPresent(
						notificationView.getUploadSucceededNotification(),300000);
				driver.sendKeyEvent(android.view.KeyEvent.KEYCODE_HOME);
				driver.startActivity("com.owncloud.android", 
						".ui.activity.FileDisplayActivity");
				
			}
		} catch (NoSuchElementException e) {
			driver.sendKeyEvent(android.view.KeyEvent.KEYCODE_HOME);
			driver.startActivity("com.owncloud.android", 
					".ui.activity.FileDisplayActivity");
		}

		fileListViewAfterUploadFile.scrollTillFindElement(BIG_FILE_NAME);

		assertTrue(fileListViewAfterUploadFile.getFileElement().isDisplayed());
		
		Common.waitTillElementIsNotPresentWithoutTimeout(
				fileListViewAfterUploadFile.getProgressCircular(), 1000);
		common.wait.until(ExpectedConditions.visibilityOf(
				fileListViewAfterUploadFile.getFileElementLayout()
				.findElement(By.id(FileListView.getLocalFileIndicator()))));
		assertTrue(fileListViewAfterUploadFile.getFileElementLayout()
				.findElement(By.id(FileListView.getLocalFileIndicator()))
				.isDisplayed());
		fileListView = new FileListView(driver);
		fileListView.scrollTillFindElement(BIG_FILE_NAME);
		assertTrue(
				fileHasBeenUploaded = fileListView.getFileElement().isDisplayed());
	}


	@Test
	@Category(UnfinishedTestCategory.class)
	public void testUploadFromGmail () throws Exception {
		FileListView fileListView = Actions.login(Config.URL, Config.user,
				Config.password, Config.isTrusted, driver);
		driver.startActivity("com.google.android.gm",
				".ConversationListActivityGmail");
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
		driver.startActivity("com.owncloud.android",
				".ui.activity.FileDisplayActivity");
		common.wait.until(ExpectedConditions
				.visibilityOfAllElementsLocatedBy(By.name(FILE_GMAIL_NAME)));
		assertEquals(Config.fileToTestSendByEmailName ,
				driver.findElementByName(FILE_GMAIL_NAME).getText());
		fileListView = new FileListView(driver);
		fileListView.scrollTillFindElement(FILE_GMAIL_NAME);
		assertTrue(fileHasBeenUploadedFromGmail = fileListView
				.getFileElement().isDisplayed());
		//TODO. correct assert if fileListView is shown in grid mode
	}


	@Test	
	@Category({FailingTestCategory.class})
	public void testKeepFileUpToDate () throws Exception {

		FileListView fileListView = Actions.login(Config.URL, Config.user,
				Config.password, Config.isTrusted, driver);
		common.assertIsInFileListView();

		Common.waitTillElementIsNotPresentWithoutTimeout(fileListView.getProgressCircular(), 
				1000);

		FileListView fileListViewAfterUploadFile = Actions
				.uploadFile(FILE_NAME, fileListView);
		fileListViewAfterUploadFile.scrollTillFindElement(FILE_NAME);
		assertTrue(fileHasBeenUploaded = fileListViewAfterUploadFile
				.getFileElement().isDisplayed());

		ElementMenuOptions menuOptions = fileListViewAfterUploadFile
				.longPressOnElement(FILE_NAME);
		FileDetailsView fileDetailsView = menuOptions.clickOnDetails();
		fileDetailsView.checkKeepFileUpToDateCheckbox();
		Thread.sleep(3000);
		driver.sendKeyEvent(android.view.KeyEvent.KEYCODE_BACK);
		assertTrue(common.isElementPresent(
				fileListViewAfterUploadFile.getFileElementLayout(), 
				MobileBy.id(FileListView.getFavoriteFileIndicator())));
		assertTrue(fileListViewAfterUploadFile.getFileElementLayout()
				.findElement(By.id(FileListView.getFavoriteFileIndicator()))
				.isDisplayed());
	}

	@Test	
	@Category({NoIgnoreTestCategory.class})
	public void testKeepFileUpToDateAndRefresh () throws Exception {

		FileListView fileListView = Actions.login(Config.URL, Config.user,
				Config.password, Config.isTrusted, driver);
		common.assertIsInFileListView();

		Common.waitTillElementIsNotPresentWithoutTimeout(fileListView.getProgressCircular(), 
				1000);

		FileListView fileListViewAfterUploadFile = Actions
				.uploadFile(FILE_NAME, fileListView);
		fileListViewAfterUploadFile.scrollTillFindElement(FILE_NAME);
		assertTrue(fileHasBeenUploaded = fileListViewAfterUploadFile
				.getFileElement().isDisplayed());

		ElementMenuOptions menuOptions = fileListViewAfterUploadFile
				.longPressOnElement(FILE_NAME);
		FileDetailsView fileDetailsView = menuOptions.clickOnDetails();
		fileDetailsView.checkKeepFileUpToDateCheckbox();
		Thread.sleep(3000);
		driver.sendKeyEvent(android.view.KeyEvent.KEYCODE_BACK);

		fileListViewAfterUploadFile.pulldownToRefresh();
		//assertTrue(fileListView.getProgressCircular().isDisplayed());
		Common.waitTillElementIsNotPresentWithoutTimeout(fileListView.getProgressCircular(), 
				100);

		assertTrue(common.isElementPresent(
				fileListViewAfterUploadFile.getFileElementLayout(), 
				MobileBy.id(FileListView.getFavoriteFileIndicator())));
		assertTrue(fileListViewAfterUploadFile.getFileElementLayout()
				.findElement(By.id(FileListView.getFavoriteFileIndicator()))
				.isDisplayed());
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
			Actions.deleteElement(BIG_FILE_NAME,fileListView, driver);
		}

		//driver.removeApp("com.owncloud.android");
		driver.quit();
	}


}

