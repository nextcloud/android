package com.nmc.android.ui;

import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.nmc.android.OnDocScanListener;
import com.nmc.android.OnFragmentChangeListener;
import com.nmc.android.adapters.ViewPagerFragmentAdapter;
import com.owncloud.android.R;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;
import io.scanbot.sdk.process.ImageFilterType;

public class EditScannedDocumentFragment extends Fragment {

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

    private Unbinder unbinder;
    private ViewPagerFragmentAdapter pagerFragmentAdapter;
    private OnFragmentChangeListener onFragmentChangeListener;
    private OnDocScanListener onDocScanListener;

    @BindView(R.id.editScannedViewPager)
    ViewPager2 imageViewPager;
    @BindView(R.id.editScanDocCountLabel)
    AppCompatTextView scannedDocCountLabel;
    @BindView(R.id.scanMoreButton)
    AppCompatImageView scanMoreButton;
    @BindView(R.id.cropDocButton)
    AppCompatImageView cropButton;
    @BindView(R.id.filterDocButton)
    AppCompatImageView filterButton;
    @BindView(R.id.rotateDocButton)
    AppCompatImageView rotateButton;
    @BindView(R.id.deleteDocButton)
    AppCompatImageView deleteButton;

    private Bitmap selectedScannedDocFile;
    private int currentSelectedItemIndex;
    private int currentItemIndex;

    private AlertDialog applyFilterDialog;
    private int selectedFilter = 0;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            currentItemIndex = getArguments().getInt(ARG_CURRENT_INDEX, 0);
        }
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
            ((ScanActivity) requireActivity()).updateActionBarTitleAndHomeButtonByString(getResources().getString(R.string.title_edit_scan));

        }
        setHasOptionsMenu(true);
        return inflater.inflate(R.layout.fragment_edit_scanned_document, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        unbinder = ButterKnife.bind(this, view);
        setUpViewPager();
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
        imageViewPager.setAdapter(pagerFragmentAdapter);
        imageViewPager.post(() -> imageViewPager.setCurrentItem(currentItemIndex, false));
        imageViewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                currentSelectedItemIndex = position;
                selectedScannedDocFile = filesList.get(position);
                updateDocCountText(position, filesList.size());
            }
        });

        if (filesList.size() == 1) {
            scannedDocCountLabel.setVisibility(View.INVISIBLE);
        } else {
            scannedDocCountLabel.setVisibility(View.VISIBLE);
            updateDocCountText(currentItemIndex, filesList.size());
        }
    }

    private void updateDocCountText(int position, int totalSize) {
        scannedDocCountLabel.setText(String.format(getResources().getString(R.string.scanned_doc_count),
                                                   position + 1, totalSize));
    }

    @OnClick({R.id.scanMoreButton, R.id.cropDocButton, R.id.filterDocButton, R.id.rotateDocButton, R.id.deleteDocButton})
    void onClickListener(View view) {
        switch (view.getId()) {
            case R.id.scanMoreButton:
                onScanMore(false);
                break;
            case R.id.cropDocButton:
                onFragmentChangeListener.onReplaceFragment(CropScannedDocumentFragment.newInstance(currentSelectedItemIndex),
                                                           ScanActivity.FRAGMENT_CROP_SCAN_TAG, false);
                break;
            case R.id.filterDocButton:
                showApplyFilterDialog();
                break;
            case R.id.rotateDocButton:
                Fragment fragment = pagerFragmentAdapter.getFragment(currentSelectedItemIndex);
                if (fragment instanceof ScanPagerFragment) {
                    ((ScanPagerFragment) fragment).rotate();
                }
                break;
            case R.id.deleteDocButton:
                boolean isRemoved = onDocScanListener.removedScannedDoc(selectedScannedDocFile);
                if (isRemoved) {
                    setUpViewPager();
                }
                break;

        }
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

    private void showApplyFilterDialog() {
        String[] filterArray = getResources().getStringArray(R.array.edit_scan_filter_values);
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.edit_scan_filter_dialog_title)
            .setSingleChoiceItems(filterArray,
                                  selectedFilter,
                                  (dialog, which) -> {
                                      selectedFilter = which;
                                      if (filterArray[which].equalsIgnoreCase(getResources().getString(R.string.edit_scan_filter_none))) {
                                          applyFilter(ImageFilterType.NONE);
                                      } else if (filterArray[which].equalsIgnoreCase(getResources().getString(R.string.edit_scan_filter_pure_binarized))) {
                                          applyFilter(ImageFilterType.PURE_BINARIZED);
                                      } else if (filterArray[which].equalsIgnoreCase(getResources().getString(R.string.edit_scan_filter_color_enhanced))) {
                                          applyFilter(ImageFilterType.COLOR_ENHANCED);
                                      } else if (filterArray[which].equalsIgnoreCase(getResources().getString(R.string.edit_scan_filter_color_document))) {
                                          applyFilter(ImageFilterType.COLOR_DOCUMENT);
                                      } else if (filterArray[which].equalsIgnoreCase(getResources().getString(R.string.edit_scan_filter_grey))) {
                                          applyFilter(ImageFilterType.GRAYSCALE);
                                      } else if (filterArray[which].equalsIgnoreCase(getResources().getString(R.string.edit_scan_filter_b_n_w))) {
                                          applyFilter(ImageFilterType.BLACK_AND_WHITE);
                                      }

                                      dialog.dismiss();
                                  })
            .setOnCancelListener(new DialogInterface.OnCancelListener() {
                @Override
                public void onCancel(DialogInterface dialog) {
                }
            });
        applyFilterDialog = builder.create();
        applyFilterDialog.show();
    }

    private void applyFilter(ImageFilterType imageFilterType) {
        Fragment fragment = pagerFragmentAdapter.getFragment(currentSelectedItemIndex);
        if (fragment instanceof ScanPagerFragment) {
            ((ScanPagerFragment) fragment).applyFilter(imageFilterType);
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
        super.onDestroyView();
        if (unbinder != null) {
            unbinder.unbind();
        }
        if (applyFilterDialog != null && applyFilterDialog.isShowing()) {
            applyFilterDialog.dismiss();
        }

    }

}
