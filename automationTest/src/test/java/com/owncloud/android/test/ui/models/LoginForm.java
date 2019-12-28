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

import java.time.Duration;

import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.CacheLookup;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.PageFactory;

import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.android.AndroidElement;
import io.appium.java_client.pagefactory.AppiumFieldDecorator;

public class LoginForm {
	final AndroidDriver<AndroidElement> driver;
	
	@CacheLookup
	@FindBy(css = "#user")
	private WebElement userNameEdit;
	
	@CacheLookup
	@FindBy(css = "#password")
	private WebElement passwordEdit;

	@CacheLookup
	@FindBy(css = "#submit-form") 
	private AndroidElement loginButton;

	
	public LoginForm (AndroidDriver<AndroidElement> driver) {
		this.driver = driver;
		PageFactory.initElements(new AppiumFieldDecorator(driver,Duration.ofSeconds(60, 1)), this);
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
		userNameEdit.sendKeys(usr);
	}

	private void typePassword (String password) {
		passwordEdit.clear();
		passwordEdit.sendKeys(password);
	}
}
