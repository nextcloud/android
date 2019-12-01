package com.owncloud.android.test.ui.models;

import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.openqa.selenium.By;
import org.openqa.selenium.support.CacheLookup;
import org.openqa.selenium.support.PageFactory;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.android.AndroidElement;
import io.appium.java_client.pagefactory.AndroidFindBy;
import io.appium.java_client.pagefactory.AppiumFieldDecorator;

public class AuthOptions {
	final AndroidDriver driver;
	
	@CacheLookup
	@AndroidFindBy(uiAutomator = "new UiSelector()"
			+ ".className(\"android.widget.Button\")")
	private AndroidElement connectButton;
    
    public AuthOptions (AndroidDriver driver){
        this.driver = driver;
		PageFactory.initElements(new AppiumFieldDecorator(driver,5, TimeUnit.SECONDS), this);
    }
    public LoginForm SinginWithWeb(){
        connectButton.click();
       
        WebDriverWait wait = new WebDriverWait(driver, 30);
        wait.until(ExpectedConditions
                        .visibilityOfElementLocated(By.className("android.webkit.WebView")));                             
        Set<String> contextNames = driver.getContextHandles();
        driver.context((String) contextNames.toArray()[1]);

        LoginForm loginForm = new LoginForm(driver);
        return loginForm;
    }
}