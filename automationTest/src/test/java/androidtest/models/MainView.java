package androidtest.models;

import java.util.List;

import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.android.AndroidElement;
import io.appium.java_client.pagefactory.AndroidFindBy;
import io.appium.java_client.pagefactory.AppiumFieldDecorator;

import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.support.CacheLookup;
import org.openqa.selenium.support.PageFactory;

import androidtest.actions.Actions;

public class MainView {
	final AndroidDriver driver;
	
	@AndroidFindBy(uiAutomator = "new UiSelector().description(\"More options\")")
	private AndroidElement menuButton;
	
	@CacheLookup
	@AndroidFindBy(id = "com.owncloud.android:id/list_root")
	private AndroidElement filesLayout;
	
	@CacheLookup
	@AndroidFindBy(uiAutomator = "new UiSelector().resourceId(\"android:id/action_bar_title\")")
	private AndroidElement titleText;
	
	@AndroidFindBy(id = "android:id/progress_circular")
	private AndroidElement progressCircular;

	@CacheLookup
	@AndroidFindBy(uiAutomator = "new UiSelector().description(\"New folder\")")
	private AndroidElement newFolderButton;
	
	@CacheLookup
	@AndroidFindBy(uiAutomator = "new UiSelector().description(\"Upload\")")
	private AndroidElement uploadButton;
	
	private AndroidElement waitAMomentText;
	
	@AndroidFindBy(id = "com.owncloud.android:id/ListItemLayout")
	private List<AndroidElement> listItemLayout;
	
	@AndroidFindBy(id = "com.owncloud.android:id/list_root")
	private AndroidElement listRootLayout;
	
	@AndroidFindBy(name = "Files")
	private AndroidElement filesElementUploadFile;
	
	private AndroidElement fileElement;
	
	private AndroidElement fileElementLayout;
	
	private static String localFileIndicator = "com.owncloud.android:id/localFileIndicator";
	private static String favoriteFileIndicator = "com.owncloud.android:id/favoriteIcon";
	
	
	public MainView (AndroidDriver driver) {
		this.driver = driver;
		PageFactory.initElements(new AppiumFieldDecorator(driver), this);
	}

	public MenuList clickOnMenuButton () {
		menuButton.click();
		MenuList menuList = new MenuList (driver);
		return menuList;
	}
	
	public SettingsView getSettingsView () {
		SettingsView settingsView = new SettingsView(driver);
		return settingsView;
	}
	
	public NewFolderPopUp clickOnNewFolderButton () {
		newFolderButton.click();
		NewFolderPopUp newFolderPopUp = new NewFolderPopUp(driver);
		return newFolderPopUp;
	}
	
	public void clickOnUploadButton () {
		uploadButton.click();
	}
	
	public FilesView clickOnFilesElementUploadFile () {
		filesElementUploadFile.click();
		FilesView filesView = new FilesView(driver);
		return filesView;
	}
	
	public AndroidElement getTitleTextElement () {
		return titleText;
	}
	
	public AndroidElement getWaitAMomentTextElement () {
		return waitAMomentText;
	}
	
	public AndroidElement getListRootElement () {
		return listRootLayout;
	}
	
	public List<AndroidElement> getListItemLayout () {
		return listItemLayout;
	}
	
	public AndroidElement getFileElement () {
		return fileElement;
	}
	
	public ElementMenuOptions longPressOnElement (String elementName) {
		scrollTillFindElement(elementName).tap(1, 1000);
		//fileElement.tap(1, 1000);
		ElementMenuOptions menuOptions = new ElementMenuOptions(driver);
		return menuOptions;
	}
	
	public AndroidElement scrollTillFindElement (String elementName) {
        fileElement = Actions.scrollTillFindElement (elementName,filesLayout,driver);
		try {
        	fileElementLayout = (AndroidElement) driver.findElementByAndroidUIAutomator("new UiSelector().description(\"LinearLayout-"+ elementName +"\")");
        } catch (NoSuchElementException e) {
        	fileElementLayout = null;
        }
		return fileElement;
	}
	
	public AndroidElement getFileElementLayout () {
		return fileElementLayout;
	}
	
	public AndroidElement getProgressCircular () {
		return progressCircular;
	}
	
	public static String getLocalFileIndicator() {
		return localFileIndicator;
	}
	
	public static String getFavoriteFileIndicator() {
		return favoriteFileIndicator;
	}


}
