package com.owncloud.android.test.ui.models;

import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.android.AndroidElement;
import io.appium.java_client.pagefactory.AndroidFindBy;
import io.appium.java_client.pagefactory.AppiumFieldDecorator;

import org.openqa.selenium.support.CacheLookup;
import org.openqa.selenium.support.PageFactory;

import com.owncloud.android.test.ui.testSuites.Config;

public class GmailEmailView {
	final AndroidDriver driver;
	
	@CacheLookup
	@AndroidFindBy(name = Config.fileToTestSendByEmailName)
	private AndroidElement fileButton;
		
	public GmailEmailView (AndroidDriver driver) {
		this.driver = driver;
		PageFactory.initElements(new AppiumFieldDecorator(driver), this);
	}
	
	public ImageView clickOnfileButton (){
		fileButton.click();
		ImageView imageView = new ImageView(driver);
		return imageView;
	}
}