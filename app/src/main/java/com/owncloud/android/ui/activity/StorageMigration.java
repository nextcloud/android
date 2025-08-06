/*
 *   Nextcloud Android client application
 *
 *   @author Bartosz Przybylski
 *   Copyright (C) 2016 Bartosz Przybylski
 *   Copyright (C) 2016 Nextcloud
 *
 *   SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.ui.activity;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.AsyncTask;
import android.view.View;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.nextcloud.client.account.User;
import com.owncloud.android.MainApp;
import com.owncloud.android.R;
import com.owncloud.android.datamodel.FileDataStorageManager;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.utils.FileStorageUtils;
import com.owncloud.android.utils.theme.ViewThemeUtils;

import java.io.File;

import androidx.appcompat.app.AlertDialog;
import androidx.core.content.res.ResourcesCompat;

/**
 * @author Bartosz Przybylski
 */
public class StorageMigration {
    private static final String TAG = StorageMigration.class.getName();

    private final Context mContext;
    private final User user;
    private final String mSourceStoragePath;
    private final String mTargetStoragePath;
    private final ViewThemeUtils viewThemeUtils;

    private StorageMigrationProgressListener mListener;

    public interface StorageMigrationProgressListener {
        void onStorageMigrationFinished(String storagePath, boolean succeed);
        void onCancelMigration();
    }

    public StorageMigration(Context context, User user, String sourcePath, String targetPath, ViewThemeUtils viewThemeUtils) {
        mContext = context;
        this.user = user;
        mSourceStoragePath = sourcePath;
        mTargetStoragePath = targetPath;
        this.viewThemeUtils = viewThemeUtils;
    }

    public void setStorageMigrationProgressListener(StorageMigrationProgressListener listener) {
        mListener = listener;
    }

    public void migrate() {
        if (storageFolderAlreadyExists()) {
            askToOverride();
        } else {
            ProgressDialog progressDialog = createMigrationProgressDialog();
            progressDialog.show();
            new FileMigrationTask(
                    mContext,
                    user,
                    mSourceStoragePath,
                    mTargetStoragePath,
                    progressDialog,
                    mListener,
                    viewThemeUtils).execute();

            progressDialog.getButton(ProgressDialog.BUTTON_POSITIVE).setVisibility(View.GONE);
        }
    }

    private boolean storageFolderAlreadyExists() {
        File f = new File(mTargetStoragePath, MainApp.getDataFolder());
        return f.exists() && f.isDirectory();
    }

    public static void a(ViewThemeUtils viewThemeUtils, Context context) {
        final MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(context)
            .setMessage(R.string.file_migration_directory_already_exists)
            .setCancelable(true)
            .setOnCancelListener(dialogInterface -> {

            })
            .setNegativeButton(R.string.common_cancel, (dialogInterface, i) -> {

            })
            .setNeutralButton(R.string.file_migration_use_data_folder, (dialogInterface, i) -> {

            })
            .setPositiveButton(R.string.file_migration_override_data_folder, (dialogInterface, i) -> {

            });

        viewThemeUtils.dialog.colorMaterialAlertDialogBackground(context, builder);

        AlertDialog alertDialog = builder.create();

        alertDialog.show();
    }

    private void askToOverride() {
        final MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(mContext)
                .setMessage(R.string.file_migration_directory_already_exists)
                .setCancelable(true)
                .setOnCancelListener(dialogInterface -> {
                    if (mListener != null) {
                        mListener.onCancelMigration();
                    }
                })
                .setNegativeButton(R.string.common_cancel, (dialogInterface, i) -> {
                    if (mListener != null) {
                        mListener.onCancelMigration();
                    }
                })
                .setNeutralButton(R.string.file_migration_use_data_folder, (dialogInterface, i) -> {
                    ProgressDialog progressDialog = createMigrationProgressDialog();
                    progressDialog.show();
                    new StoragePathSwitchTask(
                            mContext,
                            user,
                            mSourceStoragePath,
                            mTargetStoragePath,
                            progressDialog,
                            mListener,
                            viewThemeUtils).execute();

                    progressDialog.getButton(ProgressDialog.BUTTON_POSITIVE).setVisibility(View.GONE);

                })
                .setPositiveButton(R.string.file_migration_override_data_folder, (dialogInterface, i) -> {
                    ProgressDialog progressDialog = createMigrationProgressDialog();
                    progressDialog.show();
                    new FileMigrationTask(
                            mContext,
                            user,
                            mSourceStoragePath,
                            mTargetStoragePath,
                            progressDialog,
                            mListener,
                            viewThemeUtils).execute();

                    progressDialog.getButton(ProgressDialog.BUTTON_POSITIVE).setVisibility(View.GONE);
                });

        viewThemeUtils.dialog.colorMaterialAlertDialogBackground(mContext, builder);
        builder.create().show();
    }

    private ProgressDialog createMigrationProgressDialog() {
        ProgressDialog progressDialog = new ProgressDialog(mContext);
        progressDialog.setCancelable(false);
        progressDialog.setTitle(R.string.file_migration_dialog_title);
        progressDialog.setMessage(mContext.getString(R.string.file_migration_preparing));
        progressDialog.setButton(
                ProgressDialog.BUTTON_POSITIVE,
                mContext.getString(R.string.drawer_close),
                (dialogInterface, i) -> dialogInterface.dismiss());
        return progressDialog;
    }

    private static abstract class FileMigrationTaskBase extends AsyncTask<Void, Integer, Integer> {
        protected String mStorageSource;
        protected String mStorageTarget;
        protected Context mContext;
        protected User user;
        protected ProgressDialog mProgressDialog;
        protected StorageMigrationProgressListener mListener;

        protected String mAuthority;
        protected Account[] mOcAccounts;
        protected ViewThemeUtils viewThemeUtils;

        public FileMigrationTaskBase(Context context,
                                     User user,
                                     String source,
                                     String target,
                                     ProgressDialog progressDialog,
                                     StorageMigrationProgressListener listener,
                                     ViewThemeUtils viewThemeUtils) throws SecurityException {
            mContext = context;
            this.user = user;
            mStorageSource = source;
            mStorageTarget = target;
            mProgressDialog = progressDialog;
            mListener = listener;
            this.viewThemeUtils = viewThemeUtils;
            mAuthority = mContext.getString(R.string.authority);
            mOcAccounts = AccountManager.get(mContext).getAccountsByType(MainApp.getAccountType(context));
        }

        @Override
        protected void onProgressUpdate(Integer... progress) {
            if (progress.length > 1 && progress[0] != 0) {
                mProgressDialog.setMessage(mContext.getString(progress[0]));
            }
        }

        @Override
        protected void onPostExecute(Integer code) {
            if (code != 0) {
                mProgressDialog.setMessage(mContext.getString(code));
            } else {
                mProgressDialog.setMessage(mContext.getString(R.string.file_migration_ok_finished));
            }

            boolean succeed = code == 0;
            if (succeed) {
                mProgressDialog.hide();
            } else {

                if (code == R.string.file_migration_failed_not_readable) {
                    mProgressDialog.hide();
                    askToStillMove();
                } else {
                    mProgressDialog.getButton(ProgressDialog.BUTTON_POSITIVE).setVisibility(View.VISIBLE);
                    mProgressDialog.setIndeterminateDrawable(ResourcesCompat.getDrawable(mContext.getResources(),
                                                                                         R.drawable.image_fail,
                                                                                         null));
                }
            }

            if (mListener != null) {
                mListener.onStorageMigrationFinished(succeed ? mStorageTarget : mStorageSource, succeed);
            }
        }

