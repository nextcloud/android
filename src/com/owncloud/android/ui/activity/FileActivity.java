/**
 *   ownCloud Android client application
 *
 *   @author David A. Velasco
 *   Copyright (C) 2011  Bartek Przybylski
 *   Copyright (C) 2015 ownCloud Inc.
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
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
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
import com.owncloud.android.lib.common.operations.OnRemoteOperationListener;
import com.owncloud.android.lib.common.operations.RemoteOperation;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.common.operations.RemoteOperationResult.ResultCode;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.operations.CreateShareViaLinkOperation;
import com.owncloud.android.operations.CreateShareWithShareeOperation;
import com.owncloud.android.operations.GetSharesForFileOperation;
import com.owncloud.android.operations.SynchronizeFileOperation;
import com.owncloud.android.operations.SynchronizeFolderOperation;
import com.owncloud.android.operations.UnshareOperation;
import com.owncloud.android.services.OperationsService;
import com.owncloud.android.services.OperationsService.OperationsServiceBinder;
import com.owncloud.android.ui.NavigationDrawerItem;
import com.owncloud.android.ui.adapter.NavigationDrawerListAdapter;
import com.owncloud.android.ui.dialog.LoadingDialog;
import com.owncloud.android.ui.dialog.SharePasswordDialogFragment;
import com.owncloud.android.utils.ErrorMessageAdapter;

import java.util.ArrayList;


/**
 * Activity with common behaviour for activities handling {@link OCFile}s in ownCloud
 * {@link Account}s .
 */
