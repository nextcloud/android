package com.owncloud.android.test.ui.models;


import org.openqa.selenium.support.CacheLookup;
import org.openqa.selenium.support.PageFactory;

import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.android.AndroidElement;
import io.appium.java_client.pagefactory.AndroidFindBy;
import io.appium.java_client.pagefactory.AppiumFieldDecorator;

public class FirstRun {
    final AndroidDriver<AndroidElement> driver;

    @CacheLookup
    @AndroidFindBy(uiAutomator = "new UiSelector()" 
        + ".resourceId(\"com.nextcloud.android.qa:id/login\")")
    private AndroidElement loginButton;

    public FirstRun (AndroidDriver<AndroidElement> driver){
        this.driver = driver;
        PageFactory.initElements(new AppiumFieldDecorator(driver),this);
    }

    public ConnectionTest ChooseLogin(){
        loginButton.click();
        ConnectionTest connectionTest = new ConnectionTest(driver);
        return connectionTest;
    }
}