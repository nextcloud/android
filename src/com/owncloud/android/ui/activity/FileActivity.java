/**
 *   ownCloud Android client application
 *
 *   @author David A. Velasco
 *   Copyright (C) 2011  Bartek Przybylski
 *   Copyright (C) 2016 ownCloud Inc.
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
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.design.widget.NavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.owncloud.android.BuildConfig;
import com.owncloud.android.MainApp;
import com.owncloud.android.R;
import com.owncloud.android.authentication.AccountUtils;
import com.owncloud.android.authentication.AuthenticatorActivity;
import com.owncloud.android.datamodel.FileDataStorageManager;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.files.FileOperationsHelper;
import com.owncloud.android.files.services.FileDownloader;
import com.owncloud.android.files.services.FileDownloader.FileDownloaderBinder;
import com.owncloud.android.files.services.FileUploader;
import com.owncloud.android.files.services.FileUploader.FileUploaderBinder;
import com.owncloud.android.lib.common.OwnCloudAccount;
import com.owncloud.android.lib.common.OwnCloudClient;
import com.owncloud.android.lib.common.OwnCloudClientManagerFactory;
import com.owncloud.android.lib.common.OwnCloudCredentials;
import com.owncloud.android.lib.common.network.CertificateCombinedException;
import com.owncloud.android.lib.common.operations.OnRemoteOperationListener;
import com.owncloud.android.lib.common.operations.RemoteOperation;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.common.operations.RemoteOperationResult.ResultCode;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.lib.resources.status.OCCapability;
import com.owncloud.android.operations.CreateShareWithShareeOperation;
import com.owncloud.android.operations.GetSharesForFileOperation;
import com.owncloud.android.operations.SynchronizeFileOperation;
import com.owncloud.android.operations.SynchronizeFolderOperation;
import com.owncloud.android.operations.UnshareOperation;
import com.owncloud.android.operations.UpdateSharePermissionsOperation;
import com.owncloud.android.operations.UpdateShareViaLinkOperation;
import com.owncloud.android.services.OperationsService;
import com.owncloud.android.services.OperationsService.OperationsServiceBinder;
import com.owncloud.android.ui.NavigationDrawerItem;
import com.owncloud.android.ui.adapter.NavigationDrawerListAdapter;
import com.owncloud.android.ui.dialog.ConfirmationDialogFragment;
import com.owncloud.android.ui.dialog.LoadingDialog;
import com.owncloud.android.ui.dialog.SslUntrustedCertDialog;
import com.owncloud.android.utils.ErrorMessageAdapter;

import java.util.ArrayList;


/**
 * Activity with common behaviour for activities handling {@link OCFile}s in ownCloud
 * {@link Account}s .
 */
