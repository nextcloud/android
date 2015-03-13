package androidtest.models;

import java.util.HashMap;

import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.android.AndroidElement;
import io.appium.java_client.pagefactory.AndroidFindBy;
import io.appium.java_client.pagefactory.AppiumFieldDecorator;

import org.openqa.selenium.remote.RemoteWebElement;
import org.openqa.selenium.support.CacheLookup;
import org.openqa.selenium.support.PageFactory;

public class FilesView {
	final AndroidDriver driver;
	
	@CacheLookup
	@AndroidFindBy(id = "com.owncloud.android:id/list_root")
	private AndroidElement fileLayout;
	
	@CacheLookup
	@AndroidFindBy(id = "com.owncloud.android:id/upload_files_btn_upload")
	private AndroidElement uploadButton;
	
	private AndroidElement fileElement;
	
	public FilesView (AndroidDriver driver) {
		this.driver = driver;
		PageFactory.initElements(new AppiumFieldDecorator(driver), this);
	}
	
	public MainView clickOnUploadButton () {
		uploadButton.click();
		MainView mainView = new MainView (driver);
		return mainView;
	}
	
	public void scrollTillFindFile (String fileName) {
        HashMap<String, String> scrollObject = new HashMap<String, String>();
        scrollObject.put("text", fileName);
        scrollObject.put("element", ( (RemoteWebElement) fileLayout).getId());
        driver.executeScript("mobile: scrollTo", scrollObject);
		fileElement = (AndroidElement) driver.findElementByName(fileName);
	}
	
	public void clickOnFileName (String fileName) {
		scrollTillFindFile(fileName);
		fileElement.click();
	}
}
