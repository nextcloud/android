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
import org.openqa.selenium.By;
import org.openqa.selenium.support.ui.ExpectedConditions;

import androidtest.actions.Actions;
import androidtest.groups.NoIgnoreTestCategory;
import androidtest.groups.SmokeTestCategory;
import androidtest.models.MainView;


@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class DeleteFileTestSuite{
	
	AndroidDriver driver;
	Common common;
	private final String FILE_NAME = "test";
	
	@Rule public TestName name = new TestName();
	
	@Before
	public void setUp() throws Exception {
		common=new Common();
		driver=common.setUpCommonDriver();
	}

	@Test
	@Category({NoIgnoreTestCategory.class, SmokeTestCategory.class})
	public void testDeleteFile () throws Exception {		
		MainView mainView = Actions.login(Config.URL, Config.user,Config.password, Config.isTrusted, driver);
		common.assertIsInMainView();
		
		//TODO. if the file already exists, do not upload
		MainView mainViewAfterUploadFile = Actions.uploadFile(FILE_NAME, mainView);
		
		mainViewAfterUploadFile.scrollTillFindElement(FILE_NAME);
		Common.waitTillElementIsNotPresent(mainViewAfterUploadFile.getProgressCircular(), 1000);
		common.wait.until(ExpectedConditions.visibilityOf(mainViewAfterUploadFile.getFileElementLayout().findElement(By.id(MainView.getLocalFileIndicator()))));
		
		Actions.deleteElement(FILE_NAME,mainViewAfterUploadFile, driver);
		assertFalse(mainViewAfterUploadFile.getFileElement().isDisplayed());
	}

	@After
	public void tearDown() throws Exception {
		common.takeScreenShotOnFailed(name.getMethodName());
		driver.removeApp("com.owncloud.android");
		driver.quit();
	}

}