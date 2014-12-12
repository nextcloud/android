package com.owncloud.android.ui.activity;

import java.io.File;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Parcelable;
import android.widget.Toast;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.owncloud.android.R;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.db.UploadDbHandler;
import com.owncloud.android.db.UploadDbObject;
import com.owncloud.android.files.services.FileUploadService;
import com.owncloud.android.files.services.FileUploadService.FileUploaderBinder;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.ui.errorhandling.ExceptionHandler;
import com.owncloud.android.ui.fragment.UploadListFragment;
import com.owncloud.android.ui.preview.PreviewImageActivity;
import com.owncloud.android.ui.preview.PreviewImageFragment;

/**
 * Activity listing pending, active, and completed uploads. User can delete
 * completed uploads from view. Content of this list of coming from
 * {@link UploadDbHandler}.
 * 
 * @author LukeOwncloud
 *
 */
public class UploadListActivity extends FileActivity implements UploadListFragment.ContainerActivity {

    private static final String TAG = "UploadListActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Thread.setDefaultUncaughtExceptionHandler(new ExceptionHandler(this));
        setContentView(R.layout.upload_list_layout);
    }

    // ////////////////////////////////////////
    // UploadListFragment.ContainerActivity
    // ////////////////////////////////////////
    @Override
    public boolean onUploadItemClick(UploadDbObject file) {
        File f = new File(file.getLocalPath());
        if(!f.exists()) {
            Toast.makeText(this, "Cannot open. Local file does not exist.", Toast.LENGTH_SHORT).show();
            return true;
        }
        
        if (PreviewImageFragment.canBePreviewed(file.getOCFile())) {
            // preview image
            Intent showDetailsIntent = new Intent(this, PreviewImageActivity.class);
            showDetailsIntent.putExtra(EXTRA_FILE, (Parcelable)file.getOCFile());
            showDetailsIntent.putExtra(EXTRA_ACCOUNT, getAccount());
            startActivity(showDetailsIntent);            
        } else {
            //open file
            getFileOperationsHelper().openFile(file.getOCFile());
        }
        return true;
    }

    @SuppressWarnings("unused")
    private void openDetails(UploadDbObject file) {
        OCFile ocFile = file.getOCFile();
        Intent showDetailsIntent = new Intent(this, FileDisplayActivity.class);
        showDetailsIntent.putExtra(FileActivity.EXTRA_FILE, (Parcelable) ocFile);
        showDetailsIntent.putExtra(FileActivity.EXTRA_ACCOUNT, file.getAccount(this));
        startActivity(showDetailsIntent);
    }
    
    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        boolean retval = true;
        switch (item.getItemId()) {
        case R.id.action_retry_uploads: {
            Log_OC.d(TAG, "FileUploadService.retry() called by onMenuItemSelected()");
            FileUploadService.retry(this);
            break;
        }
        case R.id.action_clear_failed_uploads: {
            UploadDbHandler db = UploadDbHandler.getInstance(this);
            db.clearFailedUploads();
            break;
        }
        case R.id.action_clear_finished_uploads: {
            UploadDbHandler db = UploadDbHandler.getInstance(this);
            db.clearFinishedUploads();
            break;
        }
        default:
            retval = super.onOptionsItemSelected(item);
        }
        return retval;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getSherlock().getMenuInflater();
        inflater.inflate(R.menu.upload_list_menu, menu);
        return true;
    }
    
    @Override
    protected ServiceConnection newTransferenceServiceConnection() {
        return new UploadListServiceConnection();
    }

    /** Defines callbacks for service binding, passed to bindService() */
    private class UploadListServiceConnection implements ServiceConnection {

        @Override
        public void onServiceConnected(ComponentName component, IBinder service) {
                
            if (component.equals(new ComponentName(UploadListActivity.this, FileUploadService.class))) {
                Log_OC.d(TAG, "UploadListActivty connected to Upload service");
                mUploaderBinder = (FileUploaderBinder) service;
            } else {
                return;
            }
            
        }

        @Override
        public void onServiceDisconnected(ComponentName component) {
            if (component.equals(new ComponentName(UploadListActivity.this, FileUploadService.class))) {
                Log_OC.d(TAG, "UploadListActivty suddenly disconnected from Upload service");
                mUploaderBinder = null;
            }
        }
    };  

}
