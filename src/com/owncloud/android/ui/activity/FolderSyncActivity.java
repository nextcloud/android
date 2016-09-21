/**
 *   Nextcloud Android client application
 *
 *   @author Andy Scherzinger
 *   Copyright (C) 2016 Andy Scherzinger
 *   Copyright (C) 2016 Nextcloud
 *
 *   This program is free software; you can redistribute it and/or
 *   modify it under the terms of the GNU AFFERO GENERAL PUBLIC LICENSE
 *   License as published by the Free Software Foundation; either
 *   version 3 of the License, or any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU AFFERO GENERAL PUBLIC LICENSE for more details.
 *
 *   You should have received a copy of the GNU Affero General Public
 *   License along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.owncloud.android.ui.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.MenuItem;
import android.view.View;

import com.owncloud.android.MainApp;
import com.owncloud.android.R;
import com.owncloud.android.datamodel.MediaFolder;
import com.owncloud.android.datamodel.MediaProvider;
import com.owncloud.android.ui.adapter.FolderSyncAdapter;

import java.util.List;
import java.util.TimerTask;
import com.owncloud.android.ui.adapter.FolderSyncAdapter;

/**
 * Activity displaying all auto-synced folders and/or instant upload media folders.
 */
public class FolderSyncActivity extends DrawerActivity {
    private static final String TAG = FolderSyncActivity.class.getSimpleName();
    private RecyclerView mRecyclerView;
    private FolderSyncAdapter mAdapter;

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
        mRecyclerView = (RecyclerView) findViewById(android.R.id.list);

        final int gridWidth = 4;
        mAdapter = new FolderSyncAdapter(this, gridWidth, new FolderSyncAdapter.ClickListener() {
            @Override
            public void onClick(View view, int section, int relative, int absolute) {
                selectItem(FolderSyncActivity.this);
            }
        }, mRecyclerView);

        final GridLayoutManager lm = new GridLayoutManager(this, gridWidth);
        mAdapter.setLayoutManager(lm);
        mRecyclerView.setLayoutManager(lm);
        mRecyclerView.setAdapter(mAdapter);

        load();
    }

    public static void selectItem(final Activity context) {
        // TODO implement selectItem()
        return;
    }

    private void load() {
        if (mAdapter.getItemCount() > 0) return;
        setListShown(false);
        final Handler mHandler = new Handler();
        new Thread(new Runnable() {
            @Override
            public void run() {
                final List<MediaFolder> mediaFolders = MediaProvider.getAllShownImagesPath(FolderSyncActivity.this);

                for (MediaFolder mediaFolder : mediaFolders) {
                    Log.d(TAG, mediaFolder.path);
                }

                mHandler.post(new TimerTask() {
                    @Override
                    public void run() {
                        mAdapter.setMediaFolders(mediaFolders);
                        setListShown(true);
                    }
                });
            }
        }).start();
    }

    void setListShown(boolean shown) {
        if (mRecyclerView != null) {
            mRecyclerView.setVisibility(shown ?
                    View.VISIBLE : View.GONE);

            // TODO show/hide loading visuals
            /**
            mProgress.setVisibility(shown ?
                    View.GONE : View.VISIBLE);
            mEmpty.setVisibility(shown && mAdapter.getItemCount() == 0 ?
                    View.VISIBLE : View.GONE);
             **/
        }
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
