package com.owncloud.android.ui.fragment;

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
import android.graphics.PointF;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;
import android.widget.Toast;

import com.labters.documentscanner.libraries.NativeClass;
import com.owncloud.android.R;
import com.owncloud.android.databinding.FragmentScanDocumentBinding;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.utils.BitmapUtils;

import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;

public class ScanDocumentFragment extends Fragment {
    private static final String TAG = ScanDocumentFragment.class.getName();

    protected final CompositeDisposable disposable;
    private final NativeClass nativeClassOpenCV;
    private final OnProcessImage onProcessImageCallback;
    private boolean inverted;
    private Bitmap originalImage;
    private Bitmap editedImage;
    private Bitmap nonInvertedImage;
    private FragmentScanDocumentBinding binding;

    public ScanDocumentFragment(OnProcessImage onProcessImage, Bitmap originalImage, Bitmap editedImage) {
        this.editedImage = editedImage.copy(editedImage.getConfig(), true);
        this.originalImage = originalImage.copy(originalImage.getConfig(), true);
        nonInvertedImage = editedImage.copy(editedImage.getConfig(), true);
        onProcessImageCallback = onProcessImage;
        nativeClassOpenCV = new NativeClass();
        disposable = new CompositeDisposable();
    }

    // bitmap are too large to be passed by bundle
    public static ScanDocumentFragment newInstance(OnProcessImage onProcessImage,
                                                   Bitmap originalImage, Bitmap editedImage) {
        return new ScanDocumentFragment(onProcessImage, originalImage, editedImage);
    }

    public Bitmap getEditedImage() {
        return editedImage;
    }

    public void forceUpdateImages(Bitmap bitmap) {
        editedImage = BitmapUtils.scaleToFitCenterBitmap(bitmap,
                                                         binding.holderImageCrop.getWidth(),
                                                         binding.holderImageCrop.getHeight());
        originalImage = BitmapUtils.scaleToFitCenterBitmap(bitmap,
                                                           binding.holderImageCrop.getWidth(),
                                                           binding.holderImageCrop.getHeight());
        nonInvertedImage = BitmapUtils.scaleToFitCenterBitmap(bitmap,
                                                              binding.holderImageCrop.getWidth(),
                                                              binding.holderImageCrop.getHeight());
        binding.imageViewScanDocument.setImageBitmap(editedImage);
    }

    private void updateEditedImage(Bitmap bitmap) {
        editedImage = BitmapUtils.scaleToFitCenterBitmap(bitmap,
                                                         binding.holderImageCrop.getWidth(),
                                                         binding.holderImageCrop.getHeight());
        binding.imageViewScanDocument.setImageBitmap(editedImage);
    }

    public boolean isNotInverted() {
        return !inverted;
    }

