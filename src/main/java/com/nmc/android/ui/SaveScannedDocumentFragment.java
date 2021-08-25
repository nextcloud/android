package com.nmc.android.ui;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.CompoundButton;
import android.widget.ProgressBar;
import android.widget.ScrollView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.nextcloud.client.di.Injectable;
import com.nextcloud.client.jobs.BackgroundJobManager;
import com.nextcloud.client.preferences.AppPreferences;
import com.nmc.android.utils.FileUtils;
import com.nmc.android.utils.KeyboardUtils;
import com.owncloud.android.R;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.ui.activity.FolderPickerActivity;
import com.owncloud.android.utils.DisplayUtils;
import com.owncloud.android.utils.theme.ThemeButtonUtils;
import com.owncloud.android.utils.theme.ThemeCheckableUtils;
import com.owncloud.android.utils.theme.ThemeTextInputUtils;

import javax.inject.Inject;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.AppCompatCheckBox;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.Fragment;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;

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
    @BindView(R.id.saveScanProgressBar)
    ProgressBar progressBar;
    private boolean isFileNameEditable = false;
    private String remotePath = "/";
    private OCFile remoteFilePath;

    @Inject BackgroundJobManager backgroundJobManager;
    @Inject AppPreferences appPreferences;

    @Override
    public void onCreate(@Nullable @org.jetbrains.annotations.Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //Fragment screen orientation normal both portrait and landscape
        requireActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
        setRetainInstance(true);
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
        initViews();
        setDefaultRemotePath();
        updateSaveLocationText(appPreferences.getUploadScansLastPath());
        implementCheckListeners();
    }

    /**
     * create default OCFile if default Scan folder is selected
     */
    private void setDefaultRemotePath() {
        if (remoteFilePath == null &&
            appPreferences.getUploadScansLastPath().equalsIgnoreCase(ScanActivity.DEFAULT_UPLOAD_SCAN_PATH)) {
            remoteFilePath = new OCFile(appPreferences.getUploadScansLastPath());
            remoteFilePath.setFolder();
        }
    }

    private void initViews() {
        fileNameInput.setText(FileUtils.scannedFileName());
        ThemeCheckableUtils.tintSwitch(pdfPasswordSwitch, 0);
        ThemeCheckableUtils.tintCheckbox(0, pdfCheckBox, pngCheckBox, jpgCheckBox, pdfOcrCheckBox, txtFileCheckBox);
        ThemeButtonUtils.colorPrimaryButton(saveButton, requireContext());

        pdfOcrCheckBox.setChecked(true);
        ThemeTextInputUtils.colorTextInputLayout(pdfPasswordInputLayout,
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
            fileTypesStringBuilder.append(SAVE_TYPE_JPG);
            fileTypesStringBuilder.append(",");
        }
        if (pngCheckBox.isChecked()) {
            fileTypesStringBuilder.append(SAVE_TYPE_PNG);
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

        showPromptToSave(fileName, fileTypesStringBuilder, pdfPassword);
    }

    private void showPromptToSave(String fileName, StringBuilder fileTypesStringBuilder, String pdfPassword) {
        try {
            AlertDialog alertDialog = new AlertDialog.Builder(requireContext())
                .setMessage(R.string.dialog_save_scan_message)
                .setPositiveButton(R.string.dialog_ok, (dialog, which) -> startSaving(fileName,
                                                                                      fileTypesStringBuilder, pdfPassword))
                .create();

            alertDialog.show();
            ThemeButtonUtils.themeBorderlessButton(alertDialog.getButton(AlertDialog.BUTTON_POSITIVE));
        } catch (WindowManager.BadTokenException e) {
            Log_OC.e(TAG, "Error showing wrong storage info, so skipping it: " + e.getMessage());
        }
    }

    private void startSaving(String fileName, StringBuilder fileTypesStringBuilder, String pdfPassword) {
        //start the save and upload worker
        backgroundJobManager.scheduleImmediateScanDocUploadJob(
            fileTypesStringBuilder.toString(),
            fileName,
            remotePath,
            pdfPassword);

        //save the selected location to save scans in preference
        appPreferences.setUploadScansLastPath(remotePath);

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
        if (requestCode == SELECT_LOCATION_REQUEST_CODE) {
            if (data != null) {
                OCFile chosenFolder = data.getParcelableExtra(FolderPickerActivity.EXTRA_FOLDER);
                if (chosenFolder != null) {
                    remoteFilePath = chosenFolder;
                    updateSaveLocationText(chosenFolder.getRemotePath());
                }
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }
}
