package androidtest.tests;

import java.io.File;
import java.net.URL;

import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.remote.DesiredCapabilities;

import androidtest.models.LoginForm;
import androidtest.models.MainView;
import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.android.AndroidElement;
import junit.framework.TestCase;

public class CommonTest extends TestCase{
	AndroidDriver driver;
	final int waitingTime = 30;
	
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
	
	protected MainView login(String url, String user, String password) throws InterruptedException {
		LoginForm loginForm = new LoginForm(driver);
		loginForm.typeHostUrl(url);
		loginForm.clickOnUserName();
		waitForTextPresent("Secure connection established", loginForm.getServerStatusTextElement());
		assertTrue(waitForTextPresent("Secure connection established", loginForm.getServerStatusTextElement()));	
		loginForm.typeUserName(user);
		loginForm.typePassword(password);
		return loginForm.clickOnConnectButton();
	}
	
}
