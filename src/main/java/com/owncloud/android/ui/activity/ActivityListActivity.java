package com.owncloud.android.ui.activity;

import android.os.Bundle;
import android.view.MenuItem;

import com.owncloud.android.R;
import com.owncloud.android.lib.common.utils.Log_OC;

/**
 * Activity displaying all server side stored activity items.
 */
public class ActivityListActivity extends FileActivity {

    private static final String TAG = ActivityListActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log_OC.v(TAG, "onCreate() start");
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activitiy_list_layout);

        // setup toolbar
        setupToolbar();

        // setup drawer
        setupDrawer(R.id.nav_activity);
        getSupportActionBar().setTitle(getString(R.string.drawer_item_activity));

        setupContent();
    }

    /**
     * sets up the UI elements and loads all activity items.
     */
    private void setupContent() {
        // TODO add all (recycler) view relevant code + data loading + adapter etc.
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        boolean retval;

        switch (item.getItemId()) {
            case android.R.id.home:
                if (isDrawerOpen()) {
                    closeDrawer();
                } else {
                    openDrawer();
                }

            default:
                retval = super.onOptionsItemSelected(item);
        }

        return retval;
    }
}
