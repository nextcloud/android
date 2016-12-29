/**
 * Nextcloud Android client application
 *
 * @author Andy Scherzinger
 * Copyright (C) 2016 Andy Scherzinger
 * Copyright (C) 2016 Nextcloud
 * <p>
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU AFFERO GENERAL PUBLIC LICENSE
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or any later version.
 * <p>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU AFFERO GENERAL PUBLIC LICENSE for more details.
 * <p>
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
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.SwitchCompat;
import android.text.style.StyleSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import com.owncloud.android.R;
import com.owncloud.android.datamodel.SyncedFolderDisplayItem;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.ui.activity.FolderPickerActivity;
import com.owncloud.android.ui.dialog.parcel.SyncedFolderParcelable;
import com.owncloud.android.utils.DisplayUtils;

/**
 * Dialog to show the preferences/configuration of a synced folder allowing the user to change the different parameters.
 */
public class SyncedFolderPreferencesDialogFragment extends DialogFragment {

    private final static String TAG = SyncedFolderPreferencesDialogFragment.class.getSimpleName();
    public static final String SYNCED_FOLDER_PARCELABLE = "SyncedFolderParcelable";
    public static final int REQUEST_CODE__SELECT_REMOTE_FOLDER = 0;

    private CharSequence[] mUploadBehaviorItemStrings;

    protected View mView = null;
    private SwitchCompat mEnabledSwitch;
    private CheckBox mUploadOnWifiCheckbox;
    private CheckBox mUploadOnChargingCheckbox;
    private CheckBox mUploadUseSubfoldersCheckbox;
    private TextView mUploadBehaviorSummary;
    private TextView mLocalFolderPath;
    private TextView mRemoteFolderSummary;

    private SyncedFolderParcelable mSyncedFolder;

    public static SyncedFolderPreferencesDialogFragment newInstance(SyncedFolderDisplayItem syncedFolder, int section) {
        SyncedFolderPreferencesDialogFragment dialogFragment = new SyncedFolderPreferencesDialogFragment();

        if (syncedFolder == null) {
            throw new IllegalArgumentException("SyncedFolder is mandatory but NULL!");
        }

        Bundle args = new Bundle();
        args.putParcelable(SYNCED_FOLDER_PARCELABLE, new SyncedFolderParcelable(syncedFolder, section));
        dialogFragment.setArguments(args);
        dialogFragment.setStyle(STYLE_NORMAL,R.style.Theme_ownCloud_Dialog);

        return dialogFragment;
    }

    @Override
    public void onAttach(Activity activity) {
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

        setCancelable(false);
        mView = null;

        mSyncedFolder = getArguments().getParcelable(SYNCED_FOLDER_PARCELABLE);
        mUploadBehaviorItemStrings = getResources().getTextArray(R.array.pref_behaviour_entries);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log_OC.d(TAG, "onCreateView, savedInstanceState is " + savedInstanceState);

        mView = inflater.inflate(R.layout.folder_sync_settings_layout, container, false);

        setupDialogElements(mView);
        setupListeners(mView);

        return mView;
    }

    /**
     * find all relevant UI elements and set their values.
     *
     * @param view the parent view
     */
    private void setupDialogElements(View view) {
        // find/saves UI elements
        mEnabledSwitch = (SwitchCompat) view.findViewById(R.id.sync_enabled);
        mLocalFolderPath = (TextView) view.findViewById(R.id.folder_sync_settings_local_folder_path);

        mRemoteFolderSummary = (TextView) view.findViewById(R.id.remote_folder_summary);

        mUploadOnWifiCheckbox = (CheckBox) view.findViewById(R.id.setting_instant_upload_on_wifi_checkbox);
        mUploadOnChargingCheckbox = (CheckBox) view.findViewById(R.id.setting_instant_upload_on_charging_checkbox);
        mUploadUseSubfoldersCheckbox = (CheckBox) view.findViewById(R.id
                .setting_instant_upload_path_use_subfolders_checkbox);

        mUploadBehaviorSummary = (TextView) view.findViewById(R.id.setting_instant_behaviour_summary);

        // Set values
        setEnabled(mSyncedFolder.getEnabled());
        mLocalFolderPath.setText(
                DisplayUtils.createTextWithSpan(
                        String.format(
                                getString(R.string.folder_sync_preferences_folder_path),
                                mSyncedFolder.getLocalPath()),
                        mSyncedFolder.getFolderName(),
                        new StyleSpan(Typeface.BOLD)));

        mRemoteFolderSummary.setText(mSyncedFolder.getRemotePath());

        mUploadOnWifiCheckbox.setChecked(mSyncedFolder.getWifiOnly());
        mUploadOnChargingCheckbox.setChecked(mSyncedFolder.getChargingOnly());
        mUploadUseSubfoldersCheckbox.setChecked(mSyncedFolder.getSubfolderByDate());

        mUploadBehaviorSummary.setText(mUploadBehaviorItemStrings[mSyncedFolder.getUploadActionInteger()]);
    }

    /**
     * set correct icon/flag.
     *
     * @param enabled if enabled or disabled
     */
    private void setEnabled(boolean enabled) {
        mSyncedFolder.setEnabled(enabled);
        mEnabledSwitch.setChecked(enabled);
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
        mRemoteFolderSummary.setText(path);
    }

    /**
     * setup all listeners.
     *
     * @param view the parent view
     */
    private void setupListeners(View view) {
        view.findViewById(R.id.save).setOnClickListener(new OnSyncedFolderSaveClickListener());
        view.findViewById(R.id.cancel).setOnClickListener(new OnSyncedFolderCancelClickListener());

        view.findViewById(R.id.setting_instant_upload_on_wifi_container).setOnClickListener(
                new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        mSyncedFolder.setWifiOnly(!mSyncedFolder.getWifiOnly());
                        mUploadOnWifiCheckbox.toggle();
                    }
                });

        view.findViewById(R.id.setting_instant_upload_on_charging_container).setOnClickListener(
                new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        mSyncedFolder.setChargingOnly(!mSyncedFolder.getChargingOnly());
                        mUploadOnChargingCheckbox.toggle();
                    }
                });

        view.findViewById(R.id.setting_instant_upload_path_use_subfolders_container).setOnClickListener(
                new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        mSyncedFolder.setSubfolderByDate(!mSyncedFolder.getSubfolderByDate());
                        mUploadUseSubfoldersCheckbox.toggle();
                    }
                });

        view.findViewById(R.id.remote_folder_container).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent action = new Intent(getActivity(), FolderPickerActivity.class);
                action.putExtra(
                        FolderPickerActivity.EXTRA_ACTION, getResources().getText(R.string.choose_remote_folder));
                getActivity().startActivityForResult(action, REQUEST_CODE__SELECT_REMOTE_FOLDER);
            }
        });

        view.findViewById(R.id.sync_enabled).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                setEnabled(!mSyncedFolder.getEnabled());
            }
        });

        view.findViewById(R.id.setting_instant_behaviour_container).setOnClickListener(
                new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
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
                                                        dialog.dismiss();
                                                    }
                                                });
                        builder.create().show();
                    }
                });
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
        super.onDestroyView();
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

    public interface OnSyncedFolderPreferenceListener {
        void onSaveSyncedFolderPreference(SyncedFolderParcelable syncedFolder);

        void onCancelSyncedFolderPreference();
    }
}
