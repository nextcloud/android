/*
 * IONOS HiDrive Next - Android Client
 *
 * SPDX-FileCopyrightText: 2025 STRATO GmbH.
 * SPDX-License-Identifier: GPL-2.0
 */

package com.owncloud.android.ui.fragment;

import android.os.Bundle;
import android.view.View;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.nextcloud.android.common.ui.theme.utils.ColorRole;
import com.nextcloud.client.device.DeviceInfo;
import com.nextcloud.client.di.Injectable;
import com.nextcloud.client.documentscan.AppScanOptionalFeature;
import com.owncloud.android.R;
import com.owncloud.android.databinding.SimpleFileListActionsBottomSheetFragmentBinding;
import com.owncloud.android.ui.activity.FileActivity;
import com.owncloud.android.utils.theme.ThemeUtils;
import com.owncloud.android.utils.theme.ViewThemeUtils;

/**
 * Simple FAB menu {@link android.app.Dialog} styled as a bottom sheet for main actions.
 */
public class SimpleOCFileListBottomSheetDialog extends BottomSheetDialog implements Injectable {

    private SimpleFileListActionsBottomSheetFragmentBinding binding;
    private final OCFileListBottomSheetActions actions;
    private final DeviceInfo deviceInfo;
    private final ThemeUtils themeUtils;
    private final ViewThemeUtils viewThemeUtils;

    private final AppScanOptionalFeature appScanOptionalFeature;


    public SimpleOCFileListBottomSheetDialog(FileActivity fileActivity,
                                             OCFileListBottomSheetActions actions,
                                             DeviceInfo deviceInfo,
                                             ThemeUtils themeUtils,
                                             ViewThemeUtils viewThemeUtils,
                                             AppScanOptionalFeature appScanOptionalFeature) {
        super(fileActivity);
        this.actions = actions;
        this.deviceInfo = deviceInfo;
        this.themeUtils = themeUtils;
        this.viewThemeUtils = viewThemeUtils;
        this.appScanOptionalFeature = appScanOptionalFeature;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = SimpleFileListActionsBottomSheetFragmentBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        viewThemeUtils.ionos.platform.colorViewBackground(binding.getRoot(), ColorRole.SURFACE);

        binding.addToCloud.setText(getContext().getResources().getString(R.string.add_to_cloud,
                                                                         themeUtils.getDefaultDisplayNameForRootFolder(getContext())));

        if (!deviceInfo.hasCamera(getContext())) {
            binding.menuDirectCameraUpload.setVisibility(View.GONE);
        }

        setupClickListener();
    }

    private void setupClickListener() {
        binding.menuMkdir.setOnClickListener(v -> {
            actions.createFolder();
            dismiss();
        });

        binding.menuUploadFromApp.setOnClickListener(v -> {
            actions.uploadFromApp();
            dismiss();
        });

        binding.menuDirectCameraUpload.setOnClickListener(v -> {
            actions.directCameraUpload();
            dismiss();
        });

        if (appScanOptionalFeature.isAvailable()) {
            binding.menuScanDocUpload.setOnClickListener(v -> {
                actions.scanDocUpload();
                dismiss();
            });
        } else {
            binding.menuScanDocUpload.setVisibility(View.GONE);
        }

        binding.menuUploadFiles.setOnClickListener(v -> {
            actions.uploadFiles();
            dismiss();
        });
    }

    @Override
    protected void onStop() {
        super.onStop();
        binding = null;
    }
}
