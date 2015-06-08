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

import org.openqa.selenium.support.PageFactory;

public class PassCodeView {
	final AndroidDriver driver;
	
	@AndroidFindBy(uiAutomator = "new UiSelector()"
			+ ".className(\"android.widget.EditText\").index(0)")
	private AndroidElement codeElement1;
	
	@AndroidFindBy(uiAutomator = "new UiSelector()"
			+ ".className(\"android.widget.EditText\").index(1)")
	private AndroidElement codeElement2;
	
	@AndroidFindBy(uiAutomator = "new UiSelector()"
			+ ".className(\"android.widget.EditText\").index(2)")
	private AndroidElement codeElement3;
	
	@AndroidFindBy(uiAutomator = "new UiSelector()"
			+ ".className(\"android.widget.EditText\").index(3)")
	private AndroidElement codeElement4;
	
	@AndroidFindBy(name = "Cancel")
	private AndroidElement cancelButton;
	
	public PassCodeView (AndroidDriver driver) {
		this.driver = driver;
		PageFactory.initElements(new AppiumFieldDecorator(driver), this);
	}
	
	public PassCodeView enterPasscode(String codeNumber1, String codeNumber2, 
			String codeNumber3, String codeNumber4){
		codeElement1
		  .sendKeys(codeNumber1 + codeNumber1 + codeNumber1 + codeNumber1);
		return this;
	}
	public SettingsView reenterPasscode(String codeNumber1,
			String codeNumber2, String codeNumber3, String codeNumber4){
		codeElement1
		   .sendKeys(codeNumber1 + codeNumber1 + codeNumber1 + codeNumber1);
		SettingsView settingsView = new SettingsView(driver);
		return settingsView;
	}

}
