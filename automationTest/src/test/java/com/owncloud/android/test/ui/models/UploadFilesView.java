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

import com.owncloud.android.test.ui.actions.Actions;

public class UploadFilesView{
	final AndroidDriver driver;
	
	@CacheLookup
	@AndroidFindBy(id = "com.owncloud.android:id/list_root")
	private AndroidElement filesLayout;
	
	@CacheLookup
	@AndroidFindBy(id = "com.owncloud.android:id/upload_files_btn_upload")
	private AndroidElement uploadButton;
	
	private AndroidElement fileElement;
	
	public UploadFilesView (AndroidDriver driver) {
		this.driver = driver;
		PageFactory.initElements(new AppiumFieldDecorator(driver), this);
	}
	
	public FileListView clickOnUploadButton () {
		uploadButton.click();
		FileListView fileListView = new FileListView (driver);
		return fileListView;
	}
	
	//change to scrollTillFindElement
	public void scrollTillFindFile (String fileName) {
		fileElement = Actions
				.scrollTillFindElement(fileName,filesLayout,driver);
	}
	
	public void clickOnFileName (String fileName) {
		scrollTillFindFile(fileName);
		fileElement.click();
	}
}
