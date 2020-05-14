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

import android.accounts.Account;
import android.os.Build;
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
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.ui.activity.FileDisplayActivity;
import com.owncloud.android.utils.DisplayUtils;
import com.owncloud.android.utils.ThemeUtils;

import org.jetbrains.annotations.NotNull;

import javax.inject.Inject;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.SearchView;
import androidx.core.view.MenuItemCompat;

public class PreviewTextStringFragment extends PreviewTextFragment {
    private static final String EXTRA_FILE = "FILE";

    @Inject UserAccountManager accountManager;

    /**
     * Creates an empty fragment for previews.
     * <p>
     * MUST BE KEPT: the system uses it when tries to re-instantiate a fragment automatically (for instance, when the
     * device is turned a aside).
     * <p>
     * DO NOT CALL IT: an {@link OCFile} and {@link Account} must be provided for a successful construction
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
            mSearchQuery = args.getString(FileDisplayActivity.EXTRA_SEARCH_QUERY);
        }
        mSearchOpen = args.getBoolean(FileDisplayActivity.EXTRA_SEARCH, false);

        mHandler = new Handler();
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

        FloatingActionButton fabMain = view.findViewById(R.id.text_preview_fab);

        if (android.os.Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            fabMain.setVisibility(View.GONE);
        } else {
            fabMain.setVisibility(View.VISIBLE);
            fabMain.setEnabled(true);
            fabMain.setOnClickListener(v -> edit());
            ThemeUtils.tintFloatingActionButton(fabMain, requireContext());
            ThemeUtils.drawableFloatingActionButton(fabMain, R.drawable.ic_edit, requireContext());
        }

        return view;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onCreateOptionsMenu(@NotNull Menu menu, @NotNull MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);

        MenuItem menuItem = menu.findItem(R.id.action_search);
        menuItem.setVisible(true);
        mSearchView = (SearchView) MenuItemCompat.getActionView(menuItem);
        mSearchView.setOnQueryTextListener(this);
        mSearchView.setMaxWidth(Integer.MAX_VALUE);

        if (mSearchOpen) {
            mSearchView.setIconified(false);
            mSearchView.setQuery(mSearchQuery, true);
            mSearchView.clearFocus();
        }
    }

    @Override
    public void onPrepareOptionsMenu(@NonNull Menu menu) {
        super.onPrepareOptionsMenu(menu);

        menu.findItem(R.id.action_sync_account).setVisible(false);
        menu.findItem(R.id.action_sort).setVisible(false);
        menu.findItem(R.id.action_switch_view).setVisible(false);
    }

    void loadAndShowTextPreview() {
        if (mTextPreview != null) {
            mOriginalText = getFile().getRichWorkspace();
            setText(mTextPreview, mOriginalText, getFile(), requireActivity(), true, false);
            mTextPreview.setVisibility(View.VISIBLE);
        }

        if (mMultiView != null) {
            mMultiView.setVisibility(View.GONE);
        }
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
