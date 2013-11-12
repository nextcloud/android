package com.owncloud.android.oc_framework_test_project;

import java.io.IOException;
import com.owncloud.android.oc_framework.authentication.AccountUtils.AccountNotFoundException;
import com.owncloud.android.oc_framework.network.OwnCloudClientUtils;
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

public class TestActivity extends Activity {
	
	private static final String TAG = "TestActivity";
	
	private Account mAccount;
	private WebdavClient mClient;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_test);

		String accountName = "admin@beta.owncloud.com";
		String accountHost = "beta.owncloud.com";
		String accountUser = "admin";
		String accountPass = "owncloud42";
		String accountType = "owncloud";	
		String accountTypePass = "owncloud.password";
		String authorities = "org.owncloud";

		mAccount = new Account(accountName, accountType);
		AccountManager am = AccountManager.get(this);
		am.addAccountExplicitly(mAccount, accountPass, null);
		

		try {
			mClient = OwnCloudClientUtils.createOwnCloudClient(mAccount, this.getApplicationContext(), authorities);
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

	public RemoteOperationResult createFolder(String remotePath, boolean createFullPath) {
		
		CreateRemoteFolderOperation createOperation = new CreateRemoteFolderOperation(remotePath, createFullPath);
		RemoteOperationResult result =  createOperation.execute(mClient);
		
		return result;
	}
}
