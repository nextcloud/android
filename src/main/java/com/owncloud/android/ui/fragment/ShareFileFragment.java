/*
 * ownCloud Android client application
 *
 * @author masensio
 * @author David A. Velasco
 * @author Juan Carlos González Cabrero
 * @author Andy Scherzinger
 * Copyright (C) 2015 ownCloud Inc.
 * Copyright (C) 2018 Andy Scherzinger
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.owncloud.android.ui.fragment;

import android.accounts.Account;
import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.TextView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;
import com.owncloud.android.R;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.datamodel.ThumbnailsCacheManager;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.lib.resources.shares.OCShare;
import com.owncloud.android.lib.resources.shares.ShareType;
import com.owncloud.android.lib.resources.status.OCCapability;
import com.owncloud.android.ui.activity.FileActivity;
import com.owncloud.android.ui.adapter.ShareUserListAdapter;
import com.owncloud.android.ui.dialog.ExpirationDatePickerDialogFragment;
import com.owncloud.android.ui.dialog.SharePasswordDialogFragment;
import com.owncloud.android.utils.DisplayUtils;
import com.owncloud.android.utils.MimeTypeUtil;
import com.owncloud.android.utils.ThemeUtils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.SwitchCompat;
import androidx.fragment.app.Fragment;

/**
 * Fragment for Sharing a file with sharees (users or groups) or creating
 * a public link.
 * <p/>
 * A simple {@link Fragment} subclass.
 * <p/>
 * Activities that contain this fragment must implement the
 * {@link ShareFragmentListener} interface
 * to handle interaction events.
 * <p/>
 * Use the {@link ShareFileFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class ShareFileFragment extends Fragment implements ShareUserListAdapter.ShareUserAdapterListener {

    private static final String TAG = ShareFileFragment.class.getSimpleName();

    /**
     * The fragment initialization parameters.
     */
    private static final String ARG_FILE = "FILE";
    private static final String ARG_ACCOUNT = "ACCOUNT";

