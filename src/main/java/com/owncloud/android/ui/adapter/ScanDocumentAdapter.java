package com.owncloud.android.ui.adapter;

/*
 * Nextcloud Android client application
 *
 * @author thelittlefireman
 * Copyright (C) 2020 thelittlefireman
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

import android.graphics.Bitmap;

import com.owncloud.android.ui.fragment.ScanDocumentFragment;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.Lifecycle;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.adapter.FragmentStateAdapter;

public class ScanDocumentAdapter extends FragmentStateAdapter {

    private final List<ScanDocumentFragment> scanDocumentFragmentList;

    private final ScanDocumentFragment.OnProcessImage mOnProcessImage;

    public ScanDocumentAdapter(ScanDocumentFragment.OnProcessImage onProcessImage,
                               @NonNull FragmentManager fragmentManager,
                               @NonNull Lifecycle lifecycle) {
        super(fragmentManager, lifecycle);
        scanDocumentFragmentList = new ArrayList<>();
        mOnProcessImage = onProcessImage;
    }

    public void addScanImage(Bitmap originalImage, int position) {
        scanDocumentFragmentList.add(position, ScanDocumentFragment.newInstance(mOnProcessImage, originalImage, originalImage));
        notifyDataSetChanged();
    }

    public void deleteScanImage(int position) {
        scanDocumentFragmentList.remove(position);
        notifyDataSetChanged();
    }

    public void changeScanImage(Bitmap originalImage, int position) {
        scanDocumentFragmentList.get(position).forceUpdateImages(originalImage);
        notifyDataSetChanged();
    }

    public List<Bitmap> getEditedImageList() {
        List<Bitmap> bitmapList = new ArrayList<>();
        for (ScanDocumentFragment scanDocumentFragment : scanDocumentFragmentList) {
            bitmapList.add(scanDocumentFragment.getEditedImage());
        }
        return bitmapList;
    }

    @Override
    public void onAttachedToRecyclerView(@NonNull RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        return scanDocumentFragmentList.get(position);
    }

    public ScanDocumentFragment getCurrentFragment(int position) {
        return scanDocumentFragmentList.get(position);
    }

    @Override
    public int getItemCount() {
        return scanDocumentFragmentList.size();
    }
}
