package com.owncloud.android.oc_framework_test_project;

import java.io.IOException;

import com.owncloud.android.oc_framework.accounts.AccountUtils.AccountNotFoundException;
import com.owncloud.android.oc_framework.network.webdav.OwnCloudClientFactory;
import com.owncloud.android.oc_framework.network.webdav.WebdavClient;
import com.owncloud.android.oc_framework.operations.RemoteOperationResult;
import com.owncloud.android.oc_framework.operations.remote.CreateRemoteFolderOperation;

import android.os.Bundle;
import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.app.Activity;
import android.util.Log;
import android.view.Menu;

/**
 * Activity to test OC framework
 * @author masensio
 *
 */
public class TestActivity extends Activity {
	
	private static final String TAG = "TestActivity";
	
	private Account mAccount = null;
	private WebdavClient mClient;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_test);
		
		// This account must exists on the simulator / device
		String accountHost = "beta.owncloud.com";
		String accountUser = "testandroid";
		String accountName = accountUser + "@"+ accountHost;
		String accountPass = "testandroid";
		String accountType = "owncloud";	

		AccountManager am = AccountManager.get(this);
		
		Account[] ocAccounts = am.getAccountsByType(accountType);
        for (Account ac : ocAccounts) {
           if (ac.name.equals(accountName)) {
        	   mAccount = ac;
        	   break;
            }
        }

//        if (mAccount == null) {
//			mAccount = new Account(accountName, accountType);	
//			am.addAccountExplicitly(mAccount, accountPass, null);
//	        am.setUserData(mAccount, "oc_version",    "5.0.14");
//	        am.setUserData(mAccount, "oc_base_url",   "http://beta.owncloud.com/owncloud");
//        } else {
            Log.d(TAG, "oc_version --->"+ am.getUserData(mAccount, "oc_version") );
            Log.d(TAG, "oc_base_url --->"+ am.getUserData(mAccount, "oc_base_url") );
//        }
        	
        
		try {
			mClient = OwnCloudClientFactory.createOwnCloudClient(mAccount, this.getApplicationContext());
		} catch (OperationCanceledException e) {
			Log.e(TAG, "Error while trying to access to " + mAccount.name, e);
			e.printStackTrace();
		} catch (AuthenticatorException e) {
			Log.e(TAG, "Error while trying to access to " + mAccount.name, e);
			e.printStackTrace();
		} catch (AccountNotFoundException e) {
			Log.e(TAG, "Error while trying to access to " + mAccount.name, e);
			e.printStackTrace();
		} catch (IOException e) {
			Log.e(TAG, "Error while trying to access to " + mAccount.name, e);
			e.printStackTrace();
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.test, menu);
		return true;
	}

	/**
	 * Access to the library method to Create a Folder
	 * @param remotePath
	 * @param createFullPath
	 * @return
	 */
	public RemoteOperationResult createFolder(String remotePath, boolean createFullPath) {
		
		CreateRemoteFolderOperation createOperation = new CreateRemoteFolderOperation(remotePath, createFullPath);
		RemoteOperationResult result =  createOperation.execute(mClient);
		
		return result;
	}
}
