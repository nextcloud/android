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
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;

import com.owncloud.android.R;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.lib.resources.shares.OCShare;
import com.owncloud.android.lib.resources.shares.SharePermissionsBuilder;
import com.owncloud.android.ui.activity.FileActivity;

public class EditShareFragment extends Fragment {

    private static final String TAG = EditShareFragment.class.getSimpleName();

    /** The fragment initialization parameters */
    private static final String ARG_SHARE = "SHARE";
    private static final String ARG_FILE = "FILE";
    private static final String ARG_ACCOUNT = "ACCOUNT";

    /** Ids of CheckBoxes depending on R.id.canEdit CheckBox */
    private static final int sSubordinateCheckBoxIds[] = {
            R.id.canEditCreateCheckBox,
            R.id.canEditChangeCheckBox,
            R.id.canEditDeleteCheckBox
    };

    /** Share to show & edit, received as a parameter in construction time */
    private OCShare mShare;

    /** File bound to mShare, received as a parameter in construction time */
    private OCFile mFile;

    /** OC account holding the shared file, received as a parameter in construction time */
    private Account mAccount;

    /** Listener for changes on privilege checkboxes */
    private CompoundButton.OnCheckedChangeListener mOnPrivilegeChangeListener;


    /**
     * Public factory method to create new EditShareFragment instances.
     *
     * @param shareToEdit   An {@link OCShare} to show and edit in the fragment
     * @param sharedFile    The {@link OCFile} bound to 'shareToEdit'
     * @param account       The ownCloud account holding 'sharedFile'
     * @return A new instance of fragment EditShareFragment.
     */
    public static EditShareFragment newInstance(OCShare shareToEdit, OCFile sharedFile, Account account) {
        EditShareFragment fragment = new EditShareFragment();
        Bundle args = new Bundle();
        args.putParcelable(ARG_SHARE, shareToEdit);
        args.putParcelable(ARG_FILE, sharedFile);
        args.putParcelable(ARG_ACCOUNT, account);
        fragment.setArguments(args);
        return fragment;
    }

