package com.owncloud.android.test.ui.models;

import org.openqa.selenium.support.PageFactory;

import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.android.AndroidElement;
import io.appium.java_client.pagefactory.AndroidFindBy;
import io.appium.java_client.pagefactory.AppiumFieldDecorator;

public class MenuList {

	final AndroidDriver driver;
	
	@AndroidFindBy(name = "Settings")
	private AndroidElement settingsButton;
	
	public MenuList (AndroidDriver driver) {
		this.driver = driver;
		PageFactory.initElements(new AppiumFieldDecorator(driver), this);
	}
	
	public SettingsView clickOnSettingsButton () {
		settingsButton.click();
		SettingsView settingsView = new SettingsView(driver);
		return settingsView;
	}
}
