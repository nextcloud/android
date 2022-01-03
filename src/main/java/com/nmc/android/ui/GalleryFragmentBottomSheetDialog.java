package com.nmc.android.ui;

import android.os.Bundle;
import android.view.ViewGroup;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.nextcloud.client.account.User;
import com.nextcloud.client.device.DeviceInfo;
import com.nextcloud.client.preferences.AppPreferences;
import com.owncloud.android.databinding.FragmentGalleryBottomSheetBinding;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.ui.activity.FileActivity;
import com.owncloud.android.ui.fragment.GalleryFragmentBottomSheetActions;

public class GalleryFragmentBottomSheetDialog extends BottomSheetDialog {

    private FragmentGalleryBottomSheetBinding binding;
    private final GalleryFragmentBottomSheetActions actions;
    private final FileActivity fileActivity;
    private final DeviceInfo deviceInfo;
    private final User user;
    private final OCFile file;
    private final AppPreferences preferences;

    public GalleryFragmentBottomSheetDialog(FileActivity fileActivity,
                                       GalleryFragmentBottomSheetActions actions,
                                       DeviceInfo deviceInfo,
                                       User user,
                                       OCFile file,
                                       AppPreferences preferences) {
        super(fileActivity);
        this.actions = actions;
        this.fileActivity = fileActivity;
        this.deviceInfo = deviceInfo;
        this.user = user;
        this.file = file;
        this.preferences = preferences;
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = FragmentGalleryBottomSheetBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        if (getWindow() != null) {
            getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        setupClickListener();
    }

    private void setupClickListener() {

        binding.hideImages.setOnClickListener(v -> {

          actions.hideImages();
            dismiss();
        });

        binding.hideVideo.setOnClickListener(v -> {

           actions.hideVideos();
            dismiss();
        });

        binding.selectMediaFolder.setOnClickListener(v -> {

            actions.selectMediaFolder();
            dismiss();
        });

        binding.sortByModifiedDate.setOnClickListener(v -> {

          actions.sortByModifiedDate();
            dismiss();
        });

        binding.sortByCreatedDate.setOnClickListener(v -> {

          actions.sortByCreatedDate();
            dismiss();
        });

        binding.sortByUploadDate.setOnClickListener(v -> {

            actions.sortByUploadDate();
            dismiss();
        });

    }
}
