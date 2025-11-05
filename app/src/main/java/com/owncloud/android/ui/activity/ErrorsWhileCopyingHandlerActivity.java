/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2021 Chris Narkiewicz <hello@ezaquarii.com>
 * SPDX-FileCopyrightText: 2018-2020 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2017-2018 Andy Scherzinger <info@andy-scherzinger.de>
 * SPDX-FileCopyrightText: 2015 ownCloud Inc.
 * SPDX-FileCopyrightText: 2013 María Asensio Valverde <masensio@solidgear.es>
 * SPDX-FileCopyrightText: 2012-2013 David A. Velasco <dvelasco@solidgear.es>
 * SPDX-License-Identifier: GPL-2.0-only AND (AGPL-3.0-or-later OR GPL-2.0-only)
 */
package com.owncloud.android.ui.activity;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.text.method.ScrollingMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import com.nextcloud.client.account.User;
import com.nextcloud.utils.extensions.IntentExtensionsKt;
import com.owncloud.android.R;
import com.owncloud.android.datamodel.FileDataStorageManager;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.ui.dialog.IndeterminateProgressDialog;
import com.owncloud.android.utils.DisplayUtils;
import com.owncloud.android.utils.FileStorageUtils;

import java.io.File;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.DialogFragment;

/**
 * Activity reporting errors occurred when local files uploaded to an Nextcloud account with an app
 * in version under 1.3.16 where being copied to the ownCloud local folder.
 * Allows the user move the files to the Nextcloud local folder. let them unlinked to the remote files.
 * Shown when the error notification summarizing the list of errors is clicked by the user.
 */
public class ErrorsWhileCopyingHandlerActivity  extends AppCompatActivity implements OnClickListener {

    private static final String TAG = ErrorsWhileCopyingHandlerActivity.class.getSimpleName();

    public static final String EXTRA_USER =
        ErrorsWhileCopyingHandlerActivity.class.getCanonicalName() + ".EXTRA_ACCOUNT";
    public static final String EXTRA_LOCAL_PATHS =
            ErrorsWhileCopyingHandlerActivity.class.getCanonicalName() + ".EXTRA_LOCAL_PATHS";
    public static final String EXTRA_REMOTE_PATHS =
            ErrorsWhileCopyingHandlerActivity.class.getCanonicalName() + ".EXTRA_REMOTE_PATHS";

    private static final String WAIT_DIALOG_TAG = "WAIT_DIALOG";

    protected User user;
    protected FileDataStorageManager mStorageManager;
    protected List<String> mLocalPaths;
    protected List<String> mRemotePaths;
    protected ArrayAdapter<String> mAdapter;
    protected Handler mHandler;
    private DialogFragment mCurrentDialog;

    /**
     * {@link}
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

                /// read extra parameters in intent
        Intent intent = getIntent();
        user = IntentExtensionsKt.getParcelableArgument(intent, EXTRA_USER, User.class);
        mRemotePaths = intent.getStringArrayListExtra(EXTRA_REMOTE_PATHS);
        mLocalPaths = intent.getStringArrayListExtra(EXTRA_LOCAL_PATHS);
        mStorageManager = new FileDataStorageManager(user, getContentResolver());
        mHandler = new Handler();
        if (mCurrentDialog != null) {
            mCurrentDialog.dismiss();
            mCurrentDialog = null;
        }

        /// load generic layout
        setContentView(R.layout.generic_explanation);

        /// customize text message
        TextView textView = findViewById(R.id.message);
        String appName = getString(R.string.app_name);
        String message = String.format(getString(R.string.sync_foreign_files_forgotten_explanation),
                appName, appName, appName, appName, user.getAccountName());
        textView.setText(message);
        textView.setMovementMethod(new ScrollingMovementMethod());

        /// load the list of local and remote files that failed
        ListView listView = findViewById(R.id.list);
        if (mLocalPaths != null && mLocalPaths.size() > 0) {
            mAdapter = new ErrorsWhileCopyingListAdapter();
            listView.setAdapter(mAdapter);
        } else {
            listView.setVisibility(View.GONE);
            mAdapter = null;
        }

        /// customize buttons
        Button cancelBtn = findViewById(R.id.cancel);
        Button okBtn = findViewById(R.id.ok);

        okBtn.setText(R.string.foreign_files_move);
        cancelBtn.setOnClickListener(this);
        okBtn.setOnClickListener(this);
    }

        /**
         * Customized adapter, showing the local files as main text in two-lines list item and the
         * remote files as the secondary text.
         */
    public class ErrorsWhileCopyingListAdapter extends ArrayAdapter<String> {

