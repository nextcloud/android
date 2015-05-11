package androidtest.tests;

import static org.junit.Assert.*;
import io.appium.java_client.android.AndroidDriver;

import org.junit.After;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.TestName;
import org.junit.runners.MethodSorters;

import androidtest.actions.Actions;
import androidtest.groups.NoIgnoreTestCategory;
import androidtest.groups.SmokeTestCategory;
import androidtest.models.ElementMenuOptions;
import androidtest.models.MainView;
import androidtest.models.MoveView;
import androidtest.models.WaitAMomentPopUp;



@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class MoveFileTestSuite{
	AndroidDriver driver;
	Common common;
	private String FOLDER_WHERE_MOVE = "folderWhereMove";
	private String FILE_NAME = "test";
	@Rule public TestName name = new TestName();
	
	@Before
	public void setUp() throws Exception {
		common=new Common();
		driver=common.setUpCommonDriver();
	}

	@Test
	@Category({NoIgnoreTestCategory.class, SmokeTestCategory.class})
	public void testMoveFile () throws Exception {
		WaitAMomentPopUp waitAMomentPopUp;

		MainView mainView = Actions.login(Config.URL, Config.user,Config.password, Config.isTrusted, driver);
		common.assertIsInMainView();

		Common.waitTillElementIsNotPresent(mainView.getProgressCircular(), 1000);

		//check if the folder already exists and if true, delete them
		Actions.deleteElement(FOLDER_WHERE_MOVE, mainView, driver);
		Actions.deleteElement(FILE_NAME, mainView, driver);

		//Create the folder where the other is gone to be moved
		waitAMomentPopUp = Actions.createFolder(FOLDER_WHERE_MOVE, mainView);
		Common.waitTillElementIsNotPresent(waitAMomentPopUp.getWaitAMomentTextElement(), 100);
		mainView.scrollTillFindElement(FOLDER_WHERE_MOVE);
		assertTrue(mainView.getFileElement().isDisplayed());

		MainView mainViewAfterUploadFile = Actions.uploadFile(FILE_NAME, mainView);
		mainViewAfterUploadFile.scrollTillFindElement(FILE_NAME);
		assertTrue(mainViewAfterUploadFile.getFileElement().isDisplayed());

		//select to move the file
		ElementMenuOptions menuOptions = mainView.longPressOnElement(FILE_NAME);
		MoveView moveView = menuOptions.clickOnMove();

		//to move to a folder
		moveView.scrollTillFindElement(FOLDER_WHERE_MOVE).tap(1,1);
		waitAMomentPopUp = moveView.clickOnChoose();
		Common.waitTillElementIsNotPresent(waitAMomentPopUp.getWaitAMomentTextElement(), 100);

		//check that the folder moved is inside the other
		mainView.scrollTillFindElement(FOLDER_WHERE_MOVE).tap(1,1);
		Common.waitTillElementIsNotPresent(mainView.getProgressCircular(), 1000);
		Thread.sleep(1000);
		mainView.scrollTillFindElement(FILE_NAME);
		assertEquals(FILE_NAME , mainView.getFileElement().getText());

	}

	@After
	public void tearDown() throws Exception {
		common.takeScreenShotOnFailed(name.getMethodName());
		MainView mainView = new MainView(driver);
		driver.sendKeyEvent(android.view.KeyEvent.KEYCODE_BACK);
		Actions.deleteElement(FOLDER_WHERE_MOVE, mainView, driver);
		Actions.deleteElement(FILE_NAME, mainView, driver);
		driver.removeApp("com.owncloud.android");
		driver.quit();
	}

}
