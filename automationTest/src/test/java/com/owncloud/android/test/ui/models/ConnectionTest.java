package com.owncloud.android.test.ui.models;

import java.util.concurrent.TimeUnit;

import org.openqa.selenium.support.CacheLookup;
import org.openqa.selenium.support.PageFactory;

import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.android.AndroidElement;
import io.appium.java_client.pagefactory.AndroidFindBy;
import io.appium.java_client.pagefactory.AppiumFieldDecorator;

public class ConnectionTest {
	final AndroidDriver driver;
	
	@CacheLookup
	@AndroidFindBy(uiAutomator = "new UiSelector()"
			+ ".description(\"Server address\")")
	private AndroidElement hostUrlInput;
	
	@CacheLookup
	@AndroidFindBy(uiAutomator ="new UiSelector()" 
			+ ".resourceId(\"com.nextcloud.client:id/testServerButton\")")
    private AndroidElement connectButton;
    
    public ConnectionTest (AndroidDriver driver){
        this.driver = driver;
		PageFactory.initElements(new AppiumFieldDecorator(driver,5, TimeUnit.SECONDS), this);
    }

    public CertificatePopUp ServerConnectionOK(String hostUrl){
        hostUrlInput.clear();
        hostUrlInput.sendKeys(hostUrl + "\n");
        connectButton.click();
		CertificatePopUp certificatePopUp = new CertificatePopUp(driver);
		return certificatePopUp;
    }
}