package com.owncloud.android.ui.activity;

/*
 * *
 *  * Created by Ali YÜCE on 3/2/20 11:18 PM
 *  * Edited by thelittlefireman
 *  * https://github.com/mayuce/
 *  * Copyright (c) 2020 . All rights reserved.
 *  * Last modified 3/2/20 11:10 PM
 *
 */

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.os.Environment;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.labters.documentscanner.base.CropperErrorType;
import com.labters.documentscanner.base.DocumentScanActivity;
import com.labters.documentscanner.helpers.ScannerConstants;
import com.labters.documentscanner.libraries.PolygonView;
import com.owncloud.android.R;
import com.owncloud.android.databinding.EditBoxDialogBinding;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.lib.resources.files.FileUtils;
import com.owncloud.android.ui.helpers.FileOperationsHelper;
import com.owncloud.android.utils.DisplayUtils;
import com.owncloud.android.utils.ThemeUtils;

import org.lukhnos.nnio.file.Files;
import org.lukhnos.nnio.file.Paths;

import java.io.File;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import androidx.appcompat.app.AlertDialog;
import androidx.constraintlayout.widget.ConstraintLayout;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

/**
 * TODO Add a button to retake picture without loosing previous scan
 */
public class ScanDocActivity extends DocumentScanActivity {

    public static final int RESULT_OK_AND_ADD_ADD_ANOTHER_SCAN_TO_DOC = 11;
    public static final String SCAN_DOC_ACTIVITY_RESULT_PDFNAME = "SCAN_DOC_ACTIVITY_RESULT_PDFNAME";

    private FrameLayout holderImageCrop;
    private ImageView imageView;
    private PolygonView polygonView;
    private boolean isInverted;
    private ProgressBar progressBar;
    private Bitmap cropImage;
    private Unbinder unbinder;

    private String pdfName = FileOperationsHelper.getScanDocName();

    @BindView(R.id.btnClose)
    Button btnClose;

    @BindView(R.id.btnValidate)
    Button btnValidate;

    @BindView(R.id.btnValidateAndAddAnOtherScanToDoc)
    Button btnValidateAndAddAnOtherScanToDoc;

    @BindView(R.id.rlContainer)
    ConstraintLayout rlContainer;

    @OnClick(R.id.ivRename)
    void buttonRenameClick(){
        int accentColor = ThemeUtils.primaryAccentColor(this);

        // Inflate the layout for the dialog
        LayoutInflater inflater = getLayoutInflater();
        EditBoxDialogBinding binding = EditBoxDialogBinding.inflate(inflater, null, false);
        View view = binding.getRoot();

        // Setup layout
        String currentName = pdfName;
        EditText inputText = binding.userInput;
        inputText.setHighlightColor(ThemeUtils.primaryColor(this));
        inputText.setText(currentName);
        ThemeUtils.themeEditText(this, inputText, false);
        int selectionStart = 0;
        int selectionEnd = currentName.length();
        inputText.setSelection(Math.min(selectionStart, selectionEnd), Math.max(selectionStart, selectionEnd));
        inputText.requestFocus();
        inputText.getBackground().setColorFilter(accentColor, PorterDuff.Mode.SRC_ATOP);

        DialogInterface.OnClickListener clickListener = (dialog, which) -> {
            if (which == AlertDialog.BUTTON_POSITIVE) {
                String newFileName = "";

                if (binding.userInput.getText() != null) {
                    newFileName = binding.userInput.getText().toString().trim();
                }

                if (TextUtils.isEmpty(newFileName)) {
                    DisplayUtils.showSnackMessage(this, R.string.filename_empty);
                    return;
                }

                if (!FileUtils.isValidName(newFileName)) {
                    DisplayUtils.showSnackMessage(this, R.string.filename_forbidden_charaters_from_server);

                    return;
                }

                pdfName = newFileName;
            }
        };

        // Build the dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(view)
            .setPositiveButton(R.string.file_rename, clickListener)
            .setNegativeButton(R.string.common_cancel, clickListener)
            .setTitle(R.string.rename_dialog_title);
        Dialog d = builder.create();

        Window window = d.getWindow();
        if (window != null) {
            window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        }
        d.show();
    }

    @OnClick(R.id.btnValidate)
    void buttonValidateClick(){
        showProgressBar();
        disposable.add(
            Observable.fromCallable(() -> {
                cropImage = getCroppedImage();
                if (cropImage == null) {
                    return Boolean.FALSE;
                }
                if (ScannerConstants.saveStorage) {
                    saveToInternalStorage(cropImage);
                }
                return Boolean.FALSE;
            })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe((result) -> {
                    hideProgressBar();
                    if (cropImage != null) {
                        ScannerConstants.selectedImageBitmap = cropImage;
                        Intent resultIntent = new Intent();
                        resultIntent.putExtra(SCAN_DOC_ACTIVITY_RESULT_PDFNAME,pdfName);
                        setResult(RESULT_OK, resultIntent);
                        finish();
                    }
                })
                      );


    }

    @OnClick(R.id.ivRebase)
    void buttonClickRebase(){
        cropImage = ScannerConstants.selectedImageBitmap.copy(ScannerConstants.selectedImageBitmap.getConfig(), true);
        isInverted = false;
        startCropping();
    }

    @OnClick(R.id.btnClose)
    void buttonClickClose(){
        finish();
    }

    @OnClick(R.id.ivInvert)
    void buttonInvertColor(){
        showProgressBar();
        disposable.add(
            Observable.fromCallable(() -> {
                invertColor();
                return Boolean.FALSE;
            })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe((result) -> {
                    hideProgressBar();
                    Bitmap scaledBitmap = scaledBitmap(cropImage, holderImageCrop.getWidth(), holderImageCrop.getHeight());
                    imageView.setImageBitmap(scaledBitmap);
                })
                      );
    }

