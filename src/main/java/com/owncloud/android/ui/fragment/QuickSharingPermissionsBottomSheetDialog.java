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
import com.owncloud.android.R;
import com.owncloud.android.databinding.QuickSharingPermissionsBottomSheetFragmentBinding;
import com.owncloud.android.datamodel.QuickPermissionModel;
import com.owncloud.android.lib.resources.shares.OCShare;
import com.owncloud.android.ui.activity.FileActivity;
import com.owncloud.android.ui.adapter.QuickSharingPermissionsAdapter;
import com.owncloud.android.ui.fragment.util.SharingMenuHelper;

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

    public QuickSharingPermissionsBottomSheetDialog(FileActivity fileActivity,
                                                    QuickPermissionSharingBottomSheetActions actions,
                                                    OCShare ocShare) {
        super(fileActivity);
        this.actions = actions;
        this.ocShare = ocShare;
        this.fileActivity = fileActivity;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = QuickSharingPermissionsBottomSheetFragmentBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        if (getWindow() != null) {
            getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        setUpRecyclerView();
        setOnShowListener(d ->
                              BottomSheetBehavior.from((View) binding.getRoot().getParent())
                                  .setPeekHeight(binding.getRoot().getMeasuredHeight())
                         );
    }

    private void setUpRecyclerView() {
        List<QuickPermissionModel> quickPermissionModelList = getQuickPermissionList();
        QuickSharingPermissionsAdapter adapter = new QuickSharingPermissionsAdapter(quickPermissionModelList, new QuickSharingPermissionsAdapter.QuickSharingPermissionViewHolder.OnPermissionChangeListener() {
            @Override
            public void onPermissionChanged(int position) {
                handlePermissionChanged(quickPermissionModelList, position);
            }

            @Override
            public void onDismissSheet() {
                dismiss();
            }
        });
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
                                                                                                                                         .link_share_read_only))) {
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
        List<QuickPermissionModel> quickPermissionModelList = new ArrayList<>();

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
