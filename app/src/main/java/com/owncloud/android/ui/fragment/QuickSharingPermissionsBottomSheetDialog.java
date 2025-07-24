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

import android.app.Dialog;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.nextcloud.utils.extensions.OCShareExtensionsKt;
import com.owncloud.android.R;
import com.owncloud.android.databinding.QuickSharingPermissionsBottomSheetFragmentBinding;
import com.owncloud.android.datamodel.quickPermission.QuickPermission;
import com.owncloud.android.datamodel.quickPermission.QuickPermissionType;
import com.owncloud.android.lib.resources.shares.OCShare;
import com.owncloud.android.ui.activity.FileActivity;
import com.owncloud.android.ui.adapter.QuickSharingPermissionsAdapter;
import com.owncloud.android.ui.fragment.util.SharePermissionManager;
import com.owncloud.android.utils.theme.ViewThemeUtils;

import java.util.List;

import androidx.recyclerview.widget.LinearLayoutManager;

import static com.owncloud.android.lib.resources.shares.OCShare.CREATE_PERMISSION_FLAG;
import static com.owncloud.android.lib.resources.shares.OCShare.MAXIMUM_PERMISSIONS_FOR_FILE;
import static com.owncloud.android.lib.resources.shares.OCShare.MAXIMUM_PERMISSIONS_FOR_FOLDER;
import static com.owncloud.android.lib.resources.shares.OCShare.READ_PERMISSION_FLAG;

/**
 * File Details Quick Sharing permissions options {@link Dialog} styled as a bottom sheet for main actions.
 */
public class QuickSharingPermissionsBottomSheetDialog extends BottomSheetDialog {
    private QuickSharingPermissionsBottomSheetFragmentBinding binding;
    private final QuickPermissionSharingBottomSheetActions actions;
    private final FileActivity fileActivity;
    private final OCShare ocShare;
    private final ViewThemeUtils viewThemeUtils;
    private final boolean encrypted;

    public QuickSharingPermissionsBottomSheetDialog(FileActivity fileActivity,
                                                    QuickPermissionSharingBottomSheetActions actions,
                                                    OCShare ocShare,
                                                    ViewThemeUtils viewThemeUtils,
                                                    boolean encrypted) {
        super(fileActivity);
        this.actions = actions;
        this.ocShare = ocShare;
        this.fileActivity = fileActivity;
        this.viewThemeUtils = viewThemeUtils;
        this.encrypted = encrypted;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = QuickSharingPermissionsBottomSheetFragmentBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        if (getWindow() != null) {
            getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        viewThemeUtils.platform.themeDialog(binding.getRoot());

        setUpRecyclerView();
        setOnShowListener(d ->
                              BottomSheetBehavior.from((View) binding.getRoot().getParent())
                                  .setPeekHeight(binding.getRoot().getMeasuredHeight())
                         );
    }

    private void setUpRecyclerView() {
        List<QuickPermission> quickPermissionList = getQuickPermissionList();
        QuickSharingPermissionsAdapter adapter = new QuickSharingPermissionsAdapter(
            quickPermissionList,
            new QuickSharingPermissionsAdapter.QuickSharingPermissionViewHolder.OnPermissionChangeListener() {
                @Override
                public void onCustomPermissionSelected() {
                    dismiss();
                    actions.openShareDetailWithCustomPermissions(ocShare);
                }

                @Override
                public void onPermissionChanged(int position) {
                    handlePermissionChanged(quickPermissionList, position);
                }

                @Override
                public void onDismissSheet() {
                    dismiss();
                }
            },
            viewThemeUtils
        );
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(fileActivity);
        binding.rvQuickSharePermissions.setLayoutManager(linearLayoutManager);
        binding.rvQuickSharePermissions.setAdapter(adapter);
    }

    /**
     * Handle permission changed on click of selected permission
     */
    private void handlePermissionChanged(List<QuickPermission> quickPermissionList, int position) {
        final var type = quickPermissionList.get(position).getType();
        int permissionFlag = type.getPermissionFlag(ocShare.isFolder());
        actions.onQuickPermissionChanged(ocShare, permissionFlag);
        dismiss();
    }

    /**
     * Prepare the list of permissions needs to be displayed on recyclerview
     */
    private List<QuickPermission> getQuickPermissionList() {
        final var selectedType = SharePermissionManager.INSTANCE.getSelectedType(ocShare, encrypted);
        final var hasFileRequestPermission = OCShareExtensionsKt.hasFileRequestPermission(ocShare);
        return selectedType.getAvailablePermissions(hasFileRequestPermission);
    }

    @Override
    protected void onStop() {
        super.onStop();
        binding = null;
    }

    public interface QuickPermissionSharingBottomSheetActions {
        void onQuickPermissionChanged(OCShare share, int permission);

        void openShareDetailWithCustomPermissions(OCShare share);
    }
}
