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
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.gson.Gson;
import com.nextcloud.client.account.User;
import com.nextcloud.client.device.DeviceInfo;
import com.owncloud.android.R;
import com.owncloud.android.datamodel.ArbitraryDataProvider;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.files.FileMenuFilter;
import com.owncloud.android.lib.common.Creator;
import com.owncloud.android.lib.common.DirectEditing;
import com.owncloud.android.lib.resources.status.OCCapability;
import com.owncloud.android.ui.activity.FileActivity;
import com.owncloud.android.utils.MimeTypeUtil;
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
    @BindView(R.id.menu_icon_direct_camera_upload)
    public ImageView iconDirectCameraUpload;
    @BindView(R.id.menu_icon_mkdir)
    public ImageView iconMakeDir;

    @BindView(R.id.add_to_cloud)
    public TextView headline;

    @BindView(R.id.templates)
    public View templates;

    @BindView(R.id.creators)
    public LinearLayout creators;

    @BindView(R.id.creators_container)
    public LinearLayout creatorsContainer;

    @BindView(R.id.menu_direct_camera_upload)
    public View cameraView;

    @BindView(R.id.menu_create_rich_workspace)
    public View createRichWorkspace;

    private Unbinder unbinder;
    private OCFileListBottomSheetActions actions;
    private FileActivity fileActivity;
    private DeviceInfo deviceInfo;
    private User user;
    private OCFile file;

    public OCFileListBottomSheetDialog(FileActivity fileActivity,
                                       OCFileListBottomSheetActions actions,
                                       DeviceInfo deviceInfo,
                                       User user,
                                       OCFile file) {
        super(fileActivity);
        this.actions = actions;
        this.fileActivity = fileActivity;
        this.deviceInfo = deviceInfo;
        this.user = user;
        this.file = file;
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

        int primaryColor = ThemeUtils.primaryColor(getContext(), true);
        ThemeUtils.tintDrawable(iconUploadFiles.getDrawable(), primaryColor);
        ThemeUtils.tintDrawable(iconUploadFromApp.getDrawable(), primaryColor);
        ThemeUtils.tintDrawable(iconDirectCameraUpload.getDrawable(), primaryColor);
        ThemeUtils.tintDrawable(iconMakeDir.getDrawable(), primaryColor);

        headline.setText(getContext().getResources().getString(R.string.add_to_cloud,
                ThemeUtils.getDefaultDisplayNameForRootFolder(getContext())));

        OCCapability capability = fileActivity.getCapabilities();
        if (capability.getRichDocuments().isTrue() &&
            capability.getRichDocumentsDirectEditing().isTrue() &&
            android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP &&
            capability.getRichDocumentsTemplatesAvailable().isTrue() &&
            !file.isEncrypted()) {
            templates.setVisibility(View.VISIBLE);
        }

        String json = new ArbitraryDataProvider(getContext().getContentResolver())
            .getValue(user, ArbitraryDataProvider.DIRECT_EDITING);

        if (!json.isEmpty() &&
            android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP &&
            !file.isEncrypted()) {
            DirectEditing directEditing = new Gson().fromJson(json, DirectEditing.class);

            if (!directEditing.getCreators().isEmpty()) {
                creatorsContainer.setVisibility(View.VISIBLE);

                LayoutInflater vi = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);

                for (Creator creator : directEditing.getCreators().values()) {
                    View creatorView = vi.inflate(R.layout.file_list_actions_bottom_sheet_creator, null);
                    ((TextView) creatorView.findViewById(R.id.creator_name)).setText(
                        String.format(fileActivity.getString(R.string.editor_placeholder),
                                      fileActivity.getString(R.string.create_new),
                                      creator.getName()));
                    ImageView thumbnail = creatorView.findViewById(R.id.creator_thumbnail);

                    thumbnail.setImageDrawable(MimeTypeUtil.getFileTypeIcon(creator.getMimetype(),
                                                                            creator.getExtension(),
                                                                            user,
                                                                            getContext()));

                    creatorView.setOnClickListener(v -> {
                        actions.showTemplate(creator);
                        dismiss();
                    });

                    creators.addView(creatorView);
                }
            }
        }

        if (!deviceInfo.hasCamera(getContext())) {
            cameraView.setVisibility(View.GONE);
        }

        // create rich workspace
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP &&
            FileMenuFilter.isEditorAvailable(getContext().getContentResolver(),
                                             user,
                                             MimeTypeUtil.MIMETYPE_TEXT_MARKDOWN) &&
            file != null && !file.isEncrypted()) {
            // richWorkspace
            // == "": no info set -> show button
            // == null: disabled on server side -> hide button
            // != "": info set -> hide button
            if (file.getRichWorkspace() == null || !"".equals(file.getRichWorkspace())) {
                createRichWorkspace.setVisibility(View.GONE);
            } else {
                createRichWorkspace.setVisibility(View.VISIBLE);
            }
        } else {
            createRichWorkspace.setVisibility(View.GONE);
        }

        createRichWorkspace.setOnClickListener(v -> {
            actions.createRichWorkspace();
            dismiss();
        });

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

    @OnClick(R.id.menu_direct_camera_upload)
    public void directCameraUpload() {
        actions.directCameraUpload();
        dismiss();
    }

    @OnClick(R.id.menu_upload_files)
    public void uploadFiles() {
        actions.uploadFiles();
        dismiss();
    }

    @OnClick(R.id.menu_new_document)
    public void newDocument() {
        actions.newDocument();
        dismiss();
    }

    @OnClick(R.id.menu_new_spreadsheet)
    public void newSpreadsheet() {
        actions.newSpreadsheet();
        dismiss();
    }

    @OnClick(R.id.menu_new_presentation)
    public void newPresentation() {
        actions.newPresentation();
        dismiss();
    }
    @Override
    protected void onStop() {
        super.onStop();
        unbinder.unbind();
    }
}
