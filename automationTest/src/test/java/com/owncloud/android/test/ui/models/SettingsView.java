package com.owncloud.android.test.ui.models;

import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.android.AndroidElement;
import io.appium.java_client.pagefactory.AndroidFindBy;
import io.appium.java_client.pagefactory.AppiumFieldDecorator;

import org.openqa.selenium.support.CacheLookup;
import org.openqa.selenium.support.PageFactory;

import com.owncloud.android.test.ui.testSuites.Config;

public class SettingsView {
	final AndroidDriver driver;

	@CacheLookup
	@AndroidFindBy(name = Config.userAccount)
	private AndroidElement accountElement;

	@CacheLookup
	@AndroidFindBy(name = Config.userAccount2)
	private AndroidElement accountElement2;

	@AndroidFindBy(name = "Delete account")
	private AndroidElement deleteAccountElement;

	@AndroidFindBy(name = "Change password")
	private AndroidElement changePasswordElement;

	@AndroidFindBy(name = "Add account")
	private AndroidElement addAccountElement;

	@AndroidFindBy(uiAutomator = "new UiSelector().className(\"android.widget.CheckBox\").index(0)")
	private AndroidElement passcodeCheckbox;

	public SettingsView (AndroidDriver driver) {
		this.driver = driver;
		PageFactory.initElements(new AppiumFieldDecorator(driver), this);
	}

	public void tapOnAccountElement (int fingers, int milliSeconds) {
		accountElement.tap(fingers, milliSeconds);
	}


	public void tapOnAddAccount (int fingers, int milliSeconds) {
		addAccountElement.tap(fingers, milliSeconds);
	}

	public LoginForm clickOnDeleteAccountElement () {
		deleteAccountElement.click();
		LoginForm loginForm = new LoginForm(driver);
		return loginForm;
	}

	public LoginForm clickOnChangePasswordElement () {
		changePasswordElement.click();
		LoginForm loginForm = new LoginForm(driver);
		return loginForm;
	}

	public PassCodeView EnablePassCode(){
		if(!passcodeCheckbox.isSelected()){
			passcodeCheckbox.click();
		}
		PassCodeView passcodeview = new PassCodeView(driver);
		return passcodeview;
	}
	
	public PassCodeView DisablePassCode(){
		if(passcodeCheckbox.isSelected()){
			passcodeCheckbox.click();
		}
		PassCodeView passcodeview = new PassCodeView(driver);
		return passcodeview;
	}

}
