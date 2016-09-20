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
import android.support.v7.widget.SwitchCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;

import com.owncloud.android.R;
import com.owncloud.android.datamodel.FileDataStorageManager;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.lib.resources.shares.OCShare;
import com.owncloud.android.lib.resources.shares.SharePermissionsBuilder;
import com.owncloud.android.lib.resources.shares.ShareType;
import com.owncloud.android.ui.activity.FileActivity;
import com.owncloud.android.utils.AnalyticsUtils;

public class EditShareFragment extends Fragment {

    private static final String TAG = EditShareFragment.class.getSimpleName();

    /** The fragment initialization parameters */
    private static final String ARG_SHARE = "SHARE";
    private static final String ARG_FILE = "FILE";
    private static final String ARG_ACCOUNT = "ACCOUNT";

    private static final String SCREEN_NAME = "Share with Sharee";

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
     * {@inheritDoc}
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log_OC.d(TAG, "onCreate");
        if (getArguments() != null) {
            mShare = getArguments().getParcelable(ARG_SHARE);
            mFile = getArguments().getParcelable(ARG_FILE);
            /* OC account holding the shared file, received as a parameter in construction time */
            //Account mAccount = getArguments().getParcelable(ARG_ACCOUNT);
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

        ((TextView)view.findViewById(R.id.editShareTitle)).setText(
                getResources().getString(R.string.share_with_edit_title, mShare.getSharedWithDisplayName()));

        // Setup layout
        refreshUiFromState(view);

        return view;
    }


    /**
     * Updates the UI with the current permissions in the edited {@OCShare}
     *
     * @param editShareView     Root view in the fragment.
     */
    private void refreshUiFromState(View editShareView) {
        if (editShareView != null) {
            setPermissionsListening(editShareView, false);

            int sharePermissions = mShare.getPermissions();
            boolean isFederated = ShareType.FEDERATED.equals(mShare.getShareType());
            CompoundButton compound;

            compound = (CompoundButton) editShareView.findViewById(R.id.canShareSwitch);
            if(isFederated) {
                compound.setVisibility(View.INVISIBLE);
            }
            compound.setChecked((sharePermissions & OCShare.SHARE_PERMISSION_FLAG) > 0);

            compound = (CompoundButton) editShareView.findViewById(R.id.canEditSwitch);
            int anyUpdatePermission =
                    OCShare.CREATE_PERMISSION_FLAG |
                            OCShare.UPDATE_PERMISSION_FLAG |
                            OCShare.DELETE_PERMISSION_FLAG;
            boolean canEdit = (sharePermissions & anyUpdatePermission) > 0;
            compound.setChecked(canEdit);

            boolean areEditOptionsAvailable = !isFederated || isNotReshareableFederatedSupported;

            if (mFile.isFolder() && areEditOptionsAvailable) {
                /// TODO change areEditOptionsAvailable in order to delete !isFederated
                // from checking when iOS is ready
                compound = (CompoundButton) editShareView.findViewById(R.id.canEditCreateCheckBox);
                compound.setChecked((sharePermissions & OCShare.CREATE_PERMISSION_FLAG) > 0);
                compound.setVisibility((canEdit && areEditOptionsAvailable) ? View.VISIBLE : View.GONE);

                compound = (CompoundButton) editShareView.findViewById(R.id.canEditChangeCheckBox);
                compound.setChecked((sharePermissions & OCShare.UPDATE_PERMISSION_FLAG) > 0);
                compound.setVisibility((canEdit && areEditOptionsAvailable) ? View.VISIBLE : View.GONE);

                compound = (CompoundButton) editShareView.findViewById(R.id.canEditDeleteCheckBox);
                compound.setChecked((sharePermissions & OCShare.DELETE_PERMISSION_FLAG) > 0);
                compound.setVisibility((canEdit && areEditOptionsAvailable) ? View.VISIBLE : View.GONE);
            }

            setPermissionsListening(editShareView, true);

        }
    }


    /**
     * Binds or unbinds listener for user actions to enable or disable a permission on the edited share
     * to the views receiving the user events.
     *
     * @param editShareView     Root view in the fragment.
     * @param enable            When 'true', listener is bound to view; when 'false', it is unbound.
     */
    private void setPermissionsListening(View editShareView, boolean enable) {
        if (enable && mOnPrivilegeChangeListener == null) {
            mOnPrivilegeChangeListener = new OnPrivilegeChangeListener();
        }
        CompoundButton.OnCheckedChangeListener changeListener = enable ? mOnPrivilegeChangeListener : null;
        CompoundButton compound;

        compound = (CompoundButton) editShareView.findViewById(R.id.canShareSwitch);
        compound.setOnCheckedChangeListener(changeListener);

        compound = (CompoundButton) editShareView.findViewById(R.id.canEditSwitch);
        compound.setOnCheckedChangeListener(changeListener);

        if (mFile.isFolder()) {
            compound = (CompoundButton) editShareView.findViewById(R.id.canEditCreateCheckBox);
            compound.setOnCheckedChangeListener(changeListener);

            compound = (CompoundButton) editShareView.findViewById(R.id.canEditChangeCheckBox);
            compound.setOnCheckedChangeListener(changeListener);

            compound = (CompoundButton) editShareView.findViewById(R.id.canEditDeleteCheckBox);
            compound.setOnCheckedChangeListener(changeListener);
        }

    }