        ErrorsWhileCopyingListAdapter() {
            super(ErrorsWhileCopyingHandlerActivity.this, android.R.layout.two_line_list_item,
                    android.R.id.text1, mLocalPaths);
        }

        @Override
        public boolean isEnabled(int position) {
            return false;
        }

            /**
         * {@inheritDoc}
         */
        @Override
        public View getView (int position, View convertView, @NonNull ViewGroup parent) {
            View view = convertView;
            if (view == null) {
                LayoutInflater vi = (LayoutInflater) getSystemService(
                        Context.LAYOUT_INFLATER_SERVICE);
                view = vi.inflate(android.R.layout.two_line_list_item, null);
            }
            if (view != null)  {
                String localPath = getItem(position);
                if (localPath != null) {
                    TextView text1 = view.findViewById(android.R.id.text1);
                    if (text1 != null) {
                        text1.setText(String.format(getString(R.string.foreign_files_local_text), localPath));
                    }
                }
                if (mRemotePaths != null && mRemotePaths.size() > 0 && position >= 0 &&
                        position < mRemotePaths.size()) {
                    TextView text2 = view.findViewById(android.R.id.text2);
                    String remotePath = mRemotePaths.get(position);
                    if (text2 != null && remotePath != null) {
                        text2.setText(String.format(getString(R.string.foreign_files_remote_text), remotePath));
                    }
                }
            }
            return view;
        }
    }


    /**
     * Listener method to perform the MOVE / CANCEL action available in this activity.
     *
     * @param v     Clicked view (button MOVE or CANCEL)
     */
    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.ok) {
            /// perform movement operation in background thread
            Log_OC.d(TAG, "Clicked MOVE, start movement");
            new MoveFilesTask().execute();

        } else if (v.getId() == R.id.cancel) {
            /// just finish
            Log_OC.d(TAG, "Clicked CANCEL, bye");
            finish();

        } else {
            Log_OC.e(TAG, "Clicked phantom button, id: " + v.getId());
        }
    }


    /**
     * Asynchronous task performing the move of all the local files to the ownCloud folder.
     */
    @SuppressLint("StaticFieldLeak")
    private class MoveFilesTask extends AsyncTask<Void, Void, Boolean> {

        /**
         * Updates the UI before trying the movement
         */
        @Override
        protected void onPreExecute () {
            /// progress dialog and disable 'Move' button
            mCurrentDialog = IndeterminateProgressDialog.newInstance(R.string.wait_a_moment, false);
            mCurrentDialog.show(getSupportFragmentManager(), WAIT_DIALOG_TAG);
            findViewById(R.id.ok).setEnabled(false);
        }


        /**
         * Performs the movement
         *
         * @return     'False' when the movement of any file fails.
         */
        @Override
        protected Boolean doInBackground(Void... params) {
            while (!mLocalPaths.isEmpty()) {
                String currentPath = mLocalPaths.get(0);
                File currentFile = new File(currentPath);
                String expectedPath = FileStorageUtils.getSavePath(user.getAccountName()) + mRemotePaths.get(0);
                File expectedFile = new File(expectedPath);

                if (expectedFile.equals(currentFile) || currentFile.renameTo(expectedFile)) {
                    // SUCCESS
                    OCFile file = mStorageManager.getFileByPath(mRemotePaths.get(0));
                    file.setStoragePath(expectedPath);
                    mStorageManager.saveFile(file);
                    mRemotePaths.remove(0);
                    mLocalPaths.remove(0);

                } else {
                    // FAIL
                    return Boolean.FALSE;
                }
            }
            return Boolean.TRUE;
        }

        /**
         * Updates the activity UI after the movement of local files is tried.
         *
         * If the movement was successful for all the files, finishes the activity immediately.
         *
         * In other case, the list of remaining files is still available to retry the movement.
         *
         * @param result 'True' when the movement was successful.
         */
        @Override
        protected void onPostExecute(Boolean result) {
            mAdapter.notifyDataSetChanged();
            mCurrentDialog.dismiss();
            mCurrentDialog = null;
            findViewById(R.id.ok).setEnabled(true);

            if (result) {
                // nothing else to do in this activity
                DisplayUtils.showSnackMessage(findViewById(android.R.id.content), R.string.foreign_files_success);
                finish();
            } else {
                DisplayUtils.showSnackMessage(findViewById(android.R.id.content), R.string.foreign_files_fail);
            }
        }
    }
}
