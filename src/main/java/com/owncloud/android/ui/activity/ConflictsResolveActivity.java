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
import com.owncloud.android.files.services.FileDownloader;
import com.owncloud.android.files.services.FileUploader;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.ui.dialog.ConflictsResolveDialog;
import com.owncloud.android.ui.dialog.ConflictsResolveDialog.Decision;
import com.owncloud.android.ui.dialog.ConflictsResolveDialog.OnConflictDecisionMadeListener;


/**
 * Wrapper activity which will be launched if keep-in-sync file will be modified by external
 * application.
 */
public class ConflictsResolveActivity extends FileActivity implements OnConflictDecisionMadeListener {

    private static final String TAG = ConflictsResolveActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void conflictDecisionMade(Decision decision) {

        Integer behaviour = null;
        Boolean forceOverwrite = null;

        switch (decision) {
            case CANCEL:
                finish();
                return;
            case OVERWRITE:
                // use local version -> overwrite on server
                forceOverwrite = true;
                break;
            case KEEP_BOTH:
                behaviour = FileUploader.LOCAL_BEHAVIOUR_MOVE;
                break;
            case SERVER:
                // use server version -> delete local, request download
                Intent intent = new Intent(this, FileDownloader.class);
                intent.putExtra(FileDownloader.EXTRA_ACCOUNT, getAccount());
                intent.putExtra(FileDownloader.EXTRA_FILE, getFile());
                startService(intent);
                finish();
                return;
            default:
                Log_OC.e(TAG, "Unhandled conflict decision " + decision);
                return;
        }

        FileUploader.UploadRequester requester = new FileUploader.UploadRequester();
        requester.uploadUpdate(this, getAccount(), getFile(), behaviour, forceOverwrite);
        finish();
    }

    @Override
    protected void onAccountSet(boolean stateWasRecovered) {
        super.onAccountSet(stateWasRecovered);
        if (getAccount() != null) {
            OCFile file = getFile();
            if (getFile() == null) {
                Log_OC.e(TAG, "No conflictive file received");
                finish();
            } else {
                /// Check whether the 'main' OCFile handled by the Activity is contained in the current Account
                file = getStorageManager().getFileByPath(file.getRemotePath());   // file = null if not in the
                // current Account
                if (file != null) {
                    setFile(file);
                    ConflictsResolveDialog d = ConflictsResolveDialog.newInstance(this);
                    d.showDialog(this);

                } else {
                    // account was changed to a different one - just finish
                    finish();
                }
            }

        } else {
            finish();
        }

    }
}
