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

import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.gson.Gson;
import com.nextcloud.client.account.User;
import com.nextcloud.client.device.DeviceInfo;
import com.owncloud.android.R;
import com.owncloud.android.databinding.FileListActionsBottomSheetCreatorBinding;
import com.owncloud.android.databinding.FileListActionsBottomSheetFragmentBinding;
import com.owncloud.android.datamodel.ArbitraryDataProvider;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.files.FileMenuFilter;
import com.owncloud.android.lib.common.Creator;
import com.owncloud.android.lib.common.DirectEditing;
import com.owncloud.android.lib.resources.status.OCCapability;
import com.owncloud.android.ui.activity.FileActivity;
import com.owncloud.android.utils.MimeTypeUtil;
import com.owncloud.android.utils.theme.ThemeColorUtils;
import com.owncloud.android.utils.theme.ThemeDrawableUtils;
import com.owncloud.android.utils.theme.ThemeUtils;

/**
 * FAB menu {@link android.app.Dialog} styled as a bottom sheet for main actions.
 */
public class OCFileListBottomSheetDialog extends BottomSheetDialog {
    private FileListActionsBottomSheetFragmentBinding binding;
    private final OCFileListBottomSheetActions actions;
    private final FileActivity fileActivity;
    private final DeviceInfo deviceInfo;
    private final User user;
    private final OCFile file;

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
        binding = FileListActionsBottomSheetFragmentBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        if (getWindow() != null) {
            getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        int primaryColor = ThemeColorUtils.primaryColor(getContext(), true);
        ThemeDrawableUtils.tintDrawable(binding.menuIconUploadFiles.getDrawable(), primaryColor);
        ThemeDrawableUtils.tintDrawable(binding.menuIconUploadFromApp.getDrawable(), primaryColor);
        ThemeDrawableUtils.tintDrawable(binding.menuIconDirectCameraUpload.getDrawable(), primaryColor);
        ThemeDrawableUtils.tintDrawable(binding.menuIconMkdir.getDrawable(), primaryColor);

        binding.addToCloud.setText(getContext().getResources().getString(R.string.add_to_cloud,
                ThemeUtils.getDefaultDisplayNameForRootFolder(getContext())));

        OCCapability capability = fileActivity.getCapabilities();
        if (capability != null &&
            capability.getRichDocuments().isTrue() &&
            capability.getRichDocumentsDirectEditing().isTrue() &&
            capability.getRichDocumentsTemplatesAvailable().isTrue() &&
            !file.isEncrypted()) {
            binding.templates.setVisibility(View.VISIBLE);
        }

        String json = new ArbitraryDataProvider(getContext().getContentResolver())
            .getValue(user, ArbitraryDataProvider.DIRECT_EDITING);

        if (!json.isEmpty() &&
            !file.isEncrypted()) {
            DirectEditing directEditing = new Gson().fromJson(json, DirectEditing.class);

            if (!directEditing.getCreators().isEmpty()) {
                binding.creatorsContainer.setVisibility(View.VISIBLE);

                for (Creator creator : directEditing.getCreators().values()) {
                    FileListActionsBottomSheetCreatorBinding creatorViewBinding =
                        FileListActionsBottomSheetCreatorBinding.inflate(getLayoutInflater());

                    View creatorView = creatorViewBinding.getRoot();

                    creatorViewBinding.creatorName.setText(
                        String.format(fileActivity.getString(R.string.editor_placeholder),
                                      fileActivity.getString(R.string.create_new),
                                      creator.getName()));

                    creatorViewBinding.creatorThumbnail.setImageDrawable(MimeTypeUtil.getFileTypeIcon(creator.getMimetype(),
                                                                            creator.getExtension(),
                                                                            user,
                                                                            getContext()));

                    creatorView.setOnClickListener(v -> {
                        actions.showTemplate(creator, creatorViewBinding.creatorName.getText().toString());
                        dismiss();
                    });

                    binding.creators.addView(creatorView);
                }
            }
        }

        if (!deviceInfo.hasCamera(getContext())) {
            binding.menuDirectCameraUpload.setVisibility(View.GONE);
        }

        // create rich workspace
        if (FileMenuFilter.isEditorAvailable(getContext().getContentResolver(),
                                             user,
                                             MimeTypeUtil.MIMETYPE_TEXT_MARKDOWN) &&
            file != null && !file.isEncrypted()) {
            // richWorkspace
            // == "": no info set -> show button
            // == null: disabled on server side -> hide button
            // != "": info set -> hide button
            if (file.getRichWorkspace() == null || !"".equals(file.getRichWorkspace())) {
                binding.menuCreateRichWorkspace.setVisibility(View.GONE);
            } else {
                binding.menuCreateRichWorkspace.setVisibility(View.VISIBLE);
            }
        } else {
            binding.menuCreateRichWorkspace.setVisibility(View.GONE);
        }

        setupClickListener();

        setOnShowListener(d ->
                              BottomSheetBehavior.from((View) binding.getRoot().getParent())
                                  .setPeekHeight(binding.getRoot().getMeasuredHeight())
                         );
    }

    private void setupClickListener() {
        binding.menuCreateRichWorkspace.setOnClickListener(v -> {
            actions.createRichWorkspace();
            dismiss();
        });

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

        binding.menuUploadFiles.setOnClickListener(v -> {
            actions.uploadFiles();
            dismiss();
        });

        binding.menuNewDocument.setOnClickListener(v -> {
            actions.newDocument();
            dismiss();
        });

        binding.menuNewSpreadsheet.setOnClickListener(v -> {
            actions.newSpreadsheet();
            dismiss();
        });

        binding.menuNewPresentation.setOnClickListener(v -> {
            actions.newPresentation();
            dismiss();
        });
    }

    @Override
    protected void onStop() {
        super.onStop();
        binding = null;
    }
}
