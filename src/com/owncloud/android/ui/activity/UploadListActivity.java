package com.owncloud.android.ui.activity;

import java.io.File;

import android.os.Bundle;

import com.owncloud.android.R;
import com.owncloud.android.db.UploadDbHandler;
import com.owncloud.android.ui.errorhandling.ExceptionHandler;
import com.owncloud.android.ui.fragment.UploadListFragment;

/**
 * Activity listing pending, active, and completed uploads. User can delete
 * completed uploads from view. Content of this list of coming from
 * {@link UploadDbHandler}.
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
    public void onUploadItemClick(File file) {
        // TODO Auto-generated method stub

    }

    @Override
    public File getInitialFilter() {
        // TODO Auto-generated method stub
        return null;
    }

}
