package androidtest.models;

import org.openqa.selenium.support.PageFactory;

import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.android.AndroidElement;
import io.appium.java_client.pagefactory.AndroidFindBy;
import io.appium.java_client.pagefactory.AppiumFieldDecorator;

public class WaitAMomentPopUp {
	final AndroidDriver driver;
	
	@AndroidFindBy(name = "Wait a moment")
	private AndroidElement waitAMomentText;
	
	public WaitAMomentPopUp (AndroidDriver driver) {
		this.driver = driver;
		PageFactory.initElements(new AppiumFieldDecorator(driver), this);
	}
	
	public AndroidElement getWaitAMomentTextElement () {
		return waitAMomentText;
	}
}
