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
package eu.alefzero.owncloud.ui.activity;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Vector;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceScreen;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.View;
import android.widget.AdapterView.AdapterContextMenuInfo;

import com.actionbarsherlock.app.SherlockPreferenceActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;

import eu.alefzero.owncloud.OwnCloudSession;
import eu.alefzero.owncloud.R;
import eu.alefzero.owncloud.authenticator.AccountAuthenticator;
import eu.alefzero.owncloud.authenticator.AuthUtils;
import eu.alefzero.owncloud.db.DbHandler;

/**
 * An Activity that allows the user to change the application's settings.
 * @author Bartek Przybylski
 *
 */
public class Preferences extends SherlockPreferenceActivity implements OnPreferenceChangeListener {
  private static final String TAG = "OwnCloudPreferences";
  private final int mNewSession = 47;
  private final int mEditSession = 48;
  private DbHandler mDbHandler;
  private Vector<OwnCloudSession> mSessions;
  private Account[] mAccounts;
  private ListPreference mAccountList;
  private int mSelectedMenuItem;
  
  @Override
  public void onCreate(Bundle savedInstanceState){
    super.onCreate(savedInstanceState);
    mDbHandler = new DbHandler(getBaseContext());
    mSessions = new Vector<OwnCloudSession>();
    addPreferencesFromResource(R.xml.preferences);
    registerForContextMenu(getListView());
    populateAccountList();
    //populateSessionList();
  }
  
  private void populateSessionList() {
    mSessions.clear();
    mSessions = mDbHandler.getSessionList();
    PreferenceScreen ps = getPreferenceScreen();
    ps.removeAll();
    addPreferencesFromResource(R.xml.preferences);
    for (int i = 0; i < mSessions.size(); i++) {
      Preference preference = new Preference(getBaseContext());
      preference.setTitle(mSessions.get(i).getName());
      URI uri;
      try {
        uri = new URI(mSessions.get(i).getUrl());
      } catch (URISyntaxException e) {
        e.printStackTrace(); // should never happen
        continue;
      }
      preference.setSummary(uri.getScheme() + "://" + uri.getHost()+uri.getPath());
      ps.addPreference(preference);
    }
  }
  
  /**
   * Populates the account selector
   */
  private void populateAccountList(){
	  AccountManager accMan = AccountManager.get(this);
	  mAccounts = accMan.getAccountsByType(AccountAuthenticator.ACCOUNT_TYPE);
	  mAccountList = (ListPreference) findPreference("select_oc_account");
	  mAccountList.setOnPreferenceChangeListener(this);
	  
	  // Display the name of the current account if there is any
	  Account defaultAccount = AuthUtils.getCurrentOwnCloudAccount(this);
	  if(defaultAccount != null){
		  mAccountList.setSummary(defaultAccount.name);
	  }
	  
	  // Transform accounts into array of string for preferences to use
	  String[] accNames = new String[mAccounts.length];
	  for(int i = 0; i < mAccounts.length; i++){
		  Account account = mAccounts[i];
		  accNames[i] = account.name;
	  }
	  
	  mAccountList.setEntries(accNames);
	  mAccountList.setEntryValues(accNames);
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    super.onCreateOptionsMenu(menu);
    MenuInflater inflater = getSherlock().getMenuInflater();
    inflater.inflate(R.menu.prefs_menu, menu);
    return true;
  }
  
  @Override
  public boolean onMenuItemSelected(int featureId, MenuItem item) {
    super.onMenuItemSelected(featureId, item);
    Intent intent;
    
    switch (item.getItemId()) {
      case R.id.addSessionItem:
        intent = new Intent(this, PreferencesNewSession.class);
        startActivityForResult(intent, mNewSession);
        break;
      case R.id.SessionContextEdit:
        intent = new Intent(this, PreferencesNewSession.class);
        intent.putExtra("sessionId", mSessions.get(mSelectedMenuItem).getEntryId());
        intent.putExtra("sessionName", mSessions.get(mSelectedMenuItem).getName());
        intent.putExtra("sessionURL", mSessions.get(mSelectedMenuItem).getUrl());
        startActivityForResult(intent, mEditSession);
        break;
      case R.id.SessionContextRemove:
        OwnCloudSession ocs = mSessions.get(mSelectedMenuItem);
        mDbHandler.removeSessionWithId(ocs.getEntryId());
        mSessions.remove(ocs);
        getPreferenceScreen().removePreference(getPreferenceScreen().getPreference(mSelectedMenuItem+1));
        break;
      default:
        Log.w(TAG, "Unknown menu item triggered");
        return false;
    }
    return true;
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
    if (resultCode == Activity.RESULT_OK) {
      switch (requestCode) {
        case mNewSession:
          mDbHandler.addSession(data.getStringExtra("sessionName"), 
                                data.getStringExtra("sessionURL"));
          getPreferenceScreen().removeAll();
          addPreferencesFromResource(R.xml.preferences);
          populateSessionList();
          break;
        case mEditSession:
          mDbHandler.changeSessionFields(data.getIntExtra("sessionId", -1),
                                         data.getStringExtra("sessionName"),
                                         data.getStringExtra("sessionURL"));
          populateSessionList();
          break;
      }
    }
  }

  @Override
  public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
    super.onCreateContextMenu(menu, v, menuInfo);
    AdapterContextMenuInfo info = (AdapterContextMenuInfo) menuInfo;
    mSelectedMenuItem = info.position-1;
    menu.setHeaderTitle(mSessions.get(mSelectedMenuItem).getName());
    
    MenuInflater inflater = getSherlock().getMenuInflater();
    inflater.inflate(R.menu.session_context_menu, (Menu) menu);
    
  }
  
  @Override
  protected void onDestroy() {
    mDbHandler.close();
    super.onDestroy();
  }

@Override
/**
 * Updates the summary of the account selector after a new account has 
 * been selected
 */
public boolean onPreferenceChange(Preference preference, Object newValue) {
	if(preference.equals(mAccountList)) {
		mAccountList.setSummary(newValue.toString());
	}
	return true;
}
  
}