public class FileActivity extends DrawerActivity
        implements OnRemoteOperationListener, ComponentsGetter, SslUntrustedCertDialog.OnSslUntrustedCertListener {

    public static final String EXTRA_FILE = "com.owncloud.android.ui.activity.FILE";
    public static final String EXTRA_ACCOUNT = "com.owncloud.android.ui.activity.ACCOUNT";
    public static final String EXTRA_FROM_NOTIFICATION =
            "com.owncloud.android.ui.activity.FROM_NOTIFICATION";

    public static final String TAG = FileActivity.class.getSimpleName();

    private static final String DIALOG_WAIT_TAG = "DIALOG_WAIT";

    private static final String KEY_WAITING_FOR_OP_ID = "WAITING_FOR_OP_ID";
    private static final String KEY_ACTION_BAR_TITLE = "ACTION_BAR_TITLE";

    public static final int REQUEST_CODE__UPDATE_CREDENTIALS = 0;
    public static final int REQUEST_CODE__LAST_SHARED = REQUEST_CODE__UPDATE_CREDENTIALS;

    protected static final long DELAY_TO_REQUEST_OPERATIONS_LATER = 200;

    /* Dialog tags */
    private static final String DIALOG_UNTRUSTED_CERT = "DIALOG_UNTRUSTED_CERT";
    private static final String DIALOG_CERT_NOT_SAVED = "DIALOG_CERT_NOT_SAVED";

    /** OwnCloud {@link Account} where the main {@link OCFile} handled by the activity is located.*/
    private Account mAccount;

    /** Capabilites of the server where {@link #mAccount} lives */
     private OCCapability mCapabilities;

     /** Main {@link OCFile} handled by the activity.*/
    private OCFile mFile;


    /** Flag to signal that the activity will is finishing to enforce the creation of an ownCloud
     * {@link Account} */
    private boolean mRedirectingToSetupAccount = false;

    /** Flag to signal when the value of mAccount was set */
    protected boolean mAccountWasSet;

    /** Flag to signal when the value of mAccount was restored from a saved state */
    protected boolean mAccountWasRestored;

    /** Flag to signal if the activity is launched by a notification */
    private boolean mFromNotification;

    /** Messages handler associated to the main thread and the life cycle of the activity */
    private Handler mHandler;

    /** Access point to the cached database for the current ownCloud {@link Account} */
    private FileDataStorageManager mStorageManager = null;

    private FileOperationsHelper mFileOperationsHelper;

    private ServiceConnection mOperationsServiceConnection = null;

    private OperationsServiceBinder mOperationsServiceBinder = null;

    private boolean mResumed = false;

    protected FileDownloaderBinder mDownloaderBinder = null;
    protected FileUploaderBinder mUploaderBinder = null;
    private ServiceConnection mDownloadServiceConnection, mUploadServiceConnection = null;

    // TODO re-enable when "Accounts" is available in Navigation Drawer
//    protected boolean mShowAccounts = false;

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
        Account account = null;
        if(savedInstanceState != null) {
            mFile = savedInstanceState.getParcelable(FileActivity.EXTRA_FILE);
            mFromNotification = savedInstanceState.getBoolean(FileActivity.EXTRA_FROM_NOTIFICATION);
            mFileOperationsHelper.setOpIdWaitingFor(
                    savedInstanceState.getLong(KEY_WAITING_FOR_OP_ID, Long.MAX_VALUE)
                    );
            if (getSupportActionBar() != null) {
                getSupportActionBar().setTitle(savedInstanceState.getString(KEY_ACTION_BAR_TITLE));
            }
        } else {
            account = getIntent().getParcelableExtra(FileActivity.EXTRA_ACCOUNT);
            mFile = getIntent().getParcelableExtra(FileActivity.EXTRA_FILE);
            mFromNotification = getIntent().getBooleanExtra(FileActivity.EXTRA_FROM_NOTIFICATION,
                    false);
        }

        AccountUtils.updateAccountVersion(this); // best place, before any access to AccountManager
                                                 // or database

        setAccount(account, savedInstanceState != null);

        mOperationsServiceConnection = new OperationsServiceConnection();
        bindService(new Intent(this, OperationsService.class), mOperationsServiceConnection,
                Context.BIND_AUTO_CREATE);

        mDownloadServiceConnection = newTransferenceServiceConnection();
        if (mDownloadServiceConnection != null) {
            bindService(new Intent(this, FileDownloader.class), mDownloadServiceConnection,
                    Context.BIND_AUTO_CREATE);
        }
        mUploadServiceConnection = newTransferenceServiceConnection();
        if (mUploadServiceConnection != null) {
            bindService(new Intent(this, FileUploader.class), mUploadServiceConnection,
                    Context.BIND_AUTO_CREATE);
        }

    }

    @Override
    protected void onNewIntent (Intent intent) {
        Log_OC.v(TAG, "onNewIntent() start");
        Account current = AccountUtils.getCurrentOwnCloudAccount(this);
        if (current != null && mAccount != null && !mAccount.name.equals(current.name)) {
            mAccount = current;
        }
        Log_OC.v(TAG, "onNewIntent() stop");
    }

    /**
     *  Since ownCloud {@link Account}s can be managed from the system setting menu,
     *  the existence of the {@link Account} associated to the instance must be checked
     *  every time it is restarted.
     */
    @Override
    protected void onRestart() {
        Log_OC.v(TAG, "onRestart() start");
        super.onRestart();
        boolean validAccount = (mAccount != null && AccountUtils.exists(mAccount, this));
        if (!validAccount) {
            swapToDefaultAccount();
        }
        Log_OC.v(TAG, "onRestart() end");
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
        mResumed = true;
        if (mOperationsServiceBinder != null) {
            doOnResumeAndBound();
        }
    }

    @Override
    protected void onPause()  {
        if (mOperationsServiceBinder != null) {
            mOperationsServiceBinder.removeOperationListener(this);
        }
        mResumed = false;
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
    protected void setAccount(Account account, boolean savedAccount) {
        Account oldAccount = mAccount;
        boolean validAccount =
                (account != null && AccountUtils.setCurrentOwnCloudAccount(getApplicationContext(),
                        account.name));
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
     */
    private void swapToDefaultAccount() {
        // default to the most recently used account
        Account newAccount = AccountUtils.getCurrentOwnCloudAccount(getApplicationContext());
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
        outState.putBoolean(FileActivity.EXTRA_FROM_NOTIFICATION, mFromNotification);
        outState.putLong(KEY_WAITING_FOR_OP_ID, mFileOperationsHelper.getOpIdWaitingFor());
        if(getSupportActionBar() != null && getSupportActionBar().getTitle() != null) {
            // Null check in case the actionbar is used in ActionBar.NAVIGATION_MODE_LIST
            // since it doesn't have a title then
            outState.putString(KEY_ACTION_BAR_TITLE, getSupportActionBar().getTitle().toString());
        }
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
     * Getter for the ownCloud {@link Account} where the main {@link OCFile} handled by the activity
     * is located.
     *
     * @return  OwnCloud {@link Account} where the main {@link OCFile} handled by the activity
     *          is located.
     */
    public Account getAccount() {
        return mAccount;
    }

    protected void setAccount(Account account) {
        mAccount = account;
    }


    /**
     * Getter for the capabilities of the server where the current OC account lives.
     *
     * @return  Capabilities of the server where the current OC account lives. Null if the account is not
     *          set yet.
     */
    public OCCapability getCapabilities() {
        return mCapabilities;
    }


    /**
     * @return Value of mFromNotification: True if the Activity is launched by a notification
     */
    public boolean fromNotification() {
        return mFromNotification;
    }

    /**
     * @return 'True' when the Activity is finishing to enforce the setup of a new account.
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
            mCapabilities = mStorageManager.getCapability(mAccount.name);

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
     * @param operation     Operation performed.
     * @param result        Result of the removal.
     */
    @Override
    public void onRemoteOperationFinish(RemoteOperation operation, RemoteOperationResult result) {
        Log_OC.d(TAG, "Received result of operation in FileActivity - common behaviour for all the "
            + "FileActivities ");

        mFileOperationsHelper.setOpIdWaitingFor(Long.MAX_VALUE);

        dismissLoadingDialog();

        if (!result.isSuccess() && (
                result.getCode() == ResultCode.UNAUTHORIZED ||
                (result.isException() && result.getException() instanceof AuthenticatorException)
                )) {

            requestCredentialsUpdate(this);

            if (result.getCode() == ResultCode.UNAUTHORIZED) {
                Toast t = Toast.makeText(this, ErrorMessageAdapter.getErrorCauseMessage(result,
                        operation, getResources()),
                    Toast.LENGTH_LONG);
                t.show();
            }

        } else if (!result.isSuccess() && ResultCode.SSL_RECOVERABLE_PEER_UNVERIFIED.equals(result.getCode())) {

            showUntrustedCertDialog(result);

        } else if (operation == null ||
                operation instanceof CreateShareWithShareeOperation ||
                operation instanceof UnshareOperation ||
                operation instanceof SynchronizeFolderOperation ||
                operation instanceof UpdateShareViaLinkOperation ||
                operation instanceof UpdateSharePermissionsOperation
                ) {
            if (result.isSuccess()) {
                updateFileFromDB();

            } else if (result.getCode() != ResultCode.CANCELLED) {
                Toast t = Toast.makeText(this,
                        ErrorMessageAdapter.getErrorCauseMessage(result, operation, getResources()),
                        Toast.LENGTH_LONG);
                t.show();
            }

        } else if (operation instanceof SynchronizeFileOperation) {
            onSynchronizeFileOperationFinish((SynchronizeFileOperation) operation, result);

        } else if (operation instanceof GetSharesForFileOperation) {
            if (result.isSuccess() || result.getCode() == ResultCode.SHARE_NOT_FOUND) {
                updateFileFromDB();

            } else {
                Toast t = Toast.makeText(this,
                        ErrorMessageAdapter.getErrorCauseMessage(result, operation, getResources()),
                        Toast.LENGTH_LONG);
                t.show();
            }
        }
    }

    /**
     * Invalidates the credentials stored for the current OC account and requests new credentials to the user,
     * navigating to {@link AuthenticatorActivity}
     *
     * Equivalent to call requestCredentialsUpdate(context, null);
     *
     * @param context   Android Context needed to access the {@link AccountManager}. Received as a parameter
     *                  to make the method accessible to {@link android.content.BroadcastReceiver}s.
     */
    protected void requestCredentialsUpdate(Context context) {
        requestCredentialsUpdate(context, null);
    }

    /**
     * Invalidates the credentials stored for the given OC account and requests new credentials to the user,
     * navigating to {@link AuthenticatorActivity}
     *
     * @param context   Android Context needed to access the {@link AccountManager}. Received as a parameter
     *                  to make the method accessible to {@link android.content.BroadcastReceiver}s.
     * @param account   Stored OC account to request credentials update for. If null, current account will
     *                  be used.
     */
    protected void requestCredentialsUpdate(Context context, Account account) {

        try {
            /// step 1 - invalidate credentials of current account
            if (account == null) {
                account = getAccount();
            }
            OwnCloudClient client;
            OwnCloudAccount ocAccount =
                    new OwnCloudAccount(account, context);
            client = (OwnCloudClientManagerFactory.getDefaultSingleton().
                    removeClientFor(ocAccount));
            if (client != null) {
                OwnCloudCredentials cred = client.getCredentials();
                if (cred != null) {
                    AccountManager am = AccountManager.get(context);
                    if (cred.authTokenExpires()) {
                        am.invalidateAuthToken(
                                account.type,
                                cred.getAuthToken()
                        );
                    } else {
                        am.clearPassword(account);
                    }
                }
            }

            /// step 2 - request credentials to user
            Intent updateAccountCredentials = new Intent(this, AuthenticatorActivity.class);
            updateAccountCredentials.putExtra(AuthenticatorActivity.EXTRA_ACCOUNT, account);
            updateAccountCredentials.putExtra(
                    AuthenticatorActivity.EXTRA_ACTION,
                    AuthenticatorActivity.ACTION_UPDATE_EXPIRED_TOKEN);
            updateAccountCredentials.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
            startActivityForResult(updateAccountCredentials, REQUEST_CODE__UPDATE_CREDENTIALS);

        } catch (com.owncloud.android.lib.common.accounts.AccountUtils.AccountNotFoundException e) {
            Toast.makeText(context, R.string.auth_account_does_not_exist, Toast.LENGTH_SHORT).show();
        }

    }

    /**
     * Show untrusted cert dialog
     */
    public void showUntrustedCertDialog(RemoteOperationResult result) {
        // Show a dialog with the certificate info
        FragmentManager fm = getSupportFragmentManager();
        SslUntrustedCertDialog dialog = (SslUntrustedCertDialog) fm.findFragmentByTag(DIALOG_UNTRUSTED_CERT);
        if(dialog == null) {
            dialog = SslUntrustedCertDialog.newInstanceForFullSslError(
                (CertificateCombinedException) result.getException());
            FragmentTransaction ft = fm.beginTransaction();
            dialog.show(ft, DIALOG_UNTRUSTED_CERT);
        }
    }

    private void onSynchronizeFileOperationFinish(SynchronizeFileOperation operation,
                                                  RemoteOperationResult result) {
        OCFile syncedFile = operation.getLocalFile();
        if (!result.isSuccess()) {
            if (result.getCode() == ResultCode.SYNC_CONFLICT) {
                Intent i = new Intent(this, ConflictsResolveActivity.class);
                i.putExtra(ConflictsResolveActivity.EXTRA_FILE, syncedFile);
                i.putExtra(ConflictsResolveActivity.EXTRA_ACCOUNT, getAccount());
                startActivity(i);
            }

        } else {
            if (!operation.transferWasRequested()) {
                Toast msg = Toast.makeText(this, ErrorMessageAdapter.getErrorCauseMessage(result,
                        operation, getResources()), Toast.LENGTH_LONG);
                msg.show();
            }
            invalidateOptionsMenu();
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
    public void showLoadingDialog(String message) {
        // grant that only one waiting dialog is shown
        dismissLoadingDialog();
        // Construct dialog
        LoadingDialog loading = new LoadingDialog(message);
        FragmentManager fm = getSupportFragmentManager();
        FragmentTransaction ft = fm.beginTransaction();
        loading.show(ft, DIALOG_WAIT_TAG);

    }


    /**
     * Dismiss loading dialog
     */
    public void dismissLoadingDialog() {
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
            boolean wait = mOperationsServiceBinder.dispatchResultIfFinished((int)waitingForOpId,
                    this);
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
                if (mResumed) {
                    doOnResumeAndBound();
                }

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
    }


    public void restart(){
        Intent i = new Intent(this, FileDisplayActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        i.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(i);
    }

    @Override
    public void allFilesOption(){
        restart();
    }

    protected OCFile getCurrentDir() {
        OCFile file = getFile();
        if (file != null) {
            if (file.isFolder()) {
                return file;
            } else if (getStorageManager() != null) {
                String parentPath = file.getParentRemotePath();
                return getStorageManager().getFileByPath(parentPath);
            }
        }
        return null;
    }

    /* OnSslUntrustedCertListener methods */

    @Override
    public void onSavedCertificate() {
        // Nothing to do in this context
    }

    @Override
    public void onFailedSavingCertificate() {
        ConfirmationDialogFragment dialog = ConfirmationDialogFragment.newInstance(
            R.string.ssl_validator_not_saved, new String[]{}, 0, R.string.common_ok, -1, -1
        );
        dialog.show(getSupportFragmentManager(), DIALOG_CERT_NOT_SAVED);
    }

    @Override
    public void onCancelCertificate() {
        // nothing to do
    }

    private class DrawerItemClickListener implements ListView.OnItemClickListener {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            // TODO re-enable when "Accounts" is available in Navigation Drawer
//            if (mShowAccounts && position > 0){
//                position = position - 1;
//            }
            switch (position){
                // TODO re-enable when "Accounts" is available in Navigation Drawer
//                case 0: // Accounts
//                    mShowAccounts = !mShowAccounts;
//                    mNavigationDrawerAdapter.setShowAccounts(mShowAccounts);
//                    mNavigationDrawerAdapter.notifyDataSetChanged();
//                    break;

                case 0: // All Files
                    allFilesOption();
                    break;

                // TODO Enable when "On Device" is recovered ?
//                case 2:
//                    MainApp.showOnlyFilesOnDevice(true);
//                    break;

                case 1: // Uploads
                    Intent uploadListIntent = new Intent(getApplicationContext(),
                            UploadListActivity.class);
                    uploadListIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(uploadListIntent);
                    break;

                case 2: // Settings
                    Intent settingsIntent = new Intent(getApplicationContext(),
                            Preferences.class);
                    startActivity(settingsIntent);
                    break;
            }
            mDrawerLayout.closeDrawers();
        }
    }

}
