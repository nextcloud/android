package androidtest.tests;

import org.junit.After;
import org.junit.Before;
import org.junit.runners.MethodSorters;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.support.ui.ExpectedConditions;

import androidtest.actions.Actions;
import androidtest.models.MainView;


@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class DeleteFileTestSuite extends Common{
	
	private final String FILE_NAME = "test";
	
	@Before
	public void setUp() throws Exception {
		setUpCommonDriver();
	}

	@Test
	public void testDeleteFile () throws Exception {		
		MainView mainView = Actions.login(Config.URL, Config.user,Config.password, Config.isTrusted, driver);
		waitForTextPresent("ownCloud", mainView.getTitleTextElement());
		
		//TODO. if the file already exists, do not upload
		MainView mainViewAfterUploadFile = Actions.uploadFile(FILE_NAME, mainView);
		
		mainViewAfterUploadFile.scrollTillFindElement(FILE_NAME);
		waitTillElementIsNotPresent(mainViewAfterUploadFile.getProgressCircular(), 1000);
		wait.until(ExpectedConditions.visibilityOf(mainViewAfterUploadFile.getFileElementLayout().findElement(By.id(MainView.getLocalFileIndicator()))));
		
		Actions.deleteElement(FILE_NAME,mainViewAfterUploadFile, driver);
		assertFalse(mainViewAfterUploadFile.getFileElement().isDisplayed());
	}

	@After
	public void tearDown() throws Exception {
		takeScreenShotOnFailed(getName());
		driver.removeApp("com.owncloud.android");
		driver.quit();
	}

}