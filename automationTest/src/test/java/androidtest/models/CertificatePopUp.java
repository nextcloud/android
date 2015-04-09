package androidtest.models;

import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.android.AndroidElement;
import io.appium.java_client.pagefactory.AndroidFindBy;
import io.appium.java_client.pagefactory.AppiumFieldDecorator;

import org.openqa.selenium.support.PageFactory;

public class CertificatePopUp {
	final AndroidDriver driver;	
	
	@AndroidFindBy(name = "OK")
	private AndroidElement okButton;
	
	public CertificatePopUp (AndroidDriver driver) {
		this.driver = driver;
		PageFactory.initElements(new AppiumFieldDecorator(driver), this);
	}
	
	public void clickOnOkButton () {
		okButton.click();
	}
	
	public AndroidElement getOkButtonElement () {
		return okButton;
	}

}
