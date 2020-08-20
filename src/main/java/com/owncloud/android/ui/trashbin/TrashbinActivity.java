/*
 * Nextcloud Android client application
 *
 * @author Tobias Kaminsky
 * @author Chris Narkiewicz
 *
 * Copyright (C) 2018 Tobias Kaminsky
 * Copyright (C) 2018 Nextcloud GmbH.
 * Copyright (C) 2019 Chris Narkiewicz <hello@ezaquarii.com>
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

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.PopupMenu;
import android.widget.TextView;

import com.google.android.material.snackbar.Snackbar;
import com.nextcloud.client.account.CurrentAccountProvider;
import com.nextcloud.client.account.User;
import com.nextcloud.client.di.Injectable;
import com.nextcloud.client.network.ClientFactory;
import com.nextcloud.client.preferences.AppPreferences;
import com.owncloud.android.R;
import com.owncloud.android.databinding.TrashbinActivityBinding;
import com.owncloud.android.lib.resources.trashbin.model.TrashbinFile;
import com.owncloud.android.ui.EmptyRecyclerView;
import com.owncloud.android.ui.activity.DrawerActivity;
import com.owncloud.android.ui.adapter.TrashbinListAdapter;
import com.owncloud.android.ui.dialog.SortingOrderDialogFragment;
import com.owncloud.android.ui.interfaces.TrashbinActivityInterface;
import com.owncloud.android.utils.DisplayUtils;
import com.owncloud.android.utils.FileSortOrder;
import com.owncloud.android.utils.ThemeUtils;

import java.util.List;

import javax.inject.Inject;

import androidx.annotation.VisibleForTesting;
import androidx.core.content.res.ResourcesCompat;
import androidx.recyclerview.widget.LinearLayoutManager;

import static com.owncloud.android.utils.DisplayUtils.openSortingOrderDialogFragment;

/**
 * Presenting trashbin data, received from presenter
 */
public class TrashbinActivity extends DrawerActivity implements
    TrashbinActivityInterface,
    SortingOrderDialogFragment.OnSortingOrderListener,
    TrashbinContract.View,
    Injectable {

    @Inject AppPreferences preferences;
    @Inject CurrentAccountProvider accountProvider;
    @Inject ClientFactory clientFactory;
    private TrashbinListAdapter trashbinListAdapter;

    @VisibleForTesting
    TrashbinPresenter trashbinPresenter;

    private boolean active;
    private TrashbinActivityBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final User user = accountProvider.getUser();
        final RemoteTrashbinRepository trashRepository = new RemoteTrashbinRepository(user, clientFactory);
        trashbinPresenter = new TrashbinPresenter(trashRepository, this);

        binding = TrashbinActivityBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setupToolbar();
        findViewById(R.id.sort_list_button_group).setVisibility(View.VISIBLE);
        findViewById(R.id.switch_grid_view_button).setVisibility(View.GONE);
        updateActionBarTitleAndHomeButtonByString(getString(R.string.trashbin_activity_title));
        setupDrawer(R.id.nav_trashbin);
    }

    @Override
    protected void onStart() {
        super.onStart();
        active = true;
        setupContent();
    }

    private void setupContent() {
        EmptyRecyclerView recyclerView = binding.list;
        recyclerView.setEmptyView(binding.emptyList.emptyListView);
        binding.emptyList.emptyListView.setVisibility(View.GONE);
        binding.emptyList.emptyListIcon.setImageResource(R.drawable.ic_delete);
        binding.emptyList.emptyListIcon.setVisibility(View.VISIBLE);
        binding.emptyList.emptyListViewHeadline.setText(getString(R.string.trashbin_empty_headline));
        binding.emptyList.emptyListViewText.setText(getString(R.string.trashbin_empty_message));
        binding.emptyList.emptyListViewText.setVisibility(View.VISIBLE);

        trashbinListAdapter = new TrashbinListAdapter(
            this,
            getStorageManager(),
            preferences,
            this,
            getUserAccountManager().getUser()
        );
        recyclerView.setAdapter(trashbinListAdapter);
        recyclerView.setHasFixedSize(true);
        recyclerView.setHasFooter(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        ThemeUtils.colorSwipeRefreshLayout(this, binding.swipeContainingList);
        binding.swipeContainingList.setOnRefreshListener(this::loadFolder);

        findViewById(R.id.sort_button).setOnClickListener(l ->
                                                              openSortingOrderDialogFragment(getSupportFragmentManager(),
                                                                                             preferences.getSortOrderByType(
                                                                                                 FileSortOrder.Type.trashBinView,
                                                                                                 FileSortOrder.sort_new_to_old))
                                                         );

        loadFolder();
    }

    protected void loadFolder() {
        binding.swipeContainingList.setRefreshing(true);
        trashbinPresenter.loadFolder();
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

            case R.id.action_empty_trashbin:
                trashbinPresenter.emptyTrashbin();
                break;

            default:
                retval = super.onOptionsItemSelected(item);
                break;
        }

        return retval;
    }

    @Override
    public void onOverflowIconClicked(TrashbinFile file, View view) {
        PopupMenu popup = new PopupMenu(this, view);
        popup.inflate(R.menu.item_trashbin);

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
        }
    }

    @Override
    public void onRestoreIconClicked(TrashbinFile file, View view) {
        trashbinPresenter.restoreTrashbinFile(file);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_trashbin, menu);

        return true;
    }

    @Override
    protected void onPause() {
        super.onPause();
        active = false;

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
        TextView sortButton = findViewById(R.id.sort_button);
        sortButton.setText(DisplayUtils.getSortOrderStringId(sortOrder));
        trashbinListAdapter.setSortOrder(sortOrder);
    }

    @Override
    public void showTrashbinFolder(List<Object> trashbinFiles) {
        if (active) {
            trashbinListAdapter.setTrashbinFiles(trashbinFiles, true);
            binding.swipeContainingList.setRefreshing(false);
        }
    }

    @Override
    public void removeFile(TrashbinFile file) {
        if (active) {
            trashbinListAdapter.removeFile(file);
        }
    }

    @Override
    public void removeAllFiles() {
        trashbinListAdapter.removeAllFiles();
    }

    @Override
    public void showSnackbarError(int message, TrashbinFile file) {
        if (active) {
            binding.swipeContainingList.setRefreshing(false);
            Snackbar.make(binding.list,
                          String.format(getString(message), file.getFileName()), Snackbar.LENGTH_LONG)
                .show();
        }
    }

    @Override
    public void showError(int message) {
        if (active) {
            binding.swipeContainingList.setRefreshing(false);

            binding.emptyList.emptyListViewHeadline.setText(R.string.common_error);
            binding.emptyList.emptyListIcon.setImageDrawable(ResourcesCompat.getDrawable(getResources(),
                                                                                         R.drawable.ic_list_empty_error,
                                                                                         null));
            binding.emptyList.emptyListViewText.setText(message);
            binding.emptyList.emptyListViewText.setVisibility(View.VISIBLE);
            binding.emptyList.emptyListIcon.setVisibility(View.VISIBLE);
            binding.emptyList.emptyListView.setVisibility(View.VISIBLE);
        }
    }
}
