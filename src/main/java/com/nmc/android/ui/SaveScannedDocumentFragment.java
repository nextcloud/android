package com.nmc.android.ui;

import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.ProgressBar;
import android.widget.ScrollView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.pdf.PdfWriter;
import com.nmc.android.OnDocScanListener;
import com.nmc.android.OnFragmentChangeListener;
import com.nmc.android.utils.FileUtils;
import com.nmc.android.utils.KeyboardUtils;
import com.owncloud.android.R;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.ui.activity.FolderPickerActivity;
import com.owncloud.android.utils.DisplayUtils;
import com.owncloud.android.utils.theme.ThemeCheckableUtils;
import com.owncloud.android.utils.theme.ThemeTextInputUtils;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatCheckBox;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.os.HandlerCompat;
import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.Fragment;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;
import io.scanbot.sdk.ScanbotSDK;
import io.scanbot.sdk.core.contourdetector.DetectionResult;
import io.scanbot.sdk.entity.Language;
import io.scanbot.sdk.ocr.OpticalCharacterRecognizer;
import io.scanbot.sdk.ocr.process.OcrResult;
import io.scanbot.sdk.persistence.Page;
import io.scanbot.sdk.persistence.PageFileStorage;
import io.scanbot.sdk.process.PDFPageSize;
import io.scanbot.sdk.process.PDFRenderer;

import static com.owncloud.android.datamodel.OCFile.PATH_SEPARATOR;
import static java.util.Collections.emptyList;

public class SaveScannedDocumentFragment extends Fragment implements CompoundButton.OnCheckedChangeListener {

    protected static final String TAG = "SaveScannedDocumentFragment";
    private static final int SELECT_LOCATION_REQUEST_CODE = 212;

    public SaveScannedDocumentFragment() {
    }

    public static SaveScannedDocumentFragment newInstance() {
        Bundle args = new Bundle();
        SaveScannedDocumentFragment fragment = new SaveScannedDocumentFragment();
        fragment.setArguments(args);
        return fragment;
    }

    private Unbinder unbinder;
    private OnFragmentChangeListener onFragmentChangeListener;
    private OnDocScanListener onDocScanListener;

