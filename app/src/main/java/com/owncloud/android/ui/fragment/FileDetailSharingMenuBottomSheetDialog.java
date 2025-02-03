/*
 * Nextcloud Android client application
 *
 * @author TSI-mc
 * Copyright (C) 2021 TSI-mc
 * Copyright (C) 2021 Nextcloud GmbH
 *
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */

package com.owncloud.android.ui.fragment;

import android.os.Bundle;
import android.view.View;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.ionos.annotation.IonosCustomization;
import com.nextcloud.utils.mdm.MDMConfig;
import com.owncloud.android.databinding.FileDetailsSharingMenuBottomSheetFragmentBinding;
import com.owncloud.android.lib.resources.shares.OCShare;
import com.owncloud.android.lib.resources.shares.ShareType;
import com.owncloud.android.ui.activity.FileActivity;
import com.owncloud.android.ui.fragment.util.SharingMenuHelper;
import com.owncloud.android.utils.theme.ViewThemeUtils;

/**
 * File Details Sharing option menus {@link android.app.Dialog} styled as a bottom sheet for main actions.
 */
public class FileDetailSharingMenuBottomSheetDialog extends BottomSheetDialog {
    private FileDetailsSharingMenuBottomSheetFragmentBinding binding;
    private final FileDetailsSharingMenuBottomSheetActions actions;
    private final OCShare ocShare;
    private final ViewThemeUtils viewThemeUtils;
    public FileDetailSharingMenuBottomSheetDialog(FileActivity fileActivity,
                                                  FileDetailsSharingMenuBottomSheetActions actions,
                                                  OCShare ocShare,
                                                  ViewThemeUtils viewThemeUtils) {
        super(fileActivity);
        this.actions = actions;
        this.ocShare = ocShare;
        this.viewThemeUtils = viewThemeUtils;
    }

    @Override
    @IonosCustomization("Remove custom window LayoutParams. Disable icon tinting")
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = FileDetailsSharingMenuBottomSheetFragmentBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        viewThemeUtils.ionos.platform.themeDialog(binding.getRoot());

        updateUI();

        setupClickListener();

        setOnShowListener(d ->
                              BottomSheetBehavior.from((View) binding.getRoot().getParent())
                                  .setPeekHeight(binding.getRoot().getMeasuredHeight())
                         );
    }

    private void updateUI() {
        if (ocShare.getShareType() == ShareType.PUBLIC_LINK) {
            binding.menuShareAddAnotherLink.setVisibility(View.VISIBLE);

            if (MDMConfig.INSTANCE.sendFilesSupport(getContext())) {
                binding.menuShareSendLink.setVisibility(View.VISIBLE);
            }
        } else {
            binding.menuShareAddAnotherLink.setVisibility(View.GONE);
            binding.menuShareSendLink.setVisibility(View.GONE);
        }

        if (SharingMenuHelper.isSecureFileDrop(ocShare)) {
            binding.menuShareAdvancedPermissions.setVisibility(View.GONE);
            binding.menuShareAddAnotherLink.setVisibility(View.GONE);
        }
    }

    private void setupClickListener() {
        binding.menuShareAdvancedPermissions.setOnClickListener(v -> {
            actions.advancedPermissions(ocShare);
            dismiss();
        });

        binding.menuShareSendNewEmail.setOnClickListener(v -> {
            actions.sendNewEmail(ocShare);
            dismiss();
        });

        binding.menuShareUnshare.setOnClickListener(v -> {
            actions.unShare(ocShare);
            dismiss();
        });

        binding.menuShareSendLink.setOnClickListener(v -> {
            actions.sendLink(ocShare);
            dismiss();
        });

        binding.menuShareAddAnotherLink.setOnClickListener(v -> {
            actions.addAnotherLink(ocShare);
            dismiss();
        });
    }

    @Override
    protected void onStop() {
        super.onStop();
        binding = null;
    }
}
