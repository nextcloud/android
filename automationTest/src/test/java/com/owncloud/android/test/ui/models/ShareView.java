package com.owncloud.android.test.ui.models;

import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.android.AndroidElement;
import io.appium.java_client.pagefactory.AppiumFieldDecorator;

import org.openqa.selenium.support.CacheLookup;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.PageFactory;

public class ShareView {
	final AndroidDriver driver;
	
	@CacheLookup
	@FindBy(id = "android:id/select_dialog_listview")
	private AndroidElement listViewLayout;
	
	public ShareView (AndroidDriver driver) {
		this.driver = driver;
		PageFactory.initElements(new AppiumFieldDecorator(driver), this);
	}
	
	public AndroidElement getListViewLayout () {
		return listViewLayout;
	}
	
	
}
