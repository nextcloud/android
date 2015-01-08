/* ownCloud Android client application
 *   Copyright (C) 2011  Bartek Przybylski
 *   Copyright (C) 2012-2014 ownCloud Inc.
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
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.owncloud.android.MainApp;
import com.owncloud.android.R;
import com.owncloud.android.authentication.AccountUtils;
import com.owncloud.android.authentication.AuthenticatorActivity;
import com.owncloud.android.datamodel.FileDataStorageManager;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.files.FileOperationsHelper;
import com.owncloud.android.files.services.FileDownloader;
import com.owncloud.android.files.services.FileUploader;
import com.owncloud.android.files.services.FileDownloader.FileDownloaderBinder;
import com.owncloud.android.files.services.FileUploader.FileUploaderBinder;
import com.owncloud.android.lib.common.operations.OnRemoteOperationListener;
import com.owncloud.android.lib.common.operations.RemoteOperation;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.common.operations.RemoteOperationResult.ResultCode;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.operations.CreateShareOperation;
import com.owncloud.android.operations.SynchronizeFolderOperation;
import com.owncloud.android.operations.UnshareLinkOperation;

import com.owncloud.android.services.OperationsService;
import com.owncloud.android.services.OperationsService.OperationsServiceBinder;
import com.owncloud.android.ui.dialog.LoadingDialog;
import com.owncloud.android.utils.ErrorMessageAdapter;


/**
 * Activity with common behaviour for activities handling {@link OCFile}s in ownCloud {@link Account}s .
 * 
 * @author David A. Velasco
 */
