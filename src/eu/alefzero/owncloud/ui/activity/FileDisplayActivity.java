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
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.TextView;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.ActionBar.OnNavigationListener;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;

import eu.alefzero.owncloud.AccountUtils;
import eu.alefzero.owncloud.R;
import eu.alefzero.owncloud.authenticator.AccountAuthenticator;
import eu.alefzero.owncloud.datamodel.DataStorageManager;
import eu.alefzero.owncloud.datamodel.FileDataStorageManager;
import eu.alefzero.owncloud.datamodel.OCFile;
import eu.alefzero.owncloud.ui.fragment.FileListFragment;
import eu.alefzero.webdav.WebdavClient;

/**
 * Displays, what files the user has available in his ownCloud.
 * 
 * @author Bartek Przybylski
 * 
 */

public class FileDisplayActivity extends SherlockFragmentActivity implements
		OnNavigationListener, OnClickListener {
	private ArrayAdapter<String> mDirectories;
	private DataStorageManager mStorageManager;

	private static final int DIALOG_SETUP_ACCOUNT = 0;
	private static final int DIALOG_CREATE_DIR = 1;

	public void pushPath(String path) {
		mDirectories.insert(path, 0);
	}

	public boolean popPath() {
		mDirectories.remove(mDirectories.getItem(0));
		return !mDirectories.isEmpty();
	}

	@Override
  protected Dialog onCreateDialog(int id) {
    Dialog dialog;
    AlertDialog.Builder builder;
    switch(id){
    case DIALOG_SETUP_ACCOUNT:
      builder = new AlertDialog.Builder(this);
      builder.setTitle(R.string.main_tit_accsetup);
      builder.setMessage(R.string.main_wrn_accsetup);
      builder.setCancelable(false);
      builder.setPositiveButton(R.string.common_ok, this);
      builder.setNegativeButton(R.string.common_cancel, this);
      dialog = builder.create();
      break;
    case DIALOG_CREATE_DIR:
    {
      builder = new Builder(this);
      final EditText dirName = new EditText(getBaseContext());
      final Account a = AccountUtils.getCurrentOwnCloudAccount(this);
      builder.setView(dirName);
      builder.setTitle(R.string.uploader_info_dirname);
      dirName.setTextColor(R.color.setup_text_typed);

      builder.setPositiveButton(R.string.common_ok, new OnClickListener() {
        public void onClick(DialogInterface dialog, int which) {
          String s = dirName.getText().toString();
          if (s.trim().isEmpty()) {
            dialog.cancel();
            return;
          }

          String path = "";
          for (int i = mDirectories.getCount() - 2; i >= 0; --i) {
            path += "/" + mDirectories.getItem(i);
          }
          OCFile parent = mStorageManager.getFileByPath(path + "/");
          path += s + "/";
          Thread thread = new Thread(new DirectoryCreator(path, a));
          thread.start();
          
          OCFile new_file = new OCFile(path);
          new_file.setMimetype("DIR");
          new_file.setParentId(parent.getParentId());
          mStorageManager.saveFile(new_file);

          dialog.dismiss();
        }
      });
      builder.setNegativeButton(R.string.common_cancel,
          new OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
              dialog.cancel();
            }
          });
    }
    default: 
      dialog = null;
    }
      
    return dialog;
  }

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if(!accountsAreSetup()){
      showDialog(DIALOG_SETUP_ACCOUNT);
      return;
    }

		mDirectories = new CustomArrayAdapter<String>(this,
				R.layout.sherlock_spinner_dropdown_item);
		mDirectories.add("/");
		setContentView(R.layout.files);
		mStorageManager = new FileDataStorageManager(AccountUtils.getCurrentOwnCloudAccount(this), getContentResolver());
		ActionBar action_bar = getSupportActionBar();
		action_bar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
		action_bar.setDisplayShowTitleEnabled(false);
		action_bar.setListNavigationCallbacks(mDirectories, this);
		action_bar.setDisplayHomeAsUpEnabled(true);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.settingsItem: {
			Intent i = new Intent(this, Preferences.class);
			startActivity(i);
			break;
		}
		case R.id.createDirectoryItem: {
			showDialog(DIALOG_CREATE_DIR);
			break;
		}
		case android.R.id.home: {
			onBackPressed();
			break;
		}
			
		}
		return true;
	}
	
	@Override
	public void onBackPressed(){
		if(mDirectories.getCount() == 1) {
		  finish();
			return;
		}
		popPath();
		((FileListFragment) getSupportFragmentManager().findFragmentById(R.id.fileList))
				.onNavigateUp();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getSherlock().getMenuInflater();
		inflater.inflate(R.menu.menu, menu);
		return true;
	}

	 @Override
	  protected void onRestoreInstanceState(Bundle savedInstanceState) {
	    super.onRestoreInstanceState(savedInstanceState);
	    // Check, if there are ownCloud accounts
	    if(!accountsAreSetup()){
	      showDialog(DIALOG_SETUP_ACCOUNT);
	    }
	  }
	 
	 @Override
	  protected void onStart() {
	    super.onStart();
	    // Check, if there are ownCloud accounts
	    if(!accountsAreSetup()){
	      showDialog(DIALOG_SETUP_ACCOUNT);
	    }
	 }
	    
	 @Override
	protected void onResume() {
	  super.onResume();
	  if(!accountsAreSetup()){
      showDialog(DIALOG_SETUP_ACCOUNT);
    }
	}
	    
	@Override
	public boolean onNavigationItemSelected(int itemPosition, long itemId) {
		int i = itemPosition;
		while (i-- != 0) {
			onBackPressed();
		}
		return true;
	}

	private class DirectoryCreator implements Runnable {
		private String mTargetPath;
		private Account mAccount;
		private AccountManager mAm;

		public DirectoryCreator(String targetPath, Account account) {
			mTargetPath = targetPath;
			mAccount = account;
			mAm = (AccountManager) getSystemService(ACCOUNT_SERVICE);
		}

		@Override
		public void run() {
			WebdavClient wdc = new WebdavClient(Uri.parse(mAm.getUserData(
					mAccount, AccountAuthenticator.KEY_OC_URL)));

			String username = mAccount.name.substring(0,
					mAccount.name.lastIndexOf('@'));
			String password = mAm.getPassword(mAccount);

			wdc.setCredentials(username, password);
			wdc.allowUnsignedCertificates();
			wdc.createDirectory(mTargetPath);
		}

	}

	// Custom array adapter to override text colors
	private class CustomArrayAdapter<T> extends ArrayAdapter<T> {
		
		public CustomArrayAdapter(FileDisplayActivity ctx,
				int view) {
			super(ctx, view);
		}

		public View getView(int position, View convertView,
                ViewGroup parent) {
            View v = super.getView(position, convertView, parent);

            ((TextView) v).setTextColor(
                    getResources()
                    .getColorStateList(android.R.color.white));
            return v;
        }
		
		public View getDropDownView(int position, View convertView,
                ViewGroup parent) {
            View v = super.getDropDownView(position, convertView,
                    parent);

            ((TextView) v).setTextColor(getResources().getColorStateList(
                            android.R.color.white));

            return v;
        }


		
	}

	 public void onClick(DialogInterface dialog, int which) {
	    // In any case - we won't need it anymore
	    dialog.dismiss();
	    switch(which){
	    case DialogInterface.BUTTON_POSITIVE:
	      Intent intent = new Intent("android.settings.ADD_ACCOUNT_SETTINGS");
	      intent.putExtra("authorities",
	          new String[] { AccountAuthenticator.AUTH_TOKEN_TYPE });
	      startActivity(intent);
	      break;
	    case DialogInterface.BUTTON_NEGATIVE:
	      finish();
	    }
	    
	  }
	
	/**
   * Checks, whether or not there are any ownCloud accounts 
   * setup. 
   *  
   * @return true, if there is at least one account.
   */
  private boolean accountsAreSetup() {
    AccountManager accMan = AccountManager.get(this);
    Account[] accounts = accMan
        .getAccountsByType(AccountAuthenticator.ACCOUNT_TYPE); 
    return accounts.length > 0;
  }
  
  
}
