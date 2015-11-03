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
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.AppCompatButton;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.owncloud.android.R;
import com.owncloud.android.authentication.AccountUtils;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.datamodel.ThumbnailsCacheManager;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.lib.resources.shares.OCShare;
import com.owncloud.android.lib.resources.shares.ShareType;
import com.owncloud.android.ui.activity.FileActivity;
import com.owncloud.android.ui.adapter.ShareUserListAdapter;
import com.owncloud.android.utils.DisplayUtils;
import com.owncloud.android.utils.MimetypeIconUtil;

import java.util.ArrayList;

/**
 * Fragment for Sharing a file with sharees (users or groups) or creating
 * a public link.
 *
 * A simple {@link Fragment} subclass.
 *
 * Activities that contain this fragment must implement the
 * {@link ShareFileFragment.OnShareFragmentInteractionListener} interface
 * to handle interaction events.
 *
 * Use the {@link ShareFileFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class ShareFileFragment extends Fragment
        implements ShareUserListAdapter.ShareUserAdapterListener{

    private static final String TAG = ShareFileFragment.class.getSimpleName();

    // the fragment initialization parameters
    private static final String ARG_FILE = "FILE";
    private static final String ARG_ACCOUNT = "ACCOUNT";

    /** File to share, received as a parameter in construction time */
    private OCFile mFile;

    /** OC account holding the file to share, received as a parameter in construction time */
    private Account mAccount;

    /** Reference to parent listener */
    private OnShareFragmentInteractionListener mListener;

    /** List of private shares bound to the file */
    private ArrayList<OCShare> mPrivateShares;

    /** Adapter to show private shares */
    private ShareUserListAdapter mUserGroupsAdapter = null;

    /** Public share bound to the file */
    private OCShare mPublicShare;

    /** Listener for changes on switch to share / unshare publicly */
    private CompoundButton.OnCheckedChangeListener mOnShareViaLinkSwitchCheckedChangeListener;


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

    /**
     * {@inheritDoc}
     */
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
                boolean shareWithUsersEnable = AccountUtils.hasSearchUsersSupport(mAccount);
                if (shareWithUsersEnable) {
                    // Show Search Fragment
                    mListener.showSearchUsersAndGroups();
                } else {
                    String message = getString(R.string.share_sharee_unavailable);
                    Toast.makeText(getActivity(), message, Toast.LENGTH_LONG).show();
                }
            }
        });

        // Switch to create public share
        mOnShareViaLinkSwitchCheckedChangeListener = new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isResumed()) {
                    if (isChecked) {
                        ((FileActivity) getActivity()).getFileOperationsHelper().shareFileViaLink(mFile);

                    } else {
                        ((FileActivity) getActivity()).getFileOperationsHelper().unshareFileViaLink(mFile);
                    }
                } // else, nothing; very important, setCheched(...) is called automatically during Fragment
                  // recreation on device rotations
            }
        };
        Switch shareViaLinkSwitch = (Switch) view.findViewById(R.id.shareViaLinkSectionSwitch);
        shareViaLinkSwitch.setOnCheckedChangeListener(mOnShareViaLinkSwitchCheckedChangeListener);

        // Switch for expiration date
        Switch shareViaLinkExpirationSwitch = (Switch) view.findViewById(R.id.shareViaLinkExpirationSwitch);
        shareViaLinkExpirationSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    // TODO real implementation: update share with expiration date
                    // show value of expiration date
                    getExpirationDateValue().setText(R.string.placeholder_timestamp);

                } else {
                    // TODO real implementation: update share without expiration date
                    // empty value
                    getExpirationDateValue().setText(R.string.empty);
                }
            }
        });

        // Switch for password
        Switch shareViaLinkPasswordSwitch = (Switch) view.findViewById(R.id.shareViaLinkPasswordSwitch);
        shareViaLinkPasswordSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    // TODO real implementation: update share with password
                    // show
                    getExpirationPasswordValue().setVisibility(View.VISIBLE);

                } else {
                    // TODO real implementation: update share without password
                    // empty value
                    getExpirationPasswordValue().setVisibility(View.INVISIBLE);
                }
            }
        });


        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // Load data into the list of private shares
        refreshUsersOrGroupsListFromDB();

        // Load data of public share, if exists
        refreshPublicShareFromDB();
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

    /**
     * Get users and groups from the DB to fill in the "share with" list
     *
     * Depends on the parent Activity provides a {@link com.owncloud.android.datamodel.FileDataStorageManager}
     * instance ready to use. If not ready, does nothing.
     */
    public void refreshUsersOrGroupsListFromDB (){
        if (((FileActivity) mListener).getStorageManager() != null) {
            // Get Users and Groups
            mPrivateShares = ((FileActivity) mListener).getStorageManager().getSharesWithForAFile(
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
                getActivity(),
                R.layout.share_user_item,
                mPrivateShares,
                this
        );

        // Show data
        TextView noShares = (TextView) getView().findViewById(R.id.shareNoUsers);
        ListView usersList = (ListView) getView().findViewById(R.id.shareUsersList);

        if (mPrivateShares.size() > 0) {
            noShares.setVisibility(View.GONE);
            usersList.setVisibility(View.VISIBLE);
            usersList.setAdapter(mUserGroupsAdapter);

        } else {
            noShares.setVisibility(View.VISIBLE);
            usersList.setVisibility(View.GONE);
        }
    }

    @Override
    public void unshareButtonPressed(OCShare share) {
        // Unshare
        mListener.unshareWith(share);
        Log_OC.d(TAG, "Unshare - " + share.getSharedWithDisplayName());
    }



    /**
     * Get public link from the DB to fill in the "Share link" section
     *
     * Depends on the parent Activity provides a {@link com.owncloud.android.datamodel.FileDataStorageManager}
     * instance ready to use. If not ready, does nothing.
     */
    public void refreshPublicShareFromDB() {
        if (((FileActivity) mListener).getStorageManager() != null) {
            // Get public share
            mPublicShare = ((FileActivity) mListener).getStorageManager().getFirstShareByPathAndType(
                    mFile.getRemotePath(),
                    ShareType.PUBLIC_LINK,
                    ""
            );

            // Update list of users/groups
            updatePublicShareSection();
        }
    }

    /**
     * Updates in the UI the section about public share with the information in the current
     * public share bound to mFile, if any
     */
    private void updatePublicShareSection() {
        if (mPublicShare != null && ShareType.PUBLIC_LINK.equals(mPublicShare.getShareType())) {
            // public share bound -> expand section
            Switch shareViaLinkSwitch = getShareViaLinkSwitch();
            if (!shareViaLinkSwitch.isChecked()) {
                // set null listener before setChecked() to prevent infinite loop of calls
                shareViaLinkSwitch.setOnCheckedChangeListener(null);
                getShareViaLinkSwitch().setChecked(true);
                shareViaLinkSwitch.setOnCheckedChangeListener(
                        mOnShareViaLinkSwitchCheckedChangeListener
                );
            }
            getExpirationDateSection().setVisibility(View.VISIBLE);
            getPasswordSection().setVisibility(View.VISIBLE);
            getGetLinkButton().setVisibility(View.VISIBLE);

        } else {
            // no public share -> collapse section
            Switch shareViaLinkSwitch = getShareViaLinkSwitch();
            if (shareViaLinkSwitch.isChecked()) {
                shareViaLinkSwitch.setOnCheckedChangeListener(null);
                getShareViaLinkSwitch().setChecked(false);
                shareViaLinkSwitch.setOnCheckedChangeListener(
                        mOnShareViaLinkSwitchCheckedChangeListener
                );
            }
            getExpirationDateSection().setVisibility(View.GONE);
            getPasswordSection().setVisibility(View.GONE);
            getGetLinkButton().setVisibility(View.GONE);
        }
    }

    private Switch getShareViaLinkSwitch() {
        return (Switch) getView().findViewById(R.id.shareViaLinkSectionSwitch);
    }

    private View getExpirationDateSection() {
        return getView().findViewById(R.id.shareViaLinkExpirationSection);
    }

    private TextView getExpirationDateValue() {
        return (TextView) getView().findViewById(R.id.shareViaLinkExpirationValue);
    }

    private View getPasswordSection() {
        return getView().findViewById(R.id.shareViaLinkPasswordSection);
    }

    private TextView getExpirationPasswordValue() {
        return (TextView) getView().findViewById(R.id.shareViaLinkPasswordValue);
    }

    private AppCompatButton getGetLinkButton() {
        return (AppCompatButton) getView().findViewById(R.id.shareViewLinkGetLinkButton);
    }


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
        void refreshUsersOrGroupsListFromServer();
        void unshareWith(OCShare share);
    }

}
