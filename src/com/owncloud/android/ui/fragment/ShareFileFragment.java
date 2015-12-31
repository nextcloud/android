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
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.ScrollView;
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
import com.owncloud.android.lib.resources.status.OCCapability;
import com.owncloud.android.ui.activity.FileActivity;
import com.owncloud.android.ui.adapter.ShareUserListAdapter;
import com.owncloud.android.ui.dialog.ExpirationDatePickerDialogFragment;
import com.owncloud.android.utils.DisplayUtils;
import com.owncloud.android.utils.MimetypeIconUtil;

import java.text.SimpleDateFormat;

import java.util.ArrayList;
import java.util.Date;

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

    /** The fragment initialization parameters */
    private static final String ARG_FILE = "FILE";
    private static final String ARG_ACCOUNT = "ACCOUNT";

//    /** Tag for dialog */
//    private static final String FTAG_CHOOSER_DIALOG = "CHOOSER_DIALOG";

    /** File to share, received as a parameter in construction time */
    private OCFile mFile;

    /** OC account holding the file to share, received as a parameter in construction time */
    private Account mAccount;

    /** Reference to parent listener */
    private OnShareFragmentInteractionListener mListener;

    /** List of private shares bound to the file */
    private ArrayList<OCShare> mPrivateShares;

    /** Capabilities of the server */
    private OCCapability mCapabilities;

    /** Adapter to show private shares */
    private ShareUserListAdapter mUserGroupsAdapter = null;

    /** Public share bound to the file */
    private OCShare mPublicShare;

    /** Listener for changes on switch to share / unshare publicly */
    private CompoundButton.OnCheckedChangeListener mOnShareViaLinkSwitchCheckedChangeListener;

    /**
     * Listener for user actions to set, update or clear password on public link
     */
    private OnPasswordInteractionListener mOnPasswordInteractionListener = null;

    /**
     * Listener for user actions to set, update or clear expiration date on public link
     */
    private OnExpirationDateInteractionListener mOnExpirationDateInteractionListener = null;


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
        Log_OC.d(TAG, "onCreate");
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
        Log_OC.d(TAG, "onCreateView");

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
            public void onCheckedChanged(CompoundButton switchView, boolean isChecked) {
            }
        };

        // Set listener for user actions on switch for sharing/unsharing via link
        initShareViaLinkListener(view);

        // Set listener for user actions on expiration date
        initExpirationListener(view);

        // Set listener for user actions on password
        initPasswordListener(view);

        return view;
    }


    /**
     * Binds listener for user actions to create or delete a public share
     * to the views receiving the user events.
     *
     * @param shareView     Root view in the fragment.
     */
    private void initShareViaLinkListener(View shareView) {
        mOnShareViaLinkSwitchCheckedChangeListener = new OnShareViaLinkListener();
        Switch shareViaLinkSwitch = (Switch) shareView.findViewById(R.id.shareViaLinkSectionSwitch);
        shareViaLinkSwitch.setOnCheckedChangeListener(mOnShareViaLinkSwitchCheckedChangeListener);
    }

    /**
     * Listener for user actions that create or delete a public share.
     */
    private class OnShareViaLinkListener
            implements CompoundButton.OnCheckedChangeListener {

        /**
         * Called by R.id.shareViaLinkSectionSwitch to create or delete a public link.
         *
         * @param switchView    {@link Switch} toggled by the user, R.id.shareViaLinkSectionSwitch
         * @param isChecked     New switch state.
         */
        @Override
        public void onCheckedChanged(CompoundButton switchView, boolean isChecked) {
            if (!isResumed()) {
                // very important, setCheched(...) is called automatically during
                // Fragment recreation on device rotations
                return;
            }
            if (isChecked) {
                if (mCapabilities != null &&
                        mCapabilities.getFilesSharingPublicPasswordEnforced().isTrue()) {
                    // password enforced by server, request to the user before trying to create
                    ((FileActivity) getActivity()).getFileOperationsHelper().
                            requestPasswordForShareViaLink(mFile, true);

                } else {
                    // create without password if not enforced by server or we don't know if enforced;
                    ((FileActivity) getActivity()).getFileOperationsHelper().
                            shareFileViaLink(mFile, null);

                    // FileActivtiy#onCreateShareViaLinkOperationFinish still handles the guess of enforcement
                    // for server in versions previous to OwnCloudVersion#MINIMUM_VERSION_CAPABILITIES_API
                }

            } else {
                ((FileActivity) getActivity()).getFileOperationsHelper().
                        unshareFileViaLink(mFile);
            }

            // undo the toggle to grant the view will be correct if any intermediate dialog is cancelled or
            // the create/delete operation fails
            switchView.setOnCheckedChangeListener(null);
            switchView.toggle();
            switchView.setOnCheckedChangeListener(mOnShareViaLinkSwitchCheckedChangeListener);
        }
    }


    /**
     * Binds listener for user actions that start any update on a expiration date
     * for the public link to the views receiving the user events.
     *
     * @param shareView     Root view in the fragment.
     */
    private void initExpirationListener(View shareView) {
        mOnExpirationDateInteractionListener = new OnExpirationDateInteractionListener();

        ((Switch) shareView.findViewById(R.id.shareViaLinkExpirationSwitch)).
                setOnCheckedChangeListener(mOnExpirationDateInteractionListener);

        shareView.findViewById(R.id.shareViaLinkExpirationLabel).
                setOnClickListener(mOnExpirationDateInteractionListener);

        shareView.findViewById(R.id.shareViaLinkExpirationValue).
                setOnClickListener(mOnExpirationDateInteractionListener);
    }

    /**
     * Listener for user actions that start any update on the expiration date for the public link.
     */
    private class OnExpirationDateInteractionListener
            implements CompoundButton.OnCheckedChangeListener, View.OnClickListener {

        /**
         * Called by R.id.shareViaLinkExpirationSwitch to set or clear the expiration date.
         *
         * @param switchView    {@link Switch} toggled by the user, R.id.shareViaLinkExpirationSwitch
         * @param isChecked     New switch state.
         */
        @Override
        public void onCheckedChanged(CompoundButton switchView, boolean isChecked) {
            if (!isResumed()) {
                // very important, setCheched(...) is called automatically during
                // Fragment recreation on device rotations
                return;
            }
            if (isChecked) {
                ExpirationDatePickerDialogFragment dialog =
                        ExpirationDatePickerDialogFragment.newInstance(mFile, -1);
                dialog.show(
                        getActivity().getSupportFragmentManager(),
                        ExpirationDatePickerDialogFragment.DATE_PICKER_DIALOG
                );

            } else {
                ((FileActivity) getActivity()).getFileOperationsHelper().
                        setExpirationDateToShareViaLink(mFile, -1);
            }

            // undo the toggle to grant the view will be correct if the dialog is cancelled
            switchView.setOnCheckedChangeListener(null);
            switchView.toggle();
            switchView.setOnCheckedChangeListener(mOnExpirationDateInteractionListener);
        }

        /**
         * Called by R.id.shareViaLinkExpirationLabel or R.id.shareViaLinkExpirationValue
         * to change the current expiration date.
         *
         * @param expirationView      Label or value view touched by the user.
         */
        @Override
        public void onClick(View expirationView) {
            if (mPublicShare != null && mPublicShare.getExpirationDate() > 0) {
                long chosenDateInMillis = -1;
                if (mPublicShare != null) {
                    chosenDateInMillis = mPublicShare.getExpirationDate();
                }
                ExpirationDatePickerDialogFragment dialog =
                        ExpirationDatePickerDialogFragment.newInstance(
                                mFile,
                                chosenDateInMillis
                        );
                dialog.show(
                        getActivity().getSupportFragmentManager(),
                        ExpirationDatePickerDialogFragment.DATE_PICKER_DIALOG
                );
            }
        }
    }


    /**
     * Binds listener for user actions that start any update on a password for the public link
     * to the views receiving the user events.
     *
     * @param shareView     Root view in the fragment.
     */
    private void initPasswordListener(View shareView) {
        mOnPasswordInteractionListener = new OnPasswordInteractionListener();

        ((Switch) shareView.findViewById(R.id.shareViaLinkPasswordSwitch)).
                setOnCheckedChangeListener(mOnPasswordInteractionListener);

        shareView.findViewById(R.id.shareViaLinkPasswordLabel).
                setOnClickListener(mOnPasswordInteractionListener);

        shareView.findViewById(R.id.shareViaLinkPasswordValue).
                setOnClickListener(mOnPasswordInteractionListener);
    }


    /**
     * Listener for user actions that start any update on a password for the public link.
     */
    private class OnPasswordInteractionListener
            implements CompoundButton.OnCheckedChangeListener, View.OnClickListener {

        /**
         * Called by R.id.shareViaLinkPasswordSwitch to set or clear the password.
         *
         * @param switchView    {@link Switch} toggled by the user, R.id.shareViaLinkPasswordSwitch
         * @param isChecked     New switch state.
         */
        @Override
        public void onCheckedChanged(CompoundButton switchView, boolean isChecked) {
            if (!isResumed()) {
                // very important, setCheched(...) is called automatically during
                // Fragment recreation on device rotations
                return;
            }
            if (isChecked) {
                ((FileActivity) getActivity()).getFileOperationsHelper().
                        requestPasswordForShareViaLink(mFile, false);
            } else {
                ((FileActivity) getActivity()).getFileOperationsHelper().
                        setPasswordToShareViaLink(mFile, "");   // "" clears
            }

            // undo the toggle to grant the view will be correct if the dialog is cancelled
            switchView.setOnCheckedChangeListener(null);
            switchView.toggle();
            switchView.setOnCheckedChangeListener(mOnPasswordInteractionListener);
        }

        /**
         * Called by R.id.shareViaLinkPasswordLabel or R.id.shareViaLinkPasswordValue
         * to change the current password.
         *
         * @param passwordView      Label or value view touched by the user.
         */
        @Override
        public void onClick(View passwordView) {
            if (mPublicShare != null && mPublicShare.isPasswordProtected()) {
                ((FileActivity) getActivity()).getFileOperationsHelper().
                        requestPasswordForShareViaLink(mFile, false);
            }
        }
    }


    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        Log_OC.d(TAG, "onActivityCreated");

        // Load known capabilities of the server from DB
        refreshCapabilitiesFromDB();

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
     * Get known server capabilities from DB
     *
     * Depends on the parent Activity provides a {@link com.owncloud.android.datamodel.FileDataStorageManager}
     * instance ready to use. If not ready, does nothing.
     */
    public void refreshCapabilitiesFromDB() {
        if (((FileActivity)mListener).getStorageManager() != null) {
            mCapabilities = ((FileActivity)mListener).getStorageManager().
                    getCapability(mAccount.name);
        }
    }


    /**
     * Get users and groups from the DB to fill in the "share with" list.
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
            setListViewHeightBasedOnChildren(usersList);
        } else {
            noShares.setVisibility(View.VISIBLE);
            usersList.setVisibility(View.GONE);
        }

        // Set Scroll to initial position
        ScrollView scrollView = (ScrollView) getView().findViewById(R.id.shareScroll);
        scrollView.scrollTo(0, 0);
    }

    @Override
    public void unshareButtonPressed(OCShare share) {
        // Unshare
        mListener.unshareWith(share);
        Log_OC.d(TAG, "Unshare - " + share.getSharedWithDisplayName());
    }



    /**
     * Get public link from the DB to fill in the "Share link" section in the UI.
     *
     * Takes into account server capabilities before reading database.
     *
     * Depends on the parent Activity provides a {@link com.owncloud.android.datamodel.FileDataStorageManager}
     * instance ready to use. If not ready, does nothing.
     */
    public void refreshPublicShareFromDB() {
        if (isPublicShareDisabled()) {
            hidePublicShare();

        } else if (((FileActivity) mListener).getStorageManager() != null) {
            // Get public share
            mPublicShare = ((FileActivity) mListener).getStorageManager().getFirstShareByPathAndType(
                    mFile.getRemotePath(),
                    ShareType.PUBLIC_LINK,
                    ""
            );

            // Update public share section
            updatePublicShareSection();
        }
    }

    /**
     * @return  'True' when public share is disabled in the server
     */
    private boolean isPublicShareDisabled() {
        return (mCapabilities != null &&
                mCapabilities.getFilesSharingPublicEnabled().isFalse()
        );
    }

    /**
     * Updates in the UI the section about public share with the information in the current
     * public share bound to mFile, if any
     */
    private void updatePublicShareSection() {
        if (mPublicShare != null && ShareType.PUBLIC_LINK.equals(mPublicShare.getShareType())) {
            /// public share bound -> expand section
            Switch shareViaLinkSwitch = getShareViaLinkSwitch();
            if (!shareViaLinkSwitch.isChecked()) {
                // set null listener before setChecked() to prevent infinite loop of calls
                shareViaLinkSwitch.setOnCheckedChangeListener(null);
                shareViaLinkSwitch.setChecked(true);
                shareViaLinkSwitch.setOnCheckedChangeListener(
                        mOnShareViaLinkSwitchCheckedChangeListener
                );
            }
            getExpirationDateSection().setVisibility(View.VISIBLE);
            getPasswordSection().setVisibility(View.VISIBLE);
            // GetLink button
            AppCompatButton getLinkButton = getGetLinkButton();
            getLinkButton.setVisibility(View.VISIBLE);
            getLinkButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    //GetLink from the server and show ShareLinkToDialog
                    ((FileActivity) getActivity()).getFileOperationsHelper().
                            getFileWithLink(mFile);

                }
            });

            /// update state of expiration date switch and message depending on expiration date
            Switch expirationDateSwitch = getExpirationDateSwitch();
            // set null listener before setChecked() to prevent infinite loop of calls
            expirationDateSwitch.setOnCheckedChangeListener(null);
            long expirationDate = mPublicShare.getExpirationDate();
            if (expirationDate > 0) {
                if (!expirationDateSwitch.isChecked()) {
                    expirationDateSwitch.toggle();
                }
                String formattedDate =
                        SimpleDateFormat.getDateInstance().format(
                                new Date(expirationDate)
                        );
                getExpirationDateValue().setText(formattedDate);
            } else {
                if (expirationDateSwitch.isChecked()) {
                    expirationDateSwitch.toggle();
                }
                getExpirationDateValue().setText(R.string.empty);
            }
            // recover listener
            expirationDateSwitch.setOnCheckedChangeListener(
                    mOnExpirationDateInteractionListener
            );

            /// update state of password switch and message depending on password protection
            Switch passwordSwitch = getPasswordSwitch();
            // set null listener before setChecked() to prevent infinite loop of calls
            passwordSwitch.setOnCheckedChangeListener(null);
            if (mPublicShare.isPasswordProtected()) {
                if (!passwordSwitch.isChecked()) {
                    passwordSwitch.toggle();
                }
                getPasswordValue().setVisibility(View.VISIBLE);
            } else {
                if (passwordSwitch.isChecked()) {
                    passwordSwitch.toggle();
                }
                getPasswordValue().setVisibility(View.INVISIBLE);
            }
            // recover listener
            passwordSwitch.setOnCheckedChangeListener(
                    mOnPasswordInteractionListener
            );


        } else {
            /// no public share -> collapse section
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


    /// BEWARE: next methods will failed with NullPointerException if called before onCreateView() finishes

    private Switch getShareViaLinkSwitch() {
        return (Switch) getView().findViewById(R.id.shareViaLinkSectionSwitch);
    }

    private View getExpirationDateSection() {
        return getView().findViewById(R.id.shareViaLinkExpirationSection);
    }

    private Switch getExpirationDateSwitch() {
        return (Switch) getView().findViewById(R.id.shareViaLinkExpirationSwitch);
    }

    private TextView getExpirationDateValue() {
        return (TextView) getView().findViewById(R.id.shareViaLinkExpirationValue);
    }

    private View getPasswordSection() {
        return getView().findViewById(R.id.shareViaLinkPasswordSection);
    }

    private Switch getPasswordSwitch() {
        return (Switch) getView().findViewById(R.id.shareViaLinkPasswordSwitch);
    }

    private TextView getPasswordValue() {
        return (TextView) getView().findViewById(R.id.shareViaLinkPasswordValue);
    }

    private AppCompatButton getGetLinkButton() {
        return (AppCompatButton) getView().findViewById(R.id.shareViaLinkGetLinkButton);
    }

    /**
     * Hides all the UI elements related to public share
     */
    private void hidePublicShare() {
        getShareViaLinkSwitch().setVisibility(View.GONE);
        getExpirationDateSection().setVisibility(View.GONE);
        getPasswordSection().setVisibility(View.GONE);
        getGetLinkButton().setVisibility(View.GONE);
    }

    public static void setListViewHeightBasedOnChildren(ListView listView) {
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