public class FileActivity extends SherlockFragmentActivity 
implements OnRemoteOperationListener, ComponentsGetter {

    public static final String EXTRA_FILE = "com.owncloud.android.ui.activity.FILE";
    public static final String EXTRA_ACCOUNT = "com.owncloud.android.ui.activity.ACCOUNT";
    public static final String EXTRA_WAITING_TO_PREVIEW = "com.owncloud.android.ui.activity.WAITING_TO_PREVIEW";
    public static final String EXTRA_FROM_NOTIFICATION= "com.owncloud.android.ui.activity.FROM_NOTIFICATION";
    
    public static final String TAG = FileActivity.class.getSimpleName();
    
    private static final String DIALOG_WAIT_TAG = "DIALOG_WAIT";
    private static final String KEY_WAITING_FOR_OP_ID = "WAITING_FOR_OP_ID";;
    
    protected static final long DELAY_TO_REQUEST_OPERATION_ON_ACTIVITY_RESULTS = 200;
    
    
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
    
    /** Messages handler associated to the main thread and the life cycle of the activity */
    private Handler mHandler;
    
    /** Access point to the cached database for the current ownCloud {@link Account} */
    private FileDataStorageManager mStorageManager = null;
    
    private FileOperationsHelper mFileOperationsHelper;
    
    private ServiceConnection mOperationsServiceConnection = null;
    
    private OperationsServiceBinder mOperationsServiceBinder = null;
    
    protected FileDownloaderBinder mDownloaderBinder = null;
    protected FileUploaderBinder mUploaderBinder = null;
    private ServiceConnection mDownloadServiceConnection, mUploadServiceConnection = null;
    
    
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
        mHandler = new Handler();
        mFileOperationsHelper = new FileOperationsHelper(this);
        Account account;
        if(savedInstanceState != null) {
            account = savedInstanceState.getParcelable(FileActivity.EXTRA_ACCOUNT);
            mFile = savedInstanceState.getParcelable(FileActivity.EXTRA_FILE);
            mFromNotification = savedInstanceState.getBoolean(FileActivity.EXTRA_FROM_NOTIFICATION);
            mFileOperationsHelper.setOpIdWaitingFor(
                    savedInstanceState.getLong(KEY_WAITING_FOR_OP_ID, Long.MAX_VALUE)
                    );
        } else {
            account = getIntent().getParcelableExtra(FileActivity.EXTRA_ACCOUNT);
            mFile = getIntent().getParcelableExtra(FileActivity.EXTRA_FILE);
            mFromNotification = getIntent().getBooleanExtra(FileActivity.EXTRA_FROM_NOTIFICATION, false);
        }

        setAccount(account, savedInstanceState != null);
        
        mOperationsServiceConnection = new OperationsServiceConnection();
        bindService(new Intent(this, OperationsService.class), mOperationsServiceConnection, Context.BIND_AUTO_CREATE);
        
        mDownloadServiceConnection = newTransferenceServiceConnection();
        if (mDownloadServiceConnection != null) {
            bindService(new Intent(this, FileDownloader.class), mDownloadServiceConnection, Context.BIND_AUTO_CREATE);
        }
        mUploadServiceConnection = newTransferenceServiceConnection();
        if (mUploadServiceConnection != null) {
            bindService(new Intent(this, FileUploader.class), mUploadServiceConnection, Context.BIND_AUTO_CREATE);
        }
        
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
    
    @Override
    protected void onResume() {
        super.onResume();
        
        if (mOperationsServiceBinder != null) {
            doOnResumeAndBound();
        }

    }
    
    @Override
    protected void onPause()  {
        
        if (mOperationsServiceBinder != null) {
            mOperationsServiceBinder.removeOperationListener(this);
        }
        
        super.onPause();
    }
    
    
    @Override
    protected void onDestroy() {
        if (mOperationsServiceConnection != null) {
            unbindService(mOperationsServiceConnection);
            mOperationsServiceBinder = null;
        }
        if (mDownloadServiceConnection != null) {
            unbindService(mDownloadServiceConnection);
            mDownloadServiceConnection = null;
        }
        if (mUploadServiceConnection != null) {
            unbindService(mUploadServiceConnection);
            mUploadServiceConnection = null;
        }
        super.onDestroy();
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
        outState.putLong(KEY_WAITING_FOR_OP_ID, mFileOperationsHelper.getOpIdWaitingFor());
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
    
    
    public OperationsServiceBinder getOperationsServiceBinder() {
        return mOperationsServiceBinder;
    }
    
    protected ServiceConnection newTransferenceServiceConnection() {
        return null;
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
    protected void onAccountSet(boolean stateWasRecovered) {
        if (getAccount() != null) {
            mStorageManager = new FileDataStorageManager(getAccount(), getContentResolver());
            
        } else {
            Log_OC.wtf(TAG, "onAccountChanged was called with NULL account associated!");
        }
    }


    public FileDataStorageManager getStorageManager() {
        return mStorageManager;
    }


    public OnRemoteOperationListener getRemoteOperationListener() {
        return this;
    }


    public Handler getHandler() {
        return mHandler;
    }
    
    public FileOperationsHelper getFileOperationsHelper() {
        return mFileOperationsHelper;
    }
    
    /**
     * 
     * @param operation     Removal operation performed.
     * @param result        Result of the removal.
     */
    @Override
    public void onRemoteOperationFinish(RemoteOperation operation, RemoteOperationResult result) {
        Log_OC.d(TAG, "Received result of operation in FileActivity - common behaviour for all the FileActivities ");
        
        mFileOperationsHelper.setOpIdWaitingFor(Long.MAX_VALUE);
        
        if (!result.isSuccess() && (
                result.getCode() == ResultCode.UNAUTHORIZED || 
                result.isIdPRedirection() ||
                (result.isException() && result.getException() instanceof AuthenticatorException)
                )) {
            
            requestCredentialsUpdate();
            
            if (result.getCode() == ResultCode.UNAUTHORIZED) {
                dismissLoadingDialog();
                Toast t = Toast.makeText(this, ErrorMessageAdapter.getErrorCauseMessage(result, operation, getResources()), 
                        Toast.LENGTH_LONG);
                t.show();
            }

        } else if (operation instanceof CreateShareOperation) {
            onCreateShareOperationFinish((CreateShareOperation) operation, result);
            
        } else if (operation instanceof UnshareLinkOperation) {
            onUnshareLinkOperationFinish((UnshareLinkOperation)operation, result);
        
        } else if (operation instanceof SynchronizeFolderOperation) {
            onSynchronizeFolderOperationFinish((SynchronizeFolderOperation)operation, result);

        }
    }

    protected void requestCredentialsUpdate() {
        Intent updateAccountCredentials = new Intent(this, AuthenticatorActivity.class);
        updateAccountCredentials.putExtra(AuthenticatorActivity.EXTRA_ACCOUNT, getAccount());
        updateAccountCredentials.putExtra(
                AuthenticatorActivity.EXTRA_ACTION, 
                AuthenticatorActivity.ACTION_UPDATE_EXPIRED_TOKEN);
        updateAccountCredentials.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
        startActivity(updateAccountCredentials);
    }
    

    private void onCreateShareOperationFinish(CreateShareOperation operation, RemoteOperationResult result) {
        dismissLoadingDialog();
        if (result.isSuccess()) {
            updateFileFromDB();
            
            Intent sendIntent = operation.getSendIntent();
            startActivity(sendIntent);
            
        } else { 
            Toast t = Toast.makeText(this, ErrorMessageAdapter.getErrorCauseMessage(result, operation, getResources()), 
                    Toast.LENGTH_LONG);
            t.show();
        } 
    }
    
    
    private void onUnshareLinkOperationFinish(UnshareLinkOperation operation, RemoteOperationResult result) {
        dismissLoadingDialog();
        
        if (result.isSuccess()){
            updateFileFromDB();
            
        } else {
            Toast t = Toast.makeText(this, ErrorMessageAdapter.getErrorCauseMessage(result, operation, getResources()), 
                    Toast.LENGTH_LONG);
            t.show();
        } 
    }

    private void onSynchronizeFolderOperationFinish(SynchronizeFolderOperation operation, RemoteOperationResult result) {
        if (!result.isSuccess() && result.getCode() != ResultCode.CANCELLED){
            Toast t = Toast.makeText(this, ErrorMessageAdapter.getErrorCauseMessage(result, operation, getResources()),
                    Toast.LENGTH_LONG);
            t.show();
        }
    }
    
    protected void updateFileFromDB(){
        OCFile file = getFile();
        if (file != null) {
            file = getStorageManager().getFileByPath(file.getRemotePath());
            setFile(file);
        }
    }
    
    /**
     * Show loading dialog 
     */
    public void showLoadingDialog() {
        // Construct dialog
        LoadingDialog loading = new LoadingDialog(getResources().getString(R.string.wait_a_moment));
        FragmentManager fm = getSupportFragmentManager();
        FragmentTransaction ft = fm.beginTransaction();
        loading.show(ft, DIALOG_WAIT_TAG);
        
    }

    
    /**
     * Dismiss loading dialog
     */
    public void dismissLoadingDialog(){
        Fragment frag = getSupportFragmentManager().findFragmentByTag(DIALOG_WAIT_TAG);
        if (frag != null) {
            LoadingDialog loading = (LoadingDialog) frag;
            loading.dismiss();
        }
    }

    
    private void doOnResumeAndBound() {
        mOperationsServiceBinder.addOperationListener(FileActivity.this, mHandler);
        long waitingForOpId = mFileOperationsHelper.getOpIdWaitingFor();
        if (waitingForOpId <= Integer.MAX_VALUE) {
            boolean wait = mOperationsServiceBinder.dispatchResultIfFinished((int)waitingForOpId, this);
            if (!wait ) {
                dismissLoadingDialog();
            }
        }
    }


    /** 
     * Implements callback methods for service binding. Passed as a parameter to { 
     */
    private class OperationsServiceConnection implements ServiceConnection {

        @Override
        public void onServiceConnected(ComponentName component, IBinder service) {
            if (component.equals(new ComponentName(FileActivity.this, OperationsService.class))) {
                Log_OC.d(TAG, "Operations service connected");
                mOperationsServiceBinder = (OperationsServiceBinder) service;
                /*if (!mOperationsServiceBinder.isPerformingBlockingOperation()) {
                    dismissLoadingDialog();
                }*/
                doOnResumeAndBound();

            } else {
                return;
            }
        }
        

        @Override
        public void onServiceDisconnected(ComponentName component) {
            if (component.equals(new ComponentName(FileActivity.this, OperationsService.class))) {
                Log_OC.d(TAG, "Operations service disconnected");
                mOperationsServiceBinder = null;
                // TODO whatever could be waiting for the service is unbound
            }
        }
    }


    @Override
    public FileDownloaderBinder getFileDownloaderBinder() {
        return mDownloaderBinder;
    }


    @Override
    public FileUploaderBinder getFileUploaderBinder() {
        return mUploaderBinder;
    };    
    
    
}