    @BindView(R.id.scan_save_nested_scroll_view)
    NestedScrollView nestedScrollView;
    @BindView(R.id.scan_save_filename_input)
    TextInputEditText fileNameInput;
    @BindView(R.id.scan_save_filename_input_edit_btn)
    AppCompatImageView fileNameEditBtn;
    @BindView(R.id.scan_save_location_input)
    AppCompatTextView locationInput;
    @BindView(R.id.scan_save_location_edit_btn)
    AppCompatImageView locationEditBtn;
    @BindView(R.id.scan_save_without_txt_recognition_pdf_checkbox)
    AppCompatCheckBox pdfCheckBox;
    @BindView(R.id.scan_save_without_txt_recognition_jpg_checkbox)
    AppCompatCheckBox jpgCheckBox;
    @BindView(R.id.scan_save_without_txt_recognition_png_checkbox)
    AppCompatCheckBox pngCheckBox;
    @BindView(R.id.scan_save_with_txt_recognition_pdf_checkbox)
    AppCompatCheckBox pdfOcrCheckBox;
    @BindView(R.id.scan_save_with_txt_recognition_txt_checkbox)
    AppCompatCheckBox txtFileCheckBox;
    @BindView(R.id.scan_save_pdf_password_switch)
    SwitchCompat pdfPasswordSwitch;
    @BindView(R.id.scan_save_pdf_password_text_input)
    TextInputLayout pdfPasswordInputLayout;
    @BindView(R.id.scan_save_pdf_password_et)
    TextInputEditText pdfPasswordEt;
    @BindView(R.id.save_scan_btn_cancel)
    MaterialButton cancelButton;
    @BindView(R.id.save_scan_btn_save)
    MaterialButton saveButton;
    @BindView(R.id.saveScanProgressBar) ProgressBar progressBar;
    private ScanbotSDK scanbotSDK;
    private PDFRenderer pdfRenderer;
    private PageFileStorage pageFileStorage;
    private OpticalCharacterRecognizer opticalCharacterRecognizer;
    private ExecutorService executorService = Executors.newSingleThreadExecutor();
    private Handler handler = HandlerCompat.createAsync(Looper.getMainLooper());
    private boolean isFileNameEditable = false;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
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
            ((ScanActivity) requireActivity()).updateActionBarTitleAndHomeButtonByString(getResources().getString(R.string.title_save_as));
        }
        return inflater.inflate(R.layout.fragment_scan_save, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        unbinder = ButterKnife.bind(this, view);
        initScanbot();
        initViews();
        implementCheckListeners();
    }

    private void initScanbot() {
        scanbotSDK = ((ScanActivity) requireActivity()).getScanbotSDK();
        pdfRenderer = scanbotSDK.pdfRenderer();
        pageFileStorage = scanbotSDK.getPageFileStorage();
        opticalCharacterRecognizer = scanbotSDK.ocrRecognizer();
    }

    private void initViews() {
        fileNameInput.setText(FileUtils.scannedFileName());
        ThemeCheckableUtils.tintSwitch(pdfPasswordSwitch, 0);
        ThemeCheckableUtils.tintCheckbox(0, pdfCheckBox, jpgCheckBox, pngCheckBox, pdfOcrCheckBox, txtFileCheckBox);
        pdfOcrCheckBox.setChecked(true);
        ThemeTextInputUtils.colorTextInput(pdfPasswordInputLayout, pdfPasswordEt,
                                           getResources().getColor(R.color.secondary_text_color));
        pdfPasswordEt.setHighlightColor(getResources().getColor(R.color.et_highlight_color));
        pdfPasswordInputLayout.setDefaultHintTextColor(new ColorStateList(
            new int[][]{
                new int[]{-android.R.attr.state_focused},
                new int[]{android.R.attr.state_focused},
            },
            new int[]{
                Color.GRAY,
                getResources().getColor(R.color.text_color)
            }
        ));
    }

    private void implementCheckListeners() {
        pdfCheckBox.setOnCheckedChangeListener(this);
        jpgCheckBox.setOnCheckedChangeListener(this);
        pngCheckBox.setOnCheckedChangeListener(this);
        pdfOcrCheckBox.setOnCheckedChangeListener(this);
        txtFileCheckBox.setOnCheckedChangeListener(this);
        pdfPasswordSwitch.setOnCheckedChangeListener(this);
    }

    private void enableDisablePdfPasswordSwitch() {
        pdfPasswordSwitch.setEnabled(pdfCheckBox.isChecked() || pdfOcrCheckBox.isChecked());
        if (!pdfCheckBox.isChecked() && !pdfOcrCheckBox.isChecked()) {
            pdfPasswordSwitch.setChecked(false);
        }
    }

    private void showHidePdfPasswordInput(boolean isChecked) {
        pdfPasswordInputLayout.setVisibility(isChecked ? View.VISIBLE : View.GONE);
        if (isChecked) {
            nestedScrollView.post(() -> nestedScrollView.fullScroll(ScrollView.FOCUS_DOWN));
        }
        if (isChecked) {
            KeyboardUtils.showSoftKeyboard(requireContext(), pdfPasswordEt);
        } else {
            KeyboardUtils.hideKeyboardFrom(requireContext(), pdfPasswordEt);
        }
    }

    private void enableFileNameEditing() {
        isFileNameEditable = !isFileNameEditable;
        fileNameInput.setEnabled(isFileNameEditable);
        if (isFileNameEditable) {
            KeyboardUtils.showSoftKeyboard(requireContext(), fileNameInput);
        } else {
            KeyboardUtils.hideKeyboardFrom(requireContext(), fileNameInput);
        }
        fileNameInput.setSelection(fileNameInput.getText().toString().trim().length());
    }


    @OnClick({R.id.scan_save_filename_input_edit_btn, R.id.scan_save_location_edit_btn, R.id.save_scan_btn_cancel, R.id.save_scan_btn_save})
    void onClickListener(View view) {
        switch (view.getId()) {
            case R.id.scan_save_filename_input_edit_btn:
                enableFileNameEditing();
                break;
            case R.id.scan_save_location_edit_btn:
                Intent action = new Intent(requireActivity(), FolderPickerActivity.class);
                action.putExtra(FolderPickerActivity.EXTRA_ACTION, FolderPickerActivity.CHOOSE_LOCATION);
                startActivityForResult(action, SELECT_LOCATION_REQUEST_CODE);
                break;
            case R.id.save_scan_btn_cancel:
                requireActivity().onBackPressed();
                break;
            case R.id.save_scan_btn_save:
                saveScannedFiles();
                break;

        }
    }

    private void saveScannedFiles() {
        String fileName = fileNameInput.getText().toString().trim();
        if (TextUtils.isEmpty(fileName)) {
            DisplayUtils.showSnackMessage(requireActivity(), R.string.filename_empty);
            return;
        }

        if (!com.owncloud.android.lib.resources.files.FileUtils.isValidName(fileName)) {
            DisplayUtils.showSnackMessage(requireActivity(), R.string.filename_forbidden_charaters_from_server);
            return;
        }

        if (!pdfCheckBox.isChecked() && !jpgCheckBox.isChecked() && !pngCheckBox.isChecked() && !pdfOcrCheckBox.isChecked()
            && !txtFileCheckBox.isChecked()) {
            DisplayUtils.showSnackMessage(requireActivity(), R.string.scan_save_no_file_select_toast);
            return;
        }
        //do file save
        //savePDFFiles();
        //savePngFiles(fileName);
        savePDFFiles(fileName);
        //savePDFWithOCR(fileName);
        //saveTextFile(fileName);
    }

    private void savePngFiles(String fileName) {
        progressBar.setVisibility(View.VISIBLE);
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "===IMAGE FILES SAVE STARTED===");
                List<Bitmap> bitmapList = onDocScanListener.getScannedDocs();
                for (int i = 0; i < bitmapList.size(); i++) {
                    String newFileName = fileName;
                    Bitmap bitmap = bitmapList.get(i);
                    if (i > 0) {
                        newFileName += "(" + i + ")";
                    }
                    File pngFile = FileUtils.savePngImage(requireContext(), bitmap, newFileName);
                    File jpgFile = FileUtils.saveJpgImage(requireContext(), bitmap, newFileName);
                    Log.d(TAG, "IMAGE FILEs : " + pngFile + "\n" + jpgFile);
                    //FileUtils.writeTextToFile(requireContext(),"", fileName);
                }

                Log.d(TAG, "===IMAGE FILES SAVED===");

                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        progressBar.setVisibility(View.GONE);
                    }
                });
            }
        });


    }

    private void savePDFFiles(String fileName) {
        progressBar.setVisibility(View.VISIBLE);
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "===PDF FILES SAVE STARTED===");

                List<Page> pageList = getScannedPages();

                File pdfFile = pdfRenderer.renderDocumentFromPages(pageList, PDFPageSize.A4);
                if (pdfFile != null) {
                    File renamedFile = new File(pdfFile.getParent() + PATH_SEPARATOR + fileName + ".pdf");
                    if (pdfFile.renameTo(renamedFile)) {
                        Log.d(TAG, "File successfully renamed");
                    }
                    Log.d(TAG, "===PDF FILES SAVED=== : " + pdfFile + "\n" + renamedFile);
                    pdfWithPassword(renamedFile);
                }
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        progressBar.setVisibility(View.GONE);
                    }
                });
            }
        });

        // Log.d(TAG, "PDF file path : " + pdfFile.getPath());
    }

    private void pdfWithPassword(File pdfFile) {
        try {
            String userPassword = "a";
            String ownerPassword = "b";
            OutputStream fos = new FileOutputStream(pdfFile);
            Document document = new Document();
            PdfWriter pdfWriter = PdfWriter.getInstance(document, fos);
            pdfWriter.setEncryption(userPassword.getBytes(),
                                    ownerPassword.getBytes(),
                                    PdfWriter.ALLOW_PRINTING, PdfWriter.ENCRYPTION_AES_128);
            Log.d(TAG, "PASSWORD PDF DONE");
        } catch (FileNotFoundException | DocumentException e) {
            e.printStackTrace();
        }


    }


    @NotNull
    private List<Page> getScannedPages() {
        List<Page> pageList = new ArrayList<>();
        for (Bitmap bitmap : onDocScanListener.getScannedDocs()) {
            Page page = new Page(pageFileStorage.add(bitmap), emptyList(),
                                 DetectionResult.OK);
            pageList.add(page);
        }
        return pageList;
    }

    private void savePDFWithOCR(String fileName) {
        progressBar.setVisibility(View.VISIBLE);
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "===PDF OCR FILES SAVE STARTED===");
                Set<Language> set = new HashSet<>(Collections.singletonList(Language.ENG));
                OcrResult ocrResult = opticalCharacterRecognizer.recognizeTextWithPdfFromPages(getScannedPages(), PDFPageSize.A4, set);
                List<OcrResult.OCRPage> ocrPageList = ocrResult.ocrPages;
                if (ocrPageList.size() > 0) {
                    String ocrText = ocrResult.getRecognizedText();
                }
                File ocrPDFFile = ocrResult.sandwichedPdfDocumentFile;
                if (ocrPDFFile != null) {
                    File renamedFile = new File(ocrPDFFile.getParent() + PATH_SEPARATOR + fileName + "_OCR.pdf");
                    if (ocrPDFFile.renameTo(renamedFile)) {
                        Log.d(TAG, "OCR File successfully renamed");
                    }
                    Log.d(TAG, "===OCR PDF FILES SAVED=== : " + ocrPDFFile + "\n" + renamedFile);
                    pdfWithPassword(renamedFile);

                }
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        progressBar.setVisibility(View.GONE);
                    }
                });
            }
        });
    }

    private void saveTextFile(String fileName) {
        progressBar.setVisibility(View.VISIBLE);
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "===TXT FILES SAVE STARTED===");
                Set<Language> set = new HashSet<>(Collections.singletonList(Language.ENG));
                List<Bitmap> bitmapList = onDocScanListener.getScannedDocs();
                for (int i = 0; i < bitmapList.size(); i++) {
                    String newFileName = fileName;
                    Bitmap bitmap = bitmapList.get(i);
                    if (i > 0) {
                        newFileName += "(" + i + ")";
                    }
                    Page page = new Page(pageFileStorage.add(bitmap), emptyList(),
                                         DetectionResult.OK);
                    List<Page> pageList = new ArrayList<>();
                    pageList.add(page);
                    OcrResult ocrResult = opticalCharacterRecognizer.recognizeTextFromPages(pageList, set);
                    List<OcrResult.OCRPage> ocrPageList = ocrResult.ocrPages;

                    if (ocrPageList.size() > 0) {
                        String ocrText = ocrResult.getRecognizedText();
                        File txtFile = FileUtils.writeTextToFile(requireContext(), ocrText, newFileName);
                        Log.d(TAG, "TXT FILE : " + txtFile);
                    }
                }
                Log.d(TAG, "===TXT FILES SAVED===");
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        progressBar.setVisibility(View.GONE);
                    }
                });
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (unbinder != null) {
            unbinder.unbind();
        }
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        switch (buttonView.getId()) {
            case R.id.scan_save_without_txt_recognition_pdf_checkbox:
            case R.id.scan_save_with_txt_recognition_pdf_checkbox:
                enableDisablePdfPasswordSwitch();
                break;
            case R.id.scan_save_without_txt_recognition_jpg_checkbox:
                break;
            case R.id.scan_save_without_txt_recognition_png_checkbox:
                break;
            case R.id.scan_save_with_txt_recognition_txt_checkbox:
                break;
            case R.id.scan_save_pdf_password_switch:
                showHidePdfPasswordInput(isChecked);
                break;
        }
    }

    private void updateSaveLocationText(String path) {
        if (path.equalsIgnoreCase("/")) {
            path = getResources().getString(R.string.scan_save_location_root);
        }
        locationInput.setText(path);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        switch (requestCode) {
            case SELECT_LOCATION_REQUEST_CODE:
                if (data != null) {
                    OCFile chosenFolder = data.getParcelableExtra(FolderPickerActivity.EXTRA_FOLDER);
                    if (chosenFolder != null) {
                        updateSaveLocationText(chosenFolder.getRemotePath());
                    }
                }
                break;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }
}