    /**
     * Required empty public constructor
     */
    public EditShareFragment() {
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log_OC.d(TAG, "onCreate");
        if (getArguments() != null) {
            mShare = getArguments().getParcelable(ARG_SHARE);
            mFile = getArguments().getParcelable(ARG_FILE);
            mAccount = getArguments().getParcelable(ARG_ACCOUNT);
        }
    }



    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        Log_OC.d(TAG, "onActivityCreated");
        getActivity().setTitle(mShare.getSharedWithDisplayName());
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Log_OC.d(TAG, "onCreateView");

        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.edit_share_layout, container, false);

        // Setup layout
        initPrivileges(view);
        initUnshareButton(view);
        initDoneButton(view);

        return view;
    }

    /**
     * Binds listener for user actions to enable or disable a privilege on the edited share
     * to the views receiving the user events.
     *
     * @param editShareView     Root view in the fragment.
     */
    private void initPrivileges(View editShareView) {
        mOnPrivilegeChangeListener = new OnPrivilegeChangeListener();
        int sharePermissions = mShare.getPermissions();
        CheckBox checkBox;

        checkBox = (CheckBox) editShareView.findViewById(R.id.canShareCheckBox);
        checkBox.setChecked((sharePermissions & OCShare.SHARE_PERMISSION_FLAG) > 0);
        checkBox.setOnCheckedChangeListener(mOnPrivilegeChangeListener);

        checkBox = (CheckBox) editShareView.findViewById(R.id.canEditCheckBox);
        int anyUpdatePermission =
                OCShare.CREATE_PERMISSION_FLAG |
                        OCShare.UPDATE_PERMISSION_FLAG |
                        OCShare.DELETE_PERMISSION_FLAG
                ;
        boolean canEdit = (sharePermissions & anyUpdatePermission) > 0;
        checkBox.setChecked(canEdit);
        checkBox.setOnCheckedChangeListener(mOnPrivilegeChangeListener);

        if (mFile.isFolder()) {
            checkBox = (CheckBox) editShareView.findViewById(R.id.canEditCreateCheckBox);
            if (canEdit) {
                checkBox.setVisibility(View.VISIBLE);
                checkBox.setChecked((sharePermissions & OCShare.CREATE_PERMISSION_FLAG) > 0);
            }
            checkBox.setOnCheckedChangeListener(mOnPrivilegeChangeListener);

            checkBox = (CheckBox) editShareView.findViewById(R.id.canEditChangeCheckBox);
            if (canEdit) {
                checkBox.setVisibility(View.VISIBLE);
                checkBox.setChecked((sharePermissions & OCShare.UPDATE_PERMISSION_FLAG) > 0);
            }
            checkBox.setOnCheckedChangeListener(mOnPrivilegeChangeListener);

            checkBox = (CheckBox) editShareView.findViewById(R.id.canEditDeleteCheckBox);
            if (canEdit) {
                checkBox.setVisibility(View.VISIBLE);
                checkBox.setChecked((sharePermissions & OCShare.DELETE_PERMISSION_FLAG) > 0);
            }
            checkBox.setOnCheckedChangeListener(mOnPrivilegeChangeListener);

        }   // else, trust in visibility GONE in R.layout.edit_share_layout

    }


    /**
     * Listener for user actions that enable or disable a privilege
     */
    private class OnPrivilegeChangeListener
            implements CompoundButton.OnCheckedChangeListener {

        /**
         * Called by R.id.shareViaLinkSectionSwitch to create or delete a public link.
         *
         * @param checkBoxView  {@link CheckBox} toggled by the user
         * @param isChecked     New switch state.
         */
        @Override
        public void onCheckedChanged(CompoundButton checkBoxView, boolean isChecked) {
            if (!isResumed()) {
                // very important, setCheched(...) is called automatically during
                // Fragment recreation on device rotations
                return;
            }
            /// else, getView() cannot be NULL

            CheckBox subordinate;
            switch(checkBoxView.getId()) {
                case R.id.canShareCheckBox:
                    Log_OC.v(TAG, "canShareCheckBox toggled to " + isChecked);
                    /// TODO?
                    // option 1: direct update approach
                    /*
                    ((FileActivity) getActivity()).getFileOperationsHelper().
                        setPrivilegeToShare(mFile, mShare, privilege, isChecked);
                        */
                    // option 2: nothing?
                    break;

                case R.id.canEditCheckBox:
                    Log_OC.v(TAG, "canEditCheckBox toggled to " + isChecked);
                    /// sync subordinate CheckBoxes
                    if (mFile.isFolder()) {
                        if (isChecked) {
                            for (int i = 0; i < sSubordinateCheckBoxIds.length; i++) {
                                //noinspection ConstantConditions, prevented in the method beginning
                                subordinate = (CheckBox) getView().findViewById(sSubordinateCheckBoxIds[i]);
                                subordinate.setVisibility(View.VISIBLE);
                                if (!subordinate.isChecked()) {
                                    toggleDisablingListener(subordinate);
                                }
                            }
                        } else {
                            for (int i = 0; i < sSubordinateCheckBoxIds.length; i++) {
                                //noinspection ConstantConditions, prevented in the method beginning
                                subordinate = (CheckBox) getView().findViewById(sSubordinateCheckBoxIds[i]);
                                subordinate.setVisibility(View.GONE);
                                if (subordinate.isChecked()) {
                                    toggleDisablingListener(subordinate);
                                }
                            }
                        }
                    }
                    /// TODO - anything else?; only if modification-on-change approach is taken
                    break;

                case R.id.canEditCreateCheckBox:
                    Log_OC.v(TAG, "canEditCreateCheckBox toggled to " + isChecked);
                    syncCanEditCheckBox(checkBoxView, isChecked);
                    /// TODO - anything else?; only if modification-on-change approach is taken
                    break;

                case R.id.canEditChangeCheckBox:
                    Log_OC.v(TAG, "canEditChangeCheckBox toggled to " + isChecked);
                    syncCanEditCheckBox(checkBoxView, isChecked);
                    /// TODO - anything else?; only if modification-on-change approach is taken
                    break;

                case R.id.canEditDeleteCheckBox:
                    Log_OC.v(TAG, "canEditDeleteCheckBox toggled to " + isChecked);
                    syncCanEditCheckBox(checkBoxView, isChecked);
                    /// TODO - anything else?; only if modification-on-change approach is taken
                    break;
            }

            // undo the toggle to grant the view will be correct if any intermediate dialog is cancelled or
            // the create/delete operation fails
            // ONLY if direct update approach is followed
            /*
            checkBoxView.setOnCheckedChangeListener(null);
            checkBoxView.toggle();
            checkBoxView.setOnCheckedChangeListener(mOnShareViaLinkSwitchCheckedChangeListener);
            */
        }

        /**
         * Sync value of "can edit" CheckBox according to a change in one of its subordinate checkboxes.
         *
         * If all the subordinates are disabled, "can edit" has to be disabled.
         *
         * If any subordinate is enabled, "can edit" has to be enabled.
         *
         * @param subordinateCheckBoxView   Subordinate CheckBox that was changed.
         * @param isChecked                 'true' iif subordinateCheckBoxView was checked.
         */
        private void syncCanEditCheckBox(View subordinateCheckBoxView, boolean isChecked) {
            CheckBox canEditCheckBox = (CheckBox) getView().findViewById(R.id.canEditCheckBox);
            if (isChecked) {
                if (!canEditCheckBox.isChecked()) {
                    toggleDisablingListener(canEditCheckBox);
                }
            } else {
                boolean allDisabled = true;
                for (int i=0; allDisabled && i<sSubordinateCheckBoxIds.length; i++) {
                    allDisabled &=
                            sSubordinateCheckBoxIds[i] == subordinateCheckBoxView.getId() ||
                                    !((CheckBox) getView().findViewById(sSubordinateCheckBoxIds[i])).isChecked()
                    ;
                }
                if (canEditCheckBox.isChecked() && allDisabled) {
                    toggleDisablingListener(canEditCheckBox);
                    for (int i=0; i<sSubordinateCheckBoxIds.length; i++) {
                        getView().findViewById(sSubordinateCheckBoxIds[i]).setVisibility(View.GONE);
                    }
                }
            }
        }


        /**
         * Toggle value of received CheckBox granting that its change listener is not called.
         *
         * @param checkBoxView      CheckBox to toggle without reporting to the change listener
         */
        private void toggleDisablingListener(CheckBox checkBoxView) {
            checkBoxView.setOnCheckedChangeListener(null);
            checkBoxView.toggle();
            checkBoxView.setOnCheckedChangeListener(mOnPrivilegeChangeListener);
        }
    }


    /**
     * Binds listener for user interactions on the 'unshare' button with the button itself.
     *
     * @param editShareView     Root view in the fragment.
     */
    private void initUnshareButton(View editShareView) {
        TextView unshareButton  = (TextView) editShareView.findViewById(R.id.unshareButton);
        unshareButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((FileActivity) getActivity()).getFileOperationsHelper().
                        unshareFileWithUserOrGroup(
                                mFile,
                                mShare.getShareType(),
                                mShare.getShareWith()
                        )
                ;
            }
        });
    }


    /**
     * Binds listener for user interactions on the 'done' button with the button itself.
     *
     * @param editShareView     Root view in the fragment.
     */
    private void initDoneButton(View editShareView) {
        TextView doneButton  = (TextView) editShareView.findViewById(R.id.doneButton);
        doneButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SharePermissionsBuilder spb = new SharePermissionsBuilder();
                spb.setSharePermission(getCanShareCheckBox().isChecked());
                if (mFile.isFolder()) {
                    spb.setUpdatePermission(getCanEditChangeCheckBox().isChecked())
                        .setCreatePermission(getCanEditCreateCheckBox().isChecked())
                        .setDeletePermission(getCanEditDeleteCheckBox().isChecked());
                } else {
                    spb.setUpdatePermission(getCanEditCheckBox().isChecked());
                }
                int permissions = spb.build();

                ((FileActivity) getActivity()).getFileOperationsHelper().
                        setPermissionsToShare(
                                mShare,
                                permissions
                        );
            }
        });
    }

    /**
     * Shortcut to access {@link CheckBox} R.id.canShareCheckBox
     *
     * @return  {@link CheckBox} R.id.canShareCheckBox or null if called before
     *          {@link #onCreateView(LayoutInflater, ViewGroup, Bundle)} finished.
     */
    private CheckBox getCanShareCheckBox() {
        return (CheckBox) getView().findViewById(R.id.canShareCheckBox);
    }

    /**
     * Shortcut to access {@link CheckBox} R.id.canEditCheckBox
     *
     * @return  {@link CheckBox} R.id.canEditCheckBox or null if called before
     *          {@link #onCreateView(LayoutInflater, ViewGroup, Bundle)} finished.
     */
    private CheckBox getCanEditCheckBox() {
        return (CheckBox) getView().findViewById(R.id.canEditCheckBox);
    }

    /**
     * Shortcut to access {@link CheckBox} R.id.canEditCreateCheckBox
     *
     * @return  {@link CheckBox} R.id.canEditCreateCheckBox or null if called before
     *          {@link #onCreateView(LayoutInflater, ViewGroup, Bundle)} finished.
     */
    private CheckBox getCanEditCreateCheckBox() {
        return (CheckBox) getView().findViewById(R.id.canEditCreateCheckBox);
    }

    /**
     * Shortcut to access {@link CheckBox} R.id.canEditChangeCheckBox
     *
     * @return  {@link CheckBox} R.id.canEditChangeCheckBox or null if called before
     *          {@link #onCreateView(LayoutInflater, ViewGroup, Bundle)} finished.
     */
    private CheckBox getCanEditChangeCheckBox() {
        return (CheckBox) getView().findViewById(R.id.canEditChangeCheckBox);
    }

    /**
     * Shortcut to access {@link CheckBox} R.id.canEditDeleteCheckBox
     *
     * @return  {@link CheckBox} R.id.canEditDeleteCheckBox or null if called before
     *          {@link #onCreateView(LayoutInflater, ViewGroup, Bundle)} finished.
     */
    private CheckBox getCanEditDeleteCheckBox() {
        return (CheckBox) getView().findViewById(R.id.canEditDeleteCheckBox);
    }


}