//    /** Tag for dialog */
//    private static final String FTAG_CHOOSER_DIALOG = "CHOOSER_DIALOG";

    /**
     * File to share, received as a parameter in construction time.
     */
    private OCFile mFile;

    /**
     * OC account holding the file to share, received as a parameter in construction time.
     */
    private Account mAccount;

    /**
     * Reference to parent listener.
     */
    private ShareFragmentListener mListener;

    /**
     * List of private shares bound to the file.
     */
    private List<OCShare> mPrivateShares;

    /**
     * Capabilities of the server.
     */
    private OCCapability mCapabilities;

    /**
     * Public share bound to the file.
     */
    private OCShare mPublicShare;

    /**
     * Listener for changes on switch to share / unshare publicly.
     */
    private CompoundButton.OnCheckedChangeListener mOnShareViaLinkSwitchCheckedChangeListener;

    /**
     * Listener for user actions to set, update or clear password on public link.
     */
    private OnPasswordInteractionListener mOnPasswordInteractionListener;

    /**
     * Listener for user actions to set, update or clear expiration date on public link.
     */
    private OnExpirationDateInteractionListener mOnExpirationDateInteractionListener;

    /**
     * Listener for user actions to set or unset edit permission on public link.
     */
    private OnEditPermissionInteractionListener mOnEditPermissionInteractionListener;

    /**
     * Listener for user actions to set or unset hide file listing permission on public link.
     */
    private OnHideFileListingPermissionInteractionListener mOnHideFileListingPermissionInteractionListener;

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

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log_OC.d(TAG, "onCreate");
        if (getArguments() != null) {
            mFile = getArguments().getParcelable(ARG_FILE);
            mAccount = getArguments().getParcelable(ARG_ACCOUNT);
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log_OC.d(TAG, "onCreateView");

        // use grey as fallback for elements where custom theming is not available
        if (ThemeUtils.themingEnabled(getContext())) {
            getContext().getTheme().applyStyle(R.style.FallbackThemingTheme, true);
        }

        int accentColor = ThemeUtils.primaryAccentColor(getContext());

        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.share_file_layout, container, false);

        // Setup layout
        // Image
        ImageView icon = view.findViewById(R.id.shareFileIcon);
        icon.setImageDrawable(
                MimeTypeUtil.getFileTypeIcon(mFile.getMimeType(), mFile.getFileName(), mAccount, getContext())
        );
        if (MimeTypeUtil.isImage(mFile)) {
            String remoteId = String.valueOf(mFile.getRemoteId());
            Bitmap thumbnail = ThumbnailsCacheManager.getBitmapFromDiskCache(remoteId);
            if (thumbnail != null) {
                icon.setImageBitmap(thumbnail);
            }
        }

        // Title
        TextView title = view.findViewById(R.id.shareWithUsersSectionTitle);
        title.setTextColor(accentColor);

        // Name
        TextView fileNameHeader = view.findViewById(R.id.shareFileName);
        fileNameHeader.setText(getResources().getString(R.string.share_file, mFile.getFileName()));

        View headerDivider = view.findViewById(R.id.share_header_divider);
        headerDivider.getBackground().setColorFilter(ThemeUtils.primaryAccentColor(getContext()),
                PorterDuff.Mode.SRC_ATOP);

        // Size
        TextView size = view.findViewById(R.id.shareFileSize);
        if (mFile.isFolder()) {
            size.setVisibility(View.GONE);
        } else {
            size.setText(DisplayUtils.bytesToHumanReadable(mFile.getFileLength()));
        }

        //  Add User Button
        Button addUserGroupButton = view.findViewById(R.id.addUserButton);
        addUserGroupButton.getBackground().setColorFilter(accentColor, PorterDuff.Mode.SRC_ATOP);
        addUserGroupButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Show Search Fragment
                mListener.showSearchUsersAndGroups();
            }
        });

        // Set listener for user actions on switch for sharing/unsharing via link
        initShareViaLinkListener(view);

        // Set listener for user actions on expiration date
        initExpirationListener(view);

        // Set listener for user actions on password
        initPasswordListener(view);

        // Set listener for user actions on edit permission
        initEditPermissionListener(view);

        // Set listener for hide file listing
        initHideFileListingListener(view);

        // Hide share features sections that are not enabled
        hideNotEnabledShareSections(view);

        return view;
    }


    /**
     * Binds listener for user actions to create or delete a public share
     * to the views receiving the user events.
     *
     * @param shareView Root view in the fragment.
     */
    private void initShareViaLinkListener(View shareView) {
        mOnShareViaLinkSwitchCheckedChangeListener = new OnShareViaLinkListener();
        SwitchCompat shareViaLinkSwitch = shareView.findViewById(R.id.shareViaLinkSectionSwitch);
        ThemeUtils.tintSwitch(shareViaLinkSwitch, ThemeUtils.primaryAccentColor(getContext()), true);
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
         * @param switchView {@link SwitchCompat} toggled by the user, R.id.shareViaLinkSectionSwitch
         * @param isChecked  New switch state.
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
                    requestPasswordForShareViaLink(true);

                } else {
                    // create without password if not enforced by server or we don't know if enforced;
                    ((FileActivity) getActivity()).getFileOperationsHelper().
                            shareFileViaLink(mFile, null);

                    // ShareActivity#onCreateShareViaLinkOperationFinish will take care if password
                    // is enforced by the server but app doesn't know, or if server version is
                    // older than OwnCloudVersion#MINIMUM_VERSION_CAPABILITIES_API
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
     * @param shareView Root view in the fragment.
     */
    private void initExpirationListener(View shareView) {
        mOnExpirationDateInteractionListener = new OnExpirationDateInteractionListener();

        SwitchCompat expirationSwitch = shareView.findViewById(R.id.shareViaLinkExpirationSwitch);
        expirationSwitch.setOnCheckedChangeListener(mOnExpirationDateInteractionListener);

        ThemeUtils.tintSwitch(expirationSwitch, ThemeUtils.primaryAccentColor(getContext()));

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
         * @param switchView {@link SwitchCompat} toggled by the user, R.id.shareViaLinkExpirationSwitch
         * @param isChecked  New switch state.
         */
        @Override
        public void onCheckedChanged(CompoundButton switchView, boolean isChecked) {
            if (!isResumed()) {
                // very important, setCheched(...) is called automatically during
                // Fragment recreation on device rotations
                return;
            }
            if (isChecked) {
                ExpirationDatePickerDialogFragment dialog = ExpirationDatePickerDialogFragment.newInstance(mFile, -1);
                dialog.show(
                        getActivity().getSupportFragmentManager(),
                        ExpirationDatePickerDialogFragment.DATE_PICKER_DIALOG
                );

            } else {
                ((FileActivity) getActivity()).getFileOperationsHelper().setExpirationDateToShareViaLink(mFile, -1);
            }

            // undo the toggle to grant the view will be correct if the dialog is cancelled
            switchView.setOnCheckedChangeListener(null);
            switchView.toggle();
            switchView.setOnCheckedChangeListener(mOnExpirationDateInteractionListener);
        }

        /**
         * Called by R.id.shareViaLinkExpirationLabel or R.id.shareViaLinkExpirationValue to change the current
         * expiration date.
         *
         * @param expirationView Label or value view touched by the user.
         */
        @Override
        public void onClick(View expirationView) {
            if (mPublicShare != null && mPublicShare.getExpirationDate() > 0) {
                long chosenDateInMillis = -1;
                if (mPublicShare != null) {
                    chosenDateInMillis = mPublicShare.getExpirationDate();
                }
                ExpirationDatePickerDialogFragment dialog =
                        ExpirationDatePickerDialogFragment.newInstance(mFile, chosenDateInMillis);
                dialog.show(
                        getActivity().getSupportFragmentManager(),
                        ExpirationDatePickerDialogFragment.DATE_PICKER_DIALOG
                );
            }
        }
    }

    /**
     * Binds listener for user actions that start any update on a password for the public link to the views receiving
     * the user events.
     *
     * @param shareView Root view in the fragment.
     */
    private void initPasswordListener(View shareView) {
        mOnPasswordInteractionListener = new OnPasswordInteractionListener();

        SwitchCompat passwordSwitch = shareView.findViewById(R.id.shareViaLinkPasswordSwitch);
        passwordSwitch.setOnCheckedChangeListener(mOnPasswordInteractionListener);
        ThemeUtils.tintSwitch(passwordSwitch, ThemeUtils.primaryAccentColor(getContext()));

        shareView.findViewById(R.id.shareViaLinkPasswordLabel).setOnClickListener(mOnPasswordInteractionListener);

        shareView.findViewById(R.id.shareViaLinkPasswordValue).setOnClickListener(mOnPasswordInteractionListener);
    }


    /**
     * Listener for user actions that start any update on a password for the public link.
     */
    private class OnPasswordInteractionListener
            implements CompoundButton.OnCheckedChangeListener, View.OnClickListener {

        /**
         * Called by R.id.shareViaLinkPasswordSwitch to set or clear the password.
         *
         * @param switchView {@link SwitchCompat} toggled by the user, R.id.shareViaLinkPasswordSwitch
         * @param isChecked  New switch state.
         */
        @Override
        public void onCheckedChanged(CompoundButton switchView, boolean isChecked) {
            if (!isResumed()) {
                // very important, setChecked(...) is called automatically during
                // Fragment recreation on device rotations
                return;
            }
            if (isChecked) {
                requestPasswordForShareViaLink(false);
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
         * @param passwordView Label or value view touched by the user.
         */
        @Override
        public void onClick(View passwordView) {
            if (mPublicShare != null && mPublicShare.isPasswordProtected()) {
                requestPasswordForShareViaLink(false);
            }
        }
    }

    /**
     * Binds listener for user actions that start any update the edit permissions
     * for the public link to the views receiving the user events.
     *
     * @param shareView Root view in the fragment.
     */
    private void initEditPermissionListener(View shareView) {
        mOnEditPermissionInteractionListener = new OnEditPermissionInteractionListener();

        SwitchCompat permissionSwitch = shareView.findViewById(R.id.shareViaLinkEditPermissionSwitch);
        permissionSwitch.setOnCheckedChangeListener(mOnEditPermissionInteractionListener);
        ThemeUtils.tintSwitch(permissionSwitch, ThemeUtils.primaryAccentColor(getContext()));
    }

    /**
     * Binds listener for user actions that start any update the hide file listing permissions
     * for the public link to the views receiving the user events.
     *
     * @param shareView Root view in the fragment.
     */
    private void initHideFileListingListener(View shareView) {
        mOnHideFileListingPermissionInteractionListener = new OnHideFileListingPermissionInteractionListener();

        SwitchCompat permissionSwitch = shareView.findViewById(R.id.shareViaLinkFileListingPermissionSwitch);
        permissionSwitch.setOnCheckedChangeListener(mOnHideFileListingPermissionInteractionListener);
        ThemeUtils.tintSwitch(permissionSwitch, ThemeUtils.primaryAccentColor(getContext()));
    }

    /**
     * Listener for user actions that start any update on the edit permissions for the public link.
     */
    private class OnEditPermissionInteractionListener implements CompoundButton.OnCheckedChangeListener {

        /**
         * Called by R.id.shareViaLinkEditPermissionSwitch to set or clear the edit permission.
         *
         * @param switchView {@link SwitchCompat} toggled by the user, R.id.shareViaLinkEditPermissionSwitch
         * @param isChecked  New switch state.
         */
        @Override
        public void onCheckedChanged(CompoundButton switchView, boolean isChecked) {
            if (!isResumed()) {
                // very important, setCheched(...) is called automatically during
                // Fragment recreation on device rotations
                return;
            }

            ((FileActivity) getActivity()).getFileOperationsHelper().setUploadPermissionsToShare(mFile, isChecked);

            // undo the toggle to grant the view will be correct if the dialog is cancelled
            switchView.setOnCheckedChangeListener(null);
            switchView.toggle();
            switchView.setOnCheckedChangeListener(mOnEditPermissionInteractionListener);
        }
    }

    /**
     * Listener for user actions that start any update on the hide file listing permissions for the public link.
     */
    private class OnHideFileListingPermissionInteractionListener implements CompoundButton.OnCheckedChangeListener {

        /**
         * Called by R.id.shareViaLinkHideListPermissionSwitch to set or clear the edit permission.
         *
         * @param switchView {@link SwitchCompat} toggled by the user, R.id.shareViaLinkHideListPermissionSwitch
         * @param isChecked  New switch state.
         */
        @Override
        public void onCheckedChanged(CompoundButton switchView, boolean isChecked) {
            if (!isResumed()) {
                // very important, setChecked(...) is called automatically during
                // Fragment recreation on device rotations
                return;
            }

            if (mCapabilities.getFilesFileDrop().isTrue()) {
                ((FileActivity) getActivity()).getFileOperationsHelper().
                        setHideFileListingPermissionsToShare(
                                mPublicShare,
                                isChecked
                        );
            } else {
                // not supported in ownCloud
                Snackbar.make(getView(), R.string.files_drop_not_supported, Snackbar.LENGTH_LONG)
                    .setAction(R.string.learn_more, v ->
                        DisplayUtils.startLinkIntent(requireActivity(), R.string.url_server_install))
                        .show();
            }

            // undo the toggle to grant the view will be correct if the dialog is cancelled
            switchView.setOnCheckedChangeListener(null);
            switchView.toggle();
            switchView.setOnCheckedChangeListener(mOnHideFileListingPermissionInteractionListener);
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
            mListener = (ShareFragmentListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement OnShareFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    /**
     * Get known server capabilities from DB
     * <p/>
     * Depends on the parent Activity provides a {@link com.owncloud.android.datamodel.FileDataStorageManager}
     * instance ready to use. If not ready, does nothing.
     */
    public void refreshCapabilitiesFromDB() {
        if (((FileActivity) mListener).getStorageManager() != null) {
            mCapabilities = ((FileActivity) mListener).getStorageManager().getCapability(mAccount.name);
        }
    }

    /**
     * Get users and groups from the DB to fill in the "share with" list.
     * <p/>
     * Depends on the parent Activity provides a {@link com.owncloud.android.datamodel.FileDataStorageManager}
     * instance ready to use. If not ready, does nothing.
     */
    public void refreshUsersOrGroupsListFromDB() {
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
        ShareUserListAdapter mUserGroupsAdapter = new ShareUserListAdapter(
                getActivity(),
                R.layout.share_user_item,
                mPrivateShares,
                this
        );

        // Show data
        TextView noShares = getView().findViewById(R.id.shareNoUsers);
        ListView usersList = getView().findViewById(R.id.shareUsersList);

        if (mPrivateShares.size() > 0) {
            noShares.setVisibility(View.GONE);
            usersList.setVisibility(View.VISIBLE);
            usersList.setAdapter(mUserGroupsAdapter);
        } else {
            noShares.setVisibility(View.VISIBLE);
            usersList.setVisibility(View.GONE);
        }

        // Set Scroll to initial position
        ScrollView scrollView = getView().findViewById(R.id.shareScroll);
        scrollView.scrollTo(0, 0);
    }

    @Override
    public void unshareButtonPressed(OCShare share) {
        // Unshare
        Log_OC.d(TAG, "Unsharing " + share.getSharedWithDisplayName());
        mListener.unshareWith(share);
    }

    @Override
    public void editShare(OCShare share) {
        // move to fragment to edit share
        Log_OC.d(TAG, "Editing " + share.getSharedWithDisplayName());
        mListener.showEditShare(share);
    }

    /**
     * Get public link from the DB to fill in the "Share link" section in the UI.
     * <p/>
     * Takes into account server capabilities before reading database.
     * <p/>
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
     * @return 'True' when public share is disabled in the server
     */
    private boolean isPublicShareDisabled() {
        return mCapabilities != null && mCapabilities.getFilesSharingPublicEnabled().isFalse();
    }

    /**
     * Updates in the UI the section about public share with the information in the current public share bound to
     * mFile, if any.
     */
    private void updatePublicShareSection() {
        if (mPublicShare != null && ShareType.PUBLIC_LINK.equals(mPublicShare.getShareType())) {
            /// public share bound -> expand section
            SwitchCompat shareViaLinkSwitch = getShareViaLinkSwitch();
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
            if (mFile.isFolder() && !mCapabilities.getFilesSharingPublicUpload().isFalse()) {
                getEditPermissionSection().setVisibility(View.VISIBLE);
                getHideFileListingPermissionSection().setVisibility(View.VISIBLE);
            } else {
                getEditPermissionSection().setVisibility(View.GONE);
            }

            // GetLink button
            MaterialButton getLinkButton = getGetLinkButton();
            getLinkButton.getBackground().setColorFilter(ThemeUtils.primaryAccentColor(getContext()),
                    PorterDuff.Mode.SRC_ATOP);
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
            SwitchCompat expirationDateSwitch = getExpirationDateSwitch();
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
            expirationDateSwitch.setOnCheckedChangeListener(mOnExpirationDateInteractionListener);

            /// update state of password switch and message depending on password protection
            SwitchCompat passwordSwitch = getPasswordSwitch();
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
            passwordSwitch.setOnCheckedChangeListener(mOnPasswordInteractionListener);

            /// update state of the edit permission switch
            SwitchCompat editPermissionSwitch = getEditPermissionSwitch();

            // set null listener before setChecked() to prevent infinite loop of calls
            editPermissionSwitch.setOnCheckedChangeListener(null);
            if (mPublicShare.getPermissions() > OCShare.READ_PERMISSION_FLAG) {
                if (!editPermissionSwitch.isChecked()) {
                    editPermissionSwitch.toggle();
                }
                getHideFileListingPermissionSection().setVisibility(View.VISIBLE);
            } else {
                if (editPermissionSwitch.isChecked()) {
                    editPermissionSwitch.toggle();
                }
                getHideFileListingPermissionSection().setVisibility(View.GONE);
            }

            // recover listener
            editPermissionSwitch.setOnCheckedChangeListener(mOnEditPermissionInteractionListener);

            /// update state of the hide file listing permission switch
            SwitchCompat hideFileListingPermissionSwitch = getHideFileListingPermissionSwitch();

            // set null listener before setChecked() to prevent infinite loop of calls
            hideFileListingPermissionSwitch.setOnCheckedChangeListener(null);

            boolean readOnly = (mPublicShare.getPermissions() & OCShare.READ_PERMISSION_FLAG) != 0;
            hideFileListingPermissionSwitch.setChecked(!readOnly);

            // recover listener
            hideFileListingPermissionSwitch.setOnCheckedChangeListener(
                    mOnHideFileListingPermissionInteractionListener
            );

        } else {
            /// no public share -> collapse section
            SwitchCompat shareViaLinkSwitch = getShareViaLinkSwitch();
            if (shareViaLinkSwitch.isChecked()) {
                shareViaLinkSwitch.setOnCheckedChangeListener(null);
                getShareViaLinkSwitch().setChecked(false);
                shareViaLinkSwitch.setOnCheckedChangeListener(
                        mOnShareViaLinkSwitchCheckedChangeListener
                );
            }
            getExpirationDateSection().setVisibility(View.GONE);
            getPasswordSection().setVisibility(View.GONE);
            getEditPermissionSection().setVisibility(View.GONE);
            getHideFileListingPermissionSection().setVisibility(View.GONE);
            getGetLinkButton().setVisibility(View.GONE);
        }
    }


    // BEWARE: following methods will fail with NullPointerException if called before onCreateView() finishes

    private SwitchCompat getShareViaLinkSwitch() {
        return (SwitchCompat) getView().findViewById(R.id.shareViaLinkSectionSwitch);
    }

    private View getExpirationDateSection() {
        return getView().findViewById(R.id.shareViaLinkExpirationSection);
    }

    private SwitchCompat getExpirationDateSwitch() {
        return (SwitchCompat) getView().findViewById(R.id.shareViaLinkExpirationSwitch);
    }

    private TextView getExpirationDateValue() {
        return (TextView) getView().findViewById(R.id.shareViaLinkExpirationValue);
    }

    private View getPasswordSection() {
        return getView().findViewById(R.id.shareViaLinkPasswordSection);
    }

    private SwitchCompat getPasswordSwitch() {
        return (SwitchCompat) getView().findViewById(R.id.shareViaLinkPasswordSwitch);
    }

    private TextView getPasswordValue() {
        return (TextView) getView().findViewById(R.id.shareViaLinkPasswordValue);
    }

    private View getEditPermissionSection() {
        return getView().findViewById(R.id.shareViaLinkEditPermissionSection);
    }

    private SwitchCompat getEditPermissionSwitch() {
        return (SwitchCompat) getView().findViewById(R.id.shareViaLinkEditPermissionSwitch);
    }

    private SwitchCompat getHideFileListingPermissionSwitch() {
        return (SwitchCompat) getView().findViewById(R.id.shareViaLinkFileListingPermissionSwitch);
    }

    private View getHideFileListingPermissionSection() {
        return getView().findViewById(R.id.shareViaLinkHideFileListingPermissionSection);
    }

    private MaterialButton getGetLinkButton() {
        return (MaterialButton) getView().findViewById(R.id.shareViaLinkGetLinkButton);
    }

    /**
     * Hides all the UI elements related to public share
     */
    private void hidePublicShare() {
        getShareViaLinkSwitch().setVisibility(View.GONE);
        getExpirationDateSection().setVisibility(View.GONE);
        getPasswordSection().setVisibility(View.GONE);
        getEditPermissionSection().setVisibility(View.GONE);
        getGetLinkButton().setVisibility(View.GONE);
        getHideFileListingPermissionSection().setVisibility(View.GONE);
    }

    /**
     * Starts a dialog that requests a password to the user to protect a share link.
     *
     * @param   createShare     When 'true', the request for password will be followed by the creation of a new
     *                          public link; when 'false', a public share is assumed to exist, and the password
     *                          is bound to it.
     */
    public void requestPasswordForShareViaLink(boolean createShare) {
        SharePasswordDialogFragment dialog = SharePasswordDialogFragment.newInstance(mFile, createShare);
        dialog.show(getFragmentManager(), SharePasswordDialogFragment.PASSWORD_FRAGMENT);
    }

    /**
     * Hide share features sections that are not enabled
     *
     * @param view share file view
     */
    private void hideNotEnabledShareSections(View view) {
        LinearLayout shareWithUsersSection = view.findViewById(R.id.shareWithUsersSection);
        LinearLayout shareViaLinkSection = view.findViewById(R.id.shareViaLinkSection);

        boolean shareViaLinkAllowed = getActivity().getResources().getBoolean(R.bool.share_via_link_feature);
        boolean shareWithUsersAllowed = getActivity().getResources().getBoolean(R.bool.share_with_users_feature);

        // Hide share via link section if it is not enabled
        if (!shareViaLinkAllowed) {
            shareViaLinkSection.setVisibility(View.GONE);
        }

        // Hide share with users section if it is not enabled
        if (!shareWithUsersAllowed) {
            shareWithUsersSection.setVisibility(View.GONE);
        }
    }
}
