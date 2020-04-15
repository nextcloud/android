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
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.text.style.StyleSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;

import com.owncloud.android.R;
import com.owncloud.android.databinding.SyncedFoldersSettingsBinding;
import com.owncloud.android.datamodel.MediaFolderType;
import com.owncloud.android.datamodel.SyncedFolderDisplayItem;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.ui.activity.FolderPickerActivity;
import com.owncloud.android.ui.activity.UploadFilesActivity;
import com.owncloud.android.ui.dialog.parcel.SyncedFolderParcelable;
import com.owncloud.android.utils.DisplayUtils;
import com.owncloud.android.utils.ThemeUtils;

import java.io.File;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import static com.owncloud.android.datamodel.SyncedFolderDisplayItem.UNPERSISTED_ID;
import static com.owncloud.android.ui.activity.UploadFilesActivity.REQUEST_CODE_KEY;

/**
 * Dialog to show the preferences/configuration of a synced folder allowing the user to change the different parameters.
 */
public class SyncedFolderPreferencesDialogFragment extends DialogFragment {

    public static final int REQUEST_CODE__SELECT_REMOTE_FOLDER = 0;
    public static final int REQUEST_CODE__SELECT_LOCAL_FOLDER = 1;

    private final static String TAG = SyncedFolderPreferencesDialogFragment.class.getSimpleName();
    private final static String SYNCED_FOLDER_PARCELABLE = "SyncedFolderParcelable";
    private final static String BEHAVIOUR_DIALOG_STATE = "BEHAVIOUR_DIALOG_STATE";
    private final static float alphaEnabled = 1.0f;
    private final static float alphaDisabled = 0.7f;

    private CharSequence[] mUploadBehaviorItemStrings;

    private SyncedFolderParcelable mSyncedFolder;
    private boolean behaviourDialogShown;
    private AlertDialog behaviourDialog;

    private SyncedFoldersSettingsBinding binding;

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

        mSyncedFolder = getArguments().getParcelable(SYNCED_FOLDER_PARCELABLE);
        mUploadBehaviorItemStrings = getResources().getTextArray(R.array.pref_behaviour_entries);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log_OC.d(TAG, "onCreateView, savedInstanceState is " + savedInstanceState);

        binding = SyncedFoldersSettingsBinding.inflate(inflater, container, false);

        setupDialogElements();
        setupListeners();

