/*
 * ownCloud Android client application
 *
 * @author Bartek Przybylski
 * @author David A. Velasco Copyright (C) 2012 Bartek Przybylski Copyright (C) 2016 ownCloud Inc.
 * <p>
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public
 * License version 2, as published by the Free Software Foundation.
 * <p>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more
 * details.
 * <p/>
 * You should have received a copy of the GNU General Public License along with this program.  If not, see
 * <http://www.gnu.org/licenses/>.
 */

package com.owncloud.android.ui.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import com.nextcloud.client.account.User;
import com.nextcloud.java.util.Optional;
import com.owncloud.android.R;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.datamodel.UploadsStorageManager;
import com.owncloud.android.db.OCUpload;
import com.owncloud.android.files.services.FileDownloader;
import com.owncloud.android.files.services.FileUploader;
import com.owncloud.android.files.services.NameCollisionPolicy;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.lib.resources.files.ReadFileRemoteOperation;
import com.owncloud.android.lib.resources.files.model.RemoteFile;
import com.owncloud.android.ui.dialog.ConflictsResolveDialog;
import com.owncloud.android.ui.dialog.ConflictsResolveDialog.Decision;
import com.owncloud.android.ui.dialog.ConflictsResolveDialog.OnConflictDecisionMadeListener;
import com.owncloud.android.utils.FileStorageUtils;

import javax.inject.Inject;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;


/**
 * Wrapper activity which will be launched if keep-in-sync file will be modified by external application.
 */
public class ConflictsResolveActivity extends FileActivity implements OnConflictDecisionMadeListener {
    /**
     * A nullable upload entry that must be removed when and if the conflict is resolved.
     */
    public static final String EXTRA_CONFLICT_UPLOAD_ID = "CONFLICT_UPLOAD_ID";
    /**
     * Specify the upload local behaviour when there is no CONFLICT_UPLOAD.
     */
    public static final String EXTRA_LOCAL_BEHAVIOUR = "LOCAL_BEHAVIOUR";
    public static final String EXTRA_EXISTING_FILE = "EXISTING_FILE";

    private static final String TAG = ConflictsResolveActivity.class.getSimpleName();

    @Inject UploadsStorageManager uploadsStorageManager;

    private long conflictUploadId;
    private OCFile existingFile;
    private OCFile newFile;
    private int localBehaviour = FileUploader.LOCAL_BEHAVIOUR_FORGET;
    protected OnConflictDecisionMadeListener listener;

    public static Intent createIntent(OCFile file,
                                      User user,
                                      long conflictUploadId,
                                      Integer flag,
                                      Context context) {
        Intent intent = new Intent(context, ConflictsResolveActivity.class);
        if (flag != null) {
            intent.setFlags(intent.getFlags() | flag);
        }
        intent.putExtra(EXTRA_FILE, file);
        intent.putExtra(EXTRA_USER, user);
        intent.putExtra(EXTRA_CONFLICT_UPLOAD_ID, conflictUploadId);

        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null) {
            conflictUploadId = savedInstanceState.getLong(EXTRA_CONFLICT_UPLOAD_ID);
            existingFile = savedInstanceState.getParcelable(EXTRA_EXISTING_FILE);
            localBehaviour = savedInstanceState.getInt(EXTRA_LOCAL_BEHAVIOUR);
        } else {
            conflictUploadId = getIntent().getLongExtra(EXTRA_CONFLICT_UPLOAD_ID, -1);
            existingFile = getIntent().getParcelableExtra(EXTRA_EXISTING_FILE);
            localBehaviour = getIntent().getIntExtra(EXTRA_LOCAL_BEHAVIOUR, localBehaviour);
        }

        OCUpload upload = uploadsStorageManager.getUploadById(conflictUploadId);

        if (upload != null) {
            localBehaviour = upload.getLocalAction();
        }

        // new file was modified locally in file system
        newFile = getFile();

