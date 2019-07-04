/**
 *   Nextcloud Android client application
 *
 *   @author Bartosz Przybylski
 *   Copyright (C) 2016 Bartosz Przybylski
 *   Copyright (C) 2016 Nextcloud
 *
 *   This program is free software; you can redistribute it and/or
 *   modify it under the terms of the GNU AFFERO GENERAL PUBLIC LICENSE
 *   License as published by the Free Software Foundation; either
 *   version 3 of the License, or any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU AFFERO GENERAL PUBLIC LICENSE for more details.
 *
 *   You should have received a copy of the GNU Affero General Public
 *   License along with this program.  If not, see <http://www.gnu.org/licenses/>.
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

import com.owncloud.android.MainApp;
import com.owncloud.android.R;
import com.owncloud.android.datamodel.FileDataStorageManager;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.utils.FileStorageUtils;

import java.io.File;

import androidx.appcompat.app.AlertDialog;

/**
 * @author Bartosz Przybylski
 */
public class StorageMigration {
    private static final String TAG = StorageMigration.class.getName();

    private Context mContext;
    private String mSourceStoragePath;
    private String mTargetStoragePath;

    private StorageMigrationProgressListener mListener;

    public interface StorageMigrationProgressListener {
        void onStorageMigrationFinished(String storagePath, boolean succeed);
        void onCancelMigration();
    }

    public StorageMigration(Context context, String sourcePath, String targetPath) {
        mContext = context;
        mSourceStoragePath = sourcePath;
        mTargetStoragePath = targetPath;
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
                    mSourceStoragePath,
                    mTargetStoragePath,
                    progressDialog,
                    mListener).execute();

            progressDialog.getButton(ProgressDialog.BUTTON_POSITIVE).setVisibility(View.GONE);
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
                        if (mListener != null) {
                            mListener.onCancelMigration();
                        }
                    }
                })
                .setNegativeButton(R.string.common_cancel, new OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        if (mListener != null) {
                            mListener.onCancelMigration();
                        }
                    }
                })
                .setNeutralButton(R.string.file_migration_use_data_folder, new OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        ProgressDialog progressDialog = createMigrationProgressDialog();
                        progressDialog.show();
                        new StoragePathSwitchTask(
                                mContext,
                                mSourceStoragePath,
                                mTargetStoragePath,
                                progressDialog,
                                mListener).execute();

                        progressDialog.getButton(ProgressDialog.BUTTON_POSITIVE).setVisibility(View.GONE);

                    }
                })
                .setPositiveButton(R.string.file_migration_override_data_folder, new OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        ProgressDialog progressDialog = createMigrationProgressDialog();
                        progressDialog.show();
                        new FileMigrationTask(
                                mContext,
                                mSourceStoragePath,
                                mTargetStoragePath,
                                progressDialog,
                                mListener).execute();

                        progressDialog.getButton(ProgressDialog.BUTTON_POSITIVE).setVisibility(View.GONE);
                    }
                })
                .create()
                .show();
    }

    private ProgressDialog createMigrationProgressDialog() {
        ProgressDialog progressDialog = new ProgressDialog(mContext);
        progressDialog.setCancelable(false);
        progressDialog.setTitle(R.string.file_migration_dialog_title);
        progressDialog.setMessage(mContext.getString(R.string.file_migration_preparing));
        progressDialog.setButton(
                ProgressDialog.BUTTON_POSITIVE,
                mContext.getString(R.string.drawer_close),
                new OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.dismiss();
                    }
                });
        return progressDialog;
    }

    private static abstract class FileMigrationTaskBase extends AsyncTask<Void, Integer, Integer> {
        protected String mStorageSource;
        protected String mStorageTarget;
        protected Context mContext;
        protected ProgressDialog mProgressDialog;
        protected StorageMigrationProgressListener mListener;

        protected String mAuthority;
        protected Account[] mOcAccounts;

        public FileMigrationTaskBase(Context context,
                                     String source,
                                     String target,
                                     ProgressDialog progressDialog,
                                     StorageMigrationProgressListener listener) throws SecurityException {
            mContext = context;
            mStorageSource = source;
            mStorageTarget = target;
            mProgressDialog = progressDialog;
            mListener = listener;

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
                    mProgressDialog.setIndeterminateDrawable(mContext.getResources().getDrawable(R.drawable.image_fail));
                }
            }

            if (mListener != null) {
                mListener.onStorageMigrationFinished(succeed ? mStorageTarget : mStorageSource, succeed);
            }
        }

        private void askToStillMove() {
            new AlertDialog.Builder(mContext)
                    .setTitle(R.string.file_migration_source_not_readable_title)
                    .setMessage(mContext.getString(R.string.file_migration_source_not_readable, mStorageTarget))
                    .setNegativeButton(R.string.common_no, new OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            dialogInterface.dismiss();
                        }
                    })
                    .setPositiveButton(R.string.common_yes, new OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            if (mListener != null) {
                                mListener.onStorageMigrationFinished(mStorageTarget, true);
                            }
                        }
                    })
                    .create()
                    .show();
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
                                     String source,
                                     String target,
                                     ProgressDialog progressDialog,
                                     StorageMigrationProgressListener listener) {
            super(context, source, target, progressDialog, listener);
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
                                 String source,
                                 String target,
                                 ProgressDialog progressDialog,
                                 StorageMigrationProgressListener listener) {
            super(context, source, target, progressDialog, listener);
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

            for (File f : src.listFiles()) {
                if (f.isDirectory()) {
                    copyDirs(f, new File(dst, f.getName()));
                } else if (!FileStorageUtils.copyFile(f, new File(dst, f.getName()))) {
                    throw new MigrationException(R.string.file_migration_failed_while_coping);
                }
            }

        }

        private void updateIndex(Context context) throws MigrationException {
            FileDataStorageManager manager = new FileDataStorageManager(null, context.getContentResolver());

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
