/*
 * Nextcloud Android client application
 *
 * @author TSI-mc
 * Copyright (C) 2022 TSI-mc
 * Copyright (C) 2022 Nextcloud GmbH
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
import com.nextcloud.client.preferences.AppPreferences;
import com.owncloud.android.R;
import com.owncloud.android.databinding.FragmentGalleryBottomSheetBinding;
import com.owncloud.android.ui.activity.FileActivity;

import androidx.core.content.ContextCompat;

public class GalleryFragmentBottomSheetDialog extends BottomSheetDialog {

    //media view states
    private static final int MEDIA_STATE_DEFAULT = 1; //when both photos and videos selected
    private static final int MEDIA_STATE_PHOTOS_ONLY = 2;
    private static final int MEDIA_STATE_VIDEOS_ONLY = 3;

    private FragmentGalleryBottomSheetBinding binding;
    private final GalleryFragmentBottomSheetActions actions;
    private BottomSheetBehavior mBottomBehavior;
    private int currentMediaState = MEDIA_STATE_DEFAULT;

    public GalleryFragmentBottomSheetDialog(FileActivity fileActivity,
                                            GalleryFragmentBottomSheetActions actions) {
        super(fileActivity);
        this.actions = actions;
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
        if (currentMediaState == MEDIA_STATE_PHOTOS_ONLY) {
            binding.tickMarkShowImages.setVisibility(View.VISIBLE);
            binding.tickMarkShowVideo.setVisibility(View.GONE);
        } else if (currentMediaState == MEDIA_STATE_VIDEOS_ONLY) {
            binding.tickMarkShowImages.setVisibility(View.GONE);
            binding.tickMarkShowVideo.setVisibility(View.VISIBLE);
        } else {
            binding.tickMarkShowImages.setVisibility(View.VISIBLE);
            binding.tickMarkShowVideo.setVisibility(View.VISIBLE);
        }
    }

    private void setupClickListener() {

        binding.hideImages.setOnClickListener(v -> {

            if (currentMediaState == MEDIA_STATE_VIDEOS_ONLY) {
                currentMediaState = MEDIA_STATE_DEFAULT;
            } else {
                currentMediaState = MEDIA_STATE_VIDEOS_ONLY;
            }
            notifyStateChange();
            dismiss();
        });

        binding.hideVideo.setOnClickListener(v -> {

            if (currentMediaState == MEDIA_STATE_PHOTOS_ONLY) {
                currentMediaState = MEDIA_STATE_DEFAULT;
            } else {
                currentMediaState = MEDIA_STATE_PHOTOS_ONLY;
            }
            notifyStateChange();
            dismiss();
        });

        binding.selectMediaFolder.setOnClickListener(v -> {

            actions.selectMediaFolder();
            dismiss();
        });

    }

    private void notifyStateChange() {
        setupLayout();
        actions.updateMediaContent(isHideVideos(), isHideImages());
    }

    public boolean isHideImages() {
        return currentMediaState == MEDIA_STATE_VIDEOS_ONLY;
    }

    public boolean isHideVideos() {
        return currentMediaState == MEDIA_STATE_PHOTOS_ONLY;
    }

}
