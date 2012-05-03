/* ownCloud Android client application
 *   Copyright (C) 2011  Bartek Przybylski
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
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

package eu.alefzero.owncloud;

import eu.alefzero.owncloud.authenticator.AccountAuthenticator;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class AccountUtils {
  public static final String WEBDAV_PATH_1_2 = "/webdav/owncloud.php";
  public static final String WEBDAV_PATH_2_0 = "/files/webdav.php";
  public static final String CARDDAV_PATH_2_0 = "/apps/contacts/carddav.php";
  
  /**
   * Can be used to get the currently selected ownCloud account in the preferences
   * 
   * @param context The current appContext
   * @return The current account or null, if there is none yet.
   */
  public static Account getCurrentOwnCloudAccount(Context context){
	  Account[] ocAccounts = AccountManager.get(context).getAccountsByType(AccountAuthenticator.ACCOUNT_TYPE);
	  Account defaultAccount = null;
	  
	  SharedPreferences appPreferences = PreferenceManager.getDefaultSharedPreferences(context);
	  String accountName = appPreferences.getString("select_oc_account", null);
	  
	  if(accountName != null){
		  for(Account account : ocAccounts){
			  if(account.name.equals(accountName)){
				  defaultAccount = account;
				  break;
			  }
		  }
	  } else if (ocAccounts.length != 0) {
	    // we at least need to take first account as fallback
	    defaultAccount = ocAccounts[0];
	  }
	  
	return defaultAccount;
  }
}
