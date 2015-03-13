package androidtest.models;

import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.android.AndroidElement;
import io.appium.java_client.pagefactory.AndroidFindBy;
import io.appium.java_client.pagefactory.AppiumFieldDecorator;

import org.openqa.selenium.support.CacheLookup;
import org.openqa.selenium.support.PageFactory;

public class AppDetailsView {
	final AndroidDriver driver;
	
	@CacheLookup
	@AndroidFindBy(name = "Keep file up to date")
	private AndroidElement keepFileUpToDateCheckbox;
	
	@AndroidFindBy(id = "com.owncloud.android:id/fdProgressBar")
	private AndroidElement progressBar;
	
	public AppDetailsView (AndroidDriver driver) {
		this.driver = driver;
		PageFactory.initElements(new AppiumFieldDecorator(driver), this);
	}
	
	public void checkKeepFileUpToDateCheckbox () {
		if(keepFileUpToDateCheckbox.getAttribute("checked").equals("false")){
			keepFileUpToDateCheckbox.click();
		}
	}
	
	public void unCheckKeepFileUpToDateCheckbox () {
		if(keepFileUpToDateCheckbox.getAttribute("checked").equals("true")){
			keepFileUpToDateCheckbox.click();
		}
	}
	
	public AndroidElement getProgressBar (){
		return progressBar;
	}
}
