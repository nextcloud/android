package com.owncloud.android.ui.activity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.owncloud.android.R;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.db.UploadDbHandler;
import com.owncloud.android.db.UploadDbHandler.UploadStatus;
import com.owncloud.android.db.UploadDbObject;
import com.owncloud.android.files.services.FileUploadService;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.ui.errorhandling.ExceptionHandler;
import com.owncloud.android.ui.fragment.UploadListFragment;

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
    /**
     * TODO Without a menu this is a little un-intuitive.
     */
    @Override
    public boolean onUploadItemClick(UploadDbObject file) {
        OCFile ocFile = file.getOCFile();
        switch (file.getUploadStatus()) {
        case UPLOAD_IN_PROGRESS:
            if (ocFile != null) {
                getFileOperationsHelper().cancelTransference(ocFile);
            } else {
                Log_OC.e(TAG, "Could not get OCFile for " + file.getRemotePath() + ". Cannot cancel.");
            }
            break;
        case UPLOAD_SUCCEEDED:
            Intent showDetailsIntent = new Intent(this, FileDisplayActivity.class);
            showDetailsIntent.putExtra(FileActivity.EXTRA_FILE, (Parcelable)ocFile);
            showDetailsIntent.putExtra(FileActivity.EXTRA_ACCOUNT, file.getAccount(this));
            startActivity(showDetailsIntent);
            break;
        case UPLOAD_CANCELLED:
        case UPLOAD_PAUSED:
            UploadDbHandler db = UploadDbHandler.getInstance(this.getBaseContext());
            file.setUploadStatus(UploadStatus.UPLOAD_LATER);
            db.updateUpload(file);
            // no break; to start upload immediately.
        case UPLOAD_LATER:
        case UPLOAD_FAILED_RETRY:
            Log_OC.d(TAG, "FileUploadService.retry() called by onUploadItemClick()");
            FileUploadService.retry(this);
            break;
        default:
            break;
        }
        return true;
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
        case R.id.action_clear_upload_list: {
            UploadDbHandler db = UploadDbHandler.getInstance(this);
            db.cleanDoneUploads();
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


}
