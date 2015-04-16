package androidtest.tests;

import org.junit.After;
import org.junit.Before;
import org.junit.runners.MethodSorters;
import org.junit.FixMethodOrder;
import org.junit.Test;
import androidtest.actions.Actions;
import androidtest.models.ElementMenuOptions;
import androidtest.models.MainView;
import androidtest.models.MoveView;
import androidtest.models.WaitAMomentPopUp;


@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class MoveFolderTestSuite extends Common{
	private String FOLDER_TO_MOVE = "folderToMove";
	private String FOLDER_WHERE_MOVE = "folderWhereMove";

	@Before
	public void setUp() throws Exception {
		setUpCommonDriver();
	}

	@Test
	public void testMoveFolder () throws Exception {
		WaitAMomentPopUp waitAMomentPopUp;

		MainView mainView = Actions.login(Config.URL, Config.user,Config.password, Config.isTrusted, driver);
		assertTrue(waitForTextPresent("ownCloud", mainView.getTitleTextElement()));

		waitTillElementIsNotPresent(mainView.getProgressCircular(), 1000);

		//check if the folder already exists and if true, delete them
		Actions.deleteElement(FOLDER_WHERE_MOVE, mainView, driver);
		Actions.deleteElement(FOLDER_TO_MOVE, mainView, driver);

		//Create the folder where the other is gone to be moved
		waitAMomentPopUp = Actions.createFolder(FOLDER_WHERE_MOVE, mainView);
		waitTillElementIsNotPresent(waitAMomentPopUp.getWaitAMomentTextElement(), 100);
		mainView.scrollTillFindElement(FOLDER_WHERE_MOVE);
		assertTrue(mainView.getFileElement().isDisplayed());

		//Create the folder which is going to be moved
		waitAMomentPopUp = Actions.createFolder(FOLDER_TO_MOVE, mainView);
		waitTillElementIsNotPresent(waitAMomentPopUp.getWaitAMomentTextElement(), 100);
		mainView.scrollTillFindElement(FOLDER_TO_MOVE);
		assertTrue(mainView.getFileElement().isDisplayed());

		//select to move the folder
		ElementMenuOptions menuOptions = mainView.longPressOnElement(FOLDER_TO_MOVE);
		MoveView moveView = menuOptions.clickOnMove();

		//to move to a folder
		moveView.scrollTillFindElement(FOLDER_WHERE_MOVE).tap(1,1);
		waitAMomentPopUp = moveView.clickOnChoose();
		waitTillElementIsNotPresent(waitAMomentPopUp.getWaitAMomentTextElement(), 100);
		
		//check that the folder moved is inside the other
		mainView.scrollTillFindElement(FOLDER_WHERE_MOVE).tap(1,1);
		waitTillElementIsNotPresent(mainView.getProgressCircular(), 1000);
		Thread.sleep(1000);
		mainView.scrollTillFindElement(FOLDER_TO_MOVE);
		assertEquals(FOLDER_TO_MOVE , mainView.getFileElement().getText());
	}

	@After
	public void tearDown() throws Exception {
		takeScreenShotOnFailed(getName());
		MainView mainView = new MainView(driver);
		driver.sendKeyEvent(android.view.KeyEvent.KEYCODE_BACK);
		Actions.deleteElement(FOLDER_WHERE_MOVE, mainView, driver);
		Actions.deleteElement(FOLDER_TO_MOVE, mainView, driver);
		driver.removeApp("com.owncloud.android");
		driver.quit();
	}


}
