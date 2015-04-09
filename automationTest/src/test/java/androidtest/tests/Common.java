package androidtest.tests;

import java.io.File;
import java.net.URL;
import java.util.concurrent.TimeUnit;

import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.support.ui.WebDriverWait;
import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.android.AndroidElement;
import junit.framework.TestCase;

public class Common extends TestCase{
	AndroidDriver driver;
	static int waitingTime = 30;
	
	WebDriverWait wait;
	
	protected void setUpCommonDriver () throws Exception {
		File rootPath = new File(System.getProperty("user.dir"));
		File appDir = new File(rootPath,"src/test/resources");
		File app = new File(appDir,"ownCloud.apk");
		DesiredCapabilities capabilities = new DesiredCapabilities();
		capabilities.setCapability("platformName", "Android");
		capabilities.setCapability("deviceName", "Device");
		capabilities.setCapability("app", app.getAbsolutePath());
		capabilities.setCapability("app-package", "com.owncloud.android");
		capabilities.setCapability("app-activity", ".ui.activity.FileDisplayActivity");	
		capabilities.setCapability("appWaitActivity", ".authentication.AuthenticatorActivity");
		driver = new AndroidDriver(new URL("http://127.0.0.1:4723/wd/hub"), capabilities);
		driver.manage().timeouts().implicitlyWait(waitingTime, TimeUnit.SECONDS);
		wait = new WebDriverWait(driver, waitingTime, 50);

	}
	
	protected boolean waitForTextPresent(String text, AndroidElement element) throws InterruptedException{
		for (int second = 0;;second++){	
			if (second >= waitingTime)
				return false;
			try{
				if (text.equals(element.getText()))
					break;
			} catch (Exception e){
				
			}
			Thread.sleep(1000);
		}
		return true;
	}
	
	protected boolean isElementPresent(AndroidElement element, By by) {
		try {
			element.findElement(by);
			return true;
		} catch (NoSuchElementException e) {
			return false;
		}
	}
	
	protected boolean isElementPresent(AndroidElement element) {
		try{
			element.isDisplayed();
		} catch (NoSuchElementException e){
			return false;
		}
		return true;
	}
	
	//pollingTime in milliseconds
	public static void waitTillElementIsNotPresent (AndroidElement element, int pollingTime) throws Exception {
		for (int time = 0;;time += pollingTime){	
			if (time >= waitingTime * 1000) //convert to milliseconds
				break;
			try{
				element.isDisplayed();
			} catch (NoSuchElementException e){
				return;
			}
			Thread.sleep(pollingTime);
		}
		throw new TimeoutException();
	}
	
}
