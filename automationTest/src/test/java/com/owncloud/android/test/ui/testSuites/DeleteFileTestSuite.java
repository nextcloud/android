package com.owncloud.android.test.ui.testSuites;

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
import org.openqa.selenium.By;
import org.openqa.selenium.support.ui.ExpectedConditions;

import com.owncloud.android.test.ui.actions.Actions;
import com.owncloud.android.test.ui.groups.NoIgnoreTestCategory;
import com.owncloud.android.test.ui.groups.SmokeTestCategory;
import com.owncloud.android.test.ui.models.FileListView;


@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class DeleteFileTestSuite{
	
	AndroidDriver driver;
	Common common;
	private final String FILE_NAME = Config.fileToTestName;
	
	@Rule public TestName name = new TestName();
	
	@Before
	public void setUp() throws Exception {
		common=new Common();
		driver=common.setUpCommonDriver();
	}

	@Test
	@Category({NoIgnoreTestCategory.class, SmokeTestCategory.class})
	public void testDeleteFile () throws Exception {		
		FileListView fileListView = Actions.login(Config.URL, Config.user,Config.password, Config.isTrusted, driver);
		common.assertIsInFileListView();
		
		//TODO. if the file already exists, do not upload
		FileListView fileListViewAfterUploadFile = Actions.uploadFile(FILE_NAME, fileListView);
		
		fileListViewAfterUploadFile.scrollTillFindElement(FILE_NAME);
		Common.waitTillElementIsNotPresent(fileListViewAfterUploadFile.getProgressCircular(), 1000);
		common.wait.until(ExpectedConditions.visibilityOf(fileListViewAfterUploadFile.getFileElementLayout().findElement(By.id(FileListView.getLocalFileIndicator()))));
		
		Actions.deleteElement(FILE_NAME,fileListViewAfterUploadFile, driver);
		assertFalse(fileListViewAfterUploadFile.getFileElement().isDisplayed());
	}

	@After
	public void tearDown() throws Exception {
		common.takeScreenShotOnFailed(name.getMethodName());
		driver.removeApp("com.owncloud.android");
		driver.quit();
	}

}