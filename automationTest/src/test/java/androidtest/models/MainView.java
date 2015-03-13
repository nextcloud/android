package androidtest.models;

import java.util.HashMap;
import java.util.List;

import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.android.AndroidElement;
import io.appium.java_client.pagefactory.AndroidFindBy;
import io.appium.java_client.pagefactory.AppiumFieldDecorator;

import org.openqa.selenium.remote.RemoteWebElement;
import org.openqa.selenium.support.CacheLookup;
import org.openqa.selenium.support.PageFactory;

public class MainView {
	final AndroidDriver driver;
	
	@CacheLookup
	@AndroidFindBy(uiAutomator = "new UiSelector().description(\"More options\")")
	private AndroidElement menuButton;
	
	@CacheLookup
	@AndroidFindBy(id = "com.owncloud.android:id/list_root")
	private AndroidElement filesLayout;
	
	@CacheLookup
	@AndroidFindBy(uiAutomator = "new UiSelector().resourceId(\"android:id/action_bar_title\")")
	private AndroidElement titleText;
	
	@AndroidFindBy(name = "Settings")
	private AndroidElement settingsButton;

	@CacheLookup
	@AndroidFindBy(uiAutomator = "new UiSelector().description(\"New folder\")")
	private AndroidElement newFolderButton;
	
	@CacheLookup
	@AndroidFindBy(uiAutomator = "new UiSelector().description(\"Upload\")")
	private AndroidElement uploadButton;
	
	@AndroidFindBy(uiAutomator = "new UiSelector().resourceId(\"com.owncloud.android:id/user_input\")")
	private AndroidElement newFolderNameField;
	
	@AndroidFindBy(uiAutomator = "new UiSelector().resourceId(\"android:id/button1\")")
	private AndroidElement newFolderOkButton;
	
	private AndroidElement waitAMomentText;
	
	@AndroidFindBy(id = "com.owncloud.android:id/ListItemLayout")
	private List<AndroidElement> listItemLayout;
	
	@AndroidFindBy(id = "com.owncloud.android:id/list_root")
	private AndroidElement listRootLayout;
	
	@AndroidFindBy(name = "Remove")
	private AndroidElement removeFileElement;
	
	@AndroidFindBy(name = "Details")
	private AndroidElement detailsFileElement;
	
	@AndroidFindBy(name = "Remote and local")
	private AndroidElement remoteAndLocalButton;
	
	@AndroidFindBy(name = "Files")
	private AndroidElement filesElementUploadFile;
	
	private AndroidElement fileElement;
	
	private AndroidElement fileElementLayout;
	
	
	public MainView (AndroidDriver driver) {
		this.driver = driver;
		PageFactory.initElements(new AppiumFieldDecorator(driver), this);
	}

	public void clickOnMenuButton () {
		//TODO. DETECT WHEN HAPPENS WHEN THERE IS NOT BUTTON IN THE TOPBAR
		//if(menuButton.exists()){
			//menuButton.click();
		//}else{
			//Thread.sleep(10000);
			//getUiDevice().pressMenu();
		//}
		menuButton.click();
	}
	
	public SettingsView clickOnSettingsButton () {
		settingsButton.click();
		SettingsView settingsView = new SettingsView(driver);
		return settingsView;
	}
	
	public SettingsView getSettingsView () {
		SettingsView settingsView = new SettingsView(driver);
		return settingsView;
	}
	
	public void clickOnNewFolderButton () {
		newFolderButton.click();
	}
	
	public void clickOnRemoveFileElement () {
		removeFileElement.click();
	}
	
	public AppDetailsView clickOnDetailsFileElement () {
		detailsFileElement.click();
		AppDetailsView appDetailsView = new AppDetailsView(driver);
		return appDetailsView;
	}
	
	public void typeNewFolderName (String newFolderName) {
		newFolderNameField.clear();
		newFolderNameField.sendKeys(newFolderName);
		driver.sendKeyEvent(android.view.KeyEvent.KEYCODE_BACK);
	}
	
	public void clickOnNewFolderOkButton () {
		newFolderOkButton.click();
		waitAMomentText = (AndroidElement) driver.findElementByName("Wait a moment");
	}
	
	public void clickOnRemoteAndLocalButton () {
		remoteAndLocalButton.click();
		waitAMomentText = (AndroidElement) driver.findElementByName("Wait a moment");
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
	
	public void tapOnFileElement (String fileName) {
		scrollTillFindElement(fileName);
		fileElement.tap(1, 1000);
	}
	
	public AndroidElement scrollTillFindElement (String fileName) {
        HashMap<String, String> scrollObject = new HashMap<String, String>();
        scrollObject.put("text", fileName);
        scrollObject.put("element", ( (RemoteWebElement) filesLayout).getId());
        if(filesLayout.getAttribute("scrollable").equals("true")){
        	driver.executeScript("mobile: scrollTo", scrollObject);
        }
		fileElement = (AndroidElement) driver.findElementByName(fileName);
		fileElementLayout = (AndroidElement) driver.findElementByAndroidUIAutomator("new UiSelector().description(\"LinearLayout-"+ fileName +"\")");
		return fileElement;
	}
	
	public AndroidElement getFileElementLayout () {
		return fileElementLayout;
	}
}
