/*
 * Nextcloud Android client application
 *
 * @author Andy Scherzinger
 * Copyright (C) 2016 Andy Scherzinger
 * Copyright (C) 2016 Nextcloud
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
package com.owncloud.android.ui.dialog;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.TextUtils;
import android.text.style.StyleSpan;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.nextcloud.client.di.Injectable;
import com.owncloud.android.R;
import com.owncloud.android.databinding.SyncedFoldersSettingsLayoutBinding;
import com.owncloud.android.datamodel.MediaFolderType;
import com.owncloud.android.datamodel.SyncedFolderDisplayItem;
import com.owncloud.android.files.services.NameCollisionPolicy;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.ui.activity.FolderPickerActivity;
import com.owncloud.android.ui.activity.UploadFilesActivity;
import com.owncloud.android.ui.dialog.parcel.SyncedFolderParcelable;
import com.owncloud.android.utils.DisplayUtils;
import com.owncloud.android.utils.FileStorageUtils;
import com.owncloud.android.utils.theme.ViewThemeUtils;

import java.io.File;

import javax.inject.Inject;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.AppCompatCheckBox;
import androidx.appcompat.widget.SwitchCompat;
import androidx.fragment.app.DialogFragment;

import static com.owncloud.android.datamodel.SyncedFolderDisplayItem.UNPERSISTED_ID;
import static com.owncloud.android.ui.activity.UploadFilesActivity.REQUEST_CODE_KEY;

/**
 * Dialog to show the preferences/configuration of a synced folder allowing the user to change the different
 * parameters.
 */
public class SyncedFolderPreferencesDialogFragment extends DialogFragment implements Injectable {

    public static final String SYNCED_FOLDER_PARCELABLE = "SyncedFolderParcelable";
    public static final int REQUEST_CODE__SELECT_REMOTE_FOLDER = 0;
    public static final int REQUEST_CODE__SELECT_LOCAL_FOLDER = 1;

    private final static String TAG = SyncedFolderPreferencesDialogFragment.class.getSimpleName();
    private static final String BEHAVIOUR_DIALOG_STATE = "BEHAVIOUR_DIALOG_STATE";
    private static final String NAME_COLLISION_POLICY_DIALOG_STATE = "NAME_COLLISION_POLICY_DIALOG_STATE";
    private final static float alphaEnabled = 1.0f;
    private final static float alphaDisabled = 0.7f;

    @Inject ViewThemeUtils viewThemeUtils;

    private CharSequence[] mUploadBehaviorItemStrings;
    private CharSequence[] mNameCollisionPolicyItemStrings;
    private SwitchCompat mEnabledSwitch;
    private AppCompatCheckBox mUploadOnWifiCheckbox;
    private AppCompatCheckBox mUploadOnChargingCheckbox;
    private AppCompatCheckBox mUploadExistingCheckbox;
    private AppCompatCheckBox mUploadUseSubfoldersCheckbox;
    private TextView mUploadBehaviorSummary;
    private TextView mNameCollisionPolicySummary;
    private TextView mLocalFolderPath;
    private TextView mLocalFolderSummary;
    private TextView mRemoteFolderSummary;

    private SyncedFolderParcelable mSyncedFolder;
    private MaterialButton mCancel;
    private MaterialButton mSave;
    private boolean behaviourDialogShown;
    private boolean nameCollisionPolicyDialogShown;
    private AlertDialog behaviourDialog;
    private SyncedFoldersSettingsLayoutBinding binding;

    public static SyncedFolderPreferencesDialogFragment newInstance(SyncedFolderDisplayItem syncedFolder, int section) {
        if (syncedFolder == null) {
            throw new IllegalArgumentException("SyncedFolder is mandatory but NULL!");
        }

        Bundle args = new Bundle();
        args.putParcelable(SYNCED_FOLDER_PARCELABLE, new SyncedFolderParcelable(syncedFolder, section));

        SyncedFolderPreferencesDialogFragment dialogFragment = new SyncedFolderPreferencesDialogFragment();
        dialogFragment.setArguments(args);
        dialogFragment.setStyle(STYLE_NORMAL, R.style.Theme_ownCloud_Dialog);

        return dialogFragment;
    }

