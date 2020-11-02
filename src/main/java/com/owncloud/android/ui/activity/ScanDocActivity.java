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
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.owncloud.android.R;
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
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.viewpager2.widget.ViewPager2;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;

import static com.owncloud.android.ui.activity.FileActivity.REQUEST_CODE__LAST_SHARED;

public class ScanDocActivity extends AppCompatActivity implements ScanDocumentFragment.OnProcessImage {

    public static final int REQUEST_CODE__TAKE_PICTURE_FROM_CAMERA = REQUEST_CODE__LAST_SHARED + 1;
    public static final int REQUEST_CODE__RETAKE_PICTURE_FROM_CAMERA = REQUEST_CODE__LAST_SHARED + 2;
    public static final String SCAN_DOC_ACTIVITY_RESULT_PDFNAME = "SCAN_DOC_ACTIVITY_RESULT_PDFNAME";

    @BindView(R.id.progressBarScanDocument)
    ProgressBar mProgressBar;

    @BindView(R.id.ivAddAnOtherScanToDoc)
    ImageView mBtnAddAnOtherScanToDoc;

    @BindView(R.id.textViewPageCounter)
    TextView mTextViewPageCounter;

    @BindView(R.id.viewPagerScanDocument)
    ViewPager2 mViewPagerScanDocument;

    @BindView(R.id.btnClose)
    Button mBtnClose;

    @BindView(R.id.btnValidate)
    Button mBtnValidate;

    @BindView(R.id.ivNextScanDoc)
    ImageView mImageViewNextScanDoc;

    @BindView(R.id.contraintLayoutMainContainer)
    ConstraintLayout mConstraintLayoutMainContainer;

    @BindView(R.id.constraintLayout_crop_button)
    ConstraintLayout mConstraintLayoutCropButton;

    @BindView(R.id.constraintLayout_scan_doc_buttons)
    ConstraintLayout mConstraintLayoutScanDocButtons;

    @BindView(R.id.constraintLayout_pager_button)
    ConstraintLayout mConstraintLayoutPagerButton;

    @BindView(R.id.ivPreviousScanDoc)
    ImageView mImageViewPreviousScanDoc;

    int mCurrentPosition;
    private Unbinder mUnbinder;
    private String mPdfName = FileOperationsHelper.getScanDocName();
    private ScanDocumentAdapter mScanDocumentAdapter;

    public static void startScanActivityForResult(Activity activity, int requestCode) {
        Intent action = new Intent(activity, ScanDocActivity.class);
        activity.startActivityForResult(action, requestCode);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan_doc);
        mUnbinder = ButterKnife.bind(this);

        mCurrentPosition = -1;

        mScanDocumentAdapter = new ScanDocumentAdapter(this, getSupportFragmentManager(), getLifecycle());
        mViewPagerScanDocument.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
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
        mViewPagerScanDocument.setAdapter(mScanDocumentAdapter);
        mViewPagerScanDocument.setUserInputEnabled(true);