public class FileActivity extends AppCompatActivity
        implements OnRemoteOperationListener, ComponentsGetter {

    public static final String EXTRA_FILE = "com.owncloud.android.ui.activity.FILE";
    public static final String EXTRA_ACCOUNT = "com.owncloud.android.ui.activity.ACCOUNT";
    public static final String EXTRA_WAITING_TO_PREVIEW =
            "com.owncloud.android.ui.activity.WAITING_TO_PREVIEW";
    public static final String EXTRA_FROM_NOTIFICATION =
            "com.owncloud.android.ui.activity.FROM_NOTIFICATION";

    public static final String TAG = FileActivity.class.getSimpleName();

    private static final String DIALOG_WAIT_TAG = "DIALOG_WAIT";

    private static final String KEY_WAITING_FOR_OP_ID = "WAITING_FOR_OP_ID";
    private static final String DIALOG_SHARE_PASSWORD = "DIALOG_SHARE_PASSWORD";
    private static final String KEY_TRY_SHARE_AGAIN = "TRY_SHARE_AGAIN";
    private static final String KEY_ACTION_BAR_TITLE = "ACTION_BAR_TITLE";

    protected static final long DELAY_TO_REQUEST_OPERATIONS_LATER = 200;


    /** OwnCloud {@link Account} where the main {@link OCFile} handled by the activity is located.*/
    private Account mAccount;

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

    protected FileDownloaderBinder mDownloaderBinder = null;
    protected FileUploaderBinder mUploaderBinder = null;
    private ServiceConnection mDownloadServiceConnection, mUploadServiceConnection = null;

    private boolean mTryShareAgain = false;

    // Navigation Drawer
    protected DrawerLayout mDrawerLayout;
    protected ActionBarDrawerToggle mDrawerToggle;
    protected ListView mDrawerList;

    // Slide menu items
    protected String[] mDrawerTitles;
    protected String[] mDrawerContentDescriptions;

    protected ArrayList<NavigationDrawerItem> mDrawerItems;

    protected NavigationDrawerListAdapter mNavigationDrawerAdapter = null;


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
            mTryShareAgain = savedInstanceState.getBoolean(KEY_TRY_SHARE_AGAIN);
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

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        // Sync the toggle state after onRestoreInstanceState has occurred.
        if (mDrawerToggle != null) {
            mDrawerToggle.syncState();
            if (isDrawerOpen()) {
                getSupportActionBar().setTitle(R.string.app_name);
                mDrawerToggle.setDrawerIndicatorEnabled(true);
            }
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (mDrawerToggle != null) {
            mDrawerToggle.onConfigurationChanged(newConfig);
        }
    }

    @Override
    public void onBackPressed() {
        if (isDrawerOpen()) {
            closeNavDrawer();
            return;
        }
        super.onBackPressed();
    }

    /**
     * checks if the drawer exists and is opened.
     *
     * @return <code>true</code> if the drawer is open, else <code>false</code>
     */
    public boolean isDrawerOpen() {
        if(mDrawerLayout != null) {
            return mDrawerLayout.isDrawerOpen(GravityCompat.START);
        } else {
            return false;
        }
    }

    /**
     * closes the navigation drawer.
     */
    public void closeNavDrawer() {
        if(mDrawerLayout != null) {
            mDrawerLayout.closeDrawer(GravityCompat.START);
        }
    }

    protected void initDrawer(){
        // constant settings for action bar when navigation drawer is inited
        getSupportActionBar().setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);


        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        // Notification Drawer
        RelativeLayout navigationDrawerLayout = (RelativeLayout) findViewById(R.id.left_drawer);
        mDrawerList = (ListView) navigationDrawerLayout.findViewById(R.id.drawer_list);

        // TODO re-enable when "Accounts" is available in Navigation Drawer
//        // load Account in the Drawer Title
//        // User-Icon
//        ImageView userIcon = (ImageView) navigationDrawerLayout.findViewById(R.id.drawer_userIcon);
//        userIcon.setImageResource(DisplayUtils.getSeasonalIconId());
//
//        // Username
//        TextView username = (TextView) navigationDrawerLayout.findViewById(R.id.drawer_username);
//        Account account = AccountUtils.getCurrentOwnCloudAccount(getApplicationContext());
//
//        if (account != null) {
//            int lastAtPos = account.name.lastIndexOf("@");
//            username.setText(account.name.substring(0, lastAtPos));
//        }

        // Display username in drawer
        Account account = AccountUtils.getCurrentOwnCloudAccount(getApplicationContext());
        if (account != null) {
            TextView username = (TextView) navigationDrawerLayout.findViewById(R.id.drawer_username);
            int lastAtPos = account.name.lastIndexOf("@");
            username.setText(account.name.substring(0, lastAtPos));
        }

        // load slide menu items
        mDrawerTitles = getResources().getStringArray(R.array.drawer_items);

        // nav drawer content description from resources
        mDrawerContentDescriptions = getResources().
                getStringArray(R.array.drawer_content_descriptions);

        // nav drawer items
        mDrawerItems = new ArrayList<NavigationDrawerItem>();
        // adding nav drawer items to array
        // TODO re-enable when "Accounts" is available in Navigation Drawer
        // Accounts
        // mDrawerItems.add(new NavigationDrawerItem(mDrawerTitles[0],
        // mDrawerContentDescriptions[0]));
        // All Files
        mDrawerItems.add(new NavigationDrawerItem(mDrawerTitles[0], mDrawerContentDescriptions[0],
                R.drawable.ic_folder_open));

        // TODO Enable when "On Device" is recovered
        // On Device
        //mDrawerItems.add(new NavigationDrawerItem(mDrawerTitles[2],
        //        mDrawerContentDescriptions[2]));

        // Settings
        mDrawerItems.add(new NavigationDrawerItem(mDrawerTitles[1], mDrawerContentDescriptions[1],
                R.drawable.ic_settings));
        // Logs
        if (BuildConfig.DEBUG) {
            mDrawerItems.add(new NavigationDrawerItem(mDrawerTitles[2],
                    mDrawerContentDescriptions[2],R.drawable.ic_log));
        }

        // setting the nav drawer list adapter
        mNavigationDrawerAdapter = new NavigationDrawerListAdapter(getApplicationContext(), this,
                mDrawerItems);
        mDrawerList.setAdapter(mNavigationDrawerAdapter);


        mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout,R.string.drawer_open,R.string.drawer_close) {

            /** Called when a drawer has settled in a completely closed state. */
            public void onDrawerClosed(View view) {
                super.onDrawerClosed(view);
                updateActionBarTitleAndHomeButton(null);
                invalidateOptionsMenu();
            }

            /** Called when a drawer has settled in a completely open state. */
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                getSupportActionBar().setTitle(R.string.app_name);
                mDrawerToggle.setDrawerIndicatorEnabled(true);
                invalidateOptionsMenu();
            }
        };
        
        // Set the list's click listener
        mDrawerList.setOnItemClickListener(new DrawerItemClickListener());

        // Set the drawer toggle as the DrawerListener
        mDrawerLayout.setDrawerListener(mDrawerToggle);
        mDrawerToggle.setDrawerIndicatorEnabled(false);
    }

    /**
     * Updates title bar and home buttons (state and icon).
     *
     * Assumes that navigation drawer is NOT visible.
     */
    protected void updateActionBarTitleAndHomeButton(OCFile chosenFile) {
        String title = getString(R.string.default_display_name_for_root_folder);    // default
        boolean inRoot;

        /// choose the appropiate title
        if (chosenFile == null) {
            chosenFile = mFile;     // if no file is passed, current file decides
        }
        inRoot = (
                chosenFile == null ||
                (chosenFile.isFolder() && chosenFile.getParentId() == FileDataStorageManager.ROOT_PARENT_ID)
        );
        if (!inRoot) {
            title = chosenFile.getFileName();
        }

        /// set the chosen title
        ActionBar actionBar = getSupportActionBar();
        actionBar.setTitle(title);
        /// also as content description
        View actionBarTitleView = getWindow().getDecorView().findViewById(
                getResources().getIdentifier("action_bar_title", "id", "android")
        );
        if (actionBarTitleView != null) {    // it's null in Android 2.x
            actionBarTitleView.setContentDescription(title);
        }

        /// set home button properties
        mDrawerToggle.setDrawerIndicatorEnabled(inRoot);
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setDisplayShowTitleEnabled(true);

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
        outState.putBoolean(KEY_TRY_SHARE_AGAIN, mTryShareAgain);
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

    public boolean isTryShareAgain(){
        return mTryShareAgain;
    }

    public void setTryShareAgain(boolean tryShareAgain) {
       mTryShareAgain = tryShareAgain;
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
        Log_OC.d(TAG, "Received result of operation in FileActivity - common behaviour for all the "
                + "FileActivities ");

        mFileOperationsHelper.setOpIdWaitingFor(Long.MAX_VALUE);

        dismissLoadingDialog();

        if (!result.isSuccess() && (
                result.getCode() == ResultCode.UNAUTHORIZED ||
                result.isIdPRedirection() ||
                (result.isException() && result.getException() instanceof AuthenticatorException)
                )) {

            requestCredentialsUpdate();

            if (result.getCode() == ResultCode.UNAUTHORIZED) {
                dismissLoadingDialog();
                Toast t = Toast.makeText(this, ErrorMessageAdapter.getErrorCauseMessage(result,
                                operation, getResources()),
                        Toast.LENGTH_LONG);
                t.show();
            }
            mTryShareAgain = false;

        } else if (operation == null ||
                operation instanceof CreateShareWithShareeOperation ||
                operation instanceof UnshareOperation ||
                operation instanceof SynchronizeFolderOperation
                ) {
            if (result.isSuccess()) {
                updateFileFromDB();

            } else if (result.getCode() != ResultCode.CANCELLED) {
                Toast t = Toast.makeText(this,
                        ErrorMessageAdapter.getErrorCauseMessage(result, operation, getResources()),
                        Toast.LENGTH_LONG);
                t.show();
            }

        } else if (operation instanceof CreateShareViaLinkOperation) {
            onCreateShareViaLinkOperationFinish((CreateShareViaLinkOperation) operation, result);

        } else if (operation instanceof SynchronizeFileOperation) {
            onSynchronizeFileOperationFinish((SynchronizeFileOperation) operation, result);

        } else if (operation instanceof GetSharesForFileOperation) {
            if (result.isSuccess()) {
                updateFileFromDB();

            } else if (result.getCode() != ResultCode.SHARE_NOT_FOUND) {
                Toast t = Toast.makeText(this,
                        ErrorMessageAdapter.getErrorCauseMessage(result, operation, getResources()),
                        Toast.LENGTH_LONG);
                t.show();
            }
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



    private void onCreateShareViaLinkOperationFinish(CreateShareViaLinkOperation operation,
                                                     RemoteOperationResult result) {
        if (result.isSuccess()) {
            mTryShareAgain = false;
            updateFileFromDB();

            Intent sendIntent = operation.getSendIntentWithSubject(this);
            if (sendIntent != null) {
                startActivity(sendIntent);
            }

        } else {
            // Detect Failure (403) --> needs Password
            if (result.getCode() == ResultCode.SHARE_FORBIDDEN) {
                if (!isTryShareAgain()) {
                    SharePasswordDialogFragment dialog =
                            SharePasswordDialogFragment.newInstance(new OCFile(operation.getPath()),
                                    operation.getSendIntent());
                    dialog.show(getSupportFragmentManager(), DIALOG_SHARE_PASSWORD);
                } else {
                    Toast t = Toast.makeText(this,
                        ErrorMessageAdapter.getErrorCauseMessage(result, operation, getResources()),
                        Toast.LENGTH_LONG);
                    t.show();
                    mTryShareAgain = false;
                }
            } else {
                Toast t = Toast.makeText(this,
                        ErrorMessageAdapter.getErrorCauseMessage(result, operation, getResources()),
                        Toast.LENGTH_LONG);
                t.show();
            }
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
    }


    public void restart(){
        Intent i = new Intent(this, FileDisplayActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(i);
    }

//    TODO re-enable when "Accounts" is available in Navigation Drawer
//    public void closeDrawer() {
//        mDrawerLayout.closeDrawers();
//    }

    public void allFilesOption(){
        restart();
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
                    mDrawerLayout.closeDrawers();
                    break;

                // TODO Enable when "On Device" is recovered ?
//                case 2:
//                    MainApp.showOnlyFilesOnDevice(true);
//                    mDrawerLayout.closeDrawers();
//                    break;

                case 1: // Settings
                    Intent settingsIntent = new Intent(getApplicationContext(),
                            Preferences.class);
                    startActivity(settingsIntent);
                    mDrawerLayout.closeDrawers();
                    break;

                case 2: // Logs
                    Intent loggerIntent = new Intent(getApplicationContext(),
                            LogHistoryActivity.class);
                    startActivity(loggerIntent);
                    mDrawerLayout.closeDrawers();
                    break;
            }
        }
    }
}