    @OnClick(R.id.ivRotate)
    void buttonRotateClick(){
        rotateImage();
    }

    private void rotateImage(){
        showProgressBar();
        disposable.add(
            Observable.fromCallable(() -> {
                if (isInverted) {
                    invertColor();
                }
                cropImage = rotateBitmap(cropImage, 90);
                return Boolean.FALSE;
            })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe((result) -> {
                    hideProgressBar();
                    startCropping();
                })
                      );
    }

    @OnClick(R.id.btnValidateAndAddAnOtherScanToDoc)
    void onAddOtherScanDocClick(){
        showProgressBar();
        disposable.add(
            Observable.fromCallable(() -> {
                cropImage = getCroppedImage();
                if (cropImage == null) {
                    return Boolean.FALSE;
                }
                if (ScannerConstants.saveStorage) {
                    saveToInternalStorage(cropImage);
                }
                return Boolean.FALSE;
            })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe((result) -> {
                    hideProgressBar();
                    if (cropImage != null) {
                        ScannerConstants.selectedImageBitmap = cropImage;
                        Intent resultIntent = new Intent();
                        resultIntent.putExtra(SCAN_DOC_ACTIVITY_RESULT_PDFNAME,pdfName);
                        setResult(RESULT_OK_AND_ADD_ADD_ANOTHER_SCAN_TO_DOC, resultIntent);
                        finish();
                    }
                })
                      );

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan_doc);
        unbinder = ButterKnife.bind(this);
        cropImage = ScannerConstants.selectedImageBitmap;
        isInverted = false;
        if (ScannerConstants.selectedImageBitmap != null) {
            initView();
        }
        else {
            Toast.makeText(this, ScannerConstants.imageError, Toast.LENGTH_LONG).show();
            finish();
        }
    }

    @Override
    protected FrameLayout getHolderImageCrop() {
        return holderImageCrop;
    }

    @Override
    protected ImageView getImageView() {
        return imageView;
    }

    @Override
    protected PolygonView getPolygonView() {
        return polygonView;
    }

    @Override
    protected void showProgressBar() {
        setViewInteract(rlContainer, false);
        progressBar.setVisibility(View.VISIBLE);
    }

    @Override
    protected void hideProgressBar() {
        setViewInteract(rlContainer, true);
        progressBar.setVisibility(View.GONE);
    }

    @Override
    protected void showError(CropperErrorType errorType) {
        switch (errorType) {
            case CROP_ERROR:
                Toast.makeText(this, ScannerConstants.cropError, Toast.LENGTH_LONG).show();
                break;
        }
    }

    @Override
    protected Bitmap getBitmapImage() {
        return cropImage;
    }

    private void setViewInteract(View view, boolean canDo) {
        view.setEnabled(canDo);
        if (view instanceof ViewGroup) {
            for (int i = 0; i < ((ViewGroup) view).getChildCount(); i++) {
                setViewInteract(((ViewGroup) view).getChildAt(i), canDo);
            }
        }
    }

    // TODO edit style
    private void initView() {
        holderImageCrop = findViewById(R.id.holderImageCrop);
        imageView = findViewById(R.id.imageView);
        btnValidate.setText(getString(R.string.common_ok));
        btnValidateAndAddAnOtherScanToDoc.setText("+");
        btnClose.setText(getString(R.string.common_cancel));
        polygonView = findViewById(R.id.polygonView);
        progressBar = findViewById(R.id.progressBar);
        if (progressBar.getIndeterminateDrawable() != null && ScannerConstants.progressColor != null) {
            progressBar.getIndeterminateDrawable().setColorFilter(Color.parseColor(ScannerConstants.progressColor), android.graphics.PorterDuff.Mode.MULTIPLY);
        }
        else if (progressBar.getProgressDrawable() != null && ScannerConstants.progressColor != null) {
            progressBar.getProgressDrawable().setColorFilter(Color.parseColor(ScannerConstants.progressColor), android.graphics.PorterDuff.Mode.MULTIPLY);
        }
        btnValidate.setBackgroundColor(Color.parseColor(ScannerConstants.cropColor));
        btnClose.setBackgroundColor(Color.parseColor(ScannerConstants.backColor));
        startCropping();
        if(cropImage.getWidth() > cropImage.getHeight()){
            rotateImage();
        }
    }

    private void invertColor() {
        if (!isInverted) {
            Bitmap bmpMonochrome = Bitmap.createBitmap(cropImage.getWidth(), cropImage.getHeight(), Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bmpMonochrome);
            ColorMatrix ma = new ColorMatrix();
            ma.setSaturation(0);
            Paint paint = new Paint();
            paint.setColorFilter(new ColorMatrixColorFilter(ma));
            canvas.drawBitmap(cropImage, 0, 0, paint);
            cropImage = bmpMonochrome.copy(bmpMonochrome.getConfig(), true);
        } else {
            cropImage = cropImage.copy(cropImage.getConfig(), true);
        }
        isInverted = !isInverted;
    }

    private String saveToInternalStorage(Bitmap bitmapImage) {
        File directory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "cropped_" + timeStamp + ".png";
        File mypath = new File(directory, imageFileName);
        OutputStream fos = null;
        try {
            fos = Files.newOutputStream(Paths.get(mypath.getAbsolutePath()));
            bitmapImage.compress(Bitmap.CompressFormat.PNG, 100, fos);
        } catch (Exception e) {
            Log_OC.e(this,"saveToInternalStorage",e);
        } finally {
            try {
                fos.close();
            } catch (Exception e) {
                Log_OC.e(this,"saveToInternalStorage fos close",e);
            }
        }
        return directory.getAbsolutePath();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unbinder.unbind();
    }
}
