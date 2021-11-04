package com.nmc.android.ui;

import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.nmc.android.adapters.ViewPagerFragmentAdapter;
import com.nmc.android.interfaces.OnDocScanListener;
import com.nmc.android.interfaces.OnFragmentChangeListener;
import com.owncloud.android.R;
import com.owncloud.android.databinding.FragmentEditScannedDocumentBinding;

import org.jetbrains.annotations.NotNull;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;

public class EditScannedDocumentFragment extends Fragment implements View.OnClickListener {

    private static final String ARG_CURRENT_INDEX = "current_index";
    protected static final String TAG = "EditScannedDocumentFragment";

    public EditScannedDocumentFragment() {
    }

    public static EditScannedDocumentFragment newInstance(int currentIndex) {
        Bundle args = new Bundle();
        args.putInt(ARG_CURRENT_INDEX, currentIndex);
        EditScannedDocumentFragment fragment = new EditScannedDocumentFragment();
        fragment.setArguments(args);
        return fragment;
    }

    private FragmentEditScannedDocumentBinding binding;
    private ViewPagerFragmentAdapter pagerFragmentAdapter;
    private OnFragmentChangeListener onFragmentChangeListener;
    private OnDocScanListener onDocScanListener;

    private Bitmap selectedScannedDocFile;
    private int currentSelectedItemIndex;
    private int currentItemIndex;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            currentItemIndex = getArguments().getInt(ARG_CURRENT_INDEX, 0);
        }
        //Fragment screen orientation normal both portrait and landscape
        requireActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        try {
            onFragmentChangeListener = (OnFragmentChangeListener) context;
            onDocScanListener = (OnDocScanListener) context;
        } catch (Exception ignored) {

        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        if (requireActivity() instanceof ScanActivity) {
            ((ScanActivity) requireActivity()).showHideToolbar(true);
            ((ScanActivity) requireActivity()).showHideDefaultToolbarDivider(true);
            ((ScanActivity) requireActivity()).updateActionBarTitleAndHomeButtonByString(getResources().getString(R.string.title_edit_scan));
        }
        setHasOptionsMenu(true);
        binding = FragmentEditScannedDocumentBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setUpViewPager();

        binding.cropDocButton.setOnClickListener(this);
        binding.scanMoreButton.setOnClickListener(this);
        binding.filterDocButton.setOnClickListener(this);
        binding.rotateDocButton.setOnClickListener(this);
        binding.deleteDocButton.setOnClickListener(this);

    }

    private void setUpViewPager() {
        pagerFragmentAdapter = new ViewPagerFragmentAdapter(this);
        List<Bitmap> filesList = onDocScanListener.getScannedDocs();
        if (filesList.size() == 0) {
            onScanMore(true);
            return;
        }
        for (int i = 0; i < filesList.size(); i++) {
            pagerFragmentAdapter.addFragment(ScanPagerFragment.newInstance(i));
        }
        binding.editScannedViewPager.setAdapter(pagerFragmentAdapter);
        binding.editScannedViewPager.post(() -> binding.editScannedViewPager.setCurrentItem(currentItemIndex, false));
        binding.editScannedViewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                currentSelectedItemIndex = position;
                selectedScannedDocFile = filesList.get(position);
                updateDocCountText(position, filesList.size());
            }
        });

        if (filesList.size() == 1) {
            binding.editScanDocCountLabel.setVisibility(View.INVISIBLE);
        } else {
            binding.editScanDocCountLabel.setVisibility(View.VISIBLE);
            updateDocCountText(currentItemIndex, filesList.size());
        }
    }

    private void updateDocCountText(int position, int totalSize) {
        binding.editScanDocCountLabel.setText(String.format(getResources().getString(R.string.scanned_doc_count),
                                                            position + 1, totalSize));
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.scanMoreButton:
                onScanMore(false);
                break;
            case R.id.cropDocButton:
                onFragmentChangeListener.onReplaceFragment(CropScannedDocumentFragment.newInstance(currentSelectedItemIndex),
                                                           ScanActivity.FRAGMENT_CROP_SCAN_TAG, false);
                break;
            case R.id.filterDocButton:
                showFilterDialog();
                break;
            case R.id.rotateDocButton:
                Fragment fragment = pagerFragmentAdapter.getFragment(currentSelectedItemIndex);
                if (fragment instanceof ScanPagerFragment) {
                    ((ScanPagerFragment) fragment).rotate();
                }
                break;
            case R.id.deleteDocButton:
                boolean isRemoved = onDocScanListener.removedScannedDoc(selectedScannedDocFile, currentSelectedItemIndex);
                if (isRemoved) {
                    setUpViewPager();
                }
                break;

        }
    }

    @Override
    public void onConfigurationChanged(@NonNull @NotNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        setUpViewPager();
    }

    /**
     * check if fragment has to open on + button click or when all scans removed
     *
     * @param isNoItem
     */
    private void onScanMore(boolean isNoItem) {
        onFragmentChangeListener.onReplaceFragment(ScanDocumentFragment.newInstance(isNoItem ? ScanActivity.TAG : TAG),
                                                   ScanActivity.FRAGMENT_SCAN_TAG, false);
    }

    private void showFilterDialog() {
        Fragment fragment = pagerFragmentAdapter.getFragment(currentSelectedItemIndex);
        if (fragment instanceof ScanPagerFragment) {
            ((ScanPagerFragment) fragment).showApplyFilterDialog();
        }
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.edit_scan, menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_save:
                onFragmentChangeListener.onReplaceFragment(SaveScannedDocumentFragment.newInstance(),
                                                           ScanActivity.FRAGMENT_SAVE_SCAN_TAG, false);
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onDestroyView() {
        binding = null;
        super.onDestroyView();
    }
}
