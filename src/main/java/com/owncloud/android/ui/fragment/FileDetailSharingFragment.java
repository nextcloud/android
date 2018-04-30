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
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.owncloud.android.R;
import com.owncloud.android.datamodel.FileDataStorageManager;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.lib.resources.shares.OCShare;
import com.owncloud.android.ui.activity.FileActivity;
import com.owncloud.android.ui.adapter.UserListAdapter;

import java.util.ArrayList;

public class FileDetailSharingFragment  extends Fragment {

    private static final String ARG_FILE = "FILE";
    private static final String ARG_ACCOUNT = "ACCOUNT";

    // to show share with users/groups info
    private ArrayList<OCShare> mShares;

    private OCFile file;
    private Account account;

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

        setupView(view);

        return view;
    }

    private void setupView(View view) {
        // TODO populate sharing UI

        setShareByLinkInfo(file.isSharedViaLink(), view);

        setShareWithUserInfo(view);
    }

    /**
     * Updates Share by link data
     *
     * @param isShareByLink flag is share by link is enable
     */
    private void setShareByLinkInfo(boolean isShareByLink, View view) {
        TextView tv = view.findViewById(R.id.fdSharebyLink);
        if (tv != null) {
            tv.setText(isShareByLink ? R.string.filedetails_share_link_enable :
                    R.string.filedetails_share_link_disable);
        }
        ImageView linkIcon = view.findViewById(R.id.fdShareLinkIcon);
        if (linkIcon != null) {
            linkIcon.setVisibility(isShareByLink ? View.VISIBLE : View.GONE);
        }
    }

    /**
     * Update Share With data
     */
    private void setShareWithUserInfo(View view){
        // Get Users and Groups
        if (((FileActivity) getActivity()).getStorageManager() != null) {
            FileDataStorageManager fileDataStorageManager = ((FileActivity) getActivity()).getStorageManager();
            mShares = fileDataStorageManager.getSharesWithForAFile(
                    file.getRemotePath(),account.name
            );

            // Update list of users/groups
            updateListOfUserGroups(view);
        }
    }

    private void updateListOfUserGroups(View view) {
        // Update list of users/groups
        // TODO Refactoring: create a new {@link ShareUserListAdapter} instance with every call should not be needed
        UserListAdapter mUserGroupsAdapter = new UserListAdapter(
                getActivity().getApplicationContext(),
                R.layout.share_user_item, mShares
        );

        // Show data
        ListView usersList = view.findViewById(R.id.fdshareUsersList);

        // No data
        TextView noList = view.findViewById(R.id.fdShareNoUsers);

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
}