    @Override
    public void onAttach(@NonNull Activity activity) {
        super.onAttach(activity);
        if (!(activity instanceof OnSyncedFolderPreferenceListener)) {
            throw new IllegalArgumentException("The host activity must implement "
                                                   + OnSyncedFolderPreferenceListener.class.getCanonicalName());
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // keep the state of the fragment on configuration changes
        setRetainInstance(true);

        binding = null;

        mSyncedFolder = getArguments().getParcelable(SYNCED_FOLDER_PARCELABLE);
        mUploadBehaviorItemStrings = getResources().getTextArray(R.array.pref_behaviour_entries);
        mNameCollisionPolicyItemStrings = getResources().getTextArray(R.array.pref_name_collision_policy_entries);
    }

    /**
     * find all relevant UI elements and set their values.
     *
     * @param binding the parent binding
     */
    private void setupDialogElements(SyncedFoldersSettingsLayoutBinding binding) {
        if (mSyncedFolder.getType().getId() > MediaFolderType.CUSTOM.getId()) {
            // hide local folder chooser and delete for non-custom folders
            binding.localFolderContainer.setVisibility(View.GONE);
            binding.delete.setVisibility(View.GONE);
        } else if (mSyncedFolder.getId() <= UNPERSISTED_ID) {
            // Hide delete/enabled for unpersisted custom folders
            binding.delete.setVisibility(View.GONE);
            binding.syncEnabled.setVisibility(View.GONE);

            // auto set custom folder to enabled
            mSyncedFolder.setEnabled(true);

            // switch text to create headline
            binding.syncedFoldersSettingsTitle.setText(R.string.autoupload_create_new_custom_folder);

            // disable save button
            binding.save.setEnabled(false);
        } else {
            binding.localFolderContainer.setVisibility(View.GONE);
        }

        // find/saves UI elements
        mEnabledSwitch = binding.syncEnabled;
        viewThemeUtils.androidx.colorSwitchCompat(mEnabledSwitch);

        mLocalFolderPath = binding.syncedFoldersSettingsLocalFolderPath;

        mLocalFolderSummary = binding.localFolderSummary;
        mRemoteFolderSummary = binding.remoteFolderSummary;

        mUploadOnWifiCheckbox = binding.settingInstantUploadOnWifiCheckbox;

        mUploadOnChargingCheckbox = binding.settingInstantUploadOnChargingCheckbox;

        mUploadExistingCheckbox = binding.settingInstantUploadExistingCheckbox;

        mUploadUseSubfoldersCheckbox = binding.settingInstantUploadPathUseSubfoldersCheckbox;

        viewThemeUtils.platform.themeCheckbox(mUploadOnWifiCheckbox,
                                              mUploadOnChargingCheckbox,
                                              mUploadExistingCheckbox,
                                              mUploadUseSubfoldersCheckbox);

        mUploadBehaviorSummary = binding.settingInstantBehaviourSummary;

        mNameCollisionPolicySummary = binding.settingInstantNameCollisionPolicySummary;

        mCancel = binding.cancel;
        mSave = binding.save;

        viewThemeUtils.platform.colorTextButtons(mCancel, mSave);

        // Set values
        setEnabled(mSyncedFolder.isEnabled());

        if (!TextUtils.isEmpty(mSyncedFolder.getLocalPath())) {
            mLocalFolderPath.setText(
                DisplayUtils.createTextWithSpan(
                    String.format(
                        getString(R.string.synced_folders_preferences_folder_path),
                        mSyncedFolder.getLocalPath()),
                    mSyncedFolder.getFolderName(),
                    new StyleSpan(Typeface.BOLD)));
            mLocalFolderSummary.setText(FileStorageUtils.pathToUserFriendlyDisplay(
                mSyncedFolder.getLocalPath(),
                getActivity(),
                getResources()));
        } else {
            mLocalFolderSummary.setText(R.string.choose_local_folder);
        }

        if (!TextUtils.isEmpty(mSyncedFolder.getLocalPath())) {
            mRemoteFolderSummary.setText(mSyncedFolder.getRemotePath());
        } else {
            mRemoteFolderSummary.setText(R.string.choose_remote_folder);
        }

        mUploadOnWifiCheckbox.setChecked(mSyncedFolder.isWifiOnly());
        mUploadOnChargingCheckbox.setChecked(mSyncedFolder.isChargingOnly());

        mUploadExistingCheckbox.setChecked(mSyncedFolder.isExisting());
        mUploadUseSubfoldersCheckbox.setChecked(mSyncedFolder.isSubfolderByDate());

        mUploadBehaviorSummary.setText(mUploadBehaviorItemStrings[mSyncedFolder.getUploadActionInteger()]);

        final int nameCollisionPolicyIndex =
            getSelectionIndexForNameCollisionPolicy(mSyncedFolder.getNameCollisionPolicy());
        mNameCollisionPolicySummary.setText(mNameCollisionPolicyItemStrings[nameCollisionPolicyIndex]);
    }

    /**
     * set correct icon/flag.
     *
     * @param enabled if enabled or disabled
     */
    private void setEnabled(boolean enabled) {
        mSyncedFolder.setEnabled(enabled);
        mEnabledSwitch.setChecked(enabled);

        setupViews(binding, enabled);
    }

    /**
     * set (new) remote path on activity result of the folder picker activity. The result gets originally propagated to
     * the underlying activity since the picker is an activity and the result can't get passed to the dialog fragment
     * directly.
     *
     * @param path the remote path to be set
     */
    public void setRemoteFolderSummary(String path) {
        mSyncedFolder.setRemotePath(path);
        mRemoteFolderSummary.setText(path);
        checkAndUpdateSaveButtonState();
    }

    /**
     * set (new) local path on activity result of the folder picker activity. The result gets originally propagated to
     * the underlying activity since the picker is an activity and the result can't get passed to the dialog fragment
     * directly.
     *
     * @param path the local path to be set
     */
    public void setLocalFolderSummary(String path) {
        mSyncedFolder.setLocalPath(path);
        mLocalFolderSummary.setText(FileStorageUtils.pathToUserFriendlyDisplay(path, getActivity(), getResources()));
        mLocalFolderPath.setText(
            DisplayUtils.createTextWithSpan(
                String.format(
                    getString(R.string.synced_folders_preferences_folder_path),
                    mSyncedFolder.getLocalPath()),
                new File(mSyncedFolder.getLocalPath()).getName(),
                new StyleSpan(Typeface.BOLD)));
        checkAndUpdateSaveButtonState();
    }

    private void checkAndUpdateSaveButtonState() {
        if (mSyncedFolder.getLocalPath() != null && mSyncedFolder.getRemotePath() != null) {
            binding.save.setEnabled(true);
        } else {
            binding.save.setEnabled(false);
        }

        checkWritableFolder();
    }

    private void checkWritableFolder() {
        if (!mSyncedFolder.isEnabled()) {
            binding.settingInstantBehaviourContainer.setEnabled(false);
            binding.settingInstantBehaviourContainer.setAlpha(alphaDisabled);
            return;
        }

        if (mSyncedFolder.getLocalPath() != null && new File(mSyncedFolder.getLocalPath()).canWrite()) {
            binding.settingInstantBehaviourContainer.setEnabled(true);
            binding.settingInstantBehaviourContainer.setAlpha(alphaEnabled);
            mUploadBehaviorSummary.setText(mUploadBehaviorItemStrings[mSyncedFolder.getUploadActionInteger()]);
        } else {
            binding.settingInstantBehaviourContainer.setEnabled(false);
            binding.settingInstantBehaviourContainer.setAlpha(alphaDisabled);

            mSyncedFolder.setUploadAction(
                getResources().getTextArray(R.array.pref_behaviour_entryValues)[0].toString());

            mUploadBehaviorSummary.setText(R.string.auto_upload_file_behaviour_kept_in_folder);
        }
    }

    private void setupViews(SyncedFoldersSettingsLayoutBinding binding, boolean enable) {
        float alpha;
        if (enable) {
            alpha = alphaEnabled;
        } else {
            alpha = alphaDisabled;
        }
        binding.settingInstantUploadOnWifiContainer.setEnabled(enable);
        binding.settingInstantUploadOnWifiContainer.setAlpha(alpha);

        binding.settingInstantUploadOnChargingContainer.setEnabled(enable);
        binding.settingInstantUploadOnChargingContainer.setAlpha(alpha);

        binding.settingInstantUploadExistingContainer.setEnabled(enable);
        binding.settingInstantUploadExistingContainer.setAlpha(alpha);

        binding.settingInstantUploadPathUseSubfoldersContainer.setEnabled(enable);
        binding.settingInstantUploadPathUseSubfoldersContainer.setAlpha(alpha);

        binding.remoteFolderContainer.setEnabled(enable);
        binding.remoteFolderContainer.setAlpha(alpha);

        binding.localFolderContainer.setEnabled(enable);
        binding.localFolderContainer.setAlpha(alpha);

        binding.settingInstantNameCollisionPolicyContainer.setEnabled(enable);
        binding.settingInstantNameCollisionPolicyContainer.setAlpha(alpha);

        mUploadOnWifiCheckbox.setEnabled(enable);
        mUploadOnChargingCheckbox.setEnabled(enable);
        mUploadExistingCheckbox.setEnabled(enable);
        mUploadUseSubfoldersCheckbox.setEnabled(enable);

        checkWritableFolder();
    }

    /**
     * setup all listeners.
     *
     * @param binding the parent binding
     */
    private void setupListeners(SyncedFoldersSettingsLayoutBinding binding) {
        mSave.setOnClickListener(new OnSyncedFolderSaveClickListener());
        mCancel.setOnClickListener(new OnSyncedFolderCancelClickListener());
        binding.delete.setOnClickListener(new OnSyncedFolderDeleteClickListener());

        binding.settingInstantUploadOnWifiContainer.setOnClickListener(
            new OnClickListener() {
                @Override
                public void onClick(View v) {
                    mSyncedFolder.setWifiOnly(!mSyncedFolder.isWifiOnly());
                    mUploadOnWifiCheckbox.toggle();
                }
            });

        binding.settingInstantUploadOnChargingContainer.setOnClickListener(
            new OnClickListener() {
                @Override
                public void onClick(View v) {
                    mSyncedFolder.setChargingOnly(!mSyncedFolder.isChargingOnly());
                    mUploadOnChargingCheckbox.toggle();
                }
            });

        binding.settingInstantUploadExistingContainer.setOnClickListener(
            new OnClickListener() {
                @Override
                public void onClick(View v) {
                    mSyncedFolder.setExisting(!mSyncedFolder.isExisting());
                    mUploadExistingCheckbox.toggle();
                }
            });

        binding.settingInstantUploadPathUseSubfoldersContainer.setOnClickListener(
            new OnClickListener() {
                @Override
                public void onClick(View v) {
                    mSyncedFolder.setSubfolderByDate(!mSyncedFolder.isSubfolderByDate());
                    mUploadUseSubfoldersCheckbox.toggle();
                }
            });

        binding.remoteFolderContainer.setOnClickListener(v -> {
            Intent action = new Intent(getActivity(), FolderPickerActivity.class);
            getActivity().startActivityForResult(action, REQUEST_CODE__SELECT_REMOTE_FOLDER);
        });

        binding.localFolderContainer.setOnClickListener(v -> {
            Intent action = new Intent(getActivity(), UploadFilesActivity.class);
            action.putExtra(UploadFilesActivity.KEY_LOCAL_FOLDER_PICKER_MODE, true);
            action.putExtra(REQUEST_CODE_KEY, REQUEST_CODE__SELECT_LOCAL_FOLDER);
            getActivity().startActivityForResult(action, REQUEST_CODE__SELECT_LOCAL_FOLDER);
        });

        binding.syncEnabled.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                setEnabled(!mSyncedFolder.isEnabled());
            }
        });

        binding.settingInstantBehaviourContainer.setOnClickListener(
            new OnClickListener() {
                @Override
                public void onClick(View v) {
                    showBehaviourDialog();
                }
            });

        binding.settingInstantNameCollisionPolicyContainer.setOnClickListener(
            new OnClickListener() {
                @Override
                public void onClick(View v) {
                    showNameCollisionPolicyDialog();
                }
            });
    }

    private void showBehaviourDialog() {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(getActivity());
        builder.setTitle(R.string.prefs_instant_behaviour_dialogTitle)
            .setSingleChoiceItems(getResources().getTextArray(R.array.pref_behaviour_entries),
                                  mSyncedFolder.getUploadActionInteger(),
                                  new
                                      DialogInterface.OnClickListener() {
                                          public void onClick(DialogInterface dialog, int which) {
                                              mSyncedFolder.setUploadAction(
                                                  getResources().getTextArray(
                                                      R.array.pref_behaviour_entryValues)[which].toString());
                                              mUploadBehaviorSummary.setText(SyncedFolderPreferencesDialogFragment
                                                                                 .this.mUploadBehaviorItemStrings[which]);
                                              behaviourDialogShown = false;
                                              dialog.dismiss();
                                          }
                                      })
            .setOnCancelListener(new DialogInterface.OnCancelListener() {
                @Override
                public void onCancel(DialogInterface dialog) {
                    behaviourDialogShown = false;
                }
            });
        behaviourDialogShown = true;

        viewThemeUtils.dialog.colorMaterialAlertDialogBackground(getActivity(), builder);

        behaviourDialog = builder.create();
        behaviourDialog.show();
    }

    private void showNameCollisionPolicyDialog() {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(getActivity());

        builder.setTitle(R.string.pref_instant_name_collision_policy_dialogTitle)
            .setSingleChoiceItems(getResources().getTextArray(R.array.pref_name_collision_policy_entries),
                                  getSelectionIndexForNameCollisionPolicy(mSyncedFolder.getNameCollisionPolicy()),
                                  new OnNameCollisionDialogClickListener())
            .setOnCancelListener(dialog -> nameCollisionPolicyDialogShown = false);

        nameCollisionPolicyDialogShown = true;

        viewThemeUtils.dialog.colorMaterialAlertDialogBackground(getActivity(), builder);

        behaviourDialog = builder.create();
        behaviourDialog.show();
    }

    @Override
    @NonNull
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Log_OC.d(TAG, "onCreateView, savedInstanceState is " + savedInstanceState);

        binding = SyncedFoldersSettingsLayoutBinding.inflate(requireActivity().getLayoutInflater(), null, false);

        setupDialogElements(binding);
        setupListeners(binding);

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(binding.getRoot().getContext());
        builder.setView(binding.getRoot());

        viewThemeUtils.dialog.colorMaterialAlertDialogBackground(binding.getRoot().getContext(), builder);

        return builder.create();
    }

    @Override
    public void onDestroyView() {
        Log_OC.d(TAG, "destroy SyncedFolderPreferencesDialogFragment view");
        if (getDialog() != null && getRetainInstance()) {
            getDialog().setDismissMessage(null);
        }

        if (behaviourDialog != null && behaviourDialog.isShowing()) {
            behaviourDialog.dismiss();
        }

        super.onDestroyView();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        outState.putBoolean(BEHAVIOUR_DIALOG_STATE, behaviourDialogShown);
        outState.putBoolean(NAME_COLLISION_POLICY_DIALOG_STATE, nameCollisionPolicyDialogShown);

        super.onSaveInstanceState(outState);
    }

    @Override
    public void onViewStateRestored(@Nullable Bundle savedInstanceState) {
        behaviourDialogShown = savedInstanceState != null &&
            savedInstanceState.getBoolean(BEHAVIOUR_DIALOG_STATE, false);
        nameCollisionPolicyDialogShown = savedInstanceState != null &&
            savedInstanceState.getBoolean(NAME_COLLISION_POLICY_DIALOG_STATE, false);

        if (behaviourDialogShown) {
            showBehaviourDialog();
        }
        if (nameCollisionPolicyDialogShown) {
            showNameCollisionPolicyDialog();
        }

        super.onViewStateRestored(savedInstanceState);
    }

    public interface OnSyncedFolderPreferenceListener {
        void onSaveSyncedFolderPreference(SyncedFolderParcelable syncedFolder);

        void onCancelSyncedFolderPreference();

        void onDeleteSyncedFolderPreference(SyncedFolderParcelable syncedFolder);
    }

    private class OnSyncedFolderSaveClickListener implements OnClickListener {
        @Override
        public void onClick(View v) {
            dismiss();
            ((OnSyncedFolderPreferenceListener) getActivity()).onSaveSyncedFolderPreference(mSyncedFolder);
        }
    }

    private class OnSyncedFolderCancelClickListener implements OnClickListener {
        @Override
        public void onClick(View v) {
            dismiss();
            ((OnSyncedFolderPreferenceListener) getActivity()).onCancelSyncedFolderPreference();
        }
    }

    private class OnSyncedFolderDeleteClickListener implements OnClickListener {
        @Override
        public void onClick(View v) {
            dismiss();
            ((OnSyncedFolderPreferenceListener) getActivity()).onDeleteSyncedFolderPreference(mSyncedFolder);
        }
    }

    private class OnNameCollisionDialogClickListener implements DialogInterface.OnClickListener {
        @Override
        public void onClick(DialogInterface dialog, int which) {
            mSyncedFolder.setNameCollisionPolicy(getNameCollisionPolicyForSelectionIndex(which));

            mNameCollisionPolicySummary.setText(
                SyncedFolderPreferencesDialogFragment.this.mNameCollisionPolicyItemStrings[which]);
            nameCollisionPolicyDialogShown = false;
            dialog.dismiss();
        }
    }

    /**
     * Get index for name collision selection dialog.
     *
     * @return 0 if ASK_USER, 1 if OVERWRITE, 2 if RENAME, 3 if SKIP, Otherwise: 0
     */
    static private Integer getSelectionIndexForNameCollisionPolicy(NameCollisionPolicy nameCollisionPolicy) {
        switch (nameCollisionPolicy) {
            case OVERWRITE:
                return 1;
            case RENAME:
                return 2;
            case CANCEL:
                return 3;
            case ASK_USER:
            default:
                return 0;
        }
    }

    /**
     * Get index for name collision selection dialog. Inverse of getSelectionIndexForNameCollisionPolicy.
     *
     * @return ASK_USER if 0, OVERWRITE if 1, RENAME if 2, SKIP if 3. Otherwise: ASK_USER
     */
    static private NameCollisionPolicy getNameCollisionPolicyForSelectionIndex(int index) {
        switch (index) {
            case 1:
                return NameCollisionPolicy.OVERWRITE;
            case 2:
                return NameCollisionPolicy.RENAME;
            case 3:
                return NameCollisionPolicy.CANCEL;
            case 0:
            default:
                return NameCollisionPolicy.ASK_USER;
        }
    }
}
