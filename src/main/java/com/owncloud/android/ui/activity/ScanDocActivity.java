package com.owncloud.android.ui.activity;

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

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.RectF;
import android.graphics.pdf.PdfDocument;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;

import com.owncloud.android.R;
import com.owncloud.android.databinding.ActivityScanDocBinding;
import com.owncloud.android.databinding.EditBoxDialogBinding;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.lib.resources.files.FileUtils;
import com.owncloud.android.ui.adapter.ScanDocumentAdapter;
import com.owncloud.android.ui.fragment.ScanDocumentFragment;
import com.owncloud.android.ui.helpers.FileOperationsHelper;
import com.owncloud.android.utils.BitmapUtils;
import com.owncloud.android.utils.DisplayUtils;
import com.owncloud.android.utils.ThemeUtils;

import org.lukhnos.nnio.file.Files;
import org.lukhnos.nnio.file.Paths;

import java.io.IOException;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import static com.owncloud.android.ui.activity.FileActivity.REQUEST_CODE__LAST_SHARED;

public class ScanDocActivity extends AppCompatActivity implements ScanDocumentFragment.OnProcessImage {

    public static final int REQUEST_CODE__TAKE_PICTURE_FROM_CAMERA = REQUEST_CODE__LAST_SHARED + 1;
    public static final int REQUEST_CODE__RETAKE_PICTURE_FROM_CAMERA = REQUEST_CODE__LAST_SHARED + 2;
    public static final String SCAN_DOC_ACTIVITY_RESULT_PDFNAME = "SCAN_DOC_ACTIVITY_RESULT_PDFNAME";
    public static final String SAVED_STATE_BITMAP_SIZE = "SAVED_STATE_BITMAP_SIZE";

    private static final int PAGE_WIDTH = 960;
    private static final int PAGE_HEIGHT = 1280;

    int mCurrentPosition;
    private ActivityScanDocBinding binding;
    private String pdfName = FileOperationsHelper.getScanDocName();
    private ScanDocumentAdapter scanDocumentAdapter;

