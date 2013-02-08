/* ownCloud Android client application
 *   Copyright (C) 2012 Bartek Przybylski
 *   Copyright (C) 2012-2013 ownCloud Inc.
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 2 of the License, or
 *   (at your option) any later version.
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

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.files.services.FileUploader;
import com.owncloud.android.ui.dialog.ConflictsResolveDialog;
import com.owncloud.android.ui.dialog.ConflictsResolveDialog.Decision;
import com.owncloud.android.ui.dialog.ConflictsResolveDialog.OnConflictDecisionMadeListener;

import android.accounts.Account;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

/**
 * Wrapper activity which will be launched if keep-in-sync file will be modified by external
 * application. 
 * 
 * @author Bartek Przybylski
 *
 */
public class ConflictsResolveActivity extends SherlockFragmentActivity implements OnConflictDecisionMadeListener {

    public static final String EXTRA_FILE = "FILE";
    public static final String EXTRA_ACCOUNT = "ACCOUNT";

    private String TAG = ConflictsResolveActivity.class.getSimpleName();
    
    //private String mRemotePath;
    
    //private String mLocalPath;
    
    private OCFile mFile;
    private Account mOCAccount;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        //mRemotePath = getIntent().getStringExtra("remotepath");
        //mLocalPath = getIntent().getStringExtra("localpath");
        mFile = getIntent().getParcelableExtra(EXTRA_FILE);
        mOCAccount = getIntent().getParcelableExtra(EXTRA_ACCOUNT);
        ConflictsResolveDialog d = ConflictsResolveDialog.newInstance(mFile.getRemotePath(), this);
        d.showDialog(this);
    }

    @Override
    public void ConflictDecisionMade(Decision decision) {
        Intent i = new Intent(getApplicationContext(), FileUploader.class);
        
        switch (decision) {
            case CANCEL:
                finish();
                return;
            case OVERWRITE:
                i.putExtra(FileUploader.KEY_FORCE_OVERWRITE, true);
                break;
            case KEEP_BOTH:
                i.putExtra(FileUploader.KEY_LOCAL_BEHAVIOUR, FileUploader.LOCAL_BEHAVIOUR_MOVE);
                break;
            default:
                Log.wtf(TAG, "Unhandled conflict decision " + decision);
                return;
        }
        i.putExtra(FileUploader.KEY_ACCOUNT, mOCAccount);
        i.putExtra(FileUploader.KEY_FILE, mFile);
        i.putExtra(FileUploader.KEY_UPLOAD_TYPE, FileUploader.UPLOAD_SINGLE_FILE);
        
        startService(i);
        finish();
    }
}
