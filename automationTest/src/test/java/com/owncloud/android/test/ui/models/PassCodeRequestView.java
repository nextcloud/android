package com.owncloud.android.test.ui.models;

import org.openqa.selenium.support.PageFactory;

import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.android.AndroidElement;
import io.appium.java_client.pagefactory.AndroidFindBy;
import io.appium.java_client.pagefactory.AppiumFieldDecorator;

public class PassCodeRequestView {
final AndroidDriver driver;
	
	@AndroidFindBy(uiAutomator = "new UiSelector().className(\"android.widget.EditText\").index(0)")
	private AndroidElement codeElement1;
	
	@AndroidFindBy(uiAutomator = "new UiSelector().className(\"android.widget.EditText\").index(1)")
	private AndroidElement codeElement2;
	
	@AndroidFindBy(uiAutomator = "new UiSelector().className(\"android.widget.EditText\").index(2)")
	private AndroidElement codeElement3;
	
	@AndroidFindBy(uiAutomator = "new UiSelector().className(\"android.widget.EditText\").index(3)")
	private AndroidElement codeElement4;
	
	public PassCodeRequestView (AndroidDriver driver) {
		this.driver = driver;
		PageFactory.initElements(new AppiumFieldDecorator(driver), this);
	}
	
	public void enterPasscode(String codeNumber1, String codeNumber2, String codeNumber3, String codeNumber4){
		codeElement1.sendKeys(codeNumber1 + codeNumber1 + codeNumber1 + codeNumber1);
	}

}
