package com.owncloud.android.test.ui.models;

import java.util.concurrent.TimeUnit;

import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.CacheLookup;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.PageFactory;

import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.pagefactory.AppiumFieldDecorator;

public class GrantAccess {
    final AndroidDriver driver;

    @CacheLookup
    @FindBy(id = "submit")
    private WebElement grantAcessButton;

    public GrantAccess (AndroidDriver driver){
        this.driver = driver;
        PageFactory.initElements(new AppiumFieldDecorator(driver,5, TimeUnit.SECONDS), this);
    }

    public void grantAcess(){
        grantAcessButton.click();
    }
}