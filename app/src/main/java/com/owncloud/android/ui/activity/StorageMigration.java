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
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.os.AsyncTask;
import android.view.View;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.nextcloud.client.account.User;
import com.nextcloud.client.account.UserAccountManagerImpl;
import com.nextcloud.utils.AppConstants;
import com.nextcloud.utils.extensions.FileExtensionsKt;
import com.owncloud.android.MainApp;
import com.owncloud.android.R;
import com.owncloud.android.datamodel.FileDataStorageManager;
import com.owncloud.android.datamodel.UploadsStorageManager;
import com.owncloud.android.db.OCUpload;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.utils.FileStorageUtils;
import com.owncloud.android.utils.theme.ViewThemeUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.LinkedHashSet;
import java.util.Set;

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
        if (dataFoldersOverlap()) {
            Log_OC.w(TAG, "Source and target data folders overlap, skipping migration");
            notifyMigrationCancelled();
            return;
        }

        if (storageFolderAlreadyExists()) {
            askToOverride();
            return;
        }

        startFileMigration();
    }

    private boolean storageFolderAlreadyExists() {
        File f = new File(mTargetStoragePath, MainApp.getDataFolder());
        return f.exists() && f.isDirectory();
    }

    private boolean dataFoldersOverlap() {
        String sourceDataFolder = canonicalDataFolder(mSourceStoragePath);
        String targetDataFolder = canonicalDataFolder(mTargetStoragePath);

        return sourceDataFolder.equals(targetDataFolder) ||
            sourceDataFolder.startsWith(targetDataFolder + File.separator) ||
            targetDataFolder.startsWith(sourceDataFolder + File.separator);
    }

    private static String canonicalDataFolder(String storagePath) {
        return FileExtensionsKt.getCanonicalPathOrAbsolute(new File(storagePath, MainApp.getDataFolder()));
    }

    private void askToOverride() {
        final MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(mContext)
                .setMessage(R.string.file_migration_directory_already_exists)
                .setCancelable(true)
                .setOnCancelListener(dialogInterface -> notifyMigrationCancelled())
                .setNegativeButton(R.string.common_cancel, (dialogInterface, i) -> notifyMigrationCancelled())
                .setNeutralButton(R.string.file_migration_use_data_folder, (dialogInterface, i) -> startStoragePathSwitch())
                .setPositiveButton(R.string.file_migration_override_data_folder, (dialogInterface, i) -> startFileMigration());

        viewThemeUtils.dialog.colorMaterialAlertDialogBackground(mContext, builder);
        builder.create().show();
    }

    private void notifyMigrationCancelled() {
        if (mListener != null) {
            mListener.onCancelMigration();
        }
    }

    private void startFileMigration() {
        ProgressDialog progressDialog = showMigrationProgressDialog();
        new FileMigrationTask(
                mContext,
                user,
                mSourceStoragePath,
                mTargetStoragePath,
                progressDialog,
                mListener,
                viewThemeUtils).execute();
    }

    private void startStoragePathSwitch() {
        ProgressDialog progressDialog = showMigrationProgressDialog();
        new StoragePathSwitchTask(
                mContext,
                user,
                mSourceStoragePath,
                mTargetStoragePath,
                progressDialog,
                mListener,
                viewThemeUtils).execute();
    }

    private ProgressDialog showMigrationProgressDialog() {
        ProgressDialog progressDialog = createMigrationProgressDialog();
        progressDialog.show();
        progressDialog.getButton(ProgressDialog.BUTTON_POSITIVE).setVisibility(View.GONE);

        return progressDialog;
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
        @SuppressLint("StaticFieldLeak") protected Context mContext;
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
            if (progress.length > 0 && progress[0] != 0 && canShowUi()) {
                mProgressDialog.setMessage(mContext.getString(progress[0]));
            }
        }

        @Override
        protected void onPostExecute(Integer code) {
            boolean succeed = code == 0;

            if (mListener != null) {
                mListener.onStorageMigrationFinished(succeed ? mStorageTarget : mStorageSource, succeed);
            }

            if (!canShowUi()) {
                return;
            }

            if (succeed) {
                dismissProgressDialog();
                return;
            }

            if (code == R.string.file_migration_failed_not_readable) {
                dismissProgressDialog();
                askToStillMove();
                return;
            }

            mProgressDialog.setMessage(mContext.getString(code));
            mProgressDialog.getButton(ProgressDialog.BUTTON_POSITIVE).setVisibility(View.VISIBLE);
            mProgressDialog.setIndeterminateDrawable(ResourcesCompat.getDrawable(mContext.getResources(),
                                                                                 R.drawable.image_fail,
                                                                                 null));
        }

        private boolean canShowUi() {
            return !(mContext instanceof Activity activity) || (!activity.isFinishing() && !activity.isDestroyed());
        }

        private void dismissProgressDialog() {
            if (mProgressDialog.isShowing()) {
                mProgressDialog.dismiss();
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
                        Thread.sleep(AppConstants.MILLIS_PER_SECOND);
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
        private static class MigrationException extends Exception {
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

        private boolean indexUpdateStarted;

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
                deleteRecursive(new File(mStorageTarget + File.separator + MainApp.getDataFolder()));

                File srcFile = new File(mStorageSource + File.separator + MainApp.getDataFolder());

                try {
                    Files.createDirectories(srcFile.toPath());
                } catch (IOException e) {
                    Log_OC.e(TAG, "Could not create directory: " + srcFile.getAbsolutePath(), e);
                }

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

            if (!dstFile.canRead() || !dstFile.canWrite()) {
                throw new MigrationException(R.string.file_migration_failed_not_writable);
            }

            if (!srcFile.canRead()) {
                throw new MigrationException(R.string.file_migration_failed_not_readable);
            }

            if (new File(dstFile, MainApp.getDataFolder()).exists()) {
                throw new MigrationException(R.string.file_migration_failed_dir_already_exists);
            }

            if (dstFile.getFreeSpace() < FileStorageUtils.getFolderSize(new File(srcFile, MainApp.getDataFolder()))) {
                throw new MigrationException(R.string.file_migration_failed_not_enough_space);
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

            File[] files = src.listFiles();
            if (files == null) {
                throw new MigrationException(R.string.file_migration_failed_while_coping);
            }

            for (File f : files) {
                File target = new File(dst, f.getName());
                if (f.isDirectory()) {
                    copyDirs(f, target);
                    continue;
                }

                if (!FileStorageUtils.copyFile(f, target)) {
                    throw new MigrationException(R.string.file_migration_failed_while_coping);
                }

                if (!target.setLastModified(f.lastModified())) {
                    Log_OC.w(TAG, "Could not preserve modification time of " + target.getAbsolutePath());
                }
            }
        }

        private void updateIndex(Context context) throws MigrationException {
            indexUpdateStarted = true;
            FileDataStorageManager manager = new FileDataStorageManager(user, context.getContentResolver());
            UploadsStorageManager uploadsStorageManager =
                new UploadsStorageManager(UserAccountManagerImpl.fromContext(context), context.getContentResolver());
            String targetPrefix = dataFolderPrefix(mStorageTarget);

            try {
                for (String sourcePrefix : sourceDataFolderPrefixes()) {
                    manager.migrateStoredFiles(sourcePrefix, targetPrefix);
                    migrateUploadPaths(uploadsStorageManager, sourcePrefix, targetPrefix);
                }
            } catch (Exception e) {
                Log_OC.e(TAG,e.getMessage(),e);
                throw new MigrationException(R.string.file_migration_failed_while_updating_index, e);
            }
        }

        private Set<String> sourceDataFolderPrefixes() {
            Set<String> prefixes = new LinkedHashSet<>();
            prefixes.add(dataFolderPrefix(mStorageSource));
            prefixes.add(dataFolderPrefix(FileExtensionsKt.getCanonicalPathOrAbsolute(new File(mStorageSource))));

            return prefixes;
        }

        private static String dataFolderPrefix(String storagePath) {
            return storagePath + File.separator + MainApp.getDataFolder() + File.separator;
        }

        private void migrateUploadPaths(UploadsStorageManager uploadsStorageManager,
                                        String sourcePrefix,
                                        String targetPrefix) {
            for (OCUpload upload : uploadsStorageManager.getAllStoredUploads()) {
                String localPath = upload.getLocalPath();
                if (localPath == null || !localPath.startsWith(sourcePrefix)) {
                    continue;
                }

                upload.setLocalPath(targetPrefix + localPath.substring(sourcePrefix.length()));
                uploadsStorageManager.updateUpload(upload);
            }
        }

        private void cleanup() {
            File srcFile = new File(mStorageSource + File.separator + MainApp.getDataFolder());
            if (!deleteRecursive(srcFile) && srcFile.exists()) {
                Log_OC.w(TAG, "Migration cleanup step failed: " + srcFile.getAbsolutePath());
            }
        }

        private boolean deleteRecursive(File f) {
            boolean res = true;
            File[] children = f.isDirectory() ? f.listFiles() : null;
            if (children != null) {
                for (File c : children) {
                    res = deleteRecursive(c) && res;
                }
            }
            return f.delete() && res;
        }

        private void rollback() {
            File dstFile = new File(mStorageTarget + File.separator + MainApp.getDataFolder());
            if (indexUpdateStarted) {
                Log_OC.w(TAG, "Index update failed, keeping copied data: " + dstFile.getAbsolutePath());
                return;
            }

            if (!deleteRecursive(dstFile) && dstFile.exists()) {
                Log_OC.w(TAG, "Rollback step failed: " + dstFile.getAbsolutePath());
            }
        }
    }
}
