/*
 * Nextcloud Android client application
 *
 * @author Andy Scherzinger
 * Copyright (C) 2018 Andy Scherzinger
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

package com.owncloud.android.ui.fragment;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomSheetBehavior;
import android.support.design.widget.BottomSheetDialog;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.owncloud.android.R;
import com.owncloud.android.utils.ThemeUtils;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;

/**
 * FAB menu {@link android.app.Dialog} styled as a bottom sheet for main actions.
 */
public class OCFileListBottomSheetDialog extends BottomSheetDialog {
    @BindView(R.id.menu_icon_upload_files)
    public ImageView iconUploadFiles;
    @BindView(R.id.menu_icon_upload_from_app)
    public ImageView iconUploadFromApp;
    @BindView(R.id.menu_icon_mkdir)
    public ImageView iconMakeDir;
    @BindView(R.id.add_to_cloud)
    public TextView headline;

    private Unbinder unbinder;

    private OCFileListBottomSheetActions actions;


    public OCFileListBottomSheetDialog(@NonNull Context context, OCFileListBottomSheetActions actions) {
        super(context);
        this.actions = actions;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final View view = getLayoutInflater().inflate(R.layout.file_list_actions_bottom_sheet_fragment, null);
        setContentView(view);

        if (getWindow() != null) {
            getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        unbinder = ButterKnife.bind(this, view);

        int primaryColor = ThemeUtils.primaryColor(getContext());
        ThemeUtils.tintDrawable(iconUploadFiles.getDrawable(), primaryColor);
        ThemeUtils.tintDrawable(iconUploadFromApp.getDrawable(), primaryColor);
        ThemeUtils.tintDrawable(iconMakeDir.getDrawable(), primaryColor);

        headline.setText(getContext().getResources().getString(R.string.add_to_cloud,
                ThemeUtils.getDefaultDisplayNameForRootFolder(getContext())));

        setOnShowListener(d ->
                BottomSheetBehavior.from((View) view.getParent()).setPeekHeight(view.getMeasuredHeight())
        );
    }

    @OnClick(R.id.menu_mkdir)
    public void createFolder() {
        actions.createFolder();
        dismiss();
    }

    @OnClick(R.id.menu_upload_from_app)
    public void uploadFromApp() {
        actions.uploadFromApp();
        dismiss();
    }

    @OnClick(R.id.menu_upload_files)
    public void uploadFiles() {
        actions.uploadFiles();
        dismiss();
    }

    @Override
    protected void onStop() {
        super.onStop();
        unbinder.unbind();
    }
}
