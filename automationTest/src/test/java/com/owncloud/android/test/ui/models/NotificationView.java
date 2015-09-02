package com.owncloud.android.test.ui.models;

import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.android.AndroidElement;
import io.appium.java_client.pagefactory.AndroidFindBy;
import io.appium.java_client.pagefactory.AppiumFieldDecorator;

import org.openqa.selenium.support.PageFactory;

public class NotificationView {
final AndroidDriver driver;	
	
	@AndroidFindBy(name = "Upload succeeded")
	private static AndroidElement uploadSucceededNotification;
	
	@AndroidFindBy(name = "Uploading ?")
	private static AndroidElement uploadingNotification;
	
	@AndroidFindBy(uiAutomator = "new UiSelector().description(\"Clear all notifications.\")")
	private AndroidElement clearAllNotificationButton;
	
	@AndroidFindBy(uiAutomator = "new UiSelector().className(\"android.widget.FrameLayout\").index(0)")
	private AndroidElement notificationArea;

	
	public NotificationView (AndroidDriver driver) {
		this.driver = driver;
		PageFactory.initElements(new AppiumFieldDecorator(driver), this);
	}
	
	
	public AndroidElement getUploadSucceededNotification() {
		return uploadSucceededNotification;
	}
	
	public AndroidElement getUploadingNotification() {
		return uploadingNotification;
	}
	
	public AndroidElement getClearAllNotificationButton() {
		return clearAllNotificationButton;
	}
	
	public void tapOnClearAllNotification () {
		clearAllNotificationButton.tap(1, 1000);
	}
	
	public void tapOnBottomNotificationArea(){
		//TODO. it is not working
		notificationArea.getSize();
		notificationArea.tap(1, 1000);
	}

}
