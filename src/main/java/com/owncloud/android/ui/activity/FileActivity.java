/*
 *   ownCloud Android client application
 *
 *   @author David A. Velasco
 *   @author Chris Narkiewicz
 *   Copyright (C) 2011  Bartek Przybylski
 *   Copyright (C) 2016 ownCloud Inc.
 *   Copyright (C) 2019 Chris Narkiewicz <hello@ezaquarii.com>
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
import android.accounts.AuthenticatorException;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.text.TextUtils;

import com.google.android.material.snackbar.Snackbar;
import com.nextcloud.client.account.UserAccountManager;
import com.nextcloud.client.jobs.BackgroundJobManager;
import com.nextcloud.client.network.ConnectivityService;
import com.owncloud.android.MainApp;
import com.owncloud.android.R;
import com.owncloud.android.authentication.AuthenticatorActivity;
import com.owncloud.android.datamodel.ArbitraryDataProvider;
import com.owncloud.android.datamodel.OCFile;
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
import com.owncloud.android.lib.resources.shares.OCShare;
import com.owncloud.android.lib.resources.shares.ShareType;
import com.owncloud.android.operations.CreateShareViaLinkOperation;
import com.owncloud.android.operations.CreateShareWithShareeOperation;
import com.owncloud.android.operations.GetSharesForFileOperation;
import com.owncloud.android.operations.SynchronizeFileOperation;
import com.owncloud.android.operations.SynchronizeFolderOperation;
import com.owncloud.android.operations.UnshareOperation;
import com.owncloud.android.operations.UpdateNoteForShareOperation;
import com.owncloud.android.operations.UpdateSharePermissionsOperation;
import com.owncloud.android.operations.UpdateShareViaLinkOperation;
import com.owncloud.android.providers.UsersAndGroupsSearchProvider;
import com.owncloud.android.services.OperationsService;
import com.owncloud.android.services.OperationsService.OperationsServiceBinder;
import com.owncloud.android.ui.asynctasks.CheckRemoteWipeTask;
import com.owncloud.android.ui.asynctasks.LoadingVersionNumberTask;
import com.owncloud.android.ui.dialog.ConfirmationDialogFragment;
import com.owncloud.android.ui.dialog.LoadingDialog;
import com.owncloud.android.ui.dialog.ShareLinkToDialog;
import com.owncloud.android.ui.dialog.SslUntrustedCertDialog;
import com.owncloud.android.ui.fragment.FileDetailFragment;
import com.owncloud.android.ui.fragment.FileDetailSharingFragment;
import com.owncloud.android.ui.helpers.FileOperationsHelper;
import com.owncloud.android.utils.ClipboardUtil;
import com.owncloud.android.utils.DisplayUtils;
import com.owncloud.android.utils.ErrorMessageAdapter;
import com.owncloud.android.utils.FilesSyncHelper;
import com.owncloud.android.utils.theme.ThemeSnackbarUtils;
import com.owncloud.android.utils.theme.ThemeToolbarUtils;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

import javax.inject.Inject;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import static com.owncloud.android.ui.activity.FileDisplayActivity.TAG_PUBLIC_LINK;


/**
 * Activity with common behaviour for activities handling {@link OCFile}s in ownCloud {@link Account}s .
 */
