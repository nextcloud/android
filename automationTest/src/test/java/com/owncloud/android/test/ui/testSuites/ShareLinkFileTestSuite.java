package com.owncloud.android.test.ui.testSuites;

import static org.junit.Assert.assertTrue;
import io.appium.java_client.android.AndroidDriver;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.TestName;
import org.openqa.selenium.By;
import org.openqa.selenium.support.ui.ExpectedConditions;

import com.owncloud.android.test.ui.actions.Actions;
import com.owncloud.android.test.ui.groups.NoIgnoreTestCategory;
import com.owncloud.android.test.ui.groups.SmokeTestCategory;
import com.owncloud.android.test.ui.models.ElementMenuOptions;
import com.owncloud.android.test.ui.models.GmailSendMailView;
import com.owncloud.android.test.ui.models.FileListView;
import com.owncloud.android.test.ui.models.ShareView;

public class ShareLinkFileTestSuite{
	
	AndroidDriver driver;
	Common common;
	private final String FILE_NAME = Config.fileToTestName;
	private Boolean fileHasBeenCreated = false;
	
	@Rule public TestName name = new TestName();
	
	@Before
	public void setUp() throws Exception {
		common=new Common();
		driver=common.setUpCommonDriver();
	}

	@Test
	@Category({NoIgnoreTestCategory.class, SmokeTestCategory.class})
	public void testShareLinkFile () throws Exception {	
		FileListView fileListView = Actions.login(Config.URL, Config.user,Config.password, Config.isTrusted, driver);
		common.assertIsInFileListView();
		
		//TODO. if the file already exists, do not upload
		FileListView fileListViewAfterUploadFile = Actions.uploadFile(FILE_NAME, fileListView);
				
		fileListViewAfterUploadFile.scrollTillFindElement(FILE_NAME);
		assertTrue(fileHasBeenCreated = fileListViewAfterUploadFile.getFileElement().isDisplayed());
		
		ElementMenuOptions elementMenuOption = fileListViewAfterUploadFile.longPressOnElement(FILE_NAME);
		ShareView shareView = elementMenuOption.clickOnShareLinkElement();
		Actions.scrollTillFindElement("Gmail", shareView.getListViewLayout(), driver).click();
		GmailSendMailView gmailSendMailView = new GmailSendMailView(driver);
		gmailSendMailView.typeToEmailAdress(Config.gmailAccount);
		gmailSendMailView.clickOnSendButton();
		Common.waitTillElementIsNotPresent(fileListViewAfterUploadFile.getProgressCircular(), 1000);
		common.wait.until(ExpectedConditions.visibilityOf(fileListViewAfterUploadFile.getFileElementLayout().findElement(By.id(FileListView.getSharedElementIndicator()))));
		assertTrue(fileListViewAfterUploadFile.getFileElementLayout().findElement(By.id(FileListView.getSharedElementIndicator())).isDisplayed());

	}

	@After
	public void tearDown() throws Exception {
		common.takeScreenShotOnFailed(name.getMethodName());
		if (fileHasBeenCreated) {
			FileListView fileListView = new FileListView(driver);
			Actions.deleteElement(FILE_NAME,fileListView, driver);
		}
		driver.removeApp("com.owncloud.android");
		driver.quit();
	}
}
