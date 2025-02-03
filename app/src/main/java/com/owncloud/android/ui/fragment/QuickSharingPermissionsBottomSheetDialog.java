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
import com.owncloud.android.R;
import com.owncloud.android.databinding.QuickSharingPermissionsBottomSheetFragmentBinding;
import com.owncloud.android.datamodel.QuickPermissionModel;
import com.owncloud.android.lib.resources.shares.OCShare;
import com.owncloud.android.ui.activity.FileActivity;
import com.owncloud.android.ui.adapter.QuickSharingPermissionsAdapter;
import com.owncloud.android.ui.fragment.util.SharingMenuHelper;
import com.owncloud.android.utils.theme.ViewThemeUtils;

import java.util.ArrayList;
import java.util.List;

import androidx.recyclerview.widget.LinearLayoutManager;

import static com.owncloud.android.lib.resources.shares.OCShare.CREATE_PERMISSION_FLAG;
import static com.owncloud.android.lib.resources.shares.OCShare.MAXIMUM_PERMISSIONS_FOR_FILE;
import static com.owncloud.android.lib.resources.shares.OCShare.MAXIMUM_PERMISSIONS_FOR_FOLDER;
import static com.owncloud.android.lib.resources.shares.OCShare.READ_PERMISSION_FLAG;

/**
 * File Details Quick Sharing permissions options {@link android.app.Dialog} styled as a bottom sheet for main actions.
 */
public class QuickSharingPermissionsBottomSheetDialog extends BottomSheetDialog {
    private QuickSharingPermissionsBottomSheetFragmentBinding binding;
    private final QuickPermissionSharingBottomSheetActions actions;
    private final FileActivity fileActivity;
    private final OCShare ocShare;
    private final ViewThemeUtils viewThemeUtils;

    public QuickSharingPermissionsBottomSheetDialog(FileActivity fileActivity,
                                                    QuickPermissionSharingBottomSheetActions actions,
                                                    OCShare ocShare,
                                                    ViewThemeUtils viewThemeUtils) {
        super(fileActivity);
        this.actions = actions;
        this.ocShare = ocShare;
        this.fileActivity = fileActivity;
        this.viewThemeUtils = viewThemeUtils;
    }

    @Override
    @IonosCustomization("Remove custom window LayoutParams")
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = QuickSharingPermissionsBottomSheetFragmentBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        viewThemeUtils.ionos.platform.themeDialog(binding.getRoot());

        setUpRecyclerView();
        setOnShowListener(d ->
                              BottomSheetBehavior.from((View) binding.getRoot().getParent())
                                  .setPeekHeight(binding.getRoot().getMeasuredHeight())
                         );
    }

    private void setUpRecyclerView() {
        List<QuickPermissionModel> quickPermissionModelList = getQuickPermissionList();
        QuickSharingPermissionsAdapter adapter = new QuickSharingPermissionsAdapter(
            quickPermissionModelList,
            new QuickSharingPermissionsAdapter.QuickSharingPermissionViewHolder.OnPermissionChangeListener() {
                @Override
                public void onPermissionChanged(int position) {
                    handlePermissionChanged(quickPermissionModelList, position);
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
     * handle permission changed on click of selected permission
     * @param quickPermissionModelList
     * @param position
     */
    private void handlePermissionChanged(List<QuickPermissionModel> quickPermissionModelList, int position) {
        if (quickPermissionModelList.get(position).getPermissionName().equalsIgnoreCase(fileActivity.getResources().getString(R.string.link_share_allow_upload_and_editing))
            || quickPermissionModelList.get(position).getPermissionName().equalsIgnoreCase(fileActivity.getResources().getString(R.string.link_share_editing))) {
            if (ocShare.isFolder()) {
                actions.onQuickPermissionChanged(ocShare,
                                                 MAXIMUM_PERMISSIONS_FOR_FOLDER);
            } else {
                actions.onQuickPermissionChanged(ocShare,
                                                 MAXIMUM_PERMISSIONS_FOR_FILE);
            }
        } else if (quickPermissionModelList.get(position).getPermissionName().equalsIgnoreCase(fileActivity.getResources().getString(R.string
                                                                                                                                                                                                                                                                                                                     .link_share_view_only))) {
            actions.onQuickPermissionChanged(ocShare,
                                             READ_PERMISSION_FLAG);

        } else if (quickPermissionModelList.get(position).getPermissionName().equalsIgnoreCase(fileActivity.getResources().getString(R.string
                                                                                                                                         .link_share_file_drop))) {
            actions.onQuickPermissionChanged(ocShare,
                                             CREATE_PERMISSION_FLAG);
        }
        dismiss();
    }

    /**
     * prepare the list of permissions needs to be displayed on recyclerview
     * @return
     */
    private List<QuickPermissionModel> getQuickPermissionList() {

        String[] permissionArray;
        if (ocShare.isFolder()) {
            permissionArray =
                fileActivity.getResources().getStringArray(R.array.folder_share_permission_dialog_values);
        } else {
            permissionArray =
                fileActivity.getResources().getStringArray(R.array.file_share_permission_dialog_values);
        }
        //get the checked item position
        int checkedItem = SharingMenuHelper.getPermissionCheckedItem(fileActivity, ocShare, permissionArray);


        final List<QuickPermissionModel> quickPermissionModelList = new ArrayList<>(permissionArray.length);
        for (int i = 0; i < permissionArray.length; i++) {
            QuickPermissionModel quickPermissionModel = new QuickPermissionModel(permissionArray[i], checkedItem == i);
            quickPermissionModelList.add(quickPermissionModel);
        }
        return quickPermissionModelList;
    }


    @Override
    protected void onStop() {
        super.onStop();
        binding = null;
    }

    public interface QuickPermissionSharingBottomSheetActions {
        void onQuickPermissionChanged(OCShare share, int permission);
    }
}