        private void askToStillMove() {
            final MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(mContext)
                    .setTitle(R.string.file_migration_source_not_readable_title)
                    .setMessage(mContext.getString(R.string.file_migration_source_not_readable, mStorageTarget))
                    .setNegativeButton(R.string.common_no, (dialogInterface, i) -> dialogInterface.dismiss())
                    .setPositiveButton(R.string.common_yes, (dialogInterface, i) -> {
                        if (mListener != null) {
                            mListener.onStorageMigrationFinished(mStorageTarget, true);
                        }
                    });

            viewThemeUtils.dialog.colorMaterialAlertDialogBackground(mContext, builder);
            builder.create().show();
        }

        protected boolean[] saveAccountsSyncStatus() {
            boolean[] syncs = new boolean[mOcAccounts.length];
            for (int i = 0; i < mOcAccounts.length; ++i) {
                syncs[i] = ContentResolver.getSyncAutomatically(mOcAccounts[i], mAuthority);
            }
            return syncs;
        }

        protected void stopAccountsSyncing() {
            for (Account ocAccount : mOcAccounts) {
                ContentResolver.setSyncAutomatically(ocAccount, mAuthority, false);
            }
        }

        protected void waitForUnfinishedSynchronizations() {
            for (Account ocAccount : mOcAccounts) {
                while (ContentResolver.isSyncActive(ocAccount, mAuthority)) {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        Log_OC.w(TAG, "Thread interrupted while waiting for account to end syncing");
                        Thread.currentThread().interrupt();
                    }
                }
            }
        }