    /**
     * Listener for user actions that enable or disable a privilege
     */
    private class OnPrivilegeChangeListener
            implements CompoundButton.OnCheckedChangeListener {

        /**
         * Called by every {@link SwitchCompat} and {@link CheckBox} in the fragment to update
         * the state of its associated permission.
         *
         * @param compound  {@link CompoundButton} toggled by the user
         * @param isChecked     New switch state.
         */
        @Override
        public void onCheckedChanged(CompoundButton compound, boolean isChecked) {
            if (!isResumed()) {
                // very important, setCheched(...) is called automatically during
                // Fragment recreation on device rotations
                return;
            }
            /// else, getView() cannot be NULL

            CompoundButton subordinate;
            switch(compound.getId()) {
                case R.id.canShareSwitch:
                    Log_OC.v(TAG, "canShareCheckBox toggled to " + isChecked);
                    updatePermissionsToShare();
                    break;

                case R.id.canEditSwitch:
                    Log_OC.v(TAG, "canEditCheckBox toggled to " + isChecked);
                    /// sync subordinate CheckBoxes
                    boolean isFederated = ShareType.FEDERATED.equals(mShare.getShareType());
                    if (mFile.isFolder()) {
                        if (isChecked) {
                            if (!isFederated) {
                                /// not federated shares -> enable all the subpermisions
                                for (int i = 0; i < sSubordinateCheckBoxIds.length; i++) {
                                    //noinspection ConstantConditions, prevented in the method beginning
                                    subordinate = (CompoundButton) getView().findViewById(sSubordinateCheckBoxIds[i]);
                                    subordinate.setVisibility(View.VISIBLE);
                                    if (!subordinate.isChecked() &&
                                        !mFile.isSharedWithMe()) {          // see (1)
                                        toggleDisablingListener(subordinate);
                                    }
                                }
                            } else {
                                /// federated share -> enable delete subpermission, as server side; TODO why?
                                //noinspection ConstantConditions, prevented in the method beginning
                                subordinate = (CompoundButton) getView().findViewById(R.id.canEditDeleteCheckBox);
                                if (!subordinate.isChecked()) {
                                    toggleDisablingListener(subordinate);
                                }

                            }
                        } else {
                            for (int i = 0; i < sSubordinateCheckBoxIds.length; i++) {
                                //noinspection ConstantConditions, prevented in the method beginning
                                subordinate = (CompoundButton) getView().findViewById(sSubordinateCheckBoxIds[i]);
                                subordinate.setVisibility(View.GONE);
                                if (subordinate.isChecked()) {
                                    toggleDisablingListener(subordinate);
                                }
                            }
                        }
                    }

                    if(!(mFile.isFolder() && isChecked && mFile.isSharedWithMe())       // see (1)
                        || isFederated ) {
                        updatePermissionsToShare();
                    }

                    // updatePermissionsToShare()   // see (1)
                    // (1) These modifications result in an exceptional UI behaviour for the case
                    // where the switch 'can edit' is enabled for a *reshared folder*; if the same
                    // behaviour was applied than for owned folder, and the user did not have full
                    // permissions to update the folder, an error would be reported by the server
                    // and the children checkboxes would be automatically hidden again
                    break;

                case R.id.canEditCreateCheckBox:
                    Log_OC.v(TAG, "canEditCreateCheckBox toggled to " + isChecked);
                    syncCanEditSwitch(compound, isChecked);
                    updatePermissionsToShare();
                    break;

                case R.id.canEditChangeCheckBox:
                    Log_OC.v(TAG, "canEditChangeCheckBox toggled to " + isChecked);
                    syncCanEditSwitch(compound, isChecked);
                    updatePermissionsToShare();
                    break;

                case R.id.canEditDeleteCheckBox:
                    Log_OC.v(TAG, "canEditDeleteCheckBox toggled to " + isChecked);
                    syncCanEditSwitch(compound, isChecked);
                    updatePermissionsToShare();
                    break;
            }

        }

        /**
         * Sync value of "can edit" {@link SwitchCompat} according to a change in one of its subordinate checkboxes.
         *
         * If all the subordinates are disabled, "can edit" has to be disabled.
         *
         * If any subordinate is enabled, "can edit" has to be enabled.
         *
         * @param subordinateCheckBoxView   Subordinate {@link CheckBox} that was changed.
         * @param isChecked                 'true' iif subordinateCheckBoxView was checked.
         */
        private void syncCanEditSwitch(View subordinateCheckBoxView, boolean isChecked) {
            CompoundButton canEditCompound = (CompoundButton) getView().findViewById(R.id.canEditSwitch);
            if (isChecked) {
                if (!canEditCompound.isChecked()) {
                    toggleDisablingListener(canEditCompound);
                }
            } else {
                boolean allDisabled = true;
                for (int i=0; allDisabled && i<sSubordinateCheckBoxIds.length; i++) {
                    allDisabled &=
                            sSubordinateCheckBoxIds[i] == subordinateCheckBoxView.getId() ||
                                    !((CheckBox) getView().findViewById(sSubordinateCheckBoxIds[i])).isChecked()
                    ;
                }
                if (canEditCompound.isChecked() && allDisabled) {
                    toggleDisablingListener(canEditCompound);
                    for (int i=0; i<sSubordinateCheckBoxIds.length; i++) {
                        getView().findViewById(sSubordinateCheckBoxIds[i]).setVisibility(View.GONE);
                    }
                }
            }
        }


        /**
         * Toggle value of received {@link CompoundButton} granting that its change listener is not called.
         *
         * @param compound      {@link CompoundButton} (switch or checkBox) to toggle without reporting to
         *                      the change listener
         */
        private void toggleDisablingListener(CompoundButton compound) {
            compound.setOnCheckedChangeListener(null);
            compound.toggle();
            compound.setOnCheckedChangeListener(this);
        }
    }


