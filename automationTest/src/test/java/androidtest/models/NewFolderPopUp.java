package androidtest.models;

import org.openqa.selenium.support.PageFactory;

import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.android.AndroidElement;
import io.appium.java_client.pagefactory.AndroidFindBy;
import io.appium.java_client.pagefactory.AppiumFieldDecorator;

public class NewFolderPopUp {

	final AndroidDriver driver;
	
	@AndroidFindBy(uiAutomator = "new UiSelector().resourceId(\"android:id/button1\")")
	private AndroidElement newFolderOkButton;
	
	@AndroidFindBy(uiAutomator = "new UiSelector().resourceId(\"com.owncloud.android:id/user_input\")")
	private AndroidElement newFolderNameField;
	
	public NewFolderPopUp (AndroidDriver driver) {
		this.driver = driver;
		PageFactory.initElements(new AppiumFieldDecorator(driver), this);
	}
		
	public void typeNewFolderName (String newFolderName) {
		newFolderNameField.clear();
		newFolderNameField.sendKeys(newFolderName + "\n");
		//driver.hideKeyboard();
	}

	public WaitAMomentPopUp clickOnNewFolderOkButton () {
		newFolderOkButton.click();
		WaitAMomentPopUp waitAMomentPopUp = new WaitAMomentPopUp(driver);
		return waitAMomentPopUp;
	}
}
