package com.nmc.android.ui;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;

import com.nmc.android.utils.FileUtils;
import com.owncloud.android.R;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.os.HandlerCompat;
import androidx.fragment.app.Fragment;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import io.scanbot.sdk.ScanbotSDK;
import io.scanbot.sdk.process.FilterOperation;
import io.scanbot.sdk.process.ImageFilterType;
import io.scanbot.sdk.ui.EditPolygonImageView;

public class ScanPagerFragment extends Fragment {

    private static final String ARG_SCANNED_DOC_PATH = "scanned_doc_path";

    public ScanPagerFragment() {
    }

    public static ScanPagerFragment newInstance(File file) {

        Bundle args = new Bundle();
        args.putString(ARG_SCANNED_DOC_PATH, file.getPath());

        ScanPagerFragment fragment = new ScanPagerFragment();
        fragment.setArguments(args);
        return fragment;
    }

    private Unbinder unbinder;
    private String scannedDocPath;
    @BindView(R.id.editScannedImageView) EditPolygonImageView editPolygonImageView;
    @BindView(R.id.editScanImageProgressBar) ProgressBar progressBar;

    private ScanbotSDK scanbotSDK;
    private Bitmap originalBitmap;
    private Bitmap previewBitmap;

    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final Handler handler = HandlerCompat.createAsync(Looper.getMainLooper());

    private long lastRotationEventTs = 0L;
    private int rotationDegrees = 0;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            scannedDocPath = getArguments().getString(ARG_SCANNED_DOC_PATH);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        if (requireActivity() instanceof ScanActivity) {
            scanbotSDK = ((ScanActivity) requireActivity()).getScanbotSDK();
        }
        return inflater.inflate(R.layout.item_scanned_doc, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        unbinder = ButterKnife.bind(this, view);
        File file = new File(scannedDocPath);
        originalBitmap = FileUtils.convertFileToBitmap(file);
        // previewBitmap = ScanBotSdkUtils.resizeForPreview(originalBitmap);
        loadImage();
    }

    private void loadImage() {
        if (previewBitmap != null) {
            editPolygonImageView.setImageBitmap(previewBitmap);
            //Glide.with(requireContext()).load(previewBitmap).asBitmap().into(imageView);
        } else {
            //Glide.with(requireContext()).load(originalBitmap).asBitmap().into(imageView);
            editPolygonImageView.setImageBitmap(originalBitmap);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (unbinder != null) {
            unbinder.unbind();
        }
    }

    public void rotate() {
        if (System.currentTimeMillis() - lastRotationEventTs < 350) {
            return;
        }
        rotationDegrees += 90;
        editPolygonImageView.rotateClockwise();
        lastRotationEventTs = System.currentTimeMillis();
    }

    public void applyFilter(ImageFilterType imageFilterType) {
        progressBar.setVisibility(View.VISIBLE);
        executorService.execute(() -> {
            previewBitmap = scanbotSDK.imageProcessor().process(originalBitmap,
                                                                new ArrayList<>(Collections.singletonList(new FilterOperation(imageFilterType))), false);
            handler.post(() -> {
                progressBar.setVisibility(View.GONE);
                loadImage();
            });
        });
    }
}
