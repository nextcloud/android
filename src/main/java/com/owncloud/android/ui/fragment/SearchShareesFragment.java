/**
 *   ownCloud Android client application
 *
 *   @author masensio
 *   @author David A. Velasco
 *   Copyright (C) 2015 ownCloud Inc.
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License version 2,
 *   as published by the Free Software Foundation.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package com.owncloud.android.ui.fragment;

import android.accounts.Account;
import android.app.Activity;
import android.app.SearchManager;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.SearchView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.ListView;

import com.owncloud.android.MainApp;
import com.owncloud.android.R;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.lib.resources.shares.OCShare;
import com.owncloud.android.ui.activity.FileActivity;
import com.owncloud.android.ui.adapter.ShareUserListAdapter;

import java.util.ArrayList;

/**
 * Fragment for Searching sharees (users and groups)
 *
 * A simple {@link Fragment} subclass.
 *
 * Activities that contain this fragment must implement the
 * {@link ShareFragmentListener} interface
 * to handle interaction events.
 *
 * Use the {@link SearchShareesFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class SearchShareesFragment extends Fragment implements ShareUserListAdapter.ShareUserAdapterListener {
    private static final String TAG = SearchShareesFragment.class.getSimpleName();

    // the fragment initialization parameters
    private static final String ARG_FILE = "FILE";
    private static final String ARG_ACCOUNT = "ACCOUNT";

    private static final String SCREEN_NAME = "Sharee search";

    // Parameters
    private OCFile mFile;
    private Account mAccount;

    // other members
    private ArrayList<OCShare> mShares;
    private ShareUserListAdapter mUserGroupsAdapter = null;
    private ShareFragmentListener mListener;


    /**
     * Public factory method to create new SearchShareesFragment instances.
     *
     * @param fileToShare   An {@link OCFile} to be shared
     * @param account       The ownCloud account containing fileToShare
     * @return A new instance of fragment SearchShareesFragment.
     */
    public static SearchShareesFragment newInstance(OCFile fileToShare, Account account) {
        SearchShareesFragment fragment = new SearchShareesFragment();
        Bundle args = new Bundle();
        args.putParcelable(ARG_FILE, fileToShare);
        args.putParcelable(ARG_ACCOUNT, account);
        fragment.setArguments(args);
        return fragment;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mFile = getArguments().getParcelable(ARG_FILE);
            mAccount = getArguments().getParcelable(ARG_ACCOUNT);
        }

    }

    @Override
    public void onResume() {
        super.onResume();
        if (getActivity() != null) {
            MainApp.getFirebaseAnalyticsInstance().setCurrentScreen(getActivity(), SCREEN_NAME, TAG);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.search_users_groups_layout, container, false);

        // Get the SearchView and set the searchable configuration
        SearchView searchView = (SearchView) view.findViewById(R.id.searchView);
        SearchManager searchManager = (SearchManager) getActivity().getSystemService(Context.SEARCH_SERVICE);
        searchView.setSearchableInfo(searchManager.getSearchableInfo(
                getActivity().getComponentName())   // assumes parent activity is the searchable activity
        );
        searchView.setIconifiedByDefault(false);    // do not iconify the widget; expand it by default

        searchView.setImeOptions(EditorInfo.IME_FLAG_NO_EXTRACT_UI); // avoid fullscreen with softkeyboard

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                Log_OC.v(TAG, "onQueryTextSubmit intercepted, query: " + query);
                return true;    // return true to prevent the query is processed to be queried;
                // a user / group will be picked only if selected in the list of suggestions
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                return false;   // let it for the parent listener in the hierarchy / default behaviour
            }
        });

        return view;
    }


    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        getActivity().setTitle(R.string.share_with_title);

        // Load data into the list
        refreshUsersOrGroupsListFromDB();
    }


    /**
     * Get users and groups from the DB to fill in the "share with" list
     *
     * Depends on the parent Activity provides a {@link com.owncloud.android.datamodel.FileDataStorageManager}
     * instance ready to use. If not ready, does nothing.
     */
    public void refreshUsersOrGroupsListFromDB (){
        // Get Users and Groups
        if (((FileActivity) mListener).getStorageManager() != null) {
            mShares = ((FileActivity) mListener).getStorageManager().getSharesWithForAFile(
                    mFile.getRemotePath(),
                    mAccount.name
            );

            // Update list of users/groups
            updateListOfUserGroups();
        }
    }

    private void updateListOfUserGroups() {
        // Update list of users/groups
        // TODO Refactoring: create a new {@link ShareUserListAdapter} instance with every call should not be needed
        mUserGroupsAdapter = new ShareUserListAdapter(
                getActivity().getApplicationContext(),
                R.layout.share_user_item, mShares, this
        );

        // Show data
        ListView usersList = (ListView) getView().findViewById(R.id.searchUsersListView);

        if (mShares.size() > 0) {
            usersList.setVisibility(View.VISIBLE);
            usersList.setAdapter(mUserGroupsAdapter);

        } else {
            usersList.setVisibility(View.GONE);
        }
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (ShareFragmentListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        // focus the search view and request the software keyboard be shown
        View searchView = getView().findViewById(R.id.searchView);
        if (searchView.requestFocus()) {
            InputMethodManager imm = (InputMethodManager)
                    getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.showSoftInput(searchView.findFocus(), InputMethodManager.SHOW_IMPLICIT);
            }
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    @Override
    public void unshareButtonPressed(OCShare share) {
        // Unshare
        mListener.unshareWith(share);
        Log_OC.d(TAG, "Unshare - " + share.getSharedWithDisplayName());
    }

    @Override
    public void editShare(OCShare share) {
        // move to fragment to edit share
        Log_OC.d(TAG, "Editing " + share.getSharedWithDisplayName());
        mListener.showEditShare(share);
    }

}
