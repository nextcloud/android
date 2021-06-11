package com.nmc.android.ui;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.CompoundButton;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.nextcloud.client.di.Injectable;
import com.nextcloud.client.jobs.BackgroundJobManager;
import com.nmc.android.interfaces.OnDocScanListener;
import com.nmc.android.interfaces.OnFragmentChangeListener;
import com.nmc.android.utils.FileUtils;
import com.nmc.android.utils.KeyboardUtils;
import com.owncloud.android.R;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.ui.activity.FolderPickerActivity;
import com.owncloud.android.utils.DisplayUtils;
import com.owncloud.android.utils.ThemeUtils;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.inject.Inject;

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
import io.scanbot.sdk.ocr.OpticalCharacterRecognizer;
import io.scanbot.sdk.persistence.PageFileStorage;
import io.scanbot.sdk.process.PDFRenderer;

public class SaveScannedDocumentFragment extends Fragment implements CompoundButton.OnCheckedChangeListener, Injectable {

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

    public static final String SAVE_TYPE_PDF = "pdf";
    public static final String SAVE_TYPE_PNG = "png";
    public static final String SAVE_TYPE_JPG = "jpg";
    public static final String SAVE_TYPE_PDF_OCR = "pdf_ocr";
    public static final String SAVE_TYPE_TXT = "txt";

    public static final String EXTRA_SCAN_DOC_REMOTE_PATH = "scan_doc_remote_path";

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
    private String remotePath = "/";
    private OCFile remoteFilePath;

    @Inject BackgroundJobManager backgroundJobManager;

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
            ((ScanActivity) requireActivity()).showHideDefaultToolbarDivider(true);
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
        ThemeUtils.tintSwitch(pdfPasswordSwitch, 0);
        ThemeUtils.tintCheckbox(pdfCheckBox, 0);
        ThemeUtils.tintCheckbox(pngCheckBox, 0);
        ThemeUtils.tintCheckbox(jpgCheckBox, 0);
        ThemeUtils.tintCheckbox(pdfOcrCheckBox, 0);
        ThemeUtils.tintCheckbox(txtFileCheckBox, 0);
        ThemeUtils.colorPrimaryButton(saveButton, requireContext());

        pdfOcrCheckBox.setChecked(true);
        ThemeUtils.colorTextInputLayout(pdfPasswordInputLayout, getResources().getColor(R.color.secondary_text_color));
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

        fileNameInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                enableFileNameEditing();
                return true;
            }
            return false;
        });
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
            fileNameEditBtn.setImageResource(R.drawable.ic_tick);
            KeyboardUtils.showSoftKeyboard(requireContext(), fileNameInput);
            fileNameInput.setSelection(fileNameInput.getText().toString().trim().length());
        } else {
            fileNameEditBtn.setImageResource(R.drawable.ic_pencil_edit);
            KeyboardUtils.hideKeyboardFrom(requireContext(), fileNameInput);
        }
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

        StringBuilder fileTypesStringBuilder = new StringBuilder();
        if (pdfCheckBox.isChecked()) {
            fileTypesStringBuilder.append(SAVE_TYPE_PDF);
            fileTypesStringBuilder.append(",");
        }
        if (jpgCheckBox.isChecked()) {
            fileTypesStringBuilder.append(SAVE_TYPE_PNG);
            fileTypesStringBuilder.append(",");
        }
        if (pngCheckBox.isChecked()) {
            fileTypesStringBuilder.append(SAVE_TYPE_JPG);
            fileTypesStringBuilder.append(",");
        }
        if (pdfOcrCheckBox.isChecked()) {
            fileTypesStringBuilder.append(SAVE_TYPE_PDF_OCR);
            fileTypesStringBuilder.append(",");
        }
        if (txtFileCheckBox.isChecked()) {
            fileTypesStringBuilder.append(SAVE_TYPE_TXT);
        }
        String pdfPassword = pdfPasswordEt.getText().toString().trim();
        if (pdfPasswordSwitch.isChecked() && TextUtils.isEmpty(pdfPassword)) {
            DisplayUtils.showSnackMessage(requireActivity(), R.string.save_scan_empty_pdf_password);
            return;
        }

        //start the save and upload worker
        backgroundJobManager.scheduleImmediateScanDocUploadJob(
            fileTypesStringBuilder.toString(),
            fileName,
            remotePath,
            pdfPassword);

        //send the result back with the selected remote path to open selected remote path
        Intent intent = new Intent();
        Bundle bundle = new Bundle();
        bundle.putParcelable(EXTRA_SCAN_DOC_REMOTE_PATH, remoteFilePath);
        intent.putExtras(bundle);
        requireActivity().setResult(Activity.RESULT_OK, intent);
        requireActivity().finish();
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
        remotePath = path;
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
                        remoteFilePath = chosenFolder;
                        updateSaveLocationText(chosenFolder.getRemotePath());
                    }
                }
                break;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }
}