public abstract class FileActivity extends DrawerActivity
        implements OnRemoteOperationListener, ComponentsGetter, SslUntrustedCertDialog.OnSslUntrustedCertListener,
        LoadingVersionNumberTask.VersionDevInterface {

    public static final String EXTRA_FILE = "com.owncloud.android.ui.activity.FILE";
    public static final String EXTRA_ACCOUNT = "com.owncloud.android.ui.activity.ACCOUNT";
    public static final String EXTRA_USER = "com.owncloud.android.ui.activity.USER";
    public static final String EXTRA_FROM_NOTIFICATION = "com.owncloud.android.ui.activity.FROM_NOTIFICATION";
    public static final String APP_OPENED_COUNT = "APP_OPENED_COUNT";
    public static final String EXTRA_SEARCH = "com.owncloud.android.ui.activity.SEARCH";
    public static final String EXTRA_SEARCH_QUERY = "com.owncloud.android.ui.activity.SEARCH_QUERY";

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

     /** Main {@link OCFile} handled by the activity.*/
    private OCFile mFile;

    /** Flag to signal if the activity is launched by a notification */
    private boolean mFromNotification;

    /** Messages handler associated to the main thread and the life cycle of the activity */
    private Handler mHandler;

    private FileOperationsHelper mFileOperationsHelper;

    private ServiceConnection mOperationsServiceConnection;

    private OperationsServiceBinder mOperationsServiceBinder;

    private boolean mResumed;

    protected FileDownloaderBinder mDownloaderBinder;
    protected FileUploaderBinder mUploaderBinder;
    private ServiceConnection mDownloadServiceConnection;
    private ServiceConnection mUploadServiceConnection;

    @Inject
    UserAccountManager accountManager;

    @Inject
    ConnectivityService connectivityService;

    @Inject
    BackgroundJobManager backgroundJobManager;

    @Override
    public void showFiles(boolean onDeviceOnly) {
        // must be specialized in subclasses
        MainApp.showOnlyFilesOnDevice(onDeviceOnly);
        if (onDeviceOnly) {
            setupToolbar();
        } else {
            setupHomeSearchToolbarWithSortAndListButtons();
        }
    }

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
        mFileOperationsHelper = new FileOperationsHelper(this, getUserAccountManager(), connectivityService);
        Account account = null;

        if (savedInstanceState != null) {
            mFile = savedInstanceState.getParcelable(FileActivity.EXTRA_FILE);
            mFromNotification = savedInstanceState.getBoolean(FileActivity.EXTRA_FROM_NOTIFICATION);
            mFileOperationsHelper.setOpIdWaitingFor(
                    savedInstanceState.getLong(KEY_WAITING_FOR_OP_ID, Long.MAX_VALUE)
                    );
            ThemeToolbarUtils.setColoredTitle(getSupportActionBar(), savedInstanceState.getString(KEY_ACTION_BAR_TITLE), this);
        } else {
            account = getIntent().getParcelableExtra(FileActivity.EXTRA_ACCOUNT);
            mFile = getIntent().getParcelableExtra(FileActivity.EXTRA_FILE);
            mFromNotification = getIntent().getBooleanExtra(FileActivity.EXTRA_FROM_NOTIFICATION,
                    false);
        }

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
    protected void onStart() {
        super.onStart();

        fetchExternalLinks(false);
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
     * {@inheritDoc}
     */
    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
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
        FileDetailSharingFragment fragment = getShareFileFragment();
        if (fragment != null) {
            return fragment.getFile();
        }
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
     * @return Value of mFromNotification: True if the Activity is launched by a notification
     */
    public boolean fromNotification() {
        return mFromNotification;
    }

    public OperationsServiceBinder getOperationsServiceBinder() {
        return mOperationsServiceBinder;
    }

    protected ServiceConnection newTransferenceServiceConnection() {
        return null;
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
                DisplayUtils.showSnackMessage(
                        this, ErrorMessageAdapter.getErrorCauseMessage(result, operation, getResources())
                );
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
                DisplayUtils.showSnackMessage(
                        this, ErrorMessageAdapter.getErrorCauseMessage(result, operation, getResources())
                );
            }

        } else if (operation instanceof SynchronizeFileOperation) {
            onSynchronizeFileOperationFinish((SynchronizeFileOperation) operation, result);

        } else if (operation instanceof GetSharesForFileOperation) {
            if (result.isSuccess() || result.getCode() == ResultCode.SHARE_NOT_FOUND) {
                updateFileFromDB();

            } else {
                DisplayUtils.showSnackMessage(this,
                                              ErrorMessageAdapter.getErrorCauseMessage(result,
                                                                                       operation,
                                                                                       getResources()));
            }
        }

        if (operation instanceof CreateShareViaLinkOperation) {
            onCreateShareViaLinkOperationFinish((CreateShareViaLinkOperation) operation, result);
        } else if (operation instanceof CreateShareWithShareeOperation) {
            onUpdateShareInformation(result, R.string.sharee_add_failed);
        } else if (operation instanceof UpdateShareViaLinkOperation) {
            onUpdateShareInformation(result, R.string.updating_share_failed);
        } else if (operation instanceof UpdateSharePermissionsOperation) {
            onUpdateShareInformation(result, R.string.updating_share_failed);
        } else if (operation instanceof UnshareOperation) {
            onUpdateShareInformation(result, R.string.unsharing_failed);
        } else if (operation instanceof UpdateNoteForShareOperation) {
            onUpdateNoteForShareOperationFinish(result);
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
        if (account == null) {
            account = getAccount();
        }

        boolean remoteWipeSupported = accountManager.getServerVersion(account).isRemoteWipeSupported();

        if (remoteWipeSupported) {
            new CheckRemoteWipeTask(backgroundJobManager, account, new WeakReference<>(this)).execute();
        } else {
            performCredentialsUpdate(account, context);
        }
    }

    public void performCredentialsUpdate(Account account, Context context) {
        try {
            /// step 1 - invalidate credentials of current account
            OwnCloudAccount ocAccount = new OwnCloudAccount(account, context);
            OwnCloudClient client = OwnCloudClientManagerFactory.getDefaultSingleton().removeClientFor(ocAccount);

            if (client != null) {
                OwnCloudCredentials credentials = client.getCredentials();
                if (credentials != null) {
                    AccountManager accountManager = AccountManager.get(context);
                    if (credentials.authTokenExpires()) {
                        accountManager.invalidateAuthToken(account.type, credentials.getAuthToken());
                    } else {
                        accountManager.clearPassword(account);
                    }
                }
            }

            /// step 2 - request credentials to user
            Intent updateAccountCredentials = new Intent(context, AuthenticatorActivity.class);
            updateAccountCredentials.putExtra(AuthenticatorActivity.EXTRA_ACCOUNT, account);
            updateAccountCredentials.putExtra(
                AuthenticatorActivity.EXTRA_ACTION,
                AuthenticatorActivity.ACTION_UPDATE_EXPIRED_TOKEN);
            updateAccountCredentials.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
            startActivityForResult(updateAccountCredentials, REQUEST_CODE__UPDATE_CREDENTIALS);
        } catch (com.owncloud.android.lib.common.accounts.AccountUtils.AccountNotFoundException e) {
            DisplayUtils.showSnackMessage(this, R.string.auth_account_does_not_exist);
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
                Intent intent = ConflictsResolveActivity.createIntent(syncedFile, getAccount(), -1, null, this);
                startActivity(intent);
            }

        } else {
            if (!operation.transferWasRequested()) {
                DisplayUtils.showSnackMessage(this, ErrorMessageAdapter.getErrorCauseMessage(result,
                        operation, getResources()));
            }
            supportInvalidateOptionsMenu();
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
        Fragment frag = getSupportFragmentManager().findFragmentByTag(DIALOG_WAIT_TAG);
        if (frag == null) {
            Log_OC.d(TAG, "show loading dialog");
            LoadingDialog loading = LoadingDialog.newInstance(message);
            FragmentManager fm = getSupportFragmentManager();
            FragmentTransaction ft = fm.beginTransaction();
            loading.show(ft, DIALOG_WAIT_TAG);
            fm.executePendingTransactions();
        }
    }


    /**
     * Dismiss loading dialog
     */
    public void dismissLoadingDialog() {
        Fragment frag = getSupportFragmentManager().findFragmentByTag(DIALOG_WAIT_TAG);
        if (frag != null) {
            Log_OC.d(TAG, "dismiss loading dialog");
            LoadingDialog loading = (LoadingDialog) frag;
            loading.dismissAllowingStateLoss();
        }
    }


    private void doOnResumeAndBound() {
        mOperationsServiceBinder.addOperationListener(this, mHandler);
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

    public OCFile getCurrentDir() {
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

    public void checkForNewDevVersionNecessary(Context context) {
        if (getResources().getBoolean(R.bool.dev_version_direct_download_enabled)) {
            ArbitraryDataProvider arbitraryDataProvider = new ArbitraryDataProvider(getContentResolver());
            int count = arbitraryDataProvider.getIntegerValue(FilesSyncHelper.GLOBAL, APP_OPENED_COUNT);

            if (count > 10 || count == -1) {
                checkForNewDevVersion(this, context);
            }
        }
    }

    @Override
    public void returnVersion(Integer latestVersion) {
        showDevSnackbar(this, latestVersion, false, true);
    }

    public static void checkForNewDevVersion(LoadingVersionNumberTask.VersionDevInterface callback, Context context) {
        String url = context.getString(R.string.dev_latest);
        LoadingVersionNumberTask loadTask = new LoadingVersionNumberTask(callback);
        loadTask.execute(url);
    }

    public static void showDevSnackbar(Activity activity,
                                       Integer latestVersion,
                                       boolean openDirectly,
                                       boolean inBackground) {
        Integer currentVersion = -1;
        try {
            currentVersion = activity.getPackageManager().getPackageInfo(activity.getPackageName(), 0).versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            Log_OC.e(TAG, "Package not found", e);
        }

        if (latestVersion == -1 || currentVersion == -1) {
            DisplayUtils.showSnackMessage(activity, R.string.dev_version_no_information_available, Snackbar.LENGTH_LONG);
        }
        if (latestVersion > currentVersion) {
            if (openDirectly) {
                String devApkLink = (String) activity.getText(R.string.dev_link) + latestVersion + ".apk";
                Uri uriUrl = Uri.parse(devApkLink);
                Intent intent = new Intent(Intent.ACTION_VIEW, uriUrl);
                DisplayUtils.startIntentIfAppAvailable(intent, activity, R.string.no_browser_available);
            } else {
                Snackbar.make(activity.findViewById(android.R.id.content), R.string.dev_version_new_version_available,
                        Snackbar.LENGTH_LONG)
                        .setAction(activity.getString(R.string.version_dev_download), v -> {
                            String devApkLink = (String) activity.getText(R.string.dev_link) + latestVersion + ".apk";
                            Uri uriUrl = Uri.parse(devApkLink);
                            Intent intent = new Intent(Intent.ACTION_VIEW, uriUrl);
                            DisplayUtils.startIntentIfAppAvailable(intent, activity, R.string.no_browser_available);
                        }).show();
            }
        } else {
            if (!inBackground) {
                DisplayUtils.showSnackMessage(activity, R.string.dev_version_no_new_version_available, Snackbar.LENGTH_LONG);
            }
        }
    }

    public static void copyAndShareFileLink(FileActivity activity, OCFile file, String link) {
        ClipboardUtil.copyToClipboard(activity, link, false);
        Snackbar snackbar = Snackbar.make(activity.findViewById(android.R.id.content), R.string.clipboard_text_copied,
                                          Snackbar.LENGTH_LONG)
            .setAction(R.string.share, v -> showShareLinkDialog(activity, file, link));
        ThemeSnackbarUtils.colorSnackbar(activity, snackbar);
        snackbar.show();
    }

    public static void showShareLinkDialog(FileActivity activity, OCFile file, String link) {
        // Create dialog to allow the user choose an app to send the link
        Intent intentToShareLink = new Intent(Intent.ACTION_SEND);

        intentToShareLink.putExtra(Intent.EXTRA_TEXT, link);
        intentToShareLink.setType("text/plain");

        String username;
        try {
            OwnCloudAccount oca = new OwnCloudAccount(activity.getAccount(), activity);
            if (oca.getDisplayName() != null && !oca.getDisplayName().isEmpty()) {
                username = oca.getDisplayName();
            } else {
                username = com.owncloud.android.lib.common.accounts.AccountUtils
                    .getUsernameForAccount(activity.getAccount());
            }
        } catch (Exception e) {
            username = com.owncloud.android.lib.common.accounts.AccountUtils
                .getUsernameForAccount(activity.getAccount());
        }

        if (username != null) {
            intentToShareLink.putExtra(Intent.EXTRA_SUBJECT,
                                       activity.getString(R.string.subject_user_shared_with_you,
                                                          username,
                                                          file.getFileName()));
        } else {
            intentToShareLink.putExtra(Intent.EXTRA_SUBJECT,
                                       activity.getString(R.string.subject_shared_with_you,
                                                          file.getFileName()));
        }

        String[] packagesToExclude = new String[]{activity.getPackageName()};
        DialogFragment chooserDialog = ShareLinkToDialog.newInstance(intentToShareLink, packagesToExclude);
        chooserDialog.show(activity.getSupportFragmentManager(), FileDisplayActivity.FTAG_CHOOSER_DIALOG);
    }

    private void onUpdateNoteForShareOperationFinish(RemoteOperationResult result) {
        FileDetailSharingFragment sharingFragment = getShareFileFragment();

        if (result.isSuccess()) {
            if (sharingFragment != null) {
                sharingFragment.onUpdateShareInformation(result, getFile());
            }
        } else {
            DisplayUtils.showSnackMessage(this, R.string.note_could_not_sent);
        }
    }

    private void onUpdateShareInformation(RemoteOperationResult result, @StringRes int defaultError) {
        Snackbar snackbar;
        FileDetailSharingFragment sharingFragment = getShareFileFragment();

        if (result.isSuccess()) {
            updateFileFromDB();
            if (sharingFragment != null) {
                sharingFragment.onUpdateShareInformation(result, getFile());
            }
        } else if (sharingFragment != null && sharingFragment.getView() != null) {
            if (TextUtils.isEmpty(result.getMessage())) {
                snackbar = Snackbar.make(sharingFragment.getView(), defaultError, Snackbar.LENGTH_LONG);
            } else {
                snackbar = Snackbar.make(sharingFragment.getView(), result.getMessage(), Snackbar.LENGTH_LONG);
            }

            ThemeSnackbarUtils.colorSnackbar(this, snackbar);
            snackbar.show();
        }
    }

    private void onCreateShareViaLinkOperationFinish(CreateShareViaLinkOperation operation,
                                                     RemoteOperationResult result) {
        FileDetailSharingFragment sharingFragment = getShareFileFragment();

        if (result.isSuccess()) {
            updateFileFromDB();

            // if share to user and share via link multiple ocshares are returned,
            // therefore filtering for public_link
            String link = "";
            OCFile file = null;
            for (Object object : result.getData()) {
                OCShare shareLink = (OCShare) object;
                if (TAG_PUBLIC_LINK.equalsIgnoreCase(shareLink.getShareType().name())) {
                    link = shareLink.getShareLink();
                    file = getStorageManager().getFileByPath(shareLink.getPath());
                    break;
                }
            }

            copyAndShareFileLink(this, file, link);

            if (sharingFragment != null) {
                sharingFragment.onUpdateShareInformation(result, file);
            }
        } else {
            // Detect Failure (403) --> maybe needs password
            String password = operation.getPassword();
            if (result.getCode() == RemoteOperationResult.ResultCode.SHARE_FORBIDDEN &&
                TextUtils.isEmpty(password) &&
                getCapabilities().getFilesSharingPublicEnabled().isUnknown()) {
                // Was tried without password, but not sure that it's optional.

                // Try with password before giving up; see also ShareFileFragment#OnShareViaLinkListener
                if (sharingFragment != null && sharingFragment.isAdded()) { // only if added to the view hierarchy

                    sharingFragment.requestPasswordForShareViaLink(true,
                                                                   getCapabilities().getFilesSharingPublicAskForOptionalPassword()
                                                                       .isTrue());
                }

            } else {
                if (sharingFragment != null) {
                    sharingFragment.refreshSharesFromDB();
                }
                Snackbar snackbar = Snackbar.make(findViewById(android.R.id.content),
                                                  ErrorMessageAdapter.getErrorCauseMessage(result,
                                                                                           operation,
                                                                                           getResources()),
                                                  Snackbar.LENGTH_LONG);
                ThemeSnackbarUtils.colorSnackbar(this, snackbar);
                snackbar.show();
            }
        }
    }

    /**
     * Shortcut to get access to the {@link FileDetailSharingFragment} instance, if any
     *
     * @return A {@link FileDetailSharingFragment} instance, or null
     */
    private @Nullable
    @Deprecated
    FileDetailSharingFragment getShareFileFragment() {
        Fragment fragment = getSupportFragmentManager().findFragmentByTag(ShareActivity.TAG_SHARE_FRAGMENT);

        if (fragment == null) {
            fragment = getSupportFragmentManager().findFragmentByTag(FileDisplayActivity.TAG_LIST_OF_FILES);
        }

        if (fragment instanceof FileDetailSharingFragment) {
            return (FileDetailSharingFragment) fragment;
        } else if (fragment instanceof FileDetailFragment) {
            FileDetailFragment fileDetailFragment = (FileDetailFragment) fragment;
            return fileDetailFragment.getFileDetailSharingFragment();
        } else {
            return null;
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        if (UsersAndGroupsSearchProvider.ACTION_SHARE_WITH.equals(intent.getAction())) {
            Uri data = intent.getData();
            String dataString = intent.getDataString();
            String shareWith = dataString.substring(dataString.lastIndexOf('/') + 1);

            ArrayList<String> existingSharees = new ArrayList<>();
            for (OCShare share : getStorageManager().getSharesWithForAFile(getFile().getRemotePath(),
                                                                           getAccount().name)) {
                existingSharees.add(share.getShareType() + "_" + share.getShareWith());
            }

            String dataAuthority = data.getAuthority();
            ShareType shareType = UsersAndGroupsSearchProvider.getShareType(dataAuthority);

            if (!existingSharees.contains(shareType + "_" + shareWith)) {
                doShareWith(shareWith, shareType);
            }
        }
    }

    private void doShareWith(String shareeName, ShareType shareType) {
        getFileOperationsHelper().shareFileWithSharee(getFile(),
                                                      shareeName,
                                                      shareType,
                                                      getAppropriatePermissions(shareType));
    }

    private int getAppropriatePermissions(ShareType shareType) {
        if (getFile().isSharedWithMe()) {
            return OCShare.READ_PERMISSION_FLAG;    // minimum permissions
        } else if (ShareType.FEDERATED.equals(shareType)) {
            return getFile().isFolder() ? OCShare.FEDERATED_PERMISSIONS_FOR_FOLDER :
                OCShare.FEDERATED_PERMISSIONS_FOR_FILE;
        } else {
            return getFile().isFolder() ? OCShare.MAXIMUM_PERMISSIONS_FOR_FOLDER :
                OCShare.MAXIMUM_PERMISSIONS_FOR_FILE;
        }
    }
}