        listener = decision -> {
            OCFile file = newFile; // local file got changed, so either upload it or replace it again by server
            // version
            User user = getUser().orElseThrow(RuntimeException::new);
            switch (decision) {
                case CANCEL:
                    // nothing to do
                    break;
                case KEEP_LOCAL: // Upload
                    FileUploader.uploadUpdateFile(
                        getBaseContext(),
                        user,
                        file,
                        localBehaviour,
                        NameCollisionPolicy.OVERWRITE
                                                 );

                    uploadsStorageManager.removeUpload(upload);
                    break;
                case KEEP_BOTH: // Upload
                    FileUploader.uploadUpdateFile(
                        getBaseContext(),
                        user,
                        file,
                        localBehaviour,
                        NameCollisionPolicy.RENAME
                                                 );

                    uploadsStorageManager.removeUpload(upload);
                    break;
                case KEEP_SERVER: // Download
                    if (!shouldDeleteLocal()) {
                        // Overwrite local file
                        Intent intent = new Intent(getBaseContext(), FileDownloader.class);
                        intent.putExtra(FileDownloader.EXTRA_USER, getUser().orElseThrow(RuntimeException::new));
                        intent.putExtra(FileDownloader.EXTRA_FILE, file);
                        intent.putExtra(EXTRA_CONFLICT_UPLOAD_ID, conflictUploadId);
                        startService(intent);
                    } else {
                        uploadsStorageManager.removeUpload(upload);
                    }
                    break;
            }

            finish();
        };
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putLong(EXTRA_CONFLICT_UPLOAD_ID, conflictUploadId);
        outState.putParcelable(EXTRA_EXISTING_FILE, existingFile);
        outState.putInt(EXTRA_LOCAL_BEHAVIOUR, localBehaviour);
    }

    @Override
    public void conflictDecisionMade(Decision decision) {
        listener.conflictDecisionMade(decision);
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (getAccount() == null) {
            finish();
            return;
        }

        if (newFile == null) {
            Log_OC.e(TAG, "No file received");
            finish();
            return;
        }

        if (existingFile == null) {
            // fetch info of existing file from server
            ReadFileRemoteOperation operation = new ReadFileRemoteOperation(newFile.getRemotePath());

            new Thread(() -> {
                try {
                    RemoteOperationResult result = operation.execute(getAccount(), this);

                    if (result.isSuccess()) {
                        existingFile = FileStorageUtils.fillOCFile((RemoteFile) result.getData().get(0));
                        existingFile.setLastSyncDateForProperties(System.currentTimeMillis());

                        startDialog();
                    } else {
                        Log_OC.e(TAG, "ReadFileRemoteOp returned failure with code: " + result.getHttpCode());
                        showErrorAndFinish();
                    }
                } catch (Exception e) {
                    Log_OC.e(TAG, "Error when trying to fetch remote file", e);
                    showErrorAndFinish();
                }


            }).start();
        } else {
            startDialog();
        }
    }

    private void startDialog() {
        Optional<User> userOptional = getUser();

        if (!userOptional.isPresent()) {
            Log_OC.e(TAG, "User not present");
            showErrorAndFinish();
        }

        // Check whether the file is contained in the current Account
        Fragment prev = getSupportFragmentManager().findFragmentByTag("conflictDialog");

        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
        if (prev != null) {
            fragmentTransaction.remove(prev);
        }

        if (existingFile != null && getStorageManager().fileExists(newFile.getRemotePath())) {
            ConflictsResolveDialog dialog = ConflictsResolveDialog.newInstance(existingFile,
                                                                               newFile,
                                                                               userOptional.get());
            dialog.show(fragmentTransaction, "conflictDialog");
        } else {
            // Account was changed to a different one - just finish
            Log_OC.e(TAG, "Account was changed, finishing");
            showErrorAndFinish();
        }
    }

    private void showErrorAndFinish() {
        runOnUiThread(() -> Toast.makeText(this, R.string.conflict_dialog_error, Toast.LENGTH_LONG).show());
        finish();
    }

    /**
     * @return whether the local version of the files is to be deleted.
     */
    private boolean shouldDeleteLocal() {
        return localBehaviour == FileUploader.LOCAL_BEHAVIOUR_DELETE;
    }
}
