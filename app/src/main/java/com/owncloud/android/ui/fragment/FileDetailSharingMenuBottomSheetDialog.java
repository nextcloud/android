/*
 * Nextcloud Android client application
 *
 * @author TSI-mc
 * Copyright (C) 2021 TSI-mc
 * Copyright (C) 2021 Nextcloud GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package com.owncloud.android.ui.fragment;

import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
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
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = FileDetailsSharingMenuBottomSheetFragmentBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        if (getWindow() != null) {
            getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        viewThemeUtils.platform.themeDialog(binding.getRoot());

        viewThemeUtils.platform.colorImageView(binding.menuIconAddAnotherLink);
        viewThemeUtils.platform.colorImageView(binding.menuIconAdvancedPermissions);
        viewThemeUtils.platform.colorImageView(binding.menuIconSendLink);
        viewThemeUtils.platform.colorImageView(binding.menuIconUnshare);
        viewThemeUtils.platform.colorImageView(binding.menuIconSendNewEmail);

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
            binding.menuShareSendLink.setVisibility(View.VISIBLE);
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
