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

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;

import com.owncloud.android.R;
import com.owncloud.android.ui.adapter.StoragePathAdapter;
import com.owncloud.android.ui.adapter.StoragePathItem;
import com.owncloud.android.utils.FileStorageUtils;
import com.owncloud.android.utils.ThemeUtils;

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
import androidx.recyclerview.widget.RecyclerView;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;

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

    private Unbinder unbinder;

    @BindView(R.id.storage_path_recycler_view)
    RecyclerView recyclerView;

    public static LocalStoragePathPickerDialogFragment newInstance() {
        return new LocalStoragePathPickerDialogFragment();
    }

    @Override
    public void onStart() {
        super.onStart();

        int color = ThemeUtils.primaryAccentColor(getContext());

        AlertDialog alertDialog = (AlertDialog) getDialog();

        alertDialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(color);
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

        int accentColor = ThemeUtils.primaryAccentColor(getContext());

        // Inflate the layout for the dialog
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        @SuppressLint("InflateParams") View view = inflater.inflate(R.layout.storage_path_dialog, null, false);

        StoragePathAdapter adapter = new StoragePathAdapter(getPathList(), this);

        unbinder = ButterKnife.bind(this, view);
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));

        // Build the dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
        builder.setView(view)
            .setNegativeButton(R.string.common_cancel, this)
            .setTitle(ThemeUtils.getColoredTitle(getResources().getString(R.string.storage_choose_location),
                accentColor));

        return builder.create();
    }

    @Override
    public void onStop() {
        unbinder.unbind();

        super.onStop();
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        if (which == AlertDialog.BUTTON_NEGATIVE) {
            dismissAllowingStateLoss();
        }
    }

    private List<StoragePathItem> getPathList() {
        List<StoragePathItem> storagePathItems = new ArrayList<>();

        addIfExists(storagePathItems, new StoragePathItem(R.drawable.ic_image_grey600,
                                                          getString(R.string.storage_pictures),
                                                          Environment.getExternalStoragePublicDirectory(
                                                              Environment.DIRECTORY_PICTURES).getAbsolutePath()));
        addIfExists(storagePathItems, new StoragePathItem(R.drawable.ic_camera, getString(R.string.storage_camera),
                                                          Environment.getExternalStoragePublicDirectory(
                                                              Environment.DIRECTORY_DCIM).getAbsolutePath()));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            addIfExists(storagePathItems, new StoragePathItem(R.drawable.ic_document_grey600,
                                                              getString(R.string.storage_documents),
                                                              Environment.getExternalStoragePublicDirectory(
                                                                  Environment.DIRECTORY_DOCUMENTS).getAbsolutePath()));
        }
        addIfExists(storagePathItems, new StoragePathItem(R.drawable.ic_download_grey600,
                                                          getString(R.string.storage_downloads),
                                                          Environment.getExternalStoragePublicDirectory(
                                                              Environment.DIRECTORY_DOWNLOADS).getAbsolutePath()));
        addIfExists(storagePathItems, new StoragePathItem(R.drawable.ic_movie_grey600,
                                                          getString(R.string.storage_movies),
                                                          Environment.getExternalStoragePublicDirectory(
                                                              Environment.DIRECTORY_MOVIES).getAbsolutePath()));
        addIfExists(storagePathItems, new StoragePathItem(R.drawable.ic_music_grey600,
                                                          getString(R.string.storage_music),
                                                          Environment.getExternalStoragePublicDirectory(
                                                              Environment.DIRECTORY_MUSIC).getAbsolutePath()));

        String sdCard = getString(R.string.storage_internal_storage);
        for (String dir : FileStorageUtils.getStorageDirectories(requireActivity())) {
            if (internalStoragePaths.contains(dir)) {
                addIfExists(storagePathItems, new StoragePathItem(R.drawable.ic_sd_grey600, sdCard, dir));
            } else {
                addIfExists(storagePathItems, new StoragePathItem(R.drawable.ic_sd_grey600, new File(dir).getName(), dir));
            }
        }

        return storagePathItems;
    }

    private void addIfExists(List<StoragePathItem> storagePathItems, StoragePathItem item) {
        File path = new File(item.getPath());
        if (path.exists() && path.canRead()) {
            storagePathItems.add(item);
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
