package com.owncloud.android.ui.adapter;

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
        this.notifyDataSetChanged();
    }

    public void deleteScanImage(int position) {
        mScanDocumentFragmentList.remove(position);
        this.notifyDataSetChanged();
    }

    public void changeScanImage(Bitmap originalImage, int position) {
        deleteScanImage(position);
        addScanImage(originalImage, position);
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
