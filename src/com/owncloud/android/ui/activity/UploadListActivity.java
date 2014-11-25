package com.owncloud.android.ui.activity;

import java.io.File;

import android.os.Bundle;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.owncloud.android.R;
import com.owncloud.android.db.UploadDbHandler;
import com.owncloud.android.db.UploadDbObject;
import com.owncloud.android.files.services.FileUploadService;
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
    @Override
    public void onUploadItemClick(UploadDbObject file) {
        // TODO Auto-generated method stub

    }

    @Override
    public File getInitialFilter() {
        // TODO Auto-generated method stub
        return null;
    }
    
    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        boolean retval = true;
        switch (item.getItemId()) {
        case R.id.action_retry_uploads: {
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
