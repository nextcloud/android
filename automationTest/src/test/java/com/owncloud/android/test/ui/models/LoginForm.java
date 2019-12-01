/**
 *   ownCloud Android client application
 *
 *   @author purigarcia
 *   Copyright (C) 2015 ownCloud Inc.
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License version 2,
 *   as published by the Free Software Foundation.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package com.owncloud.android.test.ui.models;

import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.CacheLookup;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.PageFactory;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.android.AndroidElement;
import io.appium.java_client.pagefactory.AndroidFindBy;
import io.appium.java_client.pagefactory.AppiumFieldDecorator;

public class LoginForm {
	final AndroidDriver driver;
	
	@CacheLookup
	@FindBy(id = "user")
	private WebElement userNameEdit;
	
	@CacheLookup
	@FindBy(id = "password")
	private WebElement passwordEdit;

	@CacheLookup
	@FindBy(id = "submit")
	private AndroidElement loginButton;

	
	public LoginForm (AndroidDriver driver) {
		this.driver = driver;
		PageFactory.initElements(new AppiumFieldDecorator(driver,5, TimeUnit.SECONDS), this);
	}

	public GrantAccess login(String usr,String password){
		typeUserName(usr);
		typePassword(password);
		loginButton.click();
		GrantAccess grantAccess = new GrantAccess(driver);
		return grantAccess;
	}

	private void typeUserName (String usr) {
		userNameEdit.clear();
		userNameEdit.sendKeys(usr + "\n");
	}

	private void typePassword (String password) {
		passwordEdit.clear();
		passwordEdit.sendKeys(password + "\n");
	}
}