    public void setInverted(boolean inverted) {
        this.inverted = inverted;
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean("invertedParams", inverted);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            setInverted(savedInstanceState.getBoolean("invertedParams"));
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        binding = FragmentScanDocumentBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // wait view to be ready
        view.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            public void onGlobalLayout() {
                updateEditedImage(editedImage);
                view.getViewTreeObserver().removeOnGlobalLayoutListener(this);
            }
        });
    }

    public void setImageRotation() {
        Bitmap tempBitmap = editedImage.copy(editedImage.getConfig(), true);
        for (int i = 1; i <= 4; i++) {
            MatOfPoint2f point2f = nativeClassOpenCV.getPoint(tempBitmap);
            if (point2f == null) {
                tempBitmap = BitmapUtils.rotateBitmap(tempBitmap, 90 * i);
            } else {
                editedImage = tempBitmap.copy(editedImage.getConfig(), true);
                break;
            }
        }
    }

    public void rotateBitmap(float angle) {
        onProcessImageCallback.onProcessImageStart();
        disposable.add(
            Observable.fromCallable(() -> BitmapUtils.rotateBitmap(editedImage, angle))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe((result) -> {
                    updateEditedImage(result);
                    onProcessImageCallback.onProcessImageEnd();
                }));
    }

    public void trySetPolygonViewToADocument() {
        onProcessImageCallback.onProcessImageStart();
        disposable.add(Observable.fromCallable(() -> {
                           setImageRotation();
                           return Boolean.TRUE;
                       })
                           .subscribeOn(Schedulers.io())
                           .observeOn(AndroidSchedulers.mainThread())
                           .subscribe((result) -> {
                               updateEditedImage(originalImage.copy(originalImage.getConfig(), true));
                               if (inverted) {
                                   invertColorProcess();
                               }
                               Map<Integer, PointF> pointFs;
                               try {
                                   pointFs = getEdgePoints(editedImage);
                                   binding.polygonViewScanDocument.setPoints(pointFs);
                                   binding.polygonViewScanDocument.setVisibility(View.VISIBLE);

                                   int padding = (int) getResources().getDimension(R.dimen.scanPadding);

                                   FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(
                                       editedImage.getWidth() + 2 * padding,
                                       editedImage.getHeight() + 2 * padding
                                   );
                                   layoutParams.gravity = Gravity.CENTER;

                                   binding.polygonViewScanDocument.setLayoutParams(layoutParams);
                                   binding.polygonViewScanDocument.setPointColor(getResources().getColor(R.color.blue));

                               } catch (Exception e) {
                                   Log_OC.e(TAG, "trySetPolygonViewToADocument exception", e);
                                   showCropError();
                               }
                               onProcessImageCallback.onProcessImageEnd();
                           })
                      );
    }

    public void disablePolygonView() {
        binding.polygonViewScanDocument.setVisibility(View.INVISIBLE);
    }

    public void resetImage() {
        setInverted(false);
        updateEditedImage(originalImage.copy(originalImage.getConfig(), true));
    }

    public void cropImageFromPolygon() {
        onProcessImageCallback.onProcessImageStart();
        disposable.add(
            Observable.fromCallable(() -> cropImageProcess(binding.polygonViewScanDocument.getPoints()))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe((result) -> {
                    if (result != null) {
                        updateEditedImage(result);
                    } else {
                        showCropError();
                    }
                    onProcessImageCallback.onProcessImageEnd();
                }));

    }

    private Bitmap cropImageProcess(Map<Integer, PointF> points) {
        try {
            float xRatio = (float) editedImage.getWidth() / binding.imageViewScanDocument.getWidth();
            float yRatio = (float) editedImage.getHeight() / binding.imageViewScanDocument.getHeight();

            float x1 = (Objects.requireNonNull(points.get(0)).x) * xRatio;
            float x2 = (Objects.requireNonNull(points.get(1)).x) * xRatio;
            float x3 = (Objects.requireNonNull(points.get(2)).x) * xRatio;
            float x4 = (Objects.requireNonNull(points.get(3)).x) * xRatio;
            float y1 = (Objects.requireNonNull(points.get(0)).y) * yRatio;
            float y2 = (Objects.requireNonNull(points.get(1)).y) * yRatio;
            float y3 = (Objects.requireNonNull(points.get(2)).y) * yRatio;
            float y4 = (Objects.requireNonNull(points.get(3)).y) * yRatio;
            return nativeClassOpenCV.getScannedBitmap(editedImage, x1, y1, x2, y2, x3, y3, x4, y4);
        } catch (Exception e) {
            return null;
        }
    }

    public void invertColorImage() {
        onProcessImageCallback.onProcessImageStart();
        disposable.add(
            Observable.fromCallable(this::invertColorProcess)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe((result) -> {
                    updateEditedImage(result);
                    setInverted(isNotInverted());
                    onProcessImageCallback.onProcessImageEnd();
                }));
    }

    private Bitmap invertColorProcess() {
        if (isNotInverted()) {
            // backup image
            nonInvertedImage = editedImage.copy(editedImage.getConfig(), true);
            return BitmapUtils.grayscaleBitmap(editedImage);
        } else {
            return nonInvertedImage.copy(nonInvertedImage.getConfig(), true);
        }
    }

    private Map<Integer, PointF> getEdgePoints(Bitmap tempBitmap) {
        MatOfPoint2f point2f = nativeClassOpenCV.getPoint(tempBitmap);
        if (point2f == null) {
            point2f = new MatOfPoint2f();
        }
        Point[] points = point2f.toArray();
        List<PointF> result = new ArrayList<>(points.length);
        for (Point point : points) {
            result.add(new PointF((float) point.x, (float) point.y));
        }

        Map<Integer, PointF> orderedPoints = binding.polygonViewScanDocument.getOrderedPoints(result);
        if (!binding.polygonViewScanDocument.isValidShape(orderedPoints)) {
            Map<Integer, PointF> outlinePoints = new HashMap<>();
            outlinePoints.put(0, new PointF(0, 0));
            outlinePoints.put(1, new PointF(tempBitmap.getWidth(), 0));
            outlinePoints.put(2, new PointF(0, tempBitmap.getHeight()));
            outlinePoints.put(3, new PointF(tempBitmap.getWidth(), tempBitmap.getHeight()));
            orderedPoints = outlinePoints;
        }
        return orderedPoints;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    protected void showCropError() {
        Toast.makeText(getActivity(), R.string.upload_scan_doc_crop_error, Toast.LENGTH_LONG).show();
    }

    public interface OnProcessImage {
        void onProcessImageStart();

        void onProcessImageEnd();
    }
}
