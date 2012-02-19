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

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ActionBar.OnNavigationListener;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnCancelListener;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.Menu;
import android.support.v4.view.MenuItem;
import android.view.MenuInflater;
import android.widget.ArrayAdapter;
import eu.alefzero.owncloud.R;
import eu.alefzero.owncloud.R.id;
import eu.alefzero.owncloud.authenticator.AccountAuthenticator;
import eu.alefzero.owncloud.ui.fragment.FileDetail;
import eu.alefzero.owncloud.ui.fragment.FileList;

/**
 * Displays, what files the user has available in his ownCloud.
 * @author Bartek Przybylski
 *
 */

public class FileDisplayActivity extends android.support.v4.app.FragmentActivity implements OnNavigationListener {
  private ArrayAdapter<String> mDirectories;
 
  private static final int DIALOG_CHOOSE_ACCOUNT = 0;
  
  public void pushPath(String path) {
    mDirectories.insert(path, 0);
  }
  
  public boolean popPath() {
    mDirectories.remove(mDirectories.getItem(0));
    return !mDirectories.isEmpty();
  }
  
  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    mDirectories = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item);
    mDirectories.add("/");
    setContentView(R.layout.files);
    getActionBar().setNavigationMode(android.support.v4.app.ActionBar.NAVIGATION_MODE_LIST);
    getActionBar().setDisplayShowTitleEnabled(false);
    getActionBar().setListNavigationCallbacks(mDirectories, this);

    FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
    ft.add(R.id.file_list_container, new FileList());
    if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
      ft.add(R.id.fileDetail, new FileDetail());
    }
    ft.commit();
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
      case R.id.settingsItem :
        Intent i = new Intent(this, Preferences.class);
        startActivity(i);
        break;
    }
    return true;
  }

  @Override
  protected Dialog onCreateDialog(int id) {
    switch (id) {
      case DIALOG_CHOOSE_ACCOUNT:
        return createChooseAccountDialog();
      default:
        throw new IllegalArgumentException("Unknown dialog id: " + id);
    }
  }
  
  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    MenuInflater inflater = getMenuInflater();
    inflater.inflate(R.menu.menu, menu);
    return true;
  }

  private Dialog createChooseAccountDialog() {
    final AccountManager accMan = AccountManager.get(this);
    CharSequence[] items = new CharSequence[accMan.getAccountsByType(AccountAuthenticator.ACCOUNT_TYPE).length];
    int i = 0;
    for (Account a : accMan.getAccountsByType(AccountAuthenticator.ACCOUNT_TYPE)) {
      items[i++] = a.name;
    }
    
    AlertDialog.Builder builder = new AlertDialog.Builder(this);
    builder.setTitle(R.string.common_choose_account);
    builder.setCancelable(true);
    builder.setItems(items, new DialogInterface.OnClickListener() {
        public void onClick(DialogInterface dialog, int item) {
            //mAccount = accMan.getAccountsByType(AccountAuthenticator.ACCOUNT_TYPE)[item];
            dialog.dismiss();
        }
    });
    builder.setOnCancelListener(new OnCancelListener() {
      public void onCancel(DialogInterface dialog) {
        FileDisplayActivity.this.finish();
      }
    });
    AlertDialog alert = builder.create();
    return alert;
  }

  @Override
  public boolean onNavigationItemSelected(int itemPosition, long itemId) {
    int i = itemPosition;
    while (i-- != 0) {
      onBackPressed();
    }
    return true;
  }
  
  @Override
  public void onBackPressed() {
    popPath();
    if (mDirectories.getCount() == 0)
    {
      super.onBackPressed();
      return;
    }
    ((FileList)getSupportFragmentManager().findFragmentById(id.file_list_container)).onBackPressed();
  }
}