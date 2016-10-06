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
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;

import com.owncloud.android.R;
import com.owncloud.android.datamodel.SyncedFolderItem;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.ui.dialog.parcel.SyncedFolderParcelable;

/**
 * Dialog to show the preferences/configuration of a synced folder allowing the user
 * to change the different parameters.
 */
public class SyncedFolderPreferencesDialogFragment extends DialogFragment {

    private final static String TAG = SyncedFolderPreferencesDialogFragment.class.getSimpleName();
    public static final String SYNCED_FOLDER_PARCELABLE = "SyncedFolderParcelable";

    protected View mView = null;
    private CheckBox mUploadOnWifiCheckbox;
    private CheckBox mUploadOnChargingCheckbox;
    private CheckBox mUploadUseSubfoldersCheckbox;

    private SyncedFolderParcelable mSyncedFolder;

    public static SyncedFolderPreferencesDialogFragment newInstance(SyncedFolderItem syncedFolder) {
        SyncedFolderPreferencesDialogFragment dialogFragment = new SyncedFolderPreferencesDialogFragment();

        if (syncedFolder == null) {
            throw new IllegalArgumentException("SyncedFolder is mandatory but NULL!");
        }

        Bundle args = new Bundle();
        args.putParcelable(SYNCED_FOLDER_PARCELABLE, new SyncedFolderParcelable(syncedFolder));
        dialogFragment.setArguments(args);

        return dialogFragment;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        if (!(activity instanceof OnSyncedFolderPreferenceListener)) {
            throw new IllegalArgumentException("The host activity must implement " + OnSyncedFolderPreferenceListener.class.getCanonicalName());
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // keep the state of the fragment on configuration changes
        setRetainInstance(true);

        // TODO check UX if it shouldn't be cancelable
        //setCancelable(false);
        mView = null;

        mSyncedFolder = getArguments().getParcelable(SYNCED_FOLDER_PARCELABLE);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log_OC.d(TAG, "onCreateView, savedInstanceState is " + savedInstanceState);

        mView = inflater.inflate(R.layout.folder_sync_settings_layout, container, false);

        Button save = (Button) mView.findViewById(R.id.save);
        save.setOnClickListener(new OnSyncedFolderSaveClickListener());

        Button cancel = (Button) mView.findViewById(R.id.cancel);
        cancel.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });

        ((TextView) mView.findViewById(R.id.local_folder_summary)).setText(mSyncedFolder.getLocalPath());
        ((TextView) mView.findViewById(R.id.remote_folder_summary)).setText(mSyncedFolder.getRemotePath());

        // TODO add all necessary listeners and fields

        mUploadOnWifiCheckbox = (CheckBox) mView.findViewById(R.id.setting_instant_upload_on_wifi_checkbox);
        mUploadOnChargingCheckbox = (CheckBox) mView.findViewById(R.id.setting_instant_upload_on_charging_checkbox);
        mUploadUseSubfoldersCheckbox = (CheckBox) mView.findViewById(R.id
                .setting_instant_upload_path_use_subfolders_checkbox);

        // TODO create separate setup methods to keep code easy to read

        mView.findViewById(R.id.setting_instant_upload_on_wifi_container).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                // TODO save checkbox state to boolean
                mUploadOnWifiCheckbox.toggle();
            }
        });

        mView.findViewById(R.id.setting_instant_upload_on_charging_container).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                // TODO save checkbox state to boolean
                mUploadOnChargingCheckbox.toggle();
            }
        });

        mView.findViewById(R.id.setting_instant_upload_path_use_subfolders_container).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                // TODO save checkbox state to boolean
                mUploadUseSubfoldersCheckbox.toggle();
            }
        });

        return mView;
    }

    @Override
    @NonNull
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final Dialog dialog = super.onCreateDialog(savedInstanceState);
        dialog.setTitle(R.string.folder_sync_preferences);
        return dialog;
    }

    @Override
    public void onDestroyView() {
        Log_OC.d(TAG, "destroy SyncedFolderPreferencesDialogFragment view");
        if (getDialog() != null && getRetainInstance())
            getDialog().setDismissMessage(null);
        super.onDestroyView();
    }

    private class OnSyncedFolderSaveClickListener implements OnClickListener {
        @Override
        public void onClick(View v) {
            dismiss();
            ((OnSyncedFolderPreferenceListener) getActivity()).onSaveSyncedFolderPreference();
        }
    }

    public interface OnSyncedFolderPreferenceListener {
        public void onSaveSyncedFolderPreference();
    }
}
