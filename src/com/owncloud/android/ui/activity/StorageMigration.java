/**
 *   ownCloud Android client application
 *
 *   @author Bartosz Przybylski
 *   Copyright (C) 2015 ownCloud Inc.
 *   Copyright (C) 2015 Bartosz Przybylski
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
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.AsyncTask;
import android.support.v7.app.AlertDialog;

import com.owncloud.android.MainApp;
import com.owncloud.android.R;
import com.owncloud.android.datamodel.FileDataStorageManager;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.utils.FileStorageUtils;

import java.io.File;

/**
 * Created by Bartosz Przybylski on 07.11.2015.
 */
public class StorageMigration {
    private static final String TAG = StorageMigration.class.getName();

    public interface StorageMigrationProgressListener {
        void onStorageMigrationFinished(String storagePath, boolean succeed);
        void onCancelMigration();
    }

    private ProgressDialog mProgressDialog;
    private Context mContext;
    private String mSourceStoragePath;
    private String mTargetStoragePath;

    private StorageMigrationProgressListener mListener;

    public StorageMigration(Context context, String sourcePath, String targetPath) {
        mContext = context;
        mSourceStoragePath = sourcePath;
        mTargetStoragePath = targetPath;
    }

    public void setStorageMigrationProgressListener(StorageMigrationProgressListener listener) {
        mListener = listener;
    }

    public void migrate() {
        if (storageFolderAlreadyExists())
            askToOverride();
        else {
            AlertDialog progressDialog = createMigrationProgressDialog();
            progressDialog.show();
            new FileMigrationTask(
                    mContext,
                    mSourceStoragePath,
                    mTargetStoragePath,
                    progressDialog,
                    mListener).execute();

            progressDialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
        }
    }

    private boolean storageFolderAlreadyExists() {
        File f = new File(mTargetStoragePath, MainApp.getDataFolder());
        return f.exists() && f.isDirectory();
    }