        return binding.getRoot();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        binding = null;
    }

    /**
     * find all relevant UI elements and set their values.
     */
    private void setupDialogElements() {
        int accentColor = ThemeUtils.primaryAccentColor(getContext());

        if (mSyncedFolder.getType().getId() > MediaFolderType.CUSTOM.getId()) {
            // hide local folder chooser and delete for non-custom folders
            binding.localFolderContainer.setVisibility(View.GONE);
            binding.delete.setVisibility(View.GONE);
        } else if (mSyncedFolder.getId() <= UNPERSISTED_ID) {
            // Hide delete/enabled for non-persisted custom folders
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
        ThemeUtils.tintSwitch(binding.syncEnabled, accentColor);

        ThemeUtils.tintCheckbox(binding.settingInstantUploadOnWifiCheckbox, accentColor);
        ThemeUtils.tintCheckbox(binding.settingInstantUploadOnChargingCheckbox, accentColor);
        ThemeUtils.tintCheckbox(binding.settingInstantUploadExistingCheckbox, accentColor);
        ThemeUtils.tintCheckbox(binding.settingInstantUploadPathUseSubfoldersCheckbox, accentColor);

        ThemeUtils.themeDialogActionButton(binding.cancel);
        ThemeUtils.themeDialogActionButton(binding.save);

        // Set values
        setEnabled(mSyncedFolder.isEnabled());

        if (!TextUtils.isEmpty(mSyncedFolder.getLocalPath())) {
            binding.syncedFoldersSettingsLocalFolderPath.setText(
                    DisplayUtils.createTextWithSpan(
                            String.format(
                                    getString(R.string.synced_folders_preferences_folder_path),
                                    mSyncedFolder.getLocalPath()),
                            mSyncedFolder.getFolderName(),
                            new StyleSpan(Typeface.BOLD)));
            binding.localFolderSummary.setText(mSyncedFolder.getLocalPath());
        } else {
            binding.localFolderSummary.setText(R.string.choose_local_folder);
        }

        if (!TextUtils.isEmpty(mSyncedFolder.getLocalPath())) {
            binding.localFolderSummary.setText(mSyncedFolder.getRemotePath());
        } else {
            binding.localFolderSummary.setText(R.string.choose_remote_folder);
        }

        binding.settingInstantUploadOnWifiCheckbox.setChecked(mSyncedFolder.isWifiOnly());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            binding.settingInstantUploadOnChargingCheckbox.setChecked(mSyncedFolder.isChargingOnly());
        }
        binding.settingInstantUploadExistingCheckbox.setChecked(mSyncedFolder.isExisting());
        binding.settingInstantUploadPathUseSubfoldersCheckbox.setChecked(mSyncedFolder.isSubfolderByDate());

        binding.settingInstantBehaviourSummary
            .setText(mUploadBehaviorItemStrings[mSyncedFolder.getUploadActionInteger()]);
    }

    /**
     * set correct icon/flag.
     *
     * @param enabled if enabled or disabled
     */
    private void setEnabled(boolean enabled) {
        mSyncedFolder.setEnabled(enabled);
        binding.syncEnabled.setChecked(enabled);

        setupViews(enabled);
    }

    /**
     * set (new) remote path on activity result of the folder picker activity. The result gets originally propagated
     * to the underlying activity since the picker is an activity and the result can't get passed to the dialog
     * fragment directly.
     *
     * @param path the remote path to be set
     */
    public void setRemoteFolderSummary(String path) {
        mSyncedFolder.setRemotePath(path);
        binding.remoteFolderSummary.setText(path);
        checkAndUpdateSaveButtonState();
    }

    /**
     * set (new) local path on activity result of the folder picker activity. The result gets originally propagated
     * to the underlying activity since the picker is an activity and the result can't get passed to the dialog
     * fragment directly.
     *
     * @param path the remote path to be set
     */
    public void setLocalFolderSummary(String path) {
        mSyncedFolder.setLocalPath(path);
        binding.localFolderSummary.setText(path);
        binding.syncedFoldersSettingsLocalFolderPath.setText(
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
            checkWritableFolder(true);
        } else {
            binding.save.setEnabled(false);
            checkWritableFolder(false);
        }
    }

    private void checkWritableFolder(boolean enabled) {
        if (enabled) {
            if (mSyncedFolder.getLocalPath() != null && new File(mSyncedFolder.getLocalPath()).canWrite()) {
                binding.settingInstantBehaviourContainer.setEnabled(true);
                binding.settingInstantBehaviourContainer.setAlpha(alphaEnabled);
                binding.settingInstantBehaviourSummary
                    .setText(mUploadBehaviorItemStrings[mSyncedFolder.getUploadActionInteger()]);
            } else {
                binding.settingInstantBehaviourContainer.setEnabled(false);
                binding.settingInstantBehaviourContainer.setAlpha(alphaDisabled);

                mSyncedFolder.setUploadAction(
                    getResources().getTextArray(R.array.pref_behaviour_entryValues)[0].toString());

                binding.settingInstantBehaviourSummary
                    .setText(R.string.auto_upload_file_behaviour_kept_in_folder);
            }
        } else {
            binding.settingInstantBehaviourContainer.setEnabled(false);
            binding.settingInstantBehaviourContainer.setAlpha(alphaDisabled);
        }
    }

    private void setupViews(boolean enable) {
        float alpha;
        if (enable) {
            alpha = alphaEnabled;
        } else {
            alpha = alphaDisabled;
        }
        binding.settingInstantUploadOnWifiContainer.setEnabled(enable);
        binding.settingInstantUploadOnWifiContainer.setAlpha(alpha);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            binding.settingInstantUploadOnChargingContainer.setEnabled(enable);
            binding.settingInstantUploadOnChargingContainer.setAlpha(alpha);
        }

        binding.settingInstantUploadExistingContainer.setEnabled(enable);
        binding.settingInstantUploadExistingContainer.setAlpha(alpha);

        binding.settingInstantUploadPathUseSubfoldersContainer.setEnabled(enable);
        binding.settingInstantUploadPathUseSubfoldersContainer.setAlpha(alpha);

        binding.remoteFolderContainer.setEnabled(enable);
        binding.remoteFolderContainer.setAlpha(alpha);

        binding.localFolderContainer.setEnabled(enable);
        binding.localFolderContainer.setAlpha(alpha);

        checkWritableFolder(enable);
    }

    /**
     * setup all listeners.
     */
    private void setupListeners() {
        binding.save.setOnClickListener(new OnSyncedFolderSaveClickListener());
        binding.cancel.setOnClickListener(new OnSyncedFolderCancelClickListener());
        binding.delete.setOnClickListener(new OnSyncedFolderDeleteClickListener());

        binding.settingInstantUploadOnWifiContainer.setOnClickListener(v -> {
            mSyncedFolder.setWifiOnly(!mSyncedFolder.isWifiOnly());
            binding.settingInstantUploadOnWifiCheckbox.toggle();
        });

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            binding.settingInstantUploadOnChargingContainer.setOnClickListener(v -> {
                mSyncedFolder.setChargingOnly(!mSyncedFolder.isChargingOnly());
                binding.settingInstantUploadOnChargingCheckbox.toggle();
            });
        }

        binding.settingInstantUploadExistingContainer.setOnClickListener(v -> {
            mSyncedFolder.setExisting(!mSyncedFolder.isExisting());
            binding.settingInstantUploadExistingCheckbox.toggle();
        });

        binding.settingInstantUploadPathUseSubfoldersContainer.setOnClickListener(v -> {
            mSyncedFolder.setSubfolderByDate(!mSyncedFolder.isSubfolderByDate());
            binding.settingInstantUploadPathUseSubfoldersCheckbox.toggle();
        });

        binding.remoteFolderContainer.setOnClickListener(v -> {
            Intent action = new Intent(getActivity(), FolderPickerActivity.class);
            requireActivity().startActivityForResult(action, REQUEST_CODE__SELECT_REMOTE_FOLDER);
        });

        binding.remoteFolderSummary.setOnClickListener(textView -> {
            binding.remoteFolderSummary.setEllipsize(null);
            binding.remoteFolderSummary.setMaxLines(Integer.MAX_VALUE);
        });

        binding.localFolderContainer.setOnClickListener(v -> {
            Intent action = new Intent(getActivity(), UploadFilesActivity.class);
            action.putExtra(UploadFilesActivity.KEY_LOCAL_FOLDER_PICKER_MODE, true);
            action.putExtra(REQUEST_CODE_KEY, REQUEST_CODE__SELECT_LOCAL_FOLDER);
            requireActivity().startActivityForResult(action, REQUEST_CODE__SELECT_LOCAL_FOLDER);
        });

        binding.localFolderSummary.setOnClickListener(textView -> {
            binding.localFolderSummary.setEllipsize(null);
            binding.localFolderSummary.setMaxLines(Integer.MAX_VALUE);
        });

        binding.syncEnabled.setOnClickListener(v -> setEnabled(!mSyncedFolder.isEnabled()));

        binding.settingInstantBehaviourContainer.setOnClickListener(v -> showBehaviourDialog());
    }

    private void showBehaviourDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
        builder.setTitle(ThemeUtils.getColoredTitle(
                getResources().getString(R.string.prefs_instant_behaviour_dialogTitle),
                ThemeUtils.primaryAccentColor(getContext())))
                .setSingleChoiceItems(getResources().getTextArray(R.array.pref_behaviour_entries),
                                      mSyncedFolder.getUploadActionInteger(),
                                      (dialog, which) -> {
                                          mSyncedFolder.setUploadAction(
                                              getResources().getTextArray(
                                                  R.array.pref_behaviour_entryValues)[which].toString());
                                          binding.settingInstantBehaviourSummary
                                              .setText(mUploadBehaviorItemStrings[which]);
                                          behaviourDialogShown = false;
                                          dialog.dismiss();
                                      })
            .setOnCancelListener(dialog -> behaviourDialogShown = false);
        behaviourDialogShown = true;
        behaviourDialog = builder.create();
        behaviourDialog.show();
    }

    @Override
    @NonNull
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final Dialog dialog = super.onCreateDialog(savedInstanceState);
        dialog.setTitle(null);
        return dialog;
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

        super.onSaveInstanceState(outState);
    }

    @Override
    public void onViewStateRestored(@Nullable Bundle savedInstanceState) {
        behaviourDialogShown = savedInstanceState != null && savedInstanceState.getBoolean(BEHAVIOUR_DIALOG_STATE,
                                                                                           false);

        if (behaviourDialogShown) {
            showBehaviourDialog();
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
            ((OnSyncedFolderPreferenceListener) requireActivity()).onSaveSyncedFolderPreference(mSyncedFolder);
        }
    }

    private class OnSyncedFolderCancelClickListener implements OnClickListener {
        @Override
        public void onClick(View v) {
            dismiss();
            ((OnSyncedFolderPreferenceListener) requireActivity()).onCancelSyncedFolderPreference();
        }
    }

    private class OnSyncedFolderDeleteClickListener implements OnClickListener {
        @Override
        public void onClick(View v) {
            dismiss();
            ((OnSyncedFolderPreferenceListener) requireActivity()).onDeleteSyncedFolderPreference(mSyncedFolder);
        }
    }
}
