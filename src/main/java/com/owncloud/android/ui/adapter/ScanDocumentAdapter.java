package com.owncloud.android.ui.adapter;

/*
 * Nextcloud Android client application
 *
 * @author thelittlefireman
 * Copyright (C) 2019 thelittlefireman
 * Copyright (C) 2019 Nextcloud GmbH
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

    private final List<ScanDocumentFragment> mScanDocumentFragmentList;

    private final ScanDocumentFragment.OnProcessImage mOnProcessImage;

    public ScanDocumentAdapter(ScanDocumentFragment.OnProcessImage onProcessImage,
                               @NonNull FragmentManager fragmentManager,
                               @NonNull Lifecycle lifecycle) {
        super(fragmentManager, lifecycle);
        mScanDocumentFragmentList = new ArrayList<>();
        mOnProcessImage = onProcessImage;
    }

    public void addScanImage(Bitmap originalImage, int position) {
        mScanDocumentFragmentList.add(position, ScanDocumentFragment.newInstance(mOnProcessImage, originalImage, originalImage));
        notifyDataSetChanged();
    }

    public void deleteScanImage(int position) {
        mScanDocumentFragmentList.remove(position);
        notifyDataSetChanged();
    }

    public void changeScanImage(Bitmap originalImage, int position) {
        mScanDocumentFragmentList.get(position).forceUpdateImages(originalImage);
        notifyDataSetChanged();
    }

    public List<Bitmap> getEditedImageList() {
        List<Bitmap> bitmapList = new ArrayList<>();
        for (ScanDocumentFragment scanDocumentFragment : mScanDocumentFragmentList) {
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
        return mScanDocumentFragmentList.get(position);
    }

    public ScanDocumentFragment getCurrentFragment(int position) {
        return mScanDocumentFragmentList.get(position);
    }

    @Override
    public int getItemCount() {
        return mScanDocumentFragmentList.size();
    }
}
