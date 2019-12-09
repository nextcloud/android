package com.owncloud.android.test.ui.models;

import java.time.Duration;

import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.CacheLookup;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.PageFactory;

import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.android.AndroidElement;
import io.appium.java_client.pagefactory.AppiumFieldDecorator;

public class GrantAccess {
    final AndroidDriver<AndroidElement> driver;

    @CacheLookup
    @FindBy(id = "submit")
    private WebElement grantAcessButton;

    public GrantAccess (AndroidDriver<AndroidElement> driver){
        this.driver = driver;
        PageFactory.initElements(new AppiumFieldDecorator(driver,Duration.ofSeconds(10, 1)), this);
    }

    public void grantAcess(){
        grantAcessButton.click();
    }
}