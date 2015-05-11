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
public class CreateFolderTestSuite{
	
	AndroidDriver driver;
	Common common;
	private Boolean folderHasBeenCreated = false;
	private final String FOLDER_NAME = "testCreateFolder";
	private String CurrentCreatedFolder = "";
	
	@Rule public TestName name = new TestName();

	@Before
	public void setUp() throws Exception {
		common=new Common();
		driver=common.setUpCommonDriver();
	}

	@Test
	@Category({NoIgnoreTestCategory.class, SmokeTestCategory.class})
	public void testCreateNewFolder () throws Exception {
		String NEW_FOLDER_NAME = "testCreateFolder";

		MainView mainView = Actions.login(Config.URL, Config.user,Config.password, Config.isTrusted, driver);
		common.assertIsInMainView();

		//check if the folder already exists and if true, delete them
		Actions.deleteElement(NEW_FOLDER_NAME, mainView, driver);

		WaitAMomentPopUp waitAMomentPopUp = Actions.createFolder(NEW_FOLDER_NAME, mainView);
		Common.waitTillElementIsNotPresent(waitAMomentPopUp.getWaitAMomentTextElement(), 100);
		mainView.scrollTillFindElement(FOLDER_NAME);
		assertNotNull(mainView.getFileElement());
		assertTrue(folderHasBeenCreated=mainView.getFileElement().isDisplayed());	
		CurrentCreatedFolder = FOLDER_NAME;
		assertEquals(FOLDER_NAME , mainView.getFileElement().getText());
	}

	@After
	public void tearDown() throws Exception {
		common.takeScreenShotOnFailed(name.getMethodName());
		if (folderHasBeenCreated) {
			MainView mainView = new MainView(driver);
			Actions.deleteElement(CurrentCreatedFolder, mainView, driver);
		}
		driver.removeApp("com.owncloud.android");
		driver.quit();
	}

}
