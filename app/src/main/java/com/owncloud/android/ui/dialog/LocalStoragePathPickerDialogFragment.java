/*
 * Nextcloud Android client application
 *
 * @author Andy Scherzinger
 * Copyright (C) 2019 Andy Scherzinger
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package com.owncloud.android.ui.dialog;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;

import com.owncloud.android.R;
import com.owncloud.android.databinding.StoragePathDialogBinding;
import com.owncloud.android.ui.adapter.StoragePathAdapter;
import com.owncloud.android.ui.adapter.StoragePathItem;
import com.owncloud.android.utils.FileStorageUtils;
import com.owncloud.android.utils.theme.ThemeButtonUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.LinearLayoutManager;

/**
 * Picker dialog for choosing a (storage) path.
 */
public class LocalStoragePathPickerDialogFragment extends DialogFragment
    implements DialogInterface.OnClickListener, StoragePathAdapter.StoragePathAdapterListener {

    public static final String LOCAL_STORAGE_PATH_PICKER_FRAGMENT = "LOCAL_STORAGE_PATH_PICKER_FRAGMENT";

    private static Set<String> internalStoragePaths = new HashSet<>();

    static {
        internalStoragePaths.add("/storage/emulated/legacy");
        internalStoragePaths.add("/storage/emulated/0");
        internalStoragePaths.add("/mnt/sdcard");
    }

    private StoragePathDialogBinding binding;

    public static LocalStoragePathPickerDialogFragment newInstance() {
        return new LocalStoragePathPickerDialogFragment();
    }

    @Override
    public void onStart() {
        super.onStart();

        AlertDialog alertDialog = (AlertDialog) getDialog();

        if (alertDialog != null) {
            ThemeButtonUtils.themeBorderlessButton(alertDialog.getButton(AlertDialog.BUTTON_NEGATIVE));
        }
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        if (!(getActivity() instanceof StoragePathAdapter.StoragePathAdapterListener)) {
            throw new IllegalArgumentException("Calling activity must implement " +
                "StoragePathAdapter.StoragePathAdapterListener");
        }

        // Inflate the layout for the dialog
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        binding = StoragePathDialogBinding.inflate(inflater, null, false);
        View view = binding.getRoot();

        StoragePathAdapter adapter = new StoragePathAdapter(getPathList(), this);

        binding.storagePathRecyclerView.setAdapter(adapter);
        binding.storagePathRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));

        // Build the dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
        builder.setView(view)
            .setNegativeButton(R.string.common_cancel, this)
            .setTitle(R.string.storage_choose_location);

        return builder.create();
    }

    @Override
    public void onDestroyView() {
        binding = null;
        super.onDestroyView();
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        if (which == AlertDialog.BUTTON_NEGATIVE) {
            dismissAllowingStateLoss();
        }
    }

    private List<StoragePathItem> getPathList() {
        List<StoragePathItem> storagePathItems = new ArrayList<>();

        for (FileStorageUtils.StandardDirectory standardDirectory : FileStorageUtils.StandardDirectory.getStandardDirectories()) {
            addIfExists(storagePathItems, standardDirectory.getIcon(), getString(standardDirectory.getDisplayName()),
                Environment.getExternalStoragePublicDirectory(standardDirectory.getName()).getAbsolutePath());
        }

        String sdCard = getString(R.string.storage_internal_storage);
        for (String dir : FileStorageUtils.getStorageDirectories(requireActivity())) {
            if (internalStoragePaths.contains(dir)) {
                addIfExists(storagePathItems, R.drawable.ic_sd_grey600, sdCard, dir);
            } else {
                addIfExists(storagePathItems, R.drawable.ic_sd_grey600, new File(dir).getName(), dir);
            }
        }

        return storagePathItems;
    }

    private void addIfExists(List<StoragePathItem> storagePathItems, int icon, String name, String path) {
        File file = new File(path);
        if (file.exists() && file.canRead()) {
            storagePathItems.add(new StoragePathItem(icon, name, path));
        }
    }

    @Override
    public void chosenPath(String path) {
        if (getActivity() != null) {
            ((StoragePathAdapter.StoragePathAdapterListener) getActivity()).chosenPath(path);
        }
        dismissAllowingStateLoss();
    }
}
