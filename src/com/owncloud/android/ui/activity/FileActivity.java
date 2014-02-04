/* ownCloud Android client application
 *   Copyright (C) 2011  Bartek Przybylski
 *   Copyright (C) 2012-2013 ownCloud Inc.
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License version 2,
 *   as published by the Free Software Foundation.
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

package com.owncloud.android.ui.activity;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.accounts.OperationCanceledException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.webkit.MimeTypeMap;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.owncloud.android.MainApp;
import com.owncloud.android.R;
import com.owncloud.android.authentication.AccountUtils;
import com.owncloud.android.datamodel.OCFile;

import com.owncloud.android.lib.accounts.OwnCloudAccount;
import com.owncloud.android.lib.network.webdav.WebdavUtils;

import com.owncloud.android.utils.Log_OC;


/**
 * Activity with common behaviour for activities handling {@link OCFile}s in ownCloud {@link Account}s .
 * 
 * @author David A. Velasco
 */
public abstract class FileActivity extends SherlockFragmentActivity {

    public static final String EXTRA_FILE = "com.owncloud.android.ui.activity.FILE";
    public static final String EXTRA_ACCOUNT = "com.owncloud.android.ui.activity.ACCOUNT";
    public static final String EXTRA_WAITING_TO_PREVIEW = "com.owncloud.android.ui.activity.WAITING_TO_PREVIEW";
    public static final String EXTRA_FROM_NOTIFICATION= "com.owncloud.android.ui.activity.FROM_NOTIFICATION";
    
    public static final String TAG = FileActivity.class.getSimpleName(); 
    
    
    /** OwnCloud {@link Account} where the main {@link OCFile} handled by the activity is located. */
    private Account mAccount;
    
    /** Main {@link OCFile} handled by the activity.*/
    private OCFile mFile;
    
    /** Flag to signal that the activity will is finishing to enforce the creation of an ownCloud {@link Account} */
    private boolean mRedirectingToSetupAccount = false;
    
    /** Flag to signal when the value of mAccount was set */ 
    private boolean mAccountWasSet;
    
    /** Flag to signal when the value of mAccount was restored from a saved state */ 
    private boolean mAccountWasRestored;
    
    /** Flag to signal if the activity is launched by a notification */
    private boolean mFromNotification;
    

    
    /**
     * Loads the ownCloud {@link Account} and main {@link OCFile} to be handled by the instance of 
     * the {@link FileActivity}.
     * 
     * Grants that a valid ownCloud {@link Account} is associated to the instance, or that the user 
     * is requested to create a new one.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Account account;
        if(savedInstanceState != null) {
            account = savedInstanceState.getParcelable(FileActivity.EXTRA_ACCOUNT);
            mFile = savedInstanceState.getParcelable(FileActivity.EXTRA_FILE);
            mFromNotification = savedInstanceState.getBoolean(FileActivity.EXTRA_FROM_NOTIFICATION);
        } else {
            account = getIntent().getParcelableExtra(FileActivity.EXTRA_ACCOUNT);
            mFile = getIntent().getParcelableExtra(FileActivity.EXTRA_FILE);
            mFromNotification = getIntent().getBooleanExtra(FileActivity.EXTRA_FROM_NOTIFICATION, false);
        }

        setAccount(account, savedInstanceState != null);
       
    }

    
    /**
     *  Since ownCloud {@link Account}s can be managed from the system setting menu, 
     *  the existence of the {@link Account} associated to the instance must be checked 
     *  every time it is restarted.
     */
    @Override
    protected void onRestart() {
        super.onRestart();
        boolean validAccount = (mAccount != null && AccountUtils.setCurrentOwnCloudAccount(getApplicationContext(), mAccount.name));
        if (!validAccount) {
            swapToDefaultAccount();
        }
        
    }

    
    @Override 
    protected void onStart() {
        super.onStart();
        if (mAccountWasSet) {
            onAccountSet(mAccountWasRestored);
        }
    }
    
    
    /**
     *  Sets and validates the ownCloud {@link Account} associated to the Activity. 
     * 
     *  If not valid, tries to swap it for other valid and existing ownCloud {@link Account}.
     *  
     *  POSTCONDITION: updates {@link #mAccountWasSet} and {@link #mAccountWasRestored}. 
     * 
     *  @param account          New {@link Account} to set.
     *  @param savedAccount     When 'true', account was retrieved from a saved instance state.
     */
    private void setAccount(Account account, boolean savedAccount) {
        Account oldAccount = mAccount;
        boolean validAccount = (account != null && AccountUtils.setCurrentOwnCloudAccount(getApplicationContext(), account.name));
        if (validAccount) {
            mAccount = account;
            mAccountWasSet = true;
            mAccountWasRestored = (savedAccount || mAccount.equals(oldAccount));
            
        } else {
            swapToDefaultAccount();
        }
    }

    
    /**
     *  Tries to swap the current ownCloud {@link Account} for other valid and existing. 
     * 
     *  If no valid ownCloud {@link Account} exists, the the user is requested 
     *  to create a new ownCloud {@link Account}.
     *  
     *  POSTCONDITION: updates {@link #mAccountWasSet} and {@link #mAccountWasRestored}.
     *   
     *  @return     'True' if the checked {@link Account} was valid.
     */
    private void swapToDefaultAccount() {
        // default to the most recently used account
        Account newAccount  = AccountUtils.getCurrentOwnCloudAccount(getApplicationContext());
        if (newAccount == null) {
            /// no account available: force account creation
            createFirstAccount();
            mRedirectingToSetupAccount = true;
            mAccountWasSet = false;
            mAccountWasRestored = false;
            
        } else {
            mAccountWasSet = true;
            mAccountWasRestored = (newAccount.equals(mAccount));
            mAccount = newAccount;
        }
    }


