package androidtest.actions;

import io.appium.java_client.android.AndroidDriver;
import androidtest.models.MainView;
import androidtest.models.SettingsView;

public class Actions {
	
	public static void deleteAccount (MainView mainView) {	
		mainView.clickOnMenuButton();
		SettingsView settingView = mainView.clickOnSettingsButton();
		deleteAccount(settingView);
		
	}
	
	public static void deleteAccount (SettingsView settingsView) {
		settingsView.tapOnAccountElement(1, 1000);
		settingsView.clickOnDeleteAccountElement();
	}
	
	public static void clickOnMainLayout(AndroidDriver driver){
		driver.tap(1, 0, 0, 1);
	}
	
	
}
