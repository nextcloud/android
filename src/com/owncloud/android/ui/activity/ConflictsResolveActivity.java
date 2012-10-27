package com.owncloud.android.ui.activity;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.owncloud.android.files.services.FileUploader;
import com.owncloud.android.ui.dialog.ConflictsResolveDialog;
import com.owncloud.android.ui.dialog.ConflictsResolveDialog.Decision;
import com.owncloud.android.ui.dialog.ConflictsResolveDialog.OnConflictDecisionMadeListener;

import android.accounts.Account;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

public class ConflictsResolveActivity extends SherlockFragmentActivity implements OnConflictDecisionMadeListener {

    private String TAG = ConflictsResolveActivity.class.getSimpleName();
    
    private String mRemotePath;
    
    private String mLocalPath;
    
    private Account mOCAccount;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mRemotePath = getIntent().getStringExtra("remotepath");
        mLocalPath = getIntent().getStringExtra("localpath");
        mOCAccount = getIntent().getParcelableExtra("account");
        ConflictsResolveDialog d = ConflictsResolveDialog.newInstance(mRemotePath, this);
        d.showDialog(this);
    }

    @Override
    public void ConflictDecisionMade(Decision decision) {
        Intent i = new Intent(getApplicationContext(), FileUploader.class);
        
        switch (decision) {
            case CANCEL:
                return;
            case OVERWRITE:
                i.putExtra(FileUploader.KEY_FORCE_OVERWRITE, true);
            case KEEP_BOTH: // fallthrough
                break;
            default:
                Log.wtf(TAG, "Unhandled conflict decision " + decision);
                return;
        }
        i.putExtra(FileUploader.KEY_ACCOUNT, mOCAccount);
        i.putExtra(FileUploader.KEY_REMOTE_FILE, mRemotePath);
        i.putExtra(FileUploader.KEY_LOCAL_FILE, mLocalPath);
        i.putExtra(FileUploader.KEY_UPLOAD_TYPE, FileUploader.UPLOAD_SINGLE_FILE);
        
        startService(i);
        finish();
    }
}
