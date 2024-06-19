/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2021 TSI-mc
 * SPDX-FileCopyrightText: 2022 Álvaro Brey <alvaro@alvarobrey.com>
 * SPDX-FileCopyrightText: 2017-2023 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2019 Chris Narkiewicz <hello@ezaquarii.com>
 * SPDX-FileCopyrightText: 2018 Andy Scherzinger <info@andy-scherzinger.de>
 * SPDX-FileCopyrightText: 2016 ownCloud Inc.
 * SPDX-FileCopyrightText: 2015 María Asensio Valverde <masensio@solidgear.es>
 * SPDX-FileCopyrightText: 2013 David A. Velasco <dvelasco@solidgear.es>
 * SPDX-FileCopyrightText: 2011 Bartek Przybylski
 * SPDX-License-Identifier: GPL-2.0-only AND (AGPL-3.0-or-later OR GPL-2.0-only)
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
import com.nextcloud.client.account.User;
import com.nextcloud.client.account.UserAccountManager;
import com.nextcloud.client.jobs.BackgroundJobManager;
import com.nextcloud.client.jobs.download.FileDownloadWorker;
import com.nextcloud.client.jobs.upload.FileUploadHelper;
import com.nextcloud.client.network.ConnectivityService;
import com.nextcloud.utils.EditorUtils;
import com.nextcloud.utils.extensions.ActivityExtensionsKt;
import com.nextcloud.utils.extensions.BundleExtensionsKt;
import com.nextcloud.utils.extensions.FileExtensionsKt;
import com.nextcloud.utils.extensions.IntentExtensionsKt;
import com.owncloud.android.MainApp;
import com.owncloud.android.R;
import com.owncloud.android.authentication.AuthenticatorActivity;
import com.owncloud.android.datamodel.ArbitraryDataProvider;
import com.owncloud.android.datamodel.ArbitraryDataProviderImpl;
import com.owncloud.android.datamodel.OCFile;
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
import com.owncloud.android.lib.resources.files.model.ServerFileInterface;
import com.owncloud.android.lib.resources.shares.OCShare;
import com.owncloud.android.lib.resources.shares.ShareType;
import com.owncloud.android.operations.CreateShareViaLinkOperation;
import com.owncloud.android.operations.CreateShareWithShareeOperation;
import com.owncloud.android.operations.GetSharesForFileOperation;
import com.owncloud.android.operations.SynchronizeFileOperation;
import com.owncloud.android.operations.SynchronizeFolderOperation;
import com.owncloud.android.operations.UnshareOperation;
import com.owncloud.android.operations.UpdateNoteForShareOperation;
import com.owncloud.android.operations.UpdateShareInfoOperation;
import com.owncloud.android.operations.UpdateSharePermissionsOperation;
import com.owncloud.android.operations.UpdateShareViaLinkOperation;
import com.owncloud.android.providers.UsersAndGroupsSearchConfig;
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
import com.owncloud.android.ui.fragment.OCFileListFragment;
import com.owncloud.android.ui.helpers.FileOperationsHelper;
import com.owncloud.android.ui.preview.PreviewImageActivity;
import com.owncloud.android.utils.ClipboardUtil;
import com.owncloud.android.utils.DisplayUtils;
import com.owncloud.android.utils.ErrorMessageAdapter;
import com.owncloud.android.utils.FilesSyncHelper;
import com.owncloud.android.utils.theme.ViewThemeUtils;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