    /**
     * Launches the account creation activity. To use when no ownCloud account is available
     */
    private void createFirstAccount() {
        AccountManager am = AccountManager.get(getApplicationContext());
        am.addAccount(MainApp.getAccountType(), 
                        null,
                        null, 
                        null, 
                        this, 
                        new AccountCreationCallback(),                        
                        null);
    }

    
    /**
     * {@inheritDoc}
     */
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable(FileActivity.EXTRA_FILE, mFile);
        outState.putParcelable(FileActivity.EXTRA_ACCOUNT, mAccount);
        outState.putBoolean(FileActivity.EXTRA_FROM_NOTIFICATION, mFromNotification);
    }
    
    
    /**
     * Getter for the main {@link OCFile} handled by the activity.
     * 
     * @return  Main {@link OCFile} handled by the activity.
     */
    public OCFile getFile() {
        return mFile;
    }

    
    /**
     * Setter for the main {@link OCFile} handled by the activity.
     * 
     * @param file  Main {@link OCFile} to be handled by the activity.
     */
    public void setFile(OCFile file) {
        mFile = file;
    }

    
    /**
     * Getter for the ownCloud {@link Account} where the main {@link OCFile} handled by the activity is located.
     * 
     * @return  OwnCloud {@link Account} where the main {@link OCFile} handled by the activity is located.
     */
    public Account getAccount() {
        return mAccount;
    }

    /**
     * @return Value of mFromNotification: True if the Activity is launched by a notification
     */
    public boolean fromNotification() {
        return mFromNotification;
    }
    
    /**
     * @return  'True' when the Activity is finishing to enforce the setup of a new account.
     */
    protected boolean isRedirectingToSetupAccount() {
        return mRedirectingToSetupAccount;
    }
    
    
    /**
     *  @return 'True' if the server supports the Share API
     */
    public boolean isSharedSupported() {
        if (getAccount() != null) {
            AccountManager accountManager = AccountManager.get(this);
            return Boolean.parseBoolean(accountManager.getUserData(getAccount(), OwnCloudAccount.Constants.KEY_SUPPORTS_SHARE_API));
        }
        return false;
    }

    /**
     * Helper class handling a callback from the {@link AccountManager} after the creation of
     * a new ownCloud {@link Account} finished, successfully or not.
     * 
     * At this moment, only called after the creation of the first account.
     * 
     * @author David A. Velasco
     */
    public class AccountCreationCallback implements AccountManagerCallback<Bundle> {

        @Override
        public void run(AccountManagerFuture<Bundle> future) {
            FileActivity.this.mRedirectingToSetupAccount = false;
            boolean accountWasSet = false;
            if (future != null) {
                try {
                    Bundle result;
                    result = future.getResult();
                    String name = result.getString(AccountManager.KEY_ACCOUNT_NAME);
                    String type = result.getString(AccountManager.KEY_ACCOUNT_TYPE);
                    if (AccountUtils.setCurrentOwnCloudAccount(getApplicationContext(), name)) {
                        setAccount(new Account(name, type), false);
                        accountWasSet = true;
                    }
                } catch (OperationCanceledException e) {
                    Log_OC.d(TAG, "Account creation canceled");
                    
                } catch (Exception e) {
                    Log_OC.e(TAG, "Account creation finished in exception: ", e);
                }
                    
            } else {
                Log_OC.e(TAG, "Account creation callback with null bundle");
            }
            if (!accountWasSet) {
                moveTaskToBack(true);
            }
        }
        
    }
    
    
    /**
     *  Called when the ownCloud {@link Account} associated to the Activity was just updated.
     * 
     *  Child classes must grant that state depending on the {@link Account} is updated.
     */
    protected abstract void onAccountSet(boolean stateWasRecovered);
    
    

    public void openFile(OCFile file) {
        if (file != null) {
            String storagePath = file.getStoragePath();
            String encodedStoragePath = WebdavUtils.encodePath(storagePath);
            
            Intent intentForSavedMimeType = new Intent(Intent.ACTION_VIEW);
            intentForSavedMimeType.setDataAndType(Uri.parse("file://"+ encodedStoragePath), file.getMimetype());
            intentForSavedMimeType.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            
            Intent intentForGuessedMimeType = null;
            if (storagePath.lastIndexOf('.') >= 0) {
                String guessedMimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(storagePath.substring(storagePath.lastIndexOf('.') + 1));
                if (guessedMimeType != null && !guessedMimeType.equals(file.getMimetype())) {
                    intentForGuessedMimeType = new Intent(Intent.ACTION_VIEW);
                    intentForGuessedMimeType.setDataAndType(Uri.parse("file://"+ encodedStoragePath), guessedMimeType);
                    intentForGuessedMimeType.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                }
            }
            
            Intent chooserIntent = null;
            if (intentForGuessedMimeType != null) {
                chooserIntent = Intent.createChooser(intentForGuessedMimeType, getString(R.string.actionbar_open_with));
            } else {
                chooserIntent = Intent.createChooser(intentForSavedMimeType, getString(R.string.actionbar_open_with));
            }
            
            startActivity(chooserIntent);
            
        } else {
            Log_OC.wtf(TAG, "Trying to open a NULL OCFile");
        }
    }
    
}
