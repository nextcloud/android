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

package com.owncloud.android.test.ui.actions;

import java.util.HashMap;

import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.ScreenOrientation;
import org.openqa.selenium.remote.RemoteWebElement;
import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.android.AndroidElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import com.owncloud.android.test.ui.models.CertificatePopUp;
import com.owncloud.android.test.ui.models.ElementMenuOptions;
import com.owncloud.android.test.ui.models.GmailSendMailView;
import com.owncloud.android.test.ui.models.ShareView;
import com.owncloud.android.test.ui.models.UploadFilesView;
import com.owncloud.android.test.ui.models.LoginForm;
import com.owncloud.android.test.ui.models.FileListView;
import com.owncloud.android.test.ui.models.MenuList;
import com.owncloud.android.test.ui.models.NewFolderPopUp;
import com.owncloud.android.test.ui.models.RemoveConfirmationView;
import com.owncloud.android.test.ui.models.SettingsView;
import com.owncloud.android.test.ui.models.WaitAMomentPopUp;
import com.owncloud.android.test.ui.testSuites.Common;
import com.owncloud.android.test.ui.testSuites.Config;

public class Actions {

	public static FileListView login(String url, String user, String password,
			Boolean isTrusted, AndroidDriver driver) 
					throws InterruptedException {
		LoginForm loginForm = new LoginForm(driver);
		CertificatePopUp certificatePopUp = loginForm.typeHostUrl(url);	
		if(!isTrusted){
			WebDriverWait wait = new WebDriverWait(driver, 30);
			//sometimes the certificate has been already accept 
			//and it doesn't appear again
			try {
				wait.until(ExpectedConditions
						.visibilityOf(certificatePopUp.getOkButtonElement()));
				//we need to repaint the screen 
				//because of some element are misplaced
				driver.rotate(ScreenOrientation.LANDSCAPE);
				driver.rotate(ScreenOrientation.PORTRAIT);
				certificatePopUp.clickOnOkButton();
			}catch (NoSuchElementException e) {

			}

		}
		loginForm.typeUserName(user);
		loginForm.typePassword(password);
		//TODO. Assert related to check the connection?
		return loginForm.clickOnConnectButton();
	}

	public static WaitAMomentPopUp createFolder(String folderName,
			FileListView fileListView){
		NewFolderPopUp newFolderPopUp = fileListView.clickOnNewFolderButton();
		newFolderPopUp.typeNewFolderName(folderName);
		WaitAMomentPopUp waitAMomentPopUp = newFolderPopUp
				.clickOnNewFolderOkButton();
		//TODO. assert here
		return waitAMomentPopUp;
	}


	public static AndroidElement scrollTillFindElement (String elementName,
			AndroidElement element, AndroidDriver driver) {
		AndroidElement fileElement;

		if(element.getAttribute("scrollable").equals("true")){
			HashMap<String, String> scrollObject = new HashMap<String,String>();
			scrollObject.put("text", elementName);
			scrollObject.put("element", ( (RemoteWebElement) element).getId());
			driver.executeScript("mobile: scrollTo", scrollObject);
		}
		try {
			fileElement = (AndroidElement) driver
					.findElementByName(elementName);
		} catch (NoSuchElementException e) {
			fileElement = null;
		}
		return fileElement;
	}


	public static void deleteAccount (int accountPosition,FileListView fileListView) {	
		MenuList menulist = fileListView.clickOnMenuButton();
		SettingsView settingView = menulist.clickOnSettingsButton();
		deleteAccount(accountPosition,settingView);
	}

	public static void deleteAccount (int accountPosition, SettingsView settingsView) {
		settingsView.tapOnAccountElement(accountPosition,1, 1000);
		settingsView.clickOnDeleteAccountElement();
	}

	public static void clickOnMainLayout(AndroidDriver driver){
		driver.tap(1, 0, 0, 1);
	}


	public static AndroidElement deleteElement(String elementName,  
			FileListView fileListView, AndroidDriver driver) throws Exception{
		AndroidElement fileElement;
		WaitAMomentPopUp waitAMomentPopUp;
		try{
			//To open directly the "file list view" and
			//we don't need to know in which view we are
			driver.startActivity("com.owncloud.android",
					".ui.activity.FileDisplayActivity");
			fileElement = (AndroidElement) driver
					.findElementByName(elementName);
			ElementMenuOptions menuOptions = fileListView
					.longPressOnElement(elementName);
			RemoveConfirmationView removeConfirmationView = menuOptions
					.clickOnRemove();;
					waitAMomentPopUp = removeConfirmationView
							.clickOnRemoteAndLocalButton();
					Common.waitTillElementIsNotPresent(
							waitAMomentPopUp.getWaitAMomentTextElement(), 100);
		}catch(NoSuchElementException e){
			fileElement=null;
		}
		return fileElement;
	}

