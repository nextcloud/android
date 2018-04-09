/*
 * Nextcloud Android client application
 *
 * @author Andy Scherzinger
 * Copyright (C) 2018 Andy Scherzinger
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.owncloud.android.ui.fragment;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomSheetBehavior;
import android.support.design.widget.BottomSheetDialog;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.owncloud.android.R;
import com.owncloud.android.utils.ThemeUtils;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;

/**
 * FAB menu {@link android.app.Dialog} styled as a bottom sheet for main actions.
 */
public class OCFileListBottomSheetDialog extends BottomSheetDialog {
    @BindView(R.id.menu_icon_upload_files)
    ImageView mIconUploadFiles;
    @BindView(R.id.menu_icon_upload_from_app)
    ImageView mIconUploadFromApp;
    @BindView(R.id.menu_icon_mkdir)
    ImageView mIconMakeDir;

    @BindView(R.id.add_to_cloud)
    TextView mHeadline;

    @BindView(R.id.menu_upload_files)
    LinearLayout mMenuUploadFiles;
    @BindView(R.id.menu_upload_from_app)
    LinearLayout mMenuUploadFromApp;
    @BindView(R.id.menu_mkdir)
    LinearLayout mMenuMakeDir;

    private Unbinder unbinder;

    private OCFileListBottomSheetActions mActions;


    public OCFileListBottomSheetDialog(@NonNull Context context, OCFileListBottomSheetActions
            actions) {
        super(context);
        mActions = actions;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final View view = getLayoutInflater()
                .inflate(R.layout.file_list_actions_bottom_sheet_fragment, null);
        setContentView(view);
        if (getWindow() != null) {
            getWindow().setLayout(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
        }

        unbinder = ButterKnife.bind(this, view);
        ThemeUtils.tintDrawable(mIconUploadFiles.getDrawable(), ThemeUtils.primaryColor());
        ThemeUtils.tintDrawable(mIconUploadFromApp.getDrawable(), ThemeUtils.primaryColor());
        ThemeUtils.tintDrawable(mIconMakeDir.getDrawable(), ThemeUtils.primaryColor());

        mHeadline.setText(getContext().getResources().getString(R.string.add_to_cloud,
                ThemeUtils.getDefaultDisplayNameForRootFolder()));

        mMenuUploadFiles.setOnClickListener(v -> uploadFiles());
        mMenuUploadFromApp.setOnClickListener(v -> uploadFromApp());
        mMenuMakeDir.setOnClickListener(v -> createFolder());

        setOnShowListener(d ->
                BottomSheetBehavior.from((View) view.getParent())
                        .setPeekHeight(view.getMeasuredHeight())
        );
    }

    private void createFolder() {
        mActions.createFolder();
        dismiss();
    }

    private void uploadFromApp() {
        mActions.uploadFromApp();
        dismiss();
    }

    private void uploadFiles() {
        mActions.uploadFiles();
        dismiss();
    }

    @Override
    protected void onStop() {
        super.onStop();
        unbinder.unbind();
    }
}
