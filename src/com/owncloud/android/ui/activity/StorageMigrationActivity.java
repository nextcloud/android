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
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.owncloud.android.MainApp;
import com.owncloud.android.R;
import com.owncloud.android.datamodel.FileDataStorageManager;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.utils.FileStorageUtils;

import java.io.File;

/**
 * Created by Bartosz Przybylski on 07.11.2015.
 */
public class StorageMigrationActivity extends AppCompatActivity {
	private static final String TAG = StorageMigrationActivity.class.getName();
	public static final String KEY_MIGRATION_TARGET_DIR = "MIGRATION_TARGET";
	public static final String KEY_MIGRATION_SOURCE_DIR = "MIGRATION_SOURCE";

	private ProgressBar mProgressBar;
	private Button mFinishButton;
	private TextView mFeedbackText;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.migration_layout);
		mProgressBar = (ProgressBar)findViewById(R.id.migrationProgress);
		mFinishButton = (Button)findViewById(R.id.finishButton);
		mFeedbackText = (TextView)findViewById(R.id.migrationText);

		mProgressBar.setProgress(0);
		mFinishButton.setVisibility(View.INVISIBLE);
		mFeedbackText.setText(R.string.file_migration_preparing);

		mFinishButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				setResult(RESULT_CANCELED);
				finish();
			}
		});

		String source = getIntent().getStringExtra(KEY_MIGRATION_SOURCE_DIR);
		String destination = getIntent().getStringExtra(KEY_MIGRATION_TARGET_DIR);

		if (source == null || destination == null) {
			Log_OC.e(TAG, "source or destination is null");
			finish();
		}

		new FileMigrationTask().execute(source, destination);
	}

	private class FileMigrationTask extends AsyncTask<String, Integer, Integer> {

		private String mStorageTarget;
		private String mStorageSource;
		private int mProgress;

		private static final int mProgressCopyUpperBound = 98;

		private class MigrationException extends Exception {
			private int mResId;
			/*
			 * @param resId resource identifier to use for displaying error
			 */
			MigrationException(int resId) {
				super();
				this.mResId = resId;
			}

			int getResId() { return mResId; }
		}

		private class MigrationCleanupException extends Exception {
			MigrationCleanupException() {}
		}

		@Override
		protected Integer doInBackground(String... args) {

			mStorageSource = args[0];
			mStorageTarget = args[1];
			mProgress = 0;

			publishProgress(mProgress++, R.string.file_migration_preparing);

			Context context = StorageMigrationActivity.this;
			String ocAuthority = context.getString(R.string.authority);

			Account[] ocAccounts = AccountManager.get(context).getAccountsByType(MainApp.getAccountType());
			boolean[] oldAutoSync = new boolean[ocAccounts.length];

			try {
				publishProgress(mProgress++, R.string.file_migration_checking_destination);

				checkDestinationAvailability();

				publishProgress(mProgress++, R.string.file_migration_saving_accounts_configuration);
				saveAccountsSyncStatus(ocAuthority, ocAccounts, oldAutoSync);

				publishProgress(mProgress++, R.string.file_migration_waiting_for_unfinished_sync);
				stopAccountsSyncing(ocAuthority, ocAccounts);
				waitForUnfinishedSynchronizations(ocAuthority, ocAccounts);

				publishProgress(mProgress++, R.string.file_migration_migrating);
				copyFiles();

				publishProgress(mProgress++, R.string.file_migration_updating_index);
				updateIndex(context);

				publishProgress(mProgress++, R.string.file_migration_cleaning);
				cleanup();

			} catch (MigrationException e) {
				rollback();
				return e.getResId();
			} catch (MigrationCleanupException e) {
				Log_OC.w(TAG, "Migration cleanup step failed");
				return 0;
			} finally {
				publishProgress(mProgress++, R.string.file_migration_restoring_accounts_configuration);
				restoreAccountsSyncStatus(ocAuthority, ocAccounts, oldAutoSync);
			}

			publishProgress(mProgress++, R.string.file_migration_ok_finished);

			return 0;
		}

		@Override
		protected void onProgressUpdate(Integer... progress) {
			mProgressBar.setProgress(progress[0]);
			if (progress.length > 1)
				mFeedbackText.setText(progress[1]);
		}

		@Override
		protected void onPostExecute(Integer code) {
			mFinishButton.setVisibility(View.VISIBLE);
			if (code != 0) {
				mFeedbackText.setText(code);
			} else {
				mFeedbackText.setText(R.string.file_migration_ok_finished);
				mFinishButton.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View view) {
						Intent resultIntent = new Intent();
						resultIntent.putExtra(KEY_MIGRATION_TARGET_DIR, mStorageTarget);
						setResult(RESULT_OK, resultIntent);
						finish();
					}
				});
			}
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
			mProgress = Math.max(mProgress, mProgressCopyUpperBound);
			publishProgress(mProgress);
		}

		void copyDirs(File src, File dst) throws MigrationException {
			if (!dst.mkdirs())
				throw new MigrationException(R.string.file_migration_failed_while_coping);

			for (File f : src.listFiles()) {

				mProgress = Math.min(mProgress+1, mProgressCopyUpperBound);
				publishProgress(mProgress);

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

		void cleanup() throws MigrationCleanupException {
			File srcFile = new File(mStorageSource + File.separator + MainApp.getDataFolder());
			if (!srcFile.delete())
				throw new MigrationCleanupException();
		}

		void rollback() {
			File dstFile = new File(mStorageTarget + File.separator + MainApp.getDataFolder());
			if (dstFile.exists())
				if (!dstFile.delete())
					Log_OC.w(TAG, "Rollback step failed");
		}

		void saveAccountsSyncStatus(String authority, Account accounts[], boolean syncs[]) {
			for (int i = 0; i < accounts.length; ++i)
				syncs[i] = ContentResolver.getSyncAutomatically(accounts[i], authority);
		}

		void stopAccountsSyncing(String authority, Account accounts[]) {
			for (int i = 0; i < accounts.length; ++i)
				ContentResolver.setSyncAutomatically(accounts[i], authority, false);
		}

		void waitForUnfinishedSynchronizations(String authority, Account accounts[]) {
			for (int i = 0; i < accounts.length; ++i)
				while (ContentResolver.isSyncActive(accounts[i], authority))
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {
						Log_OC.w(TAG, "Thread interrupted while waiting for account to end syncing");
						Thread.currentThread().interrupt();
					}
		}

		void restoreAccountsSyncStatus(String authority, Account accounts[], boolean oldSync[]) {
			for (int i = 0; i < accounts.length; ++i)
				ContentResolver.setSyncAutomatically(accounts[i], authority, oldSync[i]);
		}

	}
}
