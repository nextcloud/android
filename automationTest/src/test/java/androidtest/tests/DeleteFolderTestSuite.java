package androidtest.tests;

import org.junit.After;
import org.junit.Before;
import org.junit.runners.MethodSorters;
import org.junit.FixMethodOrder;
import org.junit.Test;

import androidtest.actions.Actions;
import androidtest.models.MainView;
import androidtest.models.WaitAMomentPopUp;


@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class DeleteFolderTestSuite extends Common{
	private Boolean folderHasBeenCreated = false;
	private final String FOLDER_NAME = "testCreateFolder";


	@Before
	public void setUp() throws Exception {
		setUpCommonDriver();
	}

	@Test
	public void testDeleteFolder () throws Exception {
		MainView mainView = Actions.login(Config.URL, Config.user,Config.password, Config.isTrusted, driver);
		waitForTextPresent("ownCloud", mainView.getTitleTextElement());
		
		//TODO. if the folder already exists, do no created
		//create the folder
		WaitAMomentPopUp waitAMomentPopUp = Actions.createFolder(FOLDER_NAME, mainView);
		waitTillElementIsNotPresent(waitAMomentPopUp.getWaitAMomentTextElement(), 100);
		mainView.scrollTillFindElement(FOLDER_NAME);
		assertTrue(folderHasBeenCreated = mainView.getFileElement().isDisplayed());

		//delete the folder
		Actions.deleteElement(FOLDER_NAME, mainView, driver);
		assertFalse(folderHasBeenCreated =mainView.getFileElement().isDisplayed());
	}

	@After
	public void tearDown() throws Exception {
		takeScreenShotOnFailed(getName());
		if(folderHasBeenCreated){
			MainView mainView = new MainView(driver);
			Actions.deleteElement(FOLDER_NAME, mainView, driver);
		}
		driver.removeApp("com.owncloud.android");
		driver.quit();
	}

}