import javax.inject.Inject;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.ActionBar;
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
    LoadingVersionNumberTask.VersionDevInterface, FileDetailSharingFragment.OnEditShareListener {

    public static final String EXTRA_FILE = "com.owncloud.android.ui.activity.FILE";
    public static final String EXTRA_LIVE_PHOTO_FILE = "com.owncloud.android.ui.activity.LIVE.PHOTO.FILE";
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

    protected FileDownloadWorker.FileDownloadProgressListener fileDownloadProgressListener;
    protected FileUploadHelper fileUploadHelper = FileUploadHelper.Companion.instance();

    @Inject
    UserAccountManager accountManager;

    @Inject
    ConnectivityService connectivityService;

    @Inject
    BackgroundJobManager backgroundJobManager;

    @Inject
    EditorUtils editorUtils;

    @Inject
    UsersAndGroupsSearchConfig usersAndGroupsSearchConfig;

    @Inject
    ArbitraryDataProvider arbitraryDataProvider;

    @Override
    public void showFiles(boolean onDeviceOnly, boolean personalFiles) {
        // must be specialized in subclasses
        MainApp.showOnlyFilesOnDevice(onDeviceOnly);
        MainApp.showOnlyPersonalFiles(personalFiles);
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
        usersAndGroupsSearchConfig.reset();
        mHandler = new Handler();
        mFileOperationsHelper = new FileOperationsHelper(this, getUserAccountManager(), connectivityService, editorUtils);
        User user = null;

        if (savedInstanceState != null) {
            mFile = BundleExtensionsKt.getParcelableArgument(savedInstanceState, FileActivity.EXTRA_FILE, OCFile.class);
            mFromNotification = savedInstanceState.getBoolean(FileActivity.EXTRA_FROM_NOTIFICATION);
            mFileOperationsHelper.setOpIdWaitingFor(
                savedInstanceState.getLong(KEY_WAITING_FOR_OP_ID, Long.MAX_VALUE)
                                                   );
            final ActionBar actionBar = getSupportActionBar();
            if (actionBar != null && !(this instanceof PreviewImageActivity)) {
                viewThemeUtils.files.themeActionBar(this, actionBar, savedInstanceState.getString(KEY_ACTION_BAR_TITLE));
            }
        } else {
            user = IntentExtensionsKt.getParcelableArgument(getIntent(), FileActivity.EXTRA_USER, User.class);
            mFile = IntentExtensionsKt.getParcelableArgument(getIntent(), FileActivity.EXTRA_FILE, OCFile.class);
            mFromNotification = getIntent().getBooleanExtra(FileActivity.EXTRA_FROM_NOTIFICATION,
                                                            false);

            if (user != null) {
                setUser(user);
            }
        }

        mOperationsServiceConnection = new OperationsServiceConnection();
        bindService(new Intent(this, OperationsService.class), mOperationsServiceConnection,
                    Context.BIND_AUTO_CREATE);
    }

    public void checkInternetConnection() {
        if (connectivityService != null && connectivityService.isConnected()) {
            hideInfoBox();
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

        super.onDestroy();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        FileExtensionsKt.logFileSize(mFile, TAG);
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

        } else if (!result.isSuccess() && ResultCode.SSL_RECOVERABLE_PEER_UNVERIFIED == result.getCode()) {

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
        } else if (operation instanceof UpdateShareViaLinkOperation || operation instanceof UpdateShareInfoOperation) {
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
                Intent intent = ConflictsResolveActivity.createIntent(syncedFile,
                                                                      getUser().orElseThrow(RuntimeException::new),
                                                                      -1,
                                                                      null,
                                                                      this);
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
        dismissLoadingDialog();

        Fragment frag = getSupportFragmentManager().findFragmentByTag(DIALOG_WAIT_TAG);
        if (frag == null) {
            Log_OC.d(TAG, "show loading dialog");
            LoadingDialog loadingDialogFragment = LoadingDialog.newInstance(message);
            FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
            boolean isDialogFragmentReady = ActivityExtensionsKt.isDialogFragmentReady(this, loadingDialogFragment);
            if (isDialogFragmentReady) {
                loadingDialogFragment.show(fragmentTransaction, DIALOG_WAIT_TAG);
            }
        }
    }

    /**
     * Dismiss loading dialog
     */
    public void dismissLoadingDialog() {
        Fragment frag = getSupportFragmentManager().findFragmentByTag(DIALOG_WAIT_TAG);
        if (frag != null) {
            Log_OC.d(TAG, "dismiss loading dialog");
            LoadingDialog loadingDialogFragment = (LoadingDialog) frag;
            boolean isDialogFragmentReady = ActivityExtensionsKt.isDialogFragmentReady(this, loadingDialogFragment);
            if (isDialogFragmentReady) {
                loadingDialogFragment.dismiss();
            }
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
    public FileDownloadWorker.FileDownloadProgressListener getFileDownloadProgressListener() {
        return fileDownloadProgressListener;
    }

    @Override
    public FileUploadHelper getFileUploaderHelper() {
        return fileUploadHelper;
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
            ArbitraryDataProvider arbitraryDataProvider = new ArbitraryDataProviderImpl(this);
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
            String devApkLink = activity.getString(R.string.dev_link) + latestVersion + ".apk";
            if (openDirectly) {
                DisplayUtils.startLinkIntent(activity, devApkLink);
            } else {
                Snackbar.make(activity.findViewById(android.R.id.content), R.string.dev_version_new_version_available,
                              Snackbar.LENGTH_LONG)
                    .setAction(activity.getString(R.string.version_dev_download), v -> {
                        DisplayUtils.startLinkIntent(activity, devApkLink);
                    }).show();
            }
        } else {
            if (!inBackground) {
                DisplayUtils.showSnackMessage(activity, R.string.dev_version_no_new_version_available, Snackbar.LENGTH_LONG);
            }
        }
    }

    public static void copyAndShareFileLink(FileActivity activity,
                                            OCFile file,
                                            String link,
                                            final ViewThemeUtils viewThemeUtils) {
        ClipboardUtil.copyToClipboard(activity, link, false);
        Snackbar snackbar = Snackbar.make(activity.findViewById(android.R.id.content), R.string.clipboard_text_copied,
                                          Snackbar.LENGTH_LONG)
            .setAction(R.string.share, v -> showShareLinkDialog(activity, file, link));
        viewThemeUtils.material.themeSnackbar(snackbar);
        snackbar.show();
    }

    public static void showShareLinkDialog(FileActivity activity, ServerFileInterface file, String link) {
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
                sharingFragment.onUpdateShareInformation(result);
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
                sharingFragment.onUpdateShareInformation(result);
            }
        } else if (sharingFragment != null && sharingFragment.getView() != null) {
            if (TextUtils.isEmpty(result.getMessage())) {
                snackbar = Snackbar.make(sharingFragment.getView(), defaultError, Snackbar.LENGTH_LONG);
            } else {
                snackbar = Snackbar.make(sharingFragment.getView(), result.getMessage(), Snackbar.LENGTH_LONG);
            }

            viewThemeUtils.material.themeSnackbar(snackbar);
            snackbar.show();
        }
    }

    public void refreshList() {
        final Fragment fragment = getSupportFragmentManager().findFragmentByTag(FileDisplayActivity.TAG_LIST_OF_FILES);
        if (fragment instanceof OCFileListFragment listFragment) {
            listFragment.onRefresh();
        } else if (fragment instanceof FileDetailFragment detailFragment) {
            detailFragment.goBackToOCFileListFragment();
        }
    }

    private void onCreateShareViaLinkOperationFinish(CreateShareViaLinkOperation operation,
                                                     RemoteOperationResult result) {
        FileDetailSharingFragment sharingFragment = getShareFileFragment();
        final Fragment fileListFragment = getSupportFragmentManager().findFragmentByTag(FileDisplayActivity.TAG_LIST_OF_FILES);

        if (result.isSuccess()) {
            updateFileFromDB();

            // if share to user and share via link multiple ocshares are returned,
            // therefore filtering for public_link
            String link = "";
            OCFile file = null;
            for (Object object : result.getData()) {
                if (object instanceof OCShare shareLink) {
                    ShareType shareType = shareLink.getShareType();

                    if (shareType != null && TAG_PUBLIC_LINK.equalsIgnoreCase(shareType.name())) {
                        link = shareLink.getShareLink();
                        file = getStorageManager().getFileByEncryptedRemotePath(shareLink.getPath());
                        break;
                    }
                }
            }

            copyAndShareFileLink(this, file, link, viewThemeUtils);

            if (sharingFragment != null) {
                sharingFragment.onUpdateShareInformation(result, file);
            }

            if (fileListFragment instanceof OCFileListFragment ocFileListFragment && file != null) {
                if (ocFileListFragment.getAdapterFiles().contains(file)) {
                    ocFileListFragment.updateOCFile(file);
                } else {
                    DisplayUtils.showSnackMessage(this, R.string.file_activity_shared_file_cannot_be_updated);
                }
            }
        } else {
            // Detect Failure (403) --> maybe needs password
            String password = operation.getPassword();
            if (result.getCode() == RemoteOperationResult.ResultCode.SHARE_FORBIDDEN &&
                TextUtils.isEmpty(password) &&
                getCapabilities().getFilesSharingPublicEnabled().isUnknown()) {
                // Was tried without password, but not sure that it's optional.

                // Try with password before giving up; see also ShareFileFragment#OnShareViaLinkListener
                if (sharingFragment != null && sharingFragment.isAdded()) {
                    // only if added to the view hierarchy

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
                viewThemeUtils.material.themeSnackbar(snackbar);
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
        } else if (fragment instanceof FileDetailFragment fileDetailFragment) {
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
            for (OCShare share : getStorageManager().getSharesWithForAFile(getFileFromDetailFragment().getRemotePath(),
                                                                           getAccount().name)) {
                existingSharees.add(share.getShareType() + "_" + share.getShareWith());
            }

            String dataAuthority = data.getAuthority();
            ShareType shareType = UsersAndGroupsSearchProvider.getShareType(dataAuthority);

            if (!existingSharees.contains(shareType + "_" + shareWith)) {
                doShareWith(shareWith, shareType);
            } else {
                DisplayUtils.showSnackMessage(this, getString(R.string.sharee_already_added_to_file));
            }
        }
    }

    /**
     * returns the file that is selected for sharing, getFile() only returns the containing folder
     */
    private OCFile getFileFromDetailFragment() {
        FileDetailFragment fragment = getFileDetailFragment();
        if (fragment != null) {
            return fragment.getFile();
        }
        return getFile();
    }

    /**
     * open the new sharing process fragment to create the share
     *
     * @param shareeName
     * @param shareType
     */
    protected void doShareWith(String shareeName, ShareType shareType) {
        FileDetailFragment fragment = getFileDetailFragment();
        if (fragment != null) {
            fragment.initiateSharingProcess(shareeName,
                                            shareType,
                                            usersAndGroupsSearchConfig.getSearchOnlyUsers());
        }
    }

    /**
     * open the new sharing process to modify the created share
     * @param share
     * @param screenTypePermission
     * @param isReshareShown
     * @param isExpiryDateShown
     */
    @Override
    public void editExistingShare(OCShare share, int screenTypePermission, boolean isReshareShown,
                                  boolean isExpiryDateShown) {
        FileDetailFragment fragment = getFileDetailFragment();
        if (fragment != null) {
            fragment.editExistingShare(share, screenTypePermission, isReshareShown, isExpiryDateShown);
        }
    }

    /**
     * callback triggered on closing/finishing the sharing process
     */
    @Override
    public void onShareProcessClosed() {
        FileDetailFragment fragment = getFileDetailFragment();
        if (fragment != null) {
            fragment.showHideFragmentView(false);
        }
    }

    private FileDetailFragment getFileDetailFragment() {
        Fragment fragment = getSupportFragmentManager().findFragmentByTag(FileDisplayActivity.TAG_LIST_OF_FILES);
        if (fragment instanceof FileDetailFragment) {
            return (FileDetailFragment) fragment;
        }
        return null;
    }
}