        protected void restoreAccountsSyncStatus(boolean... oldSync) {
            // If we don't have the old sync statuses, then
            // probably migration failed even before saving states,
            // which is weird and should be investigated.
            // But its better than crashing on ArrayOutOfBounds.
            if (oldSync == null) {
                return;
            }
            for (int i = 0; i < mOcAccounts.length; ++i) {
                ContentResolver.setSyncAutomatically(mOcAccounts[i], mAuthority, oldSync[i]);
            }
        }
    }

    static private class StoragePathSwitchTask extends FileMigrationTaskBase {

        public StoragePathSwitchTask(Context context,
                                     User user,
                                     String source,
                                     String target,
                                     ProgressDialog progressDialog,
                                     StorageMigrationProgressListener listener,
                                     ViewThemeUtils viewThemeUtils) {
            super(context, user, source, target, progressDialog, listener, viewThemeUtils);
        }

        @Override
        protected Integer doInBackground(Void... voids) {
            publishProgress(R.string.file_migration_preparing);

            boolean[] syncStates = null;
            try {
                publishProgress(R.string.file_migration_saving_accounts_configuration);
                syncStates = saveAccountsSyncStatus();

                publishProgress(R.string.file_migration_waiting_for_unfinished_sync);
                stopAccountsSyncing();
                waitForUnfinishedSynchronizations();
            } finally {
                publishProgress(R.string.file_migration_restoring_accounts_configuration);
                restoreAccountsSyncStatus(syncStates);
            }

            return 0;
        }
    }

    static private class FileMigrationTask extends FileMigrationTaskBase {
        private class MigrationException extends Exception {
            private static final long serialVersionUID = -4575848188034992066L;
            private int mResId;

            MigrationException(int resId) {
                super();
                this.mResId = resId;
            }

            MigrationException(int resId, Throwable t) {
                super(t);
                this.mResId = resId;
            }

            private int getResId() { return mResId; }
        }

        public FileMigrationTask(Context context,
                                 User user,
                                 String source,
                                 String target,
                                 ProgressDialog progressDialog,
                                 StorageMigrationProgressListener listener,
                                 ViewThemeUtils viewThemeUtils) {
            super(context, user, source, target, progressDialog, listener, viewThemeUtils);
        }

        @Override
        protected Integer doInBackground(Void... args) {
            publishProgress(R.string.file_migration_preparing);

            boolean[] syncState = null;

            try {
                File dstFile = new File(mStorageTarget + File.separator + MainApp.getDataFolder());
                deleteRecursive(dstFile);
                dstFile.delete();

                File srcFile = new File(mStorageSource + File.separator + MainApp.getDataFolder());
                srcFile.mkdirs();

                publishProgress(R.string.file_migration_checking_destination);

                checkDestinationAvailability();

                publishProgress(R.string.file_migration_saving_accounts_configuration);
                syncState = saveAccountsSyncStatus();

                publishProgress(R.string.file_migration_waiting_for_unfinished_sync);
                stopAccountsSyncing();
                waitForUnfinishedSynchronizations();

                publishProgress(R.string.file_migration_migrating);
                copyFiles();

                publishProgress(R.string.file_migration_updating_index);
                updateIndex(mContext);

                publishProgress(R.string.file_migration_cleaning);
                cleanup();

            } catch (MigrationException e) {
                rollback();
                return e.getResId();
            } finally {
                publishProgress(R.string.file_migration_restoring_accounts_configuration);
                restoreAccountsSyncStatus(syncState);
            }

            publishProgress(R.string.file_migration_ok_finished);

            return 0;
        }


        private void checkDestinationAvailability() throws MigrationException {
            File srcFile = new File(mStorageSource);
            File dstFile = new File(mStorageTarget);

            if (!dstFile.canRead() || !srcFile.canRead()) {
                throw new MigrationException(R.string.file_migration_failed_not_readable);
            }

            if (!dstFile.canWrite() || !srcFile.canWrite()) {
                throw new MigrationException(R.string.file_migration_failed_not_writable);
            }

            if (new File(dstFile, MainApp.getDataFolder()).exists()) {
                throw new MigrationException(R.string.file_migration_failed_dir_already_exists);
            }

            try {
                if (dstFile.getFreeSpace() < FileStorageUtils.getFolderSize(new File(srcFile, MainApp.getDataFolder()))) {
                    throw new MigrationException(R.string.file_migration_failed_not_enough_space);
                }
            } catch (MigrationException e) {
                throw new RuntimeException(e);
            }
        }

        private void copyFiles() throws MigrationException {
            File srcFile = new File(mStorageSource + File.separator + MainApp.getDataFolder());
            File dstFile = new File(mStorageTarget + File.separator + MainApp.getDataFolder());

            copyDirs(srcFile, dstFile);
        }

        private void copyDirs(File src, File dst) throws MigrationException {
            if (!dst.mkdirs()) {
                throw new MigrationException(R.string.file_migration_failed_while_coping);
            }

            for (File f : src.listFiles()) {
                if (f.isDirectory()) {
                    copyDirs(f, new File(dst, f.getName()));
                } else if (!FileStorageUtils.copyFile(f, new File(dst, f.getName()))) {
                    throw new MigrationException(R.string.file_migration_failed_while_coping);
                }
            }

        }

        private void updateIndex(Context context) throws MigrationException {
            FileDataStorageManager manager = new FileDataStorageManager(user, context.getContentResolver());

            try {
                manager.migrateStoredFiles(mStorageSource, mStorageTarget);
            } catch (Exception e) {
                Log_OC.e(TAG,e.getMessage(),e);
                throw new MigrationException(R.string.file_migration_failed_while_updating_index, e);
            }
        }

        private void cleanup() {
            File srcFile = new File(mStorageSource + File.separator + MainApp.getDataFolder());
            if (!deleteRecursive(srcFile)) {
                Log_OC.w(TAG, "Migration cleanup step failed");
            }
            srcFile.delete();
        }

        private boolean deleteRecursive(File f) {
            boolean res = true;
            if (f.isDirectory()) {
                for (File c : f.listFiles()) {
                    res = deleteRecursive(c) && res;
                }
            }
            return f.delete() && res;
        }

        private void rollback() {
            File dstFile = new File(mStorageTarget + File.separator + MainApp.getDataFolder());
            if (dstFile.exists() && !dstFile.delete()) {
                Log_OC.w(TAG, "Rollback step failed");
            }
        }
    }
}