    /**
     * Updates the UI after the result of an update operation on the edited {@link OCShare} permissions.
     *
     * @param result        Result of an update on the edited {@link OCShare} permissions.
     */
    public void onUpdateSharePermissionsFinished(RemoteOperationResult result) {
        if (result.isSuccess()) {
            refreshUiFromDB(getView());
        } else {
            refreshUiFromState(getView());
        }
    }


    @Override
    public void onResume() {
        super.onResume();
        if (getActivity() != null) {
            AnalyticsUtils.setCurrentScreenName(getActivity(), SCREEN_NAME, TAG);
        }
    }

    /**
     * Get {@link OCShare} instance from DB and updates the UI.
     *
     * Depends on the parent Activity provides a {@link com.owncloud.android.datamodel.FileDataStorageManager}
     * instance ready to use. If not ready, does nothing.
     */
    public void refreshUiFromDB() {
        if (getView() != null) {
            refreshUiFromDB(getView());
        }
    }


    /**
     * Get {@link OCShare} instance from DB and updates the UI.
     *
     * Depends on the parent Activity provides a {@link com.owncloud.android.datamodel.FileDataStorageManager}
     * instance ready to use. If not ready, does nothing.
     *
     * @param editShareView     Root view in the fragment.
     */
    private void refreshUiFromDB(View editShareView) {
        FileDataStorageManager storageManager = ((FileActivity) getActivity()).getStorageManager();
        if (storageManager != null) {
            // Get edited share
            mShare = storageManager.getShareById(mShare.getId());

            // Updates UI with new state
            refreshUiFromState(editShareView);
        }
    }


    /**
     * Updates the permissions of the {@link OCShare} according to the values set in the UI
     */
    private void updatePermissionsToShare() {
        SharePermissionsBuilder spb = new SharePermissionsBuilder();
        spb.setSharePermission(getCanShareSwitch().isChecked());
        if (mFile.isFolder()) {
            spb.setUpdatePermission(getCanEditChangeCheckBox().isChecked())
                    .setCreatePermission(getCanEditCreateCheckBox().isChecked())
                    .setDeletePermission(getCanEditDeleteCheckBox().isChecked());
        } else {
            spb.setUpdatePermission(getCanEditSwitch().isChecked());
        }
        int permissions = spb.build();

        ((FileActivity) getActivity()).getFileOperationsHelper().
                setPermissionsToShare(
                        mShare,
                        permissions
                )
        ;
    }

    /**
     * Shortcut to access {@link SwitchCompat} R.id.canShareSwitch
     *
     * @return  {@link SwitchCompat} R.id.canShareCheckBox or null if called before
     *          {@link #onCreateView(LayoutInflater, ViewGroup, Bundle)} finished.
     */
    private SwitchCompat getCanShareSwitch() {
        return (SwitchCompat) getView().findViewById(R.id.canShareSwitch);
    }

    /**
     * Shortcut to access {@link SwitchCompat} R.id.canEditSwitch
     *
     * @return  {@link SwitchCompat} R.id.canEditSwitch or null if called before
     *          {@link #onCreateView(LayoutInflater, ViewGroup, Bundle)} finished.
     */
    private SwitchCompat getCanEditSwitch() {
        return (SwitchCompat) getView().findViewById(R.id.canEditSwitch);
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
