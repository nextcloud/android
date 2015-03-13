package androidtest.models;

import org.openqa.selenium.support.CacheLookup;
import org.openqa.selenium.support.PageFactory;
import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.android.AndroidElement;
import io.appium.java_client.pagefactory.AndroidFindBy;
import io.appium.java_client.pagefactory.AppiumFieldDecorator;

public class LoginForm {
	final AndroidDriver driver;
	
	@CacheLookup
	@AndroidFindBy(uiAutomator = "new UiSelector().description(\"Server address\")")
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
	
	@AndroidFindBy(uiAutomator = "new UiSelector().description(\"Testing connection\")")
	private AndroidElement serverStatusText;
	
	@AndroidFindBy(uiAutomator = "new UiSelector().description(\"Wrong username or password\")")
	private AndroidElement authStatusText;
	
	public LoginForm (AndroidDriver driver) {
		this.driver = driver;
		PageFactory.initElements(new AppiumFieldDecorator(driver), this);
	}

	public void typeHostUrl (String hostUrl) {
		hostUrlInput.clear();
		hostUrlInput.sendKeys(hostUrl);
		driver.sendKeyEvent(android.view.KeyEvent.KEYCODE_BACK);
	}
	
	public void clickOnUserName () {
		userNameInput.click();
	}
	
	public void typeUserName (String userName) {
		userNameInput.clear();
		userNameInput.sendKeys(userName);
		driver.sendKeyEvent(android.view.KeyEvent.KEYCODE_BACK);
	}
	
	public void typePassword (String password) {
		passwordInput.clear();
		passwordInput.sendKeys(password);
		driver.sendKeyEvent(android.view.KeyEvent.KEYCODE_BACK);
	}
	
	public MainView clickOnConnectButton () {
		connectButton.click();
		MainView mainView = new MainView(driver);
		return mainView;
	}
	
	public AndroidElement getServerStatusTextElement () {
		return serverStatusText;
	}
	
	public AndroidElement getAuthStatusText () {
		return authStatusText;
	}
}
