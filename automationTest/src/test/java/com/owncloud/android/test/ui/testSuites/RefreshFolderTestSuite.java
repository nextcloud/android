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

import com.owncloud.android.test.ui.actions.Actions;
import com.owncloud.android.test.ui.groups.UnfinishedTestCategory;
import com.owncloud.android.test.ui.models.FileListView;


@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class RefreshFolderTestSuite{
	AndroidDriver driver;
	Common common;
	
	@Rule public TestName name = new TestName();

	@Before
	public void setUp() throws Exception {
		common=new Common();
		driver=common.setUpCommonDriver();
	}


	@Test
	@Category({UnfinishedTestCategory.class})
	public void testPulldownToRefreshFolder () throws Exception {
		FileListView fileListView = Actions.login(Config.URL, Config.user,Config.password, Config.isTrusted, driver);
		common.assertIsInFileListView();
		//TODO. Remove the sleep and check why is not working the assert when using waitTillElementIsNotPresent
		Thread.sleep(5000);
		//waitTillElementIsNotPresent(fileListView.getProgressCircular(), 1000);
		fileListView.pulldownToRefresh();
		assertTrue(fileListView.getProgressCircular().isDisplayed());
		//TODO insert a file in the web, and check that it's shown here
	}


	@After
	public void tearDown() throws Exception {
		common.takeScreenShotOnFailed(name.getMethodName());
		driver.removeApp("com.owncloud.android");
		driver.quit();
	}

}
