package androidtest.tests;

import org.junit.After;
import org.junit.Before;
import org.junit.runners.MethodSorters;
import org.junit.FixMethodOrder;
import org.junit.Test;
import androidtest.actions.Actions;
import androidtest.models.ElementMenuOptions;
import androidtest.models.MainView;
import androidtest.models.NewFolderPopUp;
import androidtest.models.WaitAMomentPopUp;


@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class RenameFolderTestSuite extends Common{

	private Boolean folderHasBeenCreated = false;
	private final String OLD_FOLDER_NAME = "beforeRemoving";
	private final String FOLDER_NAME = "testCreateFolder";
	private String CurrentCreatedFolder = "";


	@Before
	public void setUp() throws Exception {
		setUpCommonDriver();
	}

	@Test
	public void testRenameFolder () throws Exception {
		MainView mainView = Actions.login(Config.URL, Config.user,Config.password, Config.isTrusted, driver);
		waitForTextPresent("ownCloud", mainView.getTitleTextElement());

		//TODO. if the folder already exists, do no created
		//create the folder to rename
		WaitAMomentPopUp waitAMomentPopUp = Actions.createFolder(OLD_FOLDER_NAME, mainView);
		waitTillElementIsNotPresent(waitAMomentPopUp.getWaitAMomentTextElement(), 100);
		mainView.scrollTillFindElement(OLD_FOLDER_NAME);

		assertTrue(folderHasBeenCreated = mainView.getFileElement().isDisplayed());

		//check if the folder with the new name already exists and if true, delete them
		Actions.deleteElement(FOLDER_NAME, mainView, driver);

		CurrentCreatedFolder = OLD_FOLDER_NAME;
		ElementMenuOptions menuOptions = mainView.longPressOnElement(OLD_FOLDER_NAME);
		NewFolderPopUp FolderPopUp = menuOptions.clickOnRename();
		FolderPopUp.typeNewFolderName(FOLDER_NAME);
		FolderPopUp.clickOnNewFolderOkButton();
		CurrentCreatedFolder = FOLDER_NAME;
		waitTillElementIsNotPresent(waitAMomentPopUp.getWaitAMomentTextElement(), 100);
		mainView.scrollTillFindElement(FOLDER_NAME);
		assertNotNull(mainView.getFileElement());
		assertTrue(folderHasBeenCreated = mainView.getFileElement().isDisplayed());	
		assertEquals(FOLDER_NAME , mainView.getFileElement().getText());
	}

	@After
	public void tearDown() throws Exception {
		takeScreenShotOnFailed(getName());
		if(folderHasBeenCreated){
			MainView mainView = new MainView(driver);
			Actions.deleteElement(CurrentCreatedFolder, mainView, driver);
		}
		driver.removeApp("com.owncloud.android");
		driver.quit();
	}

}
