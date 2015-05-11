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

import androidtest.actions.Actions;
import androidtest.groups.NoIgnoreTestCategory;
import androidtest.groups.SmokeTestCategory;
import androidtest.models.MainView;
import androidtest.models.WaitAMomentPopUp;


@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class DeleteFolderTestSuite{
	AndroidDriver driver;
	Common common;
	private Boolean folderHasBeenCreated = false;
	private final String FOLDER_NAME = "testCreateFolder";
	
	@Rule public TestName name = new TestName();


	@Before
	public void setUp() throws Exception {
		common=new Common();
		driver=common.setUpCommonDriver();
	}

	@Test
	@Category({NoIgnoreTestCategory.class, SmokeTestCategory.class})
	public void testDeleteFolder () throws Exception {
		MainView mainView = Actions.login(Config.URL, Config.user,Config.password, Config.isTrusted, driver);
		common.assertIsInMainView();
		
		//TODO. if the folder already exists, do no created
		//create the folder
		WaitAMomentPopUp waitAMomentPopUp = Actions.createFolder(FOLDER_NAME, mainView);
		Common.waitTillElementIsNotPresent(waitAMomentPopUp.getWaitAMomentTextElement(), 100);
		mainView.scrollTillFindElement(FOLDER_NAME);
		assertTrue(folderHasBeenCreated = mainView.getFileElement().isDisplayed());

		//delete the folder
		Actions.deleteElement(FOLDER_NAME, mainView, driver);
		assertFalse(folderHasBeenCreated =mainView.getFileElement().isDisplayed());
	}

	@After
	public void tearDown() throws Exception {
		common.takeScreenShotOnFailed(name.getMethodName());
		if(folderHasBeenCreated){
			MainView mainView = new MainView(driver);
			Actions.deleteElement(FOLDER_NAME, mainView, driver);
		}
		driver.removeApp("com.owncloud.android");
		driver.quit();
	}

}
