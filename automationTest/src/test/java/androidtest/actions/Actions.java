package androidtest.actions;

import java.util.HashMap;

import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.remote.RemoteWebElement;

import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.android.AndroidElement;

import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import androidtest.models.CertificatePopUp;
import androidtest.models.ElementMenuOptions;
import androidtest.models.FilesView;
import androidtest.models.LoginForm;
import androidtest.models.MainView;
import androidtest.models.MenuList;
import androidtest.models.NewFolderPopUp;
import androidtest.models.RemoveConfirmationView;
import androidtest.models.SettingsView;
import androidtest.models.WaitAMomentPopUp;
import androidtest.tests.Common;

public class Actions {

	public static MainView login(String url, String user, String password, Boolean isTrusted, AndroidDriver driver) throws InterruptedException {
		LoginForm loginForm = new LoginForm(driver);
		CertificatePopUp certificatePopUp = loginForm.typeHostUrl(url);	
		if(!isTrusted){
			driver.runAppInBackground(3);
			WebDriverWait wait = new WebDriverWait(driver, 30);
			wait.until(ExpectedConditions.visibilityOf(certificatePopUp.getOkButtonElement()));
			certificatePopUp.clickOnOkButton();
		}
		loginForm.typeUserName(user);
		loginForm.typePassword(password);
		//TODO. Assert related to check the connection?
		return loginForm.clickOnConnectButton();
	}

	public static WaitAMomentPopUp createFolder(String folderName, MainView mainView){
		NewFolderPopUp newFolderPopUp = mainView.clickOnNewFolderButton();
		newFolderPopUp.typeNewFolderName(folderName);
		WaitAMomentPopUp waitAMomentPopUp = newFolderPopUp.clickOnNewFolderOkButton();
		//TODO. assert here
		return waitAMomentPopUp;
	}


	public static AndroidElement scrollTillFindElement (String elementName, AndroidElement element, AndroidDriver driver) {
		AndroidElement fileElement;

		if(element.getAttribute("scrollable").equals("true")){
			HashMap<String, String> scrollObject = new HashMap<String, String>();
			scrollObject.put("text", elementName);
			scrollObject.put("element", ( (RemoteWebElement) element).getId());
			driver.executeScript("mobile: scrollTo", scrollObject);
		}
		try {
			fileElement = (AndroidElement) driver.findElementByName(elementName);
		} catch (NoSuchElementException e) {
			fileElement = null;
		}
		return fileElement;
	}


	public static void deleteAccount (MainView mainView) {	
		MenuList menulist = mainView.clickOnMenuButton();
		SettingsView settingView = menulist.clickOnSettingsButton();
		deleteAccount(settingView);
	}

	public static void deleteAccount (SettingsView settingsView) {
		settingsView.tapOnAccountElement(1, 1000);
		settingsView.clickOnDeleteAccountElement();
	}

	public static void clickOnMainLayout(AndroidDriver driver){
		driver.tap(1, 0, 0, 1);
	}

	//TODO. convert deleteFodler and deleteFile in deleteElement
	public static AndroidElement deleteElement(String elementName,  MainView mainView, AndroidDriver driver) throws Exception{
		AndroidElement fileElement;
		WaitAMomentPopUp waitAMomentPopUp;
		try{
			fileElement = (AndroidElement) driver.findElementByName(elementName);
			ElementMenuOptions menuOptions = mainView.longPressOnElement(elementName);
			RemoveConfirmationView removeConfirmationView = menuOptions.clickOnRemove();;
			waitAMomentPopUp = removeConfirmationView.clickOnRemoteAndLocalButton();
			Common.waitTillElementIsNotPresent(waitAMomentPopUp.getWaitAMomentTextElement(), 100);
		}catch(NoSuchElementException e){
			fileElement=null;
		}
		return fileElement;
	}

	public static MainView uploadFile(String elementName,  MainView mainView) throws InterruptedException{
		mainView.clickOnUploadButton();
		FilesView filesView = mainView.clickOnFilesElementUploadFile();
		filesView.clickOnFileName(elementName);
		MainView mainViewAfterUploadFile = filesView.clickOnUploadButton();
		//TO DO. detect when the file is successfully uploaded
		Thread.sleep(15000);
		return mainViewAfterUploadFile; 
	}


}
