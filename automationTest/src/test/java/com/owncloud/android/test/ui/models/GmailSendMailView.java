package com.owncloud.android.test.ui.models;

import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.android.AndroidElement;
import io.appium.java_client.pagefactory.AndroidFindBy;
import io.appium.java_client.pagefactory.AppiumFieldDecorator;

import org.openqa.selenium.support.CacheLookup;
import org.openqa.selenium.support.PageFactory;

public class GmailSendMailView {
	final AndroidDriver driver;
	
	@CacheLookup
	@AndroidFindBy(uiAutomator = "new UiSelector().description(\"To\")")
	private AndroidElement toTextField;
	
	@CacheLookup
	@AndroidFindBy(name = "Subject")
	private AndroidElement subjectTextField;
	
	@CacheLookup
	@AndroidFindBy(uiAutomator = "new UiSelector().description(\"Send\")")
	private AndroidElement sendButton;
	
	public GmailSendMailView (AndroidDriver driver) {
		this.driver = driver;
		PageFactory.initElements(new AppiumFieldDecorator(driver), this);
	}
	
	public void typeToEmailAdress (String email) {
		toTextField.sendKeys(email + "\n");
	}

	public void clickOnSendButton () {
		sendButton.click();
	}
		
	public void typeSubject (String subject) {
		subjectTextField.clear();
		subjectTextField.sendKeys(subject);
	}
}
