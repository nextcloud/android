/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2019 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2019 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
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
import com.nextcloud.utils.extensions.FileExtensionsKt;
import com.owncloud.android.R;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.ui.activity.FileDisplayActivity;
import com.owncloud.android.utils.DisplayUtils;
import com.owncloud.android.utils.theme.ViewThemeUtils;

import javax.inject.Inject;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.SearchView;
import androidx.core.view.MenuItemCompat;

public class PreviewTextStringFragment extends PreviewTextFragment {
    private static final String EXTRA_FILE = "FILE";

    @Inject UserAccountManager accountManager;
    @Inject ViewThemeUtils viewThemeUtils;

    private final static String TAG = "PreviewTextStringFragment";
    private boolean isEditorWebviewLaunched = false;

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
        FileExtensionsKt.logFileSize(getFile(), TAG);
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

        fabMain.setImageResource(R.drawable.ic_edit);
        viewThemeUtils.material.themeFAB(fabMain);

        return view;
    }

    @Override
    public void onStart() {
        if (isEditorWebviewLaunched && containerActivity instanceof FileDisplayActivity fileDisplayActivity) {
            fileDisplayActivity.getSupportFragmentManager().popBackStack();
            fileDisplayActivity.onRefresh();
        }

        super.onStart();
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
        viewThemeUtils.androidx.themeToolbarSearchView(searchView);

        if (searchOpen) {
            searchView.setIconified(false);
            searchView.setQuery(searchQuery, true);
            searchView.clearFocus();
        }
    }

    void loadAndShowTextPreview() {
        originalText = getFile().getRichWorkspace();
        setText(binding.textPreview, originalText, getFile(), requireActivity(), true, false, viewThemeUtils);

        binding.textPreview.setVisibility(View.VISIBLE);
        binding.emptyListProgress.setVisibility(View.GONE);
    }

    private void edit() {
        new Thread(() -> {
            RemoteOperationResult result = new RichWorkspaceDirectEditingRemoteOperation(getFile().getRemotePath())
                .execute(accountManager.getUser(), getContext());

            if (result.isSuccess()) {
                String url = (String) result.getSingleData();
                containerActivity.getFileOperationsHelper().openRichWorkspaceWithTextEditor(getFile(),
                                                                                            url,
                                                                                            getContext());
                isEditorWebviewLaunched = true;
            } else {
                DisplayUtils.showSnackMessage(getView(), "Error");
            }
        }).start();
    }

    // TODO on close clean search query
}
