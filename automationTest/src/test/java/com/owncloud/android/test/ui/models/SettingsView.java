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

import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.android.AndroidElement;
import io.appium.java_client.pagefactory.AndroidFindBy;
import io.appium.java_client.pagefactory.AppiumFieldDecorator;

import org.openqa.selenium.support.CacheLookup;
import org.openqa.selenium.support.PageFactory;

import com.owncloud.android.test.ui.testSuites.Config;

public class SettingsView {
	final AndroidDriver driver;

	@CacheLookup
	@AndroidFindBy(name = Config.userAccount)
	private AndroidElement accountElement;

	@CacheLookup
	@AndroidFindBy(name = Config.userAccount2)
	private AndroidElement accountElement2;

	@AndroidFindBy(name = "Delete account")
	private AndroidElement deleteAccountElement;

	@AndroidFindBy(name = "Change password")
	private AndroidElement changePasswordElement;

	@AndroidFindBy(name = "Add account")
	private AndroidElement addAccountElement;

	@AndroidFindBy(uiAutomator = "new UiSelector()"
			+ ".className(\"android.widget.CheckBox\").index(0)")
	private AndroidElement passcodeCheckbox;

	public SettingsView (AndroidDriver driver) {
		this.driver = driver;
		PageFactory.initElements(new AppiumFieldDecorator(driver), this);
	}

	public void tapOnAccountElement (int accountPosition, int fingers, int milliSeconds) {
		if(accountPosition==1)
			accountElement.tap(fingers, milliSeconds);
		else
			accountElement2.tap(fingers, milliSeconds);
	}

	public void tapOnAddAccount (int fingers, int milliSeconds) {
		addAccountElement.tap(fingers, milliSeconds);
	}

	public LoginForm clickOnDeleteAccountElement () {
		deleteAccountElement.click();
		LoginForm loginForm = new LoginForm(driver);
		return loginForm;
	}

	public LoginForm clickOnChangePasswordElement () {
		changePasswordElement.click();
		LoginForm loginForm = new LoginForm(driver);
		return loginForm;
	}

	public PassCodeView EnablePassCode(){
		if(!passcodeCheckbox.isSelected()){
			passcodeCheckbox.click();
		}
		PassCodeView passcodeview = new PassCodeView(driver);
		return passcodeview;
	}

	public PassCodeView DisablePassCode(){
		if(passcodeCheckbox.isSelected()){
			passcodeCheckbox.click();
		}
		PassCodeView passcodeview = new PassCodeView(driver);
		return passcodeview;
	}

}