    public static void startScanActivityForResult(Activity activity, int requestCode) {
        Intent action = new Intent(activity, ScanDocActivity.class);
        activity.startActivityForResult(action, requestCode);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityScanDocBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        mCurrentPosition = -1;

        scanDocumentAdapter = new ScanDocumentAdapter(this, this, getSupportFragmentManager(), getLifecycle());
        binding.viewPagerScanDocument.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                super.onPageScrolled(position, positionOffset, positionOffsetPixels);
                mCurrentPosition = position;
                updateNextPrevious();
            }

            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
            }

            @Override
            public void onPageScrollStateChanged(int state) {
                super.onPageScrollStateChanged(state);
            }
        });
        binding.viewPagerScanDocument.setAdapter(scanDocumentAdapter);
        binding.viewPagerScanDocument.setUserInputEnabled(true);

        ThemeUtils.colorPrimaryButton(binding.btnValidate, this);
        binding.progressBarScanDocument.getIndeterminateDrawable().setColorFilter(
            ThemeUtils.primaryAccentColor(this),
            PorterDuff.Mode.SRC_IN);
        binding.btnClose.setTextColor(ThemeUtils.primaryColor(this, true));

        setupClickListener();
        int oldBitmapSize = savedInstanceState != null ? savedInstanceState.getInt(SAVED_STATE_BITMAP_SIZE) : 0;
        if (oldBitmapSize != 0) {
            // recreation of the activity (rotation, etc ...)
            scanDocumentAdapter.reloadBitmapsFromCacheDirectory(oldBitmapSize);
        } else {
            // new activity => no bitmap
            FileOperationsHelper
                .takePictureFromCamera(this, REQUEST_CODE__TAKE_PICTURE_FROM_CAMERA);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        // Save UI state changes to the savedInstanceState.
        // This bundle will be passed to onCreate if the process is
        // killed and restarted or on configuration changed (rotation)
        savedInstanceState.putInt(SAVED_STATE_BITMAP_SIZE, scanDocumentAdapter.getItemCount());
    }

    private void setupClickListener() {
        binding.ivDeletePage.setOnClickListener(v -> onDeletePageClick());
        binding.ivRetakePicture.setOnClickListener(v -> onRetakePictureClick());
        binding.ivRename.setOnClickListener(v -> buttonRenameClick());
        binding.btnValidate.setOnClickListener(v -> buttonValidateClick());
        binding.ivCrop.setOnClickListener(v -> buttonClickShowCropButtons());
        binding.ivCancelCrop.setOnClickListener(v -> buttonClickHideCropButtons());
        binding.ivValidateCrop.setOnClickListener(v -> buttonClickValidateCrop());
        binding.ivReset.setOnClickListener(v -> buttonClickReset());
        binding.btnClose.setOnClickListener(v -> buttonClickClose());
        binding.ivInvert.setOnClickListener(v -> buttonInvertColor());
        binding.ivRotate.setOnClickListener(v -> buttonRotateClick());
        binding.ivAddAnOtherScanToDoc.setOnClickListener(v -> onAddOtherScanDocClick());
        binding.ivNextScanDoc.setOnClickListener(v -> onNextScanDocClick());
        binding.ivPreviousScanDoc.setOnClickListener(v -> onPreviousScanDocClick());
    }

    void onDeletePageClick() {
        scanDocumentAdapter.deleteScanImage(mCurrentPosition);
        if (scanDocumentAdapter.getItemCount() == 0) {
            FileOperationsHelper
                .takePictureFromCamera(this, REQUEST_CODE__TAKE_PICTURE_FROM_CAMERA);
        }
    }

    void onRetakePictureClick() {
        FileOperationsHelper
            .takePictureFromCamera(this, REQUEST_CODE__RETAKE_PICTURE_FROM_CAMERA);
    }

    void buttonRenameClick() {
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
        AlertDialog d = builder.create();

        Window window = d.getWindow();
        if (window != null) {
            window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        }
        d.show();
        d.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(accentColor);
        d.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(accentColor);
    }

    void buttonValidateClick() {
        // change to a static pdf file and a rename string
        createPDFDocument();
        Intent resultIntent = new Intent();
        resultIntent.putExtra(SCAN_DOC_ACTIVITY_RESULT_PDFNAME, pdfName);
        setResult(RESULT_OK, resultIntent);
        finish();

    }

    void buttonClickShowCropButtons() {
        setCropView(true);
        getCurrentScanDocumentFragment().trySetPolygonViewToADocument();
    }

    void buttonClickHideCropButtons() {
        setCropView(false);
        getCurrentScanDocumentFragment().disablePolygonView();
        getCurrentScanDocumentFragment().cancelCrop();
    }

    void buttonClickValidateCrop() {
        getCurrentScanDocumentFragment().cropImageFromPolygon();
        getCurrentScanDocumentFragment().disablePolygonView();
        setCropView(false);
    }

    void buttonClickReset() {
        getCurrentScanDocumentFragment().resetImage();
    }

    void buttonClickClose() {
        finish();
    }

    void buttonInvertColor() {
        getCurrentScanDocumentFragment().invertColorImage();
    }

    void buttonRotateClick() {
        getCurrentScanDocumentFragment().rotateBitmap(90);
    }

    void onAddOtherScanDocClick() {
        FileOperationsHelper.takePictureFromCamera(this, REQUEST_CODE__TAKE_PICTURE_FROM_CAMERA);
    }

    void onNextScanDocClick() {
        binding.viewPagerScanDocument.setCurrentItem(mCurrentPosition + 1, true);
        updateNextPrevious();
    }

    void onPreviousScanDocClick() {
        binding.viewPagerScanDocument.setCurrentItem(mCurrentPosition - 1, true);
        updateNextPrevious();
    }

    private void disableViewAndSetProgressBar(boolean isShow) {
        setViewInteract(binding.constraintLayoutMainContainer, !isShow);
        binding.progressBarScanDocument.setVisibility(isShow ? View.VISIBLE : View.GONE);
    }

    private void setCropView(boolean isEnable) {
        binding.viewPagerScanDocument.setUserInputEnabled(!isEnable);
        //Disable all
        setViewInteract(binding.constraintLayoutMainContainer, !isEnable);
        //Enable only CropButton
        setViewInteract(binding.constraintLayoutCropButton, isEnable);
        binding.constraintLayoutCropButton.setVisibility(isEnable ? View.VISIBLE : View.INVISIBLE);
        binding.constraintLayoutPagerButton.setVisibility(isEnable ? View.INVISIBLE : View.VISIBLE);
        binding.constraintLayoutScanDocButtons.setVisibility(isEnable ? View.INVISIBLE : View.VISIBLE);
        binding.ivAddAnOtherScanToDoc.setVisibility(isEnable ? View.INVISIBLE : View.VISIBLE);
        binding.btnClose.setVisibility(isEnable ? View.INVISIBLE : View.VISIBLE);
        binding.btnValidate.setVisibility(isEnable ? View.INVISIBLE : View.VISIBLE);
    }

    private void updateNextPrevious() {
        binding.ivPreviousScanDoc.setEnabled(mCurrentPosition != 0);
        binding.ivNextScanDoc.setEnabled(mCurrentPosition != scanDocumentAdapter.getItemCount());
        binding.textViewPageCounter.setText(getString(R.string.upload_scan_doc_page_counter, mCurrentPosition + 1,
                                              scanDocumentAdapter.getItemCount()));
    }

    private ScanDocumentFragment getCurrentScanDocumentFragment() {
        return scanDocumentAdapter.getCurrentFragment(mCurrentPosition);
    }

    private void setViewInteract(View view, boolean canDo) {
        view.setEnabled(canDo);
        if (view instanceof ViewGroup) {
            for (int i = 0; i < ((ViewGroup) view).getChildCount(); i++) {
                setViewInteract(((ViewGroup) view).getChildAt(i), canDo);
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CODE__TAKE_PICTURE_FROM_CAMERA &&
            resultCode == RESULT_OK) {

            BitmapFactory.Options bmOptions = new BitmapFactory.Options();
            String path = FileOperationsHelper.createImageFile(this).getAbsolutePath();
            Bitmap originalBitmap = BitmapFactory.decodeFile(path,
                                                             bmOptions);
            originalBitmap = BitmapUtils.rotateImage(originalBitmap, path);
            scanDocumentAdapter.addScanImage(originalBitmap, scanDocumentAdapter.getItemCount());
            updateNextPrevious();
            binding.viewPagerScanDocument.setCurrentItem(scanDocumentAdapter.getItemCount() - 1, false);
        } else if (requestCode == REQUEST_CODE__RETAKE_PICTURE_FROM_CAMERA && resultCode == RESULT_OK) {
            BitmapFactory.Options bmOptions = new BitmapFactory.Options();
            String path = FileOperationsHelper.createImageFile(this).getAbsolutePath();
            Bitmap picture = BitmapFactory.decodeFile(path,
                                                      bmOptions);
            picture = BitmapUtils.rotateImage(picture, path);
            scanDocumentAdapter.changeScanImage(picture, binding.viewPagerScanDocument.getCurrentItem());
            updateNextPrevious();
            binding.viewPagerScanDocument.setCurrentItem(binding.viewPagerScanDocument.getCurrentItem(), false);
        }
    }

    // region pdfedition
    public void createPDFDocument() {
        PdfDocument pdfDocument = new PdfDocument();
        Paint paint = new Paint();
        Matrix m = new Matrix();
        RectF rectFirst = new RectF();
        RectF rectSecond = new RectF();
        for (Bitmap bitmap : scanDocumentAdapter.getEditedImageList()) {
            PdfDocument.PageInfo myPageInfo =
                new PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pdfDocument.getPages().size() + 1).create();
            PdfDocument.Page page = pdfDocument.startPage(myPageInfo);
            Canvas canvas = page.getCanvas();

            //set white background
            paint.reset();
            paint.setColor(Color.parseColor("#ffffff"));
            canvas.drawPaint(paint);
            // resize if necessary
            m.reset();
            rectFirst.set(0, 0, bitmap.getWidth(), bitmap.getHeight());
            rectSecond.set(0, 0, PAGE_WIDTH, PAGE_HEIGHT);
            m.setRectToRect(rectFirst, rectSecond, Matrix.ScaleToFit.CENTER);
            Bitmap scaledBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), m, true);

            // center image
            float centerWidth = ((float) PAGE_WIDTH - bitmap.getWidth()) / 2;
            float centerHeight = ((float) PAGE_HEIGHT - bitmap.getHeight()) / 2;
            canvas.drawBitmap(bitmap, centerWidth, centerHeight, null);
            pdfDocument.finishPage(page);

            bitmap.recycle();
            scaledBitmap.recycle();
        }

        String pdfFilePath = FileOperationsHelper.createPdfFile(this).getAbsolutePath();
        try {
            pdfDocument.writeTo(Files.newOutputStream(Paths.get(pdfFilePath)));
        } catch (IOException e) {
            Log_OC.e(this, "pdf write to file", e);
        }
        pdfDocument.close();
    }

    @Override
    public void onProcessImageStart() {
        disableViewAndSetProgressBar(true);
    }

    @Override
    public void onProcessImageEnd() {
        disableViewAndSetProgressBar(false);
    }
    //endregion
}
