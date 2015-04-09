package androidtest.models;

import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.android.AndroidElement;
import io.appium.java_client.pagefactory.AndroidFindBy;
import io.appium.java_client.pagefactory.AppiumFieldDecorator;

import org.openqa.selenium.support.CacheLookup;
import org.openqa.selenium.support.PageFactory;

import androidtest.actions.Actions;

public class MoveView {
	final AndroidDriver driver;
	
	@CacheLookup
	@AndroidFindBy(id = "com.owncloud.android:id/list_root")
	private AndroidElement filesLayout;
	
	@AndroidFindBy(name = "Choose")
	private AndroidElement chooseButton;
	
	public MoveView (AndroidDriver driver) {
		this.driver = driver;
		PageFactory.initElements(new AppiumFieldDecorator(driver), this);
	}
	
	public WaitAMomentPopUp clickOnChoose () {
		chooseButton.click();
		WaitAMomentPopUp waitAMomentPopUp = new WaitAMomentPopUp(driver);
		return waitAMomentPopUp;
	}
	
	public  AndroidElement scrollTillFindElement (String elementName) {
		return Actions.scrollTillFindElement (elementName,filesLayout,driver);
	}
}