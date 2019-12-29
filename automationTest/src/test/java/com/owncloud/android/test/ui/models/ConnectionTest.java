package com.owncloud.android.test.ui.models;

import java.time.Duration;

import org.openqa.selenium.support.CacheLookup;
import org.openqa.selenium.support.PageFactory;

import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.android.AndroidElement;
import io.appium.java_client.pagefactory.AndroidFindBy;
import io.appium.java_client.pagefactory.AppiumFieldDecorator;

public class ConnectionTest {
	final AndroidDriver<AndroidElement> driver;
	
	@CacheLookup
	@AndroidFindBy(uiAutomator = "new UiSelector()"
			+ ".description(\"Server address\")")
	private AndroidElement hostUrlInput;
	
	@CacheLookup
	@AndroidFindBy(uiAutomator ="new UiSelector()" 
			+ ".resourceId(\"com.nextcloud.client:id/testServerButton\")")
    private AndroidElement connectButton;
    
    public ConnectionTest (AndroidDriver<AndroidElement> driver){
        this.driver = driver;
		PageFactory.initElements(new AppiumFieldDecorator(driver,Duration.ofSeconds(10, 1)), this);
    }

    public void ServerConnectionOK(String hostUrl){
        hostUrlInput.clear();
        hostUrlInput.sendKeys(hostUrl + "\n");
		connectButton.click();
    }
}