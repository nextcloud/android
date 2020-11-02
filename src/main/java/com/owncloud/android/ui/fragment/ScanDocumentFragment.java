package com.owncloud.android.ui.fragment;

import android.graphics.Bitmap;
import android.graphics.PointF;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Toast;

import com.labters.documentscanner.base.CropperErrorType;
import com.labters.documentscanner.helpers.ScannerConstants;
import com.labters.documentscanner.libraries.NativeClass;
import com.labters.documentscanner.libraries.PolygonView;
import com.owncloud.android.R;
import com.owncloud.android.utils.BitmapUtils;

import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;

public class ScanDocumentFragment extends Fragment {
    private final NativeClass mNativeClassOpenCV;

    private boolean isInverted;

    @BindView(R.id.holderImageCrop)
    FrameLayout mHolderImageCrop;
    private Bitmap mOriginalImage;

    private Unbinder unbinder;
    private final OnProcessImage mOnProcessImageCallback;

    private Bitmap mEditedImage;
    protected CompositeDisposable mDisposable;
    @BindView(R.id.imageViewScanDocument)
    ImageView mImageView;
    @BindView(R.id.polygonViewScanDocument)
    PolygonView mPolygonView;
    private Bitmap mNonInvertedImage;

    public ScanDocumentFragment(OnProcessImage onProcessImage, Bitmap originalImage, Bitmap editedImage) {
        mEditedImage = editedImage.copy(editedImage.getConfig(), true);
        mOriginalImage = originalImage.copy(originalImage.getConfig(), true);
        mNonInvertedImage = editedImage.copy(editedImage.getConfig(), true);
        mOnProcessImageCallback = onProcessImage;
        mNativeClassOpenCV = new NativeClass();
        mDisposable = new CompositeDisposable();
    }

    // bitmap are too large to be passed by bundle
    public static ScanDocumentFragment newInstance(OnProcessImage onProcessImage,
                                                   Bitmap originalImage, Bitmap editedImage) {
        return new ScanDocumentFragment(onProcessImage, originalImage, editedImage);
    }

    public Bitmap getEditedImage() {
        return mEditedImage;
    }

    public void forceUpdateImages(Bitmap bitmap) {
        mEditedImage = BitmapUtils.scaleToFitCenterBitmap(bitmap, mHolderImageCrop.getWidth(), mHolderImageCrop.getHeight());
        mOriginalImage = BitmapUtils.scaleToFitCenterBitmap(bitmap, mHolderImageCrop.getWidth(), mHolderImageCrop.getHeight());
        mNonInvertedImage = BitmapUtils.scaleToFitCenterBitmap(bitmap, mHolderImageCrop.getWidth(), mHolderImageCrop.getHeight());
        mImageView.setImageBitmap(mEditedImage);
    }

    private void updateEditedImage(Bitmap bitmap) {
        mEditedImage = BitmapUtils.scaleToFitCenterBitmap(bitmap, mHolderImageCrop.getWidth(), mHolderImageCrop.getHeight());
        mImageView.setImageBitmap(mEditedImage);
    }

    public boolean isInverted() {
        return isInverted;
    }

    public void setInverted(boolean inverted) {
        isInverted = inverted;
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean("invertedParams", isInverted);
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
        return inflater.inflate(R.layout.fragment_scan_document, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        unbinder = ButterKnife.bind(this, view);
        // wait view to be ready
        view.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            public void onGlobalLayout() {
                updateEditedImage(mEditedImage);
                view.getViewTreeObserver().removeOnGlobalLayoutListener(this);
            }
        });
    }

    public void setImageRotation() {
        Bitmap tempBitmap = mEditedImage.copy(mEditedImage.getConfig(), true);
        for (int i = 1; i <= 4; i++) {
            MatOfPoint2f point2f = mNativeClassOpenCV.getPoint(tempBitmap);
            if (point2f == null) {
                tempBitmap = BitmapUtils.rotateBitmap(tempBitmap, 90 * i);
            } else {
                mEditedImage = tempBitmap.copy(mEditedImage.getConfig(), true);
                break;
            }
        }
    }

    public void rotateBitmap(float angle) {
        mOnProcessImageCallback.onProcessImageStart();
        mDisposable.add(
            Observable.fromCallable(() -> BitmapUtils.rotateBitmap(mEditedImage, angle))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe((result) -> {
                    updateEditedImage(result);
                    mOnProcessImageCallback.onProcessImageEnd();
                }));
    }

    public void trySetPolygonViewToADocument() {
        mOnProcessImageCallback.onProcessImageStart();
        mDisposable.add(Observable.fromCallable(() -> {
                            setImageRotation();
                            return Boolean.TRUE;
                        })
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe((result) -> {
                                updateEditedImage(mOriginalImage.copy(mOriginalImage.getConfig(), true));
                                if (isInverted) {
                                    invertColorProcess();
                                }
                                Map<Integer, PointF> pointFs;
                                try {
                                    pointFs = getEdgePoints(mEditedImage);
                                    mPolygonView.setPoints(pointFs);
                                    mPolygonView.setVisibility(View.VISIBLE);

                                    int padding = (int) getResources().getDimension(R.dimen.scanPadding);

                                    FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(mEditedImage.getWidth() + 2 * padding, mEditedImage.getHeight() + 2 * padding);
                                    layoutParams.gravity = Gravity.CENTER;

                                    mPolygonView.setLayoutParams(layoutParams);
                                    mPolygonView.setPointColor(getResources().getColor(R.color.blue));

                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                                mOnProcessImageCallback.onProcessImageEnd();
                            })
                       );
    }

    public void disablePolygonView() {
        mPolygonView.setVisibility(View.INVISIBLE);
    }

    public void resetImage() {
        setInverted(false);
        updateEditedImage(mOriginalImage.copy(mOriginalImage.getConfig(), true));
    }

    public void cropImageFromPolygon() {
        mOnProcessImageCallback.onProcessImageStart();
        mDisposable.add(
            Observable.fromCallable(() -> cropImageProcess(mPolygonView.getPoints()))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe((result) -> {
                    updateEditedImage(result);
                    mOnProcessImageCallback.onProcessImageEnd();
                }));

    }

    private Bitmap cropImageProcess(Map<Integer, PointF> points) {
        try {

            float xRatio = (float) mEditedImage.getWidth() / mImageView.getWidth();
            float yRatio = (float) mEditedImage.getHeight() / mImageView.getHeight();

            float x1 = (Objects.requireNonNull(points.get(0)).x) * xRatio;
            float x2 = (Objects.requireNonNull(points.get(1)).x) * xRatio;
            float x3 = (Objects.requireNonNull(points.get(2)).x) * xRatio;
            float x4 = (Objects.requireNonNull(points.get(3)).x) * xRatio;
            float y1 = (Objects.requireNonNull(points.get(0)).y) * yRatio;
            float y2 = (Objects.requireNonNull(points.get(1)).y) * yRatio;
            float y3 = (Objects.requireNonNull(points.get(2)).y) * yRatio;
            float y4 = (Objects.requireNonNull(points.get(3)).y) * yRatio;
            return mNativeClassOpenCV.getScannedBitmap(mEditedImage, x1, y1, x2, y2, x3, y3, x4, y4);
        } catch (Exception e) {
            showError(CropperErrorType.CROP_ERROR);
            return null;
        }
    }

    public void invertColorImage() {
        mOnProcessImageCallback.onProcessImageStart();
        mDisposable.add(
            Observable.fromCallable(this::invertColorProcess)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe((result) -> {
                    updateEditedImage(result);
                    setInverted(!isInverted());
                    mOnProcessImageCallback.onProcessImageEnd();
                }));
    }

    private Bitmap invertColorProcess() {
        if (!isInverted()) {
            // backup image
            mNonInvertedImage = mEditedImage.copy(mEditedImage.getConfig(), true);
            return BitmapUtils.grayscaleBitmap(mEditedImage);
        } else {
            return mNonInvertedImage.copy(mNonInvertedImage.getConfig(), true);
        }
    }

    private Map<Integer, PointF> getEdgePoints(Bitmap tempBitmap) {
        MatOfPoint2f point2f = mNativeClassOpenCV.getPoint(tempBitmap);
        if (point2f == null) {
            point2f = new MatOfPoint2f();
        }
        List<Point> points = Arrays.asList(point2f.toArray());
        List<PointF> result = new ArrayList<>();
        for (int i = 0; i < points.size(); i++) {
            result.add(new PointF(((float) points.get(i).x), ((float) points.get(i).y)));
        }

        Map<Integer, PointF> orderedPoints = mPolygonView.getOrderedPoints(result);
        if (!mPolygonView.isValidShape(orderedPoints)) {
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
        unbinder.unbind();
    }

    protected void showError(CropperErrorType errorType) {
        getActivity().runOnUiThread(() -> {
            switch (errorType) {
                case CROP_ERROR:
                    Toast.makeText(getActivity(), ScannerConstants.cropError, Toast.LENGTH_LONG).show();
                    break;
            }
        });

    }

    public interface OnProcessImage {
        void onProcessImageStart();

        void onProcessImageEnd();
    }
}
