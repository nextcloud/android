/**
 *   ownCloud Android client application
 *
 *   @author masensio
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
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.owncloud.android.R;
import com.owncloud.android.datamodel.FileDataStorageManager;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.datamodel.ThumbnailsCacheManager;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.lib.resources.shares.OCShare;
import com.owncloud.android.ui.activity.FileActivity;
import com.owncloud.android.ui.adapter.ShareUserListAdapter;
import com.owncloud.android.utils.DisplayUtils;
import com.owncloud.android.utils.MimetypeIconUtil;

import java.util.ArrayList;

/**
 * Fragment for Sharing a file with users
 *
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link ShareFileFragment.OnShareFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link ShareFileFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class ShareFileFragment extends Fragment {

    private static final String TAG = ShareFileFragment.class.getSimpleName();

    // the fragment initialization parameters
    private static final String ARG_FILE = "FILE";
    private static final String ARG_ACCOUNT = "ACCOUNT";

    // Parameters
    private OCFile mFile;
    private Account mAccount;

    private ArrayList<OCShare> mShares;
    private ShareUserListAdapter mUserGroupsAdapter = null;

    private OnShareFragmentInteractionListener mListener;

    /**
     * Public factory method to create new ShareFileFragment instances.
     *
     * @param fileToShare An {@link OCFile} to show in the fragment
     * @param account     An ownCloud account
     * @return A new instance of fragment ShareFileFragment.
     */
    public static ShareFileFragment newInstance(OCFile fileToShare, Account account) {
        ShareFileFragment fragment = new ShareFileFragment();
        Bundle args = new Bundle();
        args.putParcelable(ARG_FILE, fileToShare);
        args.putParcelable(ARG_ACCOUNT, account);
        fragment.setArguments(args);
        return fragment;
    }

    public ShareFileFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mFile = getArguments().getParcelable(ARG_FILE);
            mAccount = getArguments().getParcelable(ARG_ACCOUNT);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.share_file_layout, container, false);

        // Setup layout
        // Image
        ImageView icon = (ImageView) view.findViewById(R.id.shareFileIcon);
        icon.setImageResource(MimetypeIconUtil.getFileTypeIconId(mFile.getMimetype(),
                mFile.getFileName()));
        if (mFile.isImage()) {
            String remoteId = String.valueOf(mFile.getRemoteId());
            Bitmap thumbnail = ThumbnailsCacheManager.getBitmapFromDiskCache(remoteId);
            if (thumbnail != null) {
                icon.setImageBitmap(thumbnail);
            }
        }
        // Name
        TextView filename = (TextView) view.findViewById(R.id.shareFileName);
        filename.setText(mFile.getFileName());
        // Size
        TextView size = (TextView) view.findViewById(R.id.shareFileSize);
        if (mFile.isFolder()) {
            size.setVisibility(View.GONE);
        } else {
            size.setText(DisplayUtils.bytesToHumanReadable(mFile.getFileLength()));
        }

        //  Add User Button
        Button addUserGroupButton = (Button)
                view.findViewById(R.id.addUserButton);
        addUserGroupButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Show Search Fragment
                mListener.showSearchUsersAndGroups();
            }
        });

        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        refreshUsersOrGroupsList();
    }

    // TODO: Rename method, update argument and hook method into UI event
    public void onButtonPressed(Uri uri) {
        if (mListener != null) {
            mListener.onShareFragmentInteraction(uri);
        }
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (OnShareFragmentInteractionListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnShareFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    // Get users and groups to fill the "share with" list
    public void refreshUsersOrGroupsList(){
        mShares = new ArrayList<>();

        // Get Users and Groups
        FileDataStorageManager fileDataStorageManager =
                new FileDataStorageManager(mAccount, getActivity().getContentResolver());
        mShares = fileDataStorageManager.getSharesWithForAFile(mFile.getRemotePath(),
                mAccount.name);

        // Update list of users/groups
        updateListOfUserGroups();
    }

    private void updateListOfUserGroups() {
        // Update list of users/groups
        mUserGroupsAdapter = new ShareUserListAdapter(getActivity().getApplicationContext(),
                R.layout.share_user_item, mShares);

        // Show data
        TextView noShares = (TextView) getView().findViewById(R.id.shareNoUsers);
        ListView usersList = (ListView) getView().findViewById(R.id.shareUsersList);

        if (mShares.size() > 0) {
            noShares.setVisibility(View.GONE);
            usersList.setVisibility(View.VISIBLE);
            usersList.setAdapter(mUserGroupsAdapter);

            // Add unshare options
            registerLongClickListener(usersList);

        } else {
            noShares.setVisibility(View.VISIBLE);
            usersList.setVisibility(View.GONE);
        }
    }

    private void registerLongClickListener(final ListView listView) {
        listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, final int position,
                                           long id) {
                // Show unshare button
                ImageView unshareButton = (ImageView) view.findViewById(R.id.unshareButton);
                if (unshareButton.getVisibility() == View.GONE) {
                    unshareButton.setVisibility(View.VISIBLE);
                    unshareButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            // Unshare
                            unshareWith(mShares.get(position));
                            Log_OC.d(TAG, "Unshare - " +
                                    mShares.get(position).getSharedWithDisplayName());
                        }
                    });

                } else {
                    unshareButton.setVisibility(View.GONE);
                }
                view.setAlpha(0);
                view.animate().alpha(1).setDuration(500).start();
                return false;
            }
        });
    }

    private void unshareWith(OCShare share){
        OCFile file = ((FileActivity) getActivity()).getFile();

        ((FileActivity) getActivity()).getFileOperationsHelper().
                unshareFileWithUserOrGroup(
                        file, share.getShareType(), share.getShareWith()
                );
    }


    // TODO: review if it is necessary
    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p/>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnShareFragmentInteractionListener {
        void showSearchUsersAndGroups();

        void onShareFragmentInteraction(Uri uri);
    }

}
