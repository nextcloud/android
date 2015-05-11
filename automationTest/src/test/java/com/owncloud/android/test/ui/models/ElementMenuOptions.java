package com.owncloud.android.test.ui.models;

import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.android.AndroidElement;
import io.appium.java_client.pagefactory.AndroidFindBy;
import io.appium.java_client.pagefactory.AppiumFieldDecorator;

import org.openqa.selenium.support.PageFactory;

public class ElementMenuOptions {

	final AndroidDriver driver;
	
	@AndroidFindBy(name = "Share link")
	private AndroidElement shareLinkElement;
	
	@AndroidFindBy(name = "Details")
	private AndroidElement detailsFileElement;
	
	@AndroidFindBy(name = "Rename")
	private AndroidElement renameFileElement;
	
	@AndroidFindBy(name = "Remove")
	private AndroidElement removeFileElement;
	
	@AndroidFindBy(name = "Move")
	private AndroidElement moveElement;
	
	public ElementMenuOptions (AndroidDriver driver) {
		this.driver = driver;
		PageFactory.initElements(new AppiumFieldDecorator(driver), this);
	}
	
	public FileDetailsView clickOnDetails () {
		detailsFileElement.click();
		FileDetailsView fileDetailsView = new FileDetailsView(driver);
		return fileDetailsView;
	}
	
	public RemoveConfirmationView clickOnRemove () {
		removeFileElement.click();
		RemoveConfirmationView removeConfirmationView = new RemoveConfirmationView(driver);
		return removeConfirmationView;
	}
	

	public MoveView clickOnMove () {
		moveElement.click();
		MoveView moveView = new MoveView(driver);
		return moveView;
	}

	public NewFolderPopUp clickOnRename () {
		renameFileElement.click();
		NewFolderPopUp newFolderPopUp = new NewFolderPopUp(driver);
		return newFolderPopUp;
	}
	
	public ShareView clickOnShareLinkElement () {
		shareLinkElement.click();
		ShareView shareView = new ShareView(driver);
		return shareView;
	}
}
