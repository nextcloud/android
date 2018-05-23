/*
 * Nextcloud Android client application
 *
 * @author Tobias Kaminsky
 * Copyright (C) 2018 Tobias Kaminsky
 * Copyright (C) 2018 Nextcloud GmbH.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */
package com.owncloud.android.ui.trashbin;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;

import com.owncloud.android.R;
import com.owncloud.android.lib.resources.files.TrashbinFile;
import com.owncloud.android.ui.EmptyRecyclerView;
import com.owncloud.android.ui.activity.FileActivity;
import com.owncloud.android.ui.activity.FileDisplayActivity;
import com.owncloud.android.ui.adapter.TrashbinListAdapter;
import com.owncloud.android.ui.dialog.SortingOrderDialogFragment;
import com.owncloud.android.ui.interfaces.TrashbinActivityInterface;
import com.owncloud.android.utils.FileSortOrder;
import com.owncloud.android.utils.ThemeUtils;

import java.util.List;

import butterknife.BindString;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;

import static com.owncloud.android.db.PreferenceManager.getSortOrder;

/**
 * Presenting trashbin data, received from presenter
 */
public class TrashbinActivity extends FileActivity implements TrashbinActivityInterface,
        SortingOrderDialogFragment.OnSortingOrderListener, TrashbinContract.View {

    @BindView(R.id.empty_list_view_text)
    public TextView emptyContentMessage;

    @BindView(R.id.empty_list_view_headline)
    public TextView emptyContentHeadline;

    @BindView(R.id.empty_list_icon)
    public ImageView emptyContentIcon;

    @BindView(android.R.id.list)
    public EmptyRecyclerView recyclerView;

    @BindView(R.id.swipe_containing_list)
    public SwipeRefreshLayout swipeListRefreshLayout;

    @BindString(R.string.trashbin_empty_headline)
    public String noResultsHeadline;

    @BindString(R.string.trashbin_empty_message)
    public String noResultsMessage;

    private Unbinder unbinder;
    private TrashbinListAdapter trashbinListAdapter;
    private TrashbinPresenter trashbinPresenter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        trashbinPresenter = new TrashbinPresenter(new RemoteTrashbinRepository(this), this);

        setContentView(R.layout.trashbin_activity);
        unbinder = ButterKnife.bind(this);

        // setup toolbar
        setupToolbar();

        // setup drawer
        setupDrawer(R.id.nav_trashbin);

        ThemeUtils.setColoredTitle(getSupportActionBar(), R.string.trashbin_activity_title, this);
    }

    @Override
    protected void onStart() {
        super.onStart();

        setupContent();
    }

    private void setupContent() {
        recyclerView = findViewById(android.R.id.list);
        recyclerView.setEmptyView(findViewById(R.id.empty_list_view));
        findViewById(R.id.empty_list_progress).setVisibility(View.GONE);
        emptyContentIcon.setImageResource(R.drawable.ic_delete);
        emptyContentIcon.setVisibility(View.VISIBLE);
        emptyContentHeadline.setText(noResultsHeadline);
        emptyContentMessage.setText(noResultsMessage);
        emptyContentMessage.setVisibility(View.VISIBLE);

        trashbinListAdapter = new TrashbinListAdapter(this, getStorageManager(), this);
        recyclerView.setAdapter(trashbinListAdapter);
        recyclerView.setHasFixedSize(true);
        recyclerView.setHasFooter(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        swipeListRefreshLayout.setOnRefreshListener(this::loadFolder);

        loadFolder();
    }

    private void loadFolder() {
        swipeListRefreshLayout.setRefreshing(true);
        trashbinPresenter.loadFolder();
    }

    @Override
    public void showFiles(boolean onDeviceOnly) {
        super.showFiles(onDeviceOnly);
        Intent i = new Intent(getApplicationContext(), FileDisplayActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(i);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        boolean retval = true;
        switch (item.getItemId()) {
            case android.R.id.home:
                if (isDrawerOpen()) {
                    closeDrawer();
                } else if (trashbinPresenter.isRoot()) {
                    onBackPressed();
                } else {
                    openDrawer();
                }
                break;
            case R.id.action_sort: {
                FragmentManager fm = getSupportFragmentManager();
                FragmentTransaction ft = fm.beginTransaction();
                ft.addToBackStack(null);

                SortingOrderDialogFragment mSortingOrderDialogFragment = SortingOrderDialogFragment.newInstance(
                        getSortOrder(this, null));
                mSortingOrderDialogFragment.show(ft, SortingOrderDialogFragment.SORTING_ORDER_FRAGMENT);

                break;
            }
            case R.id.action_empty_trashbin:
                trashbinPresenter.emptyTrashbin();
                break;

            default:
                retval = super.onOptionsItemSelected(item);
                break;
        }

        return retval;
    }

    public void onDestroy() {
        super.onDestroy();
        unbinder.unbind();
    }

    @Override
    public void onOverflowIconClicked(TrashbinFile file, View view) {
        PopupMenu popup = new PopupMenu(this, view);
        popup.inflate(R.menu.trashbin_actions_menu);

        popup.setOnMenuItemClickListener(item -> {
            trashbinPresenter.removeTrashbinFile(file);

            return true;
        });
        popup.show();
    }

    @Override
    public void onItemClicked(TrashbinFile file) {
        if (file.isFolder()) {
            trashbinPresenter.enterFolder(file.getRemotePath());

            mDrawerToggle.setDrawerIndicatorEnabled(false);

            Toolbar toolbar = findViewById(R.id.toolbar);
            if (toolbar != null && toolbar.getNavigationIcon() != null) {
                ThemeUtils.tintDrawable(toolbar.getNavigationIcon(), ThemeUtils.fontColor(this));
            }
        }
    }

    @Override
    public void onRestoreIconClicked(TrashbinFile file, View view) {
        trashbinPresenter.restoreTrashbinFile(file);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.trashbin_options_menu, menu);

        return true;
    }

    @Override
    protected void onPause() {
        super.onPause();
        trashbinListAdapter.cancelAllPendingTasks();
    }

    @Override
    public void onBackPressed() {
        trashbinPresenter.navigateUp();
    }

    public void close() {
        super.onBackPressed();
    }

    public void setDrawerIndicatorEnabled(boolean bool) {
        mDrawerToggle.setDrawerIndicatorEnabled(bool);
    }


    @Override
    public void onSortingOrderChosen(FileSortOrder sortOrder) {
        trashbinListAdapter.setSortOrder(sortOrder);
    }

    @Override
    public void showTrashbinFolder(List<Object> trashbinFiles) {
        trashbinListAdapter.setTrashbinFiles(trashbinFiles, true);
        swipeListRefreshLayout.setRefreshing(false);
    }

    @Override
    public void removeFile(TrashbinFile file) {
        trashbinListAdapter.removeFile(file);
    }

    @Override
    public void removeAllFiles() {
        trashbinListAdapter.removeAllFiles();
    }

    @Override
    public void showSnackbarError(int message, TrashbinFile file) {
        swipeListRefreshLayout.setRefreshing(false);
        Snackbar.make(recyclerView, String.format(getString(message), file.getFileName()), Snackbar.LENGTH_LONG).show();
    }

    @Override
    public void showError(int message) {
        swipeListRefreshLayout.setRefreshing(false);
        
        if (emptyContentMessage != null) {
            emptyContentHeadline.setText(R.string.common_error);
            emptyContentIcon.setImageDrawable(getResources().getDrawable(R.drawable.ic_list_empty_error));
            emptyContentMessage.setText(message);

            emptyContentMessage.setVisibility(View.VISIBLE);
            emptyContentIcon.setVisibility(View.VISIBLE);
        }
    }
}