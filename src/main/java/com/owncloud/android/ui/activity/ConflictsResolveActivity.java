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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null) {
            this.conflictUpload = savedInstanceState.getParcelable(EXTRA_CONFLICT_UPLOAD);
            this.localBehaviour = savedInstanceState.getInt(EXTRA_LOCAL_BEHAVIOUR);
        } else {
            this.conflictUpload = getIntent().getParcelableExtra(EXTRA_CONFLICT_UPLOAD);
            this.localBehaviour = getIntent().getIntExtra(EXTRA_LOCAL_BEHAVIOUR, this.localBehaviour);
        }

        if (this.conflictUpload != null) {
            this.localBehaviour = this.conflictUpload.getLocalAction();
        }
    }

    @Override
    public void conflictDecisionMade(Decision decision) {
        if (decision == Decision.CANCEL) {
            return;
        }

        OCFile file = getFile();
        FileUploader.UploadRequester uploadRequester = new FileUploader.UploadRequester();

        // Upload
        if (decision == Decision.KEEP_LOCAL || decision == Decision.KEEP_BOTH) {
            FileUploader.NameCollisionPolicy collisionPolicy = FileUploader.NameCollisionPolicy.OVERWRITE;
            if (decision == Decision.KEEP_BOTH) {
                collisionPolicy = FileUploader.NameCollisionPolicy.RENAME;
            }

            uploadRequester.uploadUpdate(this, getAccount(), file, localBehaviour, collisionPolicy);

            if (this.conflictUpload != null) {
                uploadsStorageManager.removeUpload(this.conflictUpload);
            }
        }

        // Download
        if (decision == Decision.KEEP_SERVER && !this.shouldDeleteLocal()) {
            // Overwrite local file
            Intent intent = new Intent(this, FileDownloader.class);
            intent.putExtra(FileDownloader.EXTRA_ACCOUNT, getAccount());
            intent.putExtra(FileDownloader.EXTRA_FILE, file);
            if (this.conflictUpload != null) {
                intent.putExtra(FileDownloader.EXTRA_CONFLICT_UPLOAD, this.conflictUpload);
            }
            startService(intent);
        }

        finish();
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (getAccount() != null) {
            OCFile file = getFile();
            if (getFile() == null) {
                Log_OC.e(TAG, "No file received");
                finish();
            } else {
                // Check whether the file is contained in the current Account
                if (getStorageManager().fileExists(file.getRemotePath())) {
                    ConflictsResolveDialog dialog = new ConflictsResolveDialog(this, !this.shouldDeleteLocal());
                    dialog.showDialog(this);
                } else {
                    // Account was changed to a different one - just finish
                    finish();
                }
            }
        } else {
            finish();
        }
    }

    /**
     * @return whether the local version of the files is to be deleted.
     */
    private boolean shouldDeleteLocal() {
        return localBehaviour == FileUploader.LOCAL_BEHAVIOUR_DELETE;
    }
}
