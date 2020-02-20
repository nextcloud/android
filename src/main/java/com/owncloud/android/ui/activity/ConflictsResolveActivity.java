/**
 *  ownCloud Android client application
 *
 *  @author Bartek Przybylski
 *  @author David A. Velasco
 *  Copyright (C) 2012 Bartek Przybylski
 *  Copyright (C) 2016 ownCloud Inc.
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License version 2,
 *  as published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *  <p/>
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.owncloud.android.ui.activity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import com.nextcloud.client.account.User;
import com.nextcloud.java.util.Optional;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.datamodel.UploadsStorageManager;
import com.owncloud.android.db.OCUpload;
import com.owncloud.android.files.services.FileDownloader;
import com.owncloud.android.files.services.FileUploader;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.ui.dialog.ConflictsResolveDialog;
import com.owncloud.android.ui.dialog.ConflictsResolveDialog.Decision;
import com.owncloud.android.ui.dialog.ConflictsResolveDialog.OnConflictDecisionMadeListener;

import javax.inject.Inject;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;


/**
 * Wrapper activity which will be launched if keep-in-sync file will be modified by external
 * application.
 */
public class ConflictsResolveActivity extends FileActivity implements OnConflictDecisionMadeListener {
    /**
     * A nullable upload entry that must be removed when and if the conflict is resolved.
     */
    public static final String EXTRA_CONFLICT_UPLOAD = "CONFLICT_UPLOAD";
    /**
     * Specify the upload local behaviour when there is no CONFLICT_UPLOAD.
     */
    public static final String EXTRA_LOCAL_BEHAVIOUR = "LOCAL_BEHAVIOUR";

    private static final String TAG = ConflictsResolveActivity.class.getSimpleName();

    @Inject UploadsStorageManager uploadsStorageManager;

    private OCUpload conflictUpload;
    private int localBehaviour = FileUploader.LOCAL_BEHAVIOUR_FORGET;


    // TODO rotate when conflict dialog open
    // TODO cancel leads to white activity?!

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null) {
            conflictUpload = savedInstanceState.getParcelable(EXTRA_CONFLICT_UPLOAD);
            localBehaviour = savedInstanceState.getInt(EXTRA_LOCAL_BEHAVIOUR);
        } else {
            conflictUpload = getIntent().getParcelableExtra(EXTRA_CONFLICT_UPLOAD);
            localBehaviour = getIntent().getIntExtra(EXTRA_LOCAL_BEHAVIOUR, localBehaviour);
        }

        if (conflictUpload != null) {
            localBehaviour = conflictUpload.getLocalAction();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putParcelable(EXTRA_CONFLICT_UPLOAD, conflictUpload);
        outState.putInt(EXTRA_LOCAL_BEHAVIOUR, localBehaviour);
    }

    @Override
    public void conflictDecisionMade(Decision decision) {
        OCFile file = getFile();

        switch (decision) {
            case CANCEL:
                // nothing to do
                break;
            case KEEP_LOCAL: // Upload
                FileUploader.uploadUpdateFile(
                    this,
                    getAccount(),
                    file,
                    localBehaviour,
                    FileUploader.NameCollisionPolicy.OVERWRITE
                );

                if (conflictUpload != null) {
                    uploadsStorageManager.removeUpload(conflictUpload);
                }
                break;
            case KEEP_BOTH: // Upload
                FileUploader.uploadUpdateFile(
                    this,
                    getAccount(),
                    file,
                    localBehaviour,
                    FileUploader.NameCollisionPolicy.RENAME
                );

                if (conflictUpload != null) {
                    uploadsStorageManager.removeUpload(conflictUpload);
                }
                break;
            case KEEP_SERVER: // Download
                if (!this.shouldDeleteLocal()) {
                    // Overwrite local file
                    Intent intent = new Intent(this, FileDownloader.class);
                    intent.putExtra(FileDownloader.EXTRA_ACCOUNT, getAccount());
                    intent.putExtra(FileDownloader.EXTRA_FILE, file);
                    if (conflictUpload != null) {
                        intent.putExtra(FileDownloader.EXTRA_CONFLICT_UPLOAD, conflictUpload);
                    }
                    startService(intent);
                }
                break;
        }

        finish();
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (getAccount() == null) {
            finish();
        }

        OCFile file = getFile();
        if (getFile() == null) {
            Log_OC.e(TAG, "No file received");
            finish();
        }

        Optional<User> userOptional = getUser();

        if (!userOptional.isPresent()) {
            Toast.makeText(this, "Error creating conflict dialog!", Toast.LENGTH_LONG).show();
            finish();
        }

        // Check whether the file is contained in the current Account
        Fragment prev = getSupportFragmentManager().findFragmentByTag("conflictDialog");

        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
        if (prev == null) {
            if (getStorageManager().fileExists(file.getRemotePath())) {
                ConflictsResolveDialog dialog = new ConflictsResolveDialog(this,
                                                                           getFile(),
                                                                           conflictUpload,
                                                                           userOptional.get()
                );
                dialog.show(fragmentTransaction, "conflictDialog");
            } else {
                // Account was changed to a different one - just finish
                finish();
            }
        }
    }

    /**
     * @return whether the local version of the files is to be deleted.
     */
    private boolean shouldDeleteLocal() {
        return localBehaviour == FileUploader.LOCAL_BEHAVIOUR_DELETE;
    }
}