    private void askToOverride() {

        new AlertDialog.Builder(mContext)
                .setMessage(R.string.file_migration_directory_already_exists)
                .setCancelable(true)
                .setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialogInterface) {
                        if (mListener != null)
                            mListener.onCancelMigration();
                    }
                })
                .setNegativeButton(R.string.common_cancel, new OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        if (mListener != null)
                            mListener.onCancelMigration();
                    }
                })
                .setNeutralButton(R.string.file_migration_use_data_folder, new OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        AlertDialog progressDialog = createMigrationProgressDialog();
                        progressDialog.show();
                        new StoragePathSwitchTask(
                                mContext,
                                mSourceStoragePath,
                                mTargetStoragePath,
                                progressDialog,
                                mListener).execute();

                        progressDialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);

                    }
                })
                .setPositiveButton(R.string.file_migration_override_data_folder, new OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        AlertDialog progressDialog = createMigrationProgressDialog();
                        progressDialog.show();
                        new FileMigrationTask(
                                mContext,
                                mSourceStoragePath,
                                mTargetStoragePath,
                                progressDialog,
                                mListener).execute();

                        progressDialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
                    }
                })
                .create()
                .show();
    }

    private AlertDialog createMigrationProgressDialog() {
        AlertDialog progressDialog = new AlertDialog.Builder(mContext)
                .setCancelable(false)
                .setTitle(R.string.file_migration_dialog_title)
                .setMessage(R.string.file_migration_preparing)
                .setPositiveButton(R.string.drawer_close, new OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.dismiss();
                    }
                })
                .create();
        return progressDialog;
    }

    abstract static private class FileMigrationTaskBase extends AsyncTask<Void, Integer, Integer> {
        protected String mStorageSource;
        protected String mStorageTarget;
        protected Context mContext;
        protected AlertDialog mProgressDialog;
        protected StorageMigrationProgressListener mListener;

        protected String mAuthority;
        protected Account[] mOcAccounts;

        public FileMigrationTaskBase(Context context, String source, String target, AlertDialog progressDialog, StorageMigrationProgressListener listener) {
            mContext = context;
            mStorageSource = source;
            mStorageTarget = target;
            mProgressDialog = progressDialog;
            mListener = listener;

            mAuthority = mContext.getString(R.string.authority);
            mOcAccounts = AccountManager.get(mContext).getAccountsByType(MainApp.getAccountType());
        }

        @Override
        protected void onProgressUpdate(Integer... progress) {
            if (progress.length > 1 && progress[0] != 0)
                mProgressDialog.setMessage(mContext.getString(progress[0]));
        }

        @Override
        protected void onPostExecute(Integer code) {
            if (code != 0) {
                mProgressDialog.setMessage(mContext.getString(code));
            } else {
                mProgressDialog.setMessage(mContext.getString(R.string.file_migration_ok_finished));
            }
            mProgressDialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE).setEnabled(true);
            boolean succeed = code == 0;
            if (mListener != null)
                mListener.onStorageMigrationFinished(succeed ? mStorageTarget : mStorageSource, succeed);
        }

        protected boolean[] saveAccountsSyncStatus() {
            boolean[] syncs = new boolean[mOcAccounts.length];
            for (int i = 0; i < mOcAccounts.length; ++i)
                syncs[i] = ContentResolver.getSyncAutomatically(mOcAccounts[i], mAuthority);
            return syncs;
        }

        protected void stopAccountsSyncing() {
            for (int i = 0; i < mOcAccounts.length; ++i)
                ContentResolver.setSyncAutomatically(mOcAccounts[i], mAuthority, false);
        }

        protected void waitForUnfinishedSynchronizations() {
            for (int i = 0; i < mOcAccounts.length; ++i)
                while (ContentResolver.isSyncActive(mOcAccounts[i], mAuthority))
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        Log_OC.w(TAG, "Thread interrupted while waiting for account to end syncing");
                        Thread.currentThread().interrupt();
                    }
        }

        protected void restoreAccountsSyncStatus(boolean oldSync[]) {
            for (int i = 0; i < mOcAccounts.length; ++i)
                ContentResolver.setSyncAutomatically(mOcAccounts[i], mAuthority, oldSync[i]);
        }
    }

    static private class StoragePathSwitchTask extends FileMigrationTaskBase {

        public StoragePathSwitchTask(Context context, String source, String target, AlertDialog progressDialog, StorageMigrationProgressListener listener) {
            super(context, source, target, progressDialog, listener);
        }

        @Override
        protected Integer doInBackground(Void... voids) {
            publishProgress(R.string.file_migration_preparing);

            Log_OC.stopLogging();
            boolean[] syncStates = new boolean[0];
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
            Log_OC.startLogging(mStorageTarget);

            return 0;
        }
    }

    static private class FileMigrationTask extends FileMigrationTaskBase {
        private class MigrationException extends Exception {
            private int mResId;

            MigrationException(int resId) {
                super();
                this.mResId = resId;
            }

            int getResId() { return mResId; }
        }

        public FileMigrationTask(Context context, String source, String target, AlertDialog progressDialog, StorageMigrationProgressListener listener) {
            super(context, source, target, progressDialog, listener);
        }

        @Override
        protected Integer doInBackground(Void... args) {
            publishProgress(R.string.file_migration_preparing);
            Log_OC.stopLogging();

            boolean[] syncState = new boolean[0];

            try {
                File dstFile = new File(mStorageTarget + File.separator + MainApp.getDataFolder());
                deleteRecursive(dstFile);
                dstFile.delete();

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
                Log_OC.startLogging(mStorageSource);
                return e.getResId();
            } finally {
                publishProgress(R.string.file_migration_restoring_accounts_configuration);
                restoreAccountsSyncStatus(syncState);
            }

            Log_OC.startLogging(mStorageTarget);
            publishProgress(R.string.file_migration_ok_finished);

            return 0;
        }


        void checkDestinationAvailability() throws MigrationException {
            File srcFile = new File(mStorageSource);
            File dstFile = new File(mStorageTarget);

            if (!dstFile.canRead() || !srcFile.canRead())
                throw new MigrationException(R.string.file_migration_failed_not_readable);

            if (!dstFile.canWrite() || !srcFile.canWrite())
                throw new MigrationException(R.string.file_migration_failed_not_writable);

            if (new File(dstFile, MainApp.getDataFolder()).exists())
                throw new MigrationException(R.string.file_migration_failed_dir_already_exists);

            if (dstFile.getFreeSpace() < FileStorageUtils.getFolderSize(new File(srcFile, MainApp.getDataFolder())))
                throw new MigrationException(R.string.file_migration_failed_not_enough_space);
        }

        void copyFiles() throws MigrationException {
            File srcFile = new File(mStorageSource + File.separator + MainApp.getDataFolder());
            File dstFile = new File(mStorageTarget + File.separator + MainApp.getDataFolder());

            copyDirs(srcFile, dstFile);
        }

        void copyDirs(File src, File dst) throws MigrationException {
            if (!dst.mkdirs())
                throw new MigrationException(R.string.file_migration_failed_while_coping);

            for (File f : src.listFiles()) {
                if (f.isDirectory())
                    copyDirs(f, new File(dst, f.getName()));
                else if (!FileStorageUtils.copyFile(f, new File(dst, f.getName())))
                    throw new MigrationException(R.string.file_migration_failed_while_coping);
            }

        }

        void updateIndex(Context context) throws MigrationException {
            FileDataStorageManager manager = new FileDataStorageManager(null, context.getContentResolver());

            try {
                manager.migrateStoredFiles(mStorageSource, mStorageTarget);
            } catch (Exception e) {
                throw new MigrationException(R.string.file_migration_failed_while_updating_index);
            }
        }

        void cleanup() {
            File srcFile = new File(mStorageSource + File.separator + MainApp.getDataFolder());
            if (!deleteRecursive(srcFile))
                Log_OC.w(TAG, "Migration cleanup step failed");
            srcFile.delete();
        }

        boolean deleteRecursive(File f) {
            boolean res = true;
            if (f.isDirectory())
                for (File c : f.listFiles())
                    res = deleteRecursive(c) && res;
            return f.delete() && res;
        }

        void rollback() {
            File dstFile = new File(mStorageTarget + File.separator + MainApp.getDataFolder());
            if (dstFile.exists())
                if (!dstFile.delete())
                    Log_OC.w(TAG, "Rollback step failed");
        }
    }
}
