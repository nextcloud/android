/*
 *
 * Nextcloud Android client application
 *
 * @author Tobias Kaminsky
 * Copyright (C) 2019 Tobias Kaminsky
 * Copyright (C) 2019 Nextcloud GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package com.owncloud.android.ui.preview;

import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.nextcloud.android.lib.richWorkspace.RichWorkspaceDirectEditingRemoteOperation;
import com.nextcloud.client.account.UserAccountManager;
import com.owncloud.android.R;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.ui.activity.FileDisplayActivity;
import com.owncloud.android.utils.DisplayUtils;
import com.owncloud.android.utils.theme.ThemeFabUtils;

import javax.inject.Inject;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.SearchView;
import androidx.core.view.MenuItemCompat;

public class PreviewTextStringFragment extends PreviewTextFragment {
    private static final String EXTRA_FILE = "FILE";

    @Inject UserAccountManager accountManager;

    /**
     * Creates an empty fragment for previews.
     */
    public PreviewTextStringFragment() {
        super();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setHasOptionsMenu(true);

        Bundle args = getArguments();

        if (args.containsKey(FileDisplayActivity.EXTRA_SEARCH_QUERY)) {
            searchQuery = args.getString(FileDisplayActivity.EXTRA_SEARCH_QUERY);
        }
        searchOpen = args.getBoolean(FileDisplayActivity.EXTRA_SEARCH, false);

        handler = new Handler();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        outState.putParcelable(PreviewTextStringFragment.EXTRA_FILE, getFile());

        super.onSaveInstanceState(outState);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);

        if (view == null) {
            throw new RuntimeException("View may not be null");
        }

        FloatingActionButton fabMain = requireActivity().findViewById(R.id.fab_main);
        fabMain.setVisibility(View.VISIBLE);
        fabMain.setEnabled(true);
        fabMain.setOnClickListener(v -> edit());
        ThemeFabUtils.colorFloatingActionButton(fabMain, R.drawable.ic_edit, requireContext());

        return view;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);

        MenuItem menuItem = menu.findItem(R.id.action_search);
        menuItem.setVisible(true);
        searchView = (SearchView) MenuItemCompat.getActionView(menuItem);
        searchView.setOnQueryTextListener(this);
        searchView.setMaxWidth(Integer.MAX_VALUE);

        if (searchOpen) {
            searchView.setIconified(false);
            searchView.setQuery(searchQuery, true);
            searchView.clearFocus();
        }
    }

    void loadAndShowTextPreview() {
        originalText = getFile().getRichWorkspace();
        setText(binding.textPreview, originalText, getFile(), requireActivity(), true, false);

        binding.textPreview.setVisibility(View.VISIBLE);
        binding.emptyListProgress.setVisibility(View.GONE);
    }

    private void edit() {
        new Thread(() -> {
            RemoteOperationResult result = new RichWorkspaceDirectEditingRemoteOperation(getFile().getRemotePath())
                .execute(accountManager.getUser().toPlatformAccount(), getContext());

            if (result.isSuccess()) {
                String url = (String) result.getSingleData();
                containerActivity.getFileOperationsHelper().openRichWorkspaceWithTextEditor(getFile(),
                                                                                            url,
                                                                                            getContext());
            } else {
                DisplayUtils.showSnackMessage(getView(), "Error");
            }
        }).start();
    }

    // TODO on close clean search query
}
