package androidtest.tests;

import org.junit.After;
import org.junit.Before;
import org.junit.runners.MethodSorters;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.support.ui.ExpectedConditions;

import androidtest.actions.Actions;
import androidtest.models.ElementMenuOptions;
import androidtest.models.MainView;
import androidtest.models.NewFolderPopUp;
import androidtest.models.WaitAMomentPopUp;


@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class RenameFileTestSuite extends Common{

	private Boolean fileHasBeenCreated = false;
	private final String OLD_FILE_NAME = "test";
	private final String FILE_NAME = "newNameFile";
	private String CurrentCreatedFile = "";


	@Before
	public void setUp() throws Exception {
		setUpCommonDriver();
	}

	@Test
	public void testRenameFile () throws Exception {
		MainView mainView = Actions.login(Config.URL, Config.user,Config.password, Config.isTrusted, driver);
		waitForTextPresent("ownCloud", mainView.getTitleTextElement());

		//TODO. if the file already exists, do not upload
		MainView mainViewAfterUploadFile = Actions.uploadFile(OLD_FILE_NAME, mainView);

		//check if the file with the new name already exists, if true delete it
		Actions.deleteElement(FILE_NAME, mainView, driver);

		mainViewAfterUploadFile.scrollTillFindElement(OLD_FILE_NAME);
		assertTrue(fileHasBeenCreated = mainViewAfterUploadFile.getFileElement().isDisplayed());
		CurrentCreatedFile = OLD_FILE_NAME;
		waitTillElementIsNotPresent(mainViewAfterUploadFile.getProgressCircular(), 1000);
		wait.until(ExpectedConditions.visibilityOf(mainViewAfterUploadFile.getFileElementLayout().findElement(By.id(MainView.getLocalFileIndicator()))));
		ElementMenuOptions menuOptions = mainViewAfterUploadFile.longPressOnElement(OLD_FILE_NAME);
		NewFolderPopUp newFolderPopUp = menuOptions.clickOnRename();
		newFolderPopUp.typeNewFolderName(FILE_NAME);
		WaitAMomentPopUp waitAMomentPopUp = newFolderPopUp.clickOnNewFolderOkButton();
		waitTillElementIsNotPresent(waitAMomentPopUp.getWaitAMomentTextElement(), 100);
		mainViewAfterUploadFile.scrollTillFindElement(FILE_NAME);
		assertNotNull(mainViewAfterUploadFile.getFileElement());
		assertTrue(mainViewAfterUploadFile.getFileElement().isDisplayed());	
		assertEquals(FILE_NAME , mainViewAfterUploadFile.getFileElement().getText());
		CurrentCreatedFile = FILE_NAME;
	}

	@After
	public void tearDown() throws Exception {
		if (fileHasBeenCreated) {
			MainView mainView = new MainView(driver);
			Actions.deleteElement(CurrentCreatedFile,mainView, driver);
		}
		driver.removeApp("com.owncloud.android");
		driver.quit();
	}

}
