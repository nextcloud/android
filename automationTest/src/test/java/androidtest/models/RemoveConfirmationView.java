package androidtest.models;

import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.android.AndroidElement;
import io.appium.java_client.pagefactory.AndroidFindBy;
import io.appium.java_client.pagefactory.AppiumFieldDecorator;

import org.openqa.selenium.support.PageFactory;

public class RemoveConfirmationView {
	final AndroidDriver driver;
	
	@AndroidFindBy(name = "Remote and local")
	private AndroidElement remoteAndLocalButton;
	
	public RemoveConfirmationView (AndroidDriver driver) {
		this.driver = driver;
		PageFactory.initElements(new AppiumFieldDecorator(driver), this);
	}
	
	public WaitAMomentPopUp clickOnRemoteAndLocalButton () {
		remoteAndLocalButton.click();
		WaitAMomentPopUp waitAMomentPopUp = new WaitAMomentPopUp(driver);
		return waitAMomentPopUp;
	}
}
