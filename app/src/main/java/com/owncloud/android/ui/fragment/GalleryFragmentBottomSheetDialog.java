package com.owncloud.android.ui.fragment;

import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.nextcloud.client.preferences.AppPreferences;
import com.owncloud.android.R;
import com.owncloud.android.databinding.FragmentGalleryBottomSheetBinding;
import com.owncloud.android.ui.activity.FileActivity;

import androidx.core.content.ContextCompat;

public class GalleryFragmentBottomSheetDialog extends BottomSheetDialog {

    private FragmentGalleryBottomSheetBinding binding;
    private final GalleryFragmentBottomSheetActions actions;
    private final AppPreferences preferences;
    private boolean isHideImageClicked;
    private boolean isHideVideoClicked;
    private BottomSheetBehavior mBottomBehavior;

    public GalleryFragmentBottomSheetDialog(FileActivity fileActivity,
                                            GalleryFragmentBottomSheetActions actions,
                                            AppPreferences preferences) {
        super(fileActivity);
        this.actions = actions;
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
        setupLayout();
        setupClickListener();
        mBottomBehavior = BottomSheetBehavior.from((View) binding.getRoot().getParent());
    }

    @Override
    public void onStart() {
        super.onStart();
        mBottomBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
    }

    private void setupLayout() {

        if (!preferences.getHideImageClicked()) {
            binding.hideImagesImageview.setImageDrawable(ContextCompat.getDrawable(getContext(), R.drawable.ic_camera));
            binding.hideImagesTextview.setText(getContext().getResources().getString(R.string.hide_images));
            binding.tickMarkHideImages.setVisibility(View.GONE);
        } else if (preferences.getHideImageClicked()) {
            binding.hideImagesImageview.setImageDrawable(ContextCompat.getDrawable(getContext(), R.drawable.ic_no_camera));
            binding.hideImagesTextview.setText(getContext().getResources().getString(R.string.show_images));
            binding.tickMarkHideImages.setVisibility(View.VISIBLE);
        }
        if (!preferences.getHideVideoClicked()) {
            binding.hideVideoImageView.setImageDrawable(ContextCompat.getDrawable(getContext(), R.drawable.ic_video_camera));
            binding.hideVideoTextview.setText(getContext().getResources().getString(R.string.hide_video));
            binding.tickMarkHideVideo.setVisibility(View.GONE);
        } else if (preferences.getHideVideoClicked()) {
            binding.hideVideoImageView.setImageDrawable(ContextCompat.getDrawable(getContext(), R.drawable.ic_no_video_camera));
            binding.hideVideoTextview.setText(getContext().getResources().getString(R.string.show_video));
            binding.tickMarkHideVideo.setVisibility(View.VISIBLE);
        }
    }

    private void setupClickListener() {

        binding.hideImages.setOnClickListener(v -> {

            if (!preferences.getHideImageClicked() && preferences.getHideVideoClicked()) {
                isHideImageClicked = true;
                isHideVideoClicked = false;
                binding.hideImagesImageview.setImageDrawable(ContextCompat.getDrawable(getContext(), R.drawable.ic_no_camera));
                binding.hideImagesTextview.setText(getContext().getResources().getString(R.string.show_images));
                binding.hideVideoImageView.setImageDrawable(ContextCompat.getDrawable(getContext(), R.drawable.ic_video_camera));
                binding.hideVideoTextview.setText(getContext().getResources().getString(R.string.hide_video));
                binding.tickMarkHideImages.setVisibility(View.VISIBLE);
                binding.tickMarkHideVideo.setVisibility(View.GONE);

            } else if (!preferences.getHideImageClicked() && !preferences.getHideVideoClicked()) {
                isHideImageClicked = true;
                binding.hideImagesImageview.setImageDrawable(ContextCompat.getDrawable(getContext(), R.drawable.ic_no_camera));
                binding.hideImagesTextview.setText(getContext().getResources().getString(R.string.show_images));
                binding.tickMarkHideImages.setVisibility(View.VISIBLE);
            } else if (preferences.getHideImageClicked() && !preferences.getHideVideoClicked()) {
                isHideImageClicked = false;
                binding.hideImagesImageview.setImageDrawable(ContextCompat.getDrawable(getContext(), R.drawable.ic_camera));
                binding.hideImagesTextview.setText(getContext().getResources().getString(R.string.hide_images));
                binding.tickMarkHideImages.setVisibility(View.GONE);

            }

            preferences.setHideImageClicked(isHideImageClicked);
            preferences.setHideVideoClicked(isHideVideoClicked);
            actions.hideImages(preferences.getHideImageClicked());
            dismiss();
        });

        binding.hideVideo.setOnClickListener(v -> {

            if (!preferences.getHideVideoClicked() && !preferences.getHideImageClicked()) {
                isHideVideoClicked = true;
                binding.hideVideoImageView.setImageDrawable(ContextCompat.getDrawable(getContext(), R.drawable.ic_no_video_camera));
                binding.hideVideoTextview.setText(getContext().getResources().getString(R.string.show_video));
                binding.tickMarkHideVideo.setVisibility(View.VISIBLE);
            } else if (!preferences.getHideVideoClicked() && preferences.getHideImageClicked()) {
                isHideVideoClicked = true;
                isHideImageClicked = false;

                binding.hideVideoImageView.setImageDrawable(ContextCompat.getDrawable(getContext(), R.drawable.ic_no_video_camera));
                binding.hideVideoTextview.setText(getContext().getResources().getString(R.string.show_video));
                binding.hideImagesImageview.setImageDrawable(ContextCompat.getDrawable(getContext(), R.drawable.ic_camera));
                binding.hideImagesTextview.setText(getContext().getResources().getString(R.string.hide_images));
                binding.tickMarkHideImages.setVisibility(View.GONE);
                binding.tickMarkHideVideo.setVisibility(View.VISIBLE);

            } else if (preferences.getHideVideoClicked() && !preferences.getHideImageClicked()) {
                isHideVideoClicked = false;
                binding.hideVideoImageView.setImageDrawable(ContextCompat.getDrawable(getContext(), R.drawable.ic_video_camera));
                binding.hideVideoTextview.setText(getContext().getResources().getString(R.string.hide_video));
                binding.tickMarkHideVideo.setVisibility(View.GONE);
            }

            preferences.setHideVideoClicked(isHideVideoClicked);
            preferences.setHideImageClicked(isHideImageClicked);
            actions.hideVideos(preferences.getHideVideoClicked());
            dismiss();
        });

        binding.selectMediaFolder.setOnClickListener(v -> {

            actions.selectMediaFolder();
            dismiss();
        });

    }
}
