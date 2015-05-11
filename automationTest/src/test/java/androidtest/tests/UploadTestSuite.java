package androidtest.tests;


import static org.junit.Assert.*;
import io.appium.java_client.MobileBy;
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
import androidtest.groups.FailingTestCategory;
import androidtest.groups.IgnoreTestCategory;
import androidtest.groups.NoIgnoreTestCategory;
import androidtest.models.AppDetailsView;
import androidtest.models.ElementMenuOptions;
import androidtest.models.GmailEmailListView;
import androidtest.models.GmailEmailView;
import androidtest.models.ImageView;
import androidtest.models.MainView;
import androidtest.models.UploadView;


@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@Category({NoIgnoreTestCategory.class})
public class UploadTestSuite{

	AndroidDriver driver;
	Common common;
	String FILE_NAME = "test";
	
	@Rule public TestName name = new TestName();
	

	@Before
	public void setUp() throws Exception {
		common=new Common();
		driver=common.setUpCommonDriver();
	}

	@Test
	@Category(NoIgnoreTestCategory.class)
	public void testUploadFile () throws Exception {

		MainView mainView = Actions.login(Config.URL, Config.user,Config.password, Config.isTrusted, driver);
		common.assertIsInMainView();

		//check if the file already exists and if true, delete it
		Actions.deleteElement(FILE_NAME, mainView, driver);

		MainView mainViewAfterUploadFile = Actions.uploadFile(FILE_NAME, mainView);

		mainViewAfterUploadFile.scrollTillFindElement(FILE_NAME);
		assertTrue(mainViewAfterUploadFile.getFileElement().isDisplayed());
		Common.waitTillElementIsNotPresent(mainViewAfterUploadFile.getProgressCircular(), 1000);
		common.wait.until(ExpectedConditions.visibilityOf(mainViewAfterUploadFile.getFileElementLayout().findElement(By.id(MainView.getLocalFileIndicator()))));


	}
	
	@Test
	@Category(IgnoreTestCategory.class)
	public void testUploadFromGmail () throws Exception {
		MainView mainView = Actions.login(Config.URL, Config.user,Config.password, Config.isTrusted, driver);
		driver.startActivity("com.google.android.gm", ".ConversationListActivityGmail");
		GmailEmailListView gmailEmailListView = new GmailEmailListView(driver);
		GmailEmailView gmailEmailView = gmailEmailListView.clickOnEmail();
		ImageView imageView = gmailEmailView.clickOnfileButton();
		imageView.clickOnOptionsButton();
		imageView.clickOnShareButton();
		imageView.clickOnOwnCloudButton();
		imageView.clickOnJustOnceButton();
		UploadView uploadView = new UploadView(driver);
		uploadView.clickOUploadButton();
		driver.startActivity("com.owncloud.android", ".ui.activity.FileDisplayActivity");
		common.wait.until(ExpectedConditions.visibilityOfAllElementsLocatedBy(By.name("test.jpg")));
		assertEquals("test.jpg" , driver.findElementByName("test.jpg").getText());
	}

	
	@Test	
	@Category({IgnoreTestCategory.class, FailingTestCategory.class})
	public void testKeepFileUpToDate () throws Exception {

		MainView mainView = Actions.login(Config.URL, Config.user,Config.password, Config.isTrusted, driver);
		common.assertIsInMainView();

		Common.waitTillElementIsNotPresent(mainView.getProgressCircular(), 1000);

		MainView mainViewAfterUploadFile = Actions.uploadFile(FILE_NAME, mainView);
		mainViewAfterUploadFile.scrollTillFindElement(FILE_NAME);
		assertTrue(mainViewAfterUploadFile.getFileElement().isDisplayed());

		ElementMenuOptions menuOptions = mainViewAfterUploadFile.longPressOnElement(FILE_NAME);
		AppDetailsView appDetailsView = menuOptions.clickOnDetails();
		appDetailsView.checkKeepFileUpToDateCheckbox();
		Thread.sleep(3000);
		driver.sendKeyEvent(android.view.KeyEvent.KEYCODE_BACK);
		assertTrue(common.isElementPresent(mainViewAfterUploadFile.getFileElementLayout(), MobileBy.id(MainView.getFavoriteFileIndicator())));

	}


	@After
	public void tearDown() throws Exception {
		common.takeScreenShotOnFailed(name.getMethodName());
		MainView mainView = new MainView(driver);
		Actions.deleteElement(FILE_NAME,mainView, driver);
		driver.removeApp("com.owncloud.android");
		driver.quit();
	}


}