        FileOperationsHelper
            .takePictureFromCamera(this, REQUEST_CODE__TAKE_PICTURE_FROM_CAMERA);

    }

    @OnClick(R.id.ivDeletePage)
    void onDeletePageClick() {
        mScanDocumentAdapter.deleteScanImage(mCurrentPosition);
        if (mScanDocumentAdapter.getItemCount() == 0) {
            FileOperationsHelper
                .takePictureFromCamera(this, REQUEST_CODE__TAKE_PICTURE_FROM_CAMERA);
        }
    }

    @OnClick(R.id.ivRetakePicture)
    void onRetakePictureClick() {
        FileOperationsHelper
            .takePictureFromCamera(this, REQUEST_CODE__RETAKE_PICTURE_FROM_CAMERA);
    }

    @OnClick(R.id.ivRename)
    void buttonRenameClick() {
        int accentColor = ThemeUtils.primaryAccentColor(this);

        // Inflate the layout for the dialog
        LayoutInflater inflater = getLayoutInflater();
        EditBoxDialogBinding binding = EditBoxDialogBinding.inflate(inflater, null, false);
        View view = binding.getRoot();

        // Setup layout
        String currentName = mPdfName;
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

                mPdfName = newFileName;
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
    void buttonValidateClick() {
        // change to a static pdf file and a rename string
        createPDFDocument();
        Intent resultIntent = new Intent();
        resultIntent.putExtra(SCAN_DOC_ACTIVITY_RESULT_PDFNAME, mPdfName);
        setResult(RESULT_OK, resultIntent);
        finish();

    }

    @OnClick(R.id.ivCrop)
    void buttonClickShowCropButtons() {
        setCropView(true);
        getCurrentScanDocumentFragment().trySetPolygonViewToADocument();
    }

    @OnClick(R.id.ivCancelCrop)
    void buttonClickHideCropButtons() {
        setCropView(false);
        getCurrentScanDocumentFragment().disablePolygonView();
    }

    @OnClick(R.id.ivValidateCrop)
    void buttonClickValidateCrop() {
        getCurrentScanDocumentFragment().cropImageFromPolygon();
        getCurrentScanDocumentFragment().disablePolygonView();
        setCropView(false);
    }

    @OnClick(R.id.ivReset)
    void buttonClickReset() {
        getCurrentScanDocumentFragment().resetImage();
    }

    @OnClick(R.id.btnClose)
    void buttonClickClose() {
        finish();
    }

    @OnClick(R.id.ivInvert)
    void buttonInvertColor() {
        getCurrentScanDocumentFragment().invertColorImage();
    }

    @OnClick(R.id.ivRotate)
    void buttonRotateClick() {
        getCurrentScanDocumentFragment().rotateBitmap(90);
    }

    @OnClick(R.id.ivAddAnOtherScanToDoc)
    void onAddOtherScanDocClick() {
        FileOperationsHelper.takePictureFromCamera(this, REQUEST_CODE__TAKE_PICTURE_FROM_CAMERA);
    }

    @OnClick(R.id.ivNextScanDoc)
    void onNextScanDocClick() {
        mViewPagerScanDocument.setCurrentItem(mCurrentPosition + 1, true);
        updateNextPrevious();
    }

    @OnClick(R.id.ivPreviousScanDoc)
    void onPreviousScanDocClick() {
        mViewPagerScanDocument.setCurrentItem(mCurrentPosition - 1, true);
        updateNextPrevious();
    }

    private void disableViewAndSetProgressBar(boolean isShow) {
        setViewInteract(mConstraintLayoutMainContainer, !isShow);
        mProgressBar.setVisibility(isShow ? View.VISIBLE : View.GONE);
    }

    private void setCropView(boolean isEnable) {
        mViewPagerScanDocument.setUserInputEnabled(!isEnable);
        //Disable all
        setViewInteract(mConstraintLayoutMainContainer, !isEnable);
        //Enable only CropButton
        setViewInteract(mConstraintLayoutCropButton, isEnable);
        mConstraintLayoutCropButton.setVisibility(isEnable ? View.VISIBLE : View.INVISIBLE);
        mConstraintLayoutPagerButton.setVisibility(isEnable ? View.INVISIBLE : View.VISIBLE);
        mConstraintLayoutScanDocButtons.setVisibility(isEnable ? View.INVISIBLE : View.VISIBLE);
        mBtnAddAnOtherScanToDoc.setVisibility(isEnable ? View.INVISIBLE : View.VISIBLE);
        mBtnClose.setVisibility(isEnable ? View.INVISIBLE : View.VISIBLE);
        mBtnValidate.setVisibility(isEnable ? View.INVISIBLE : View.VISIBLE);
    }

    private void updateNextPrevious() {
        mImageViewPreviousScanDoc.setEnabled(mCurrentPosition != 0);
        mImageViewNextScanDoc.setEnabled(mCurrentPosition != mScanDocumentAdapter.getItemCount());
        mTextViewPageCounter.setText(getString(R.string.upload_scan_doc_page_counter, mCurrentPosition + 1,
                                               mScanDocumentAdapter.getItemCount()));
    }

    private ScanDocumentFragment getCurrentScanDocumentFragment() {
        return mScanDocumentAdapter.getCurrentFragment(mCurrentPosition);
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
    public void onDestroy() {
        super.onDestroy();
        mUnbinder.unbind();
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
            mScanDocumentAdapter.addScanImage(originalBitmap, mScanDocumentAdapter.getItemCount());
            updateNextPrevious();
            mViewPagerScanDocument.setCurrentItem(mScanDocumentAdapter.getItemCount() - 1, false);
        }
        if (requestCode == REQUEST_CODE__RETAKE_PICTURE_FROM_CAMERA && resultCode == RESULT_OK) {
            BitmapFactory.Options bmOptions = new BitmapFactory.Options();
            String path = FileOperationsHelper.createImageFile(this).getAbsolutePath();
            Bitmap picture = BitmapFactory.decodeFile(path,
                                                      bmOptions);
            picture = BitmapUtils.rotateImage(picture, path);
            mScanDocumentAdapter.changeScanImage(picture, mViewPagerScanDocument.getCurrentItem());
            updateNextPrevious();
            mViewPagerScanDocument.setCurrentItem(mViewPagerScanDocument.getCurrentItem(), false);
        }
    }

    // region pdfedition
    public void createPDFDocument() {
        int pageWidth = 960;
        int pageHeight = 1280;
        PdfDocument pdfDocument = new PdfDocument();

        for (Bitmap bitmap : mScanDocumentAdapter.getEditedImageList()) {
            PdfDocument.PageInfo myPageInfo =
                new PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pdfDocument.getPages().size() + 1).create();
            PdfDocument.Page page = pdfDocument.startPage(myPageInfo);
            Canvas canvas = page.getCanvas();

            //set white background
            Paint paint = new Paint();
            paint.setColor(Color.parseColor("#ffffff"));
            canvas.drawPaint(paint);
            // resize if necessary
            Matrix m = new Matrix();
            m.setRectToRect(new RectF(0, 0, bitmap.getWidth(), bitmap.getHeight()), new RectF(0, 0, pageWidth, pageHeight), Matrix.ScaleToFit.CENTER);
            Bitmap scaledBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), m, true);

            // center image
            float centerWidth = ((float) pageWidth - bitmap.getWidth()) / 2;
            float centerHeight = ((float) pageHeight - bitmap.getHeight()) / 2;
            canvas.drawBitmap(bitmap, centerWidth, centerHeight, null);
            pdfDocument.finishPage(page);

            bitmap.recycle();
            scaledBitmap.recycle();
        }
        FileOperationsHelper.deleteOldFiles(this);
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
