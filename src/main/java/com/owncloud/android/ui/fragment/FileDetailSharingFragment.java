/*
 * Nextcloud Android client application
 *
 * @author Andy Scherzinger
 * Copyright (C) 2018 Andy Scherzinger
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU AFFERO GENERAL PUBLIC LICENSE
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU AFFERO GENERAL PUBLIC LICENSE for more details.
 *
 * You should have received a copy of the GNU Affero General Public
 * License along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.owncloud.android.ui.fragment;

import android.accounts.Account;
import android.app.SearchManager;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v7.widget.SearchView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.owncloud.android.R;
import com.owncloud.android.datamodel.FileDataStorageManager;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.lib.resources.shares.OCShare;
import com.owncloud.android.lib.resources.shares.SharePermissionsBuilder;
import com.owncloud.android.ui.activity.FileActivity;
import com.owncloud.android.ui.adapter.UserListAdapter;
import com.owncloud.android.utils.ThemeUtils;

import java.util.ArrayList;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;

public class FileDetailSharingFragment extends Fragment implements UserListAdapter.ShareeListAdapterListener {
    private static final String TAG = FileDetailSharingFragment.class.getSimpleName();

    private static final String ARG_FILE = "FILE";
    private static final String ARG_ACCOUNT = "ACCOUNT";

    // to show share with users/groups info
    private ArrayList<OCShare> mShares;

    private OCFile file;
    private Account account;

    private Unbinder unbinder;

    @BindView(R.id.fdShareTitle)
    TextView shareTitle;

    @BindView(R.id.fdShareWithUsersTitle)
    TextView shareWithUsersTitle;

    @BindView(R.id.fdSharebyLink)
    TextView sharebyLink;

    @BindView(R.id.fdShareLinkIcon)
    ImageView linkIcon;

    @BindView(R.id.searchView)
    SearchView searchView;

    @BindView(R.id.fdshareUsersList)
    ListView usersList;

    @BindView(R.id.fdShareNoUsers)
    TextView noList;

    public static FileDetailSharingFragment newInstance(OCFile file, Account account) {
        FileDetailSharingFragment fragment = new FileDetailSharingFragment();
        Bundle args = new Bundle();
        args.putParcelable(ARG_FILE, file);
        args.putParcelable(ARG_ACCOUNT, account);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {

        file = getArguments().getParcelable(ARG_FILE);
        account = getArguments().getParcelable(ARG_ACCOUNT);

        if (savedInstanceState != null) {
            file = savedInstanceState.getParcelable(FileActivity.EXTRA_FILE);
            account = savedInstanceState.getParcelable(FileActivity.EXTRA_ACCOUNT);
        }

        View view = inflater.inflate(R.layout.file_details_sharing_fragment, container, false);
        unbinder = ButterKnife.bind(this, view);

        setupView();

        return view;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unbinder.unbind();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (!(getActivity() instanceof FileActivity)) {
            throw new IllegalArgumentException("Calling activity must be of type FileActivity");
        }
    }

    private void setupView() {
        shareTitle.setTextColor(ThemeUtils.primaryAccentColor(getContext()));
        shareWithUsersTitle.setTextColor(ThemeUtils.primaryAccentColor(getContext()));

        setShareByLinkInfo(file.isSharedViaLink());
        setShareWithUserInfo();
        setupSearchView();
    }

    private void setupSearchView() {
        SearchManager searchManager = (SearchManager) getActivity().getSystemService(Context.SEARCH_SERVICE);
        // assumes parent activity is the searchable activity
        searchView.setSearchableInfo(searchManager
                .getSearchableInfo(getActivity().getComponentName())
        );

        // do not iconify the widget; expand it by default
        searchView.setIconifiedByDefault(false);

        // avoid fullscreen with softkeyboard
        searchView.setImeOptions(EditorInfo.IME_FLAG_NO_EXTRACT_UI);

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                Log_OC.v(TAG, "onQueryTextSubmit intercepted, query: " + query);
                // return true to prevent the query is processed to be queried;
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                // leave it for the parent listener in the hierarchy / default behaviour
                return false;
            }
        });
    }

    /**
     * Updates Share by link data
     *
     * @param isShareByLink flag is share by link is enable
     */
    private void setShareByLinkInfo(boolean isShareByLink) {
        sharebyLink.setText(isShareByLink ? R.string.filedetails_share_link_enable :
                    R.string.filedetails_share_link_disable);

        linkIcon.setVisibility(isShareByLink ? View.VISIBLE : View.GONE);
    }

    /**
     * Update Share With data
     */
    private void setShareWithUserInfo(){
        // Get Users and Groups
        if (((FileActivity) getActivity()).getStorageManager() != null) {
            FileDataStorageManager fileDataStorageManager = ((FileActivity) getActivity()).getStorageManager();
            mShares = fileDataStorageManager.getSharesWithForAFile(
                    file.getRemotePath(),account.name
            );

            // Update list of users/groups
            updateListOfUserGroups();
        }
    }

    private void updateListOfUserGroups() {
        // Update list of users/groups
        // TODO Refactoring: create a new {@link ShareUserListAdapter} instance with every call should not be needed
        UserListAdapter mUserGroupsAdapter = new UserListAdapter(
                getActivity().getApplicationContext(),
                R.layout.share_user_item, mShares, account, file,this
        );

        if (mShares.size() > 0) {
            usersList.setVisibility(View.VISIBLE);
            usersList.setAdapter(mUserGroupsAdapter);
            noList.setVisibility(View.GONE);
            setListViewHeightBasedOnChildren(usersList);
        } else {
            usersList.setVisibility(View.GONE);
            noList.setVisibility(View.VISIBLE);
        }
    }

    /**
     * Fix scroll in listview when the parent is a ScrollView
     */
    private static void setListViewHeightBasedOnChildren(ListView listView) {
        ListAdapter listAdapter = listView.getAdapter();
        if (listAdapter == null) {
            return;
        }
        int desiredWidth = View.MeasureSpec.makeMeasureSpec(listView.getWidth(), View.MeasureSpec.AT_MOST);
        int totalHeight = 0;
        View view = null;
        for (int i = 0; i < listAdapter.getCount(); i++) {
            view = listAdapter.getView(i, view, listView);
            if (i == 0) {
                view.setLayoutParams(new ViewGroup.LayoutParams(desiredWidth, ViewGroup.LayoutParams.WRAP_CONTENT));
            }
            view.measure(desiredWidth, View.MeasureSpec.UNSPECIFIED);
            totalHeight += view.getMeasuredHeight();
        }
        ViewGroup.LayoutParams params = listView.getLayoutParams();
        params.height = totalHeight + (listView.getDividerHeight() * (listAdapter.getCount() - 1));
        listView.setLayoutParams(params);
        listView.requestLayout();
    }

    @Override
    public void unshareWith(OCShare share) {
        ((FileActivity)getActivity()).getFileOperationsHelper()
                .unshareFileWithUserOrGroup(file, share.getShareType(), share.getShareWith());
    }

    @Override
    public int updatePermissionsToShare(OCShare share, boolean canReshare, boolean canEdit,
                                         boolean canEditCreate, boolean canEditChange,
                                         boolean canEditDelete) {
        SharePermissionsBuilder spb = new SharePermissionsBuilder();
        spb.setSharePermission(canReshare);
        if (file.isFolder()) {
            spb.setUpdatePermission(canEditChange)
                    .setCreatePermission(canEditCreate)
                    .setDeletePermission(canEditDelete);
        } else {
            spb.setUpdatePermission(canEdit);
        }
        int permissions = spb.build();

        ((FileActivity) getActivity()).getFileOperationsHelper().
                setPermissionsToShare(
                        share,
                        permissions
                )
        ;

        return permissions;
    }
}
