package com.owncloud.android.ui.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;

import com.owncloud.android.MainApp;
import com.owncloud.android.R;

/**
 * Activity displaying all auto-synced folders and/or instant upload media folders.
 */
public class FolderSyncActivity extends DrawerActivity {
    private static final String TAG = FolderSyncActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.folder_sync_layout);

        // setup toolbar
        setupToolbar();

        // setup drawer
        setupDrawer(R.id.nav_folder_sync);
        getSupportActionBar().setTitle(getString(R.string.drawer_folder_sync));

        setupContent();
    }

    private void setupContent() {
        // TODO setup/initialize UI
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        boolean retval;
        switch (item.getItemId()) {
            case android.R.id.home: {
                if (isDrawerOpen()) {
                    closeDrawer();
                } else {
                    openDrawer();
                }
            }

            default:
                retval = super.onOptionsItemSelected(item);
        }
        return retval;
    }

    @Override
    public void restart() {
        Intent i = new Intent(this, FileDisplayActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(i);
    }

    @Override
    public void showFiles(boolean onDeviceOnly) {
        MainApp.showOnlyFilesOnDevice(onDeviceOnly);
        Intent fileDisplayActivity = new Intent(getApplicationContext(), FileDisplayActivity.class);
        fileDisplayActivity.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(fileDisplayActivity);
    }
}
