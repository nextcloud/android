package com.owncloud.android.test.ui.models;

import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.android.AndroidElement;
import io.appium.java_client.pagefactory.AndroidFindBy;
import io.appium.java_client.pagefactory.AppiumFieldDecorator;
import org.openqa.selenium.support.CacheLookup;
import org.openqa.selenium.support.PageFactory;

import com.owncloud.android.test.ui.actions.Actions;
import com.owncloud.android.test.ui.testSuites.Common;


public class ImageView {
	final AndroidDriver driver;

	@CacheLookup
	@AndroidFindBy(uiAutomator = "new UiSelector().description(\"More options\")")
	private AndroidElement optionsButton;

	@AndroidFindBy(name = "Share")
	private AndroidElement shareButton;

	@AndroidFindBy(name = "ownCloud")
	private AndroidElement ownCloudButton;

	@AndroidFindBy(name = "Share with ownCloud")
	private AndroidElement shareWithOwnCloudButton;

	@AndroidFindBy(name = "Just once")
	private AndroidElement justOnceButton;

	@AndroidFindBy(id = "android:id/resolver_list")
	private AndroidElement sharingAppsLayout;

	public ImageView (AndroidDriver driver) {
		this.driver = driver;
		PageFactory.initElements(new AppiumFieldDecorator(driver), this);
	}

	public void clickOnOptionsButton(){
		optionsButton.click();
	}

	public void clickOnShareButton(){
		shareButton.click();
	}

	public void clickOnOwnCloudButton(){
		if(Common.isElementPresent(ownCloudButton)){
			Actions.scrollTillFindElement("ownCloud", sharingAppsLayout, driver);
			ownCloudButton.click();
		}else if(Common.isElementPresent(shareWithOwnCloudButton)){}
	}

	public void clickOnJustOnceButton(){
		justOnceButton.click();
	}
}
