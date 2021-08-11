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

import org.openqa.selenium.support.CacheLookup;
import org.openqa.selenium.support.PageFactory;

import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.android.AndroidElement;
import io.appium.java_client.pagefactory.AndroidFindBy;
import io.appium.java_client.pagefactory.AppiumFieldDecorator;

public class LoginForm {
	final AndroidDriver driver;
	
	@CacheLookup
	@AndroidFindBy(uiAutomator = "new UiSelector()"
			+ ".description(\"Server address\")")
	private AndroidElement hostUrlInput;
	
	@CacheLookup
	@AndroidFindBy(uiAutomator = "new UiSelector().description(\"Username\")")
	private AndroidElement userNameInput;
	
	@CacheLookup
	@AndroidFindBy(uiAutomator = "new UiSelector().description(\"Password\")")
	private AndroidElement passwordInput;
	
	@CacheLookup
	@AndroidFindBy(uiAutomator = "new UiSelector().description(\"Connect\")")
	private AndroidElement connectButton;
	
	@AndroidFindBy(uiAutomator = "new UiSelector()"
			+ ".description(\"Testing connection\")")
	private AndroidElement serverStatusText;
	
	@AndroidFindBy(uiAutomator = "new UiSelector()"
			+ ".description(\"Wrong username or password\")")
	private AndroidElement authStatusText;
	
	public LoginForm (AndroidDriver driver) {
		this.driver = driver;
		PageFactory.initElements(new AppiumFieldDecorator(driver), this);
	}

	public CertificatePopUp typeHostUrl (String hostUrl) {
		hostUrlInput.clear();
		hostUrlInput.sendKeys(hostUrl + "\n");
		CertificatePopUp certificatePopUp = new CertificatePopUp(driver);
		return certificatePopUp;
	}
	
	public void clickOnUserName () {
		userNameInput.click();
	}
	
	public void typeUserName (String userName) {
		userNameInput.clear();
		//using the \n , it not need to hide the keyboard
		//which sometimes gives problems
		userNameInput.sendKeys(userName + "\n");
		//driver.hideKeyboard();
	}
	
	public void typePassword (String password) {
		passwordInput.clear();
		passwordInput.sendKeys(password + "\n");
		//driver.hideKeyboard();
	}
	
	public FileListView clickOnConnectButton () {
		connectButton.click();
		FileListView fileListView = new FileListView(driver);
		return fileListView;
	}
	
	public AndroidElement gethostUrlInput () {
		return hostUrlInput;
	}
	
	public AndroidElement getUserNameInput () {
		return userNameInput;
	}
	
	public AndroidElement getPasswordInput () {
		return passwordInput;
	}
	
	
	public AndroidElement getServerStatusTextElement () {
		return serverStatusText;
	}
	
	public AndroidElement getAuthStatusText () {
		return authStatusText;
	}
}
