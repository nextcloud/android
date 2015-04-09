package androidtest.models;

import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.android.AndroidElement;
import io.appium.java_client.pagefactory.AndroidFindBy;
import io.appium.java_client.pagefactory.AppiumFieldDecorator;

import org.openqa.selenium.support.PageFactory;

public class ElementMenuOptions {

	final AndroidDriver driver;
	
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
	
	public AppDetailsView clickOnDetails () {
		detailsFileElement.click();
		AppDetailsView appDetailsView = new AppDetailsView(driver);
		return appDetailsView;
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
}