	public static AndroidElement shareLinkElementByGmail(String elementName,  
			FileListView fileListView, AndroidDriver driver, Common common) 
					throws Exception{
		try{
			//To open directly the "file list view" and
			//we don't need to know in which view we are
			driver.startActivity("com.owncloud.android",
					".ui.activity.FileDisplayActivity");
			ElementMenuOptions menuOptions = fileListView
					.longPressOnElement(elementName);
			ShareView shareView = menuOptions.clickOnShareLinkElement();
			Actions.scrollTillFindElement("Gmail", shareView
					.getListViewLayout(), driver).click();
			GmailSendMailView gmailSendMailView = new GmailSendMailView(driver);
			gmailSendMailView.typeToEmailAdress(Config.gmailAccount);
			gmailSendMailView.clickOnSendButton();
			Common.waitTillElementIsNotPresentWithoutTimeout(fileListView
					.getProgressCircular(), 1000);
			common.wait.until(ExpectedConditions.visibilityOf(
					fileListView.getFileElementLayout()
					.findElement(By.id(FileListView
							.getSharedElementIndicator()))));

		}catch(NoSuchElementException e){
			return null;
		}
		return (AndroidElement) fileListView.getFileElementLayout()
				.findElement(By.id(FileListView.getSharedElementIndicator()));
	}

	public static AndroidElement shareLinkElementByCopyLink(String elementName,  
			FileListView fileListView, AndroidDriver driver, Common common) 
					throws Exception{
		try{
			//To open directly the "file list view" and
			//we don't need to know in which view we are
			driver.startActivity("com.owncloud.android",
					".ui.activity.FileDisplayActivity");
			ElementMenuOptions menuOptions = fileListView
					.longPressOnElement(elementName);
			ShareView shareView = menuOptions.clickOnShareLinkElement();
			Actions.scrollTillFindElement("Copy link", shareView.getListViewLayout(), 
					driver).click();
			WaitAMomentPopUp waitAMomentPopUp = new WaitAMomentPopUp(driver);
			Common.waitTillElementIsNotPresentWithoutTimeout(waitAMomentPopUp
					.getWaitAMomentTextElement(), 100);
			common.wait.until(ExpectedConditions.visibilityOf(
					fileListView.getFileElementLayout()
					.findElement(By.id(FileListView.getSharedElementIndicator()))));
		}catch(NoSuchElementException e){
			return null;
		}
		return (AndroidElement) fileListView.getFileElementLayout()
				.findElement(By.id(FileListView.getSharedElementIndicator()));
	}
	
	
	public static void unshareLinkElement(String elementName,  
			FileListView fileListView, AndroidDriver driver, Common common) 
					throws Exception{
		try{
			//To open directly the "file list view" and
			//we don't need to know in which view we are
			driver.startActivity("com.owncloud.android",
					".ui.activity.FileDisplayActivity");
			ElementMenuOptions menuOptions = fileListView
					.longPressOnElement(elementName);
			WaitAMomentPopUp waitAMomentPopUp = menuOptions
					.clickOnUnshareLinkElement();
			Common.waitTillElementIsNotPresentWithoutTimeout(waitAMomentPopUp
					.getWaitAMomentTextElement(), 100);
			Common.waitTillElementIsNotPresent((AndroidElement) fileListView
					.getFileElementLayout()
					.findElement(By.id(FileListView.getSharedElementIndicator())
					),100);
		}catch(NoSuchElementException e){

		}
	}


	public static FileListView uploadFile(String elementName,
			FileListView fileListView) throws InterruptedException{
		fileListView.clickOnUploadButton();
		UploadFilesView uploadFilesView = fileListView
				.clickOnFilesElementUploadFile();
		uploadFilesView.clickOnFileName(elementName);
		FileListView fileListViewAfterUploadFile = uploadFilesView
				.clickOnUploadButton();
		//TO DO. detect when the file is successfully uploaded
		Thread.sleep(15000);
		return fileListViewAfterUploadFile; 
	}
	
}
