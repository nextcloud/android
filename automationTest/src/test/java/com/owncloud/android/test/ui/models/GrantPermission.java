package com.owncloud.android.test.ui.models;

import org.openqa.selenium.support.PageFactory;

import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.android.AndroidElement;
import io.appium.java_client.pagefactory.AndroidFindBy;
import io.appium.java_client.pagefactory.AppiumFieldDecorator;

public class GrantPermission {
	final AndroidDriver driver;	
	
	@AndroidFindBy(uiAutomator = "new UiSelector()" 
	+ ".resourceId(\"com.android.packageinstaller:id/permission_allow_button\")")
	private AndroidElement grantPermissionButton;
	
	public GrantPermission (AndroidDriver driver) {
		this.driver = driver;
		PageFactory.initElements(new AppiumFieldDecorator(driver), this);
	}
	
	public FileListView grantPermission () {
		grantPermissionButton.click();
		FileListView fileListView = new FileListView(driver);
		return fileListView;
	}
}