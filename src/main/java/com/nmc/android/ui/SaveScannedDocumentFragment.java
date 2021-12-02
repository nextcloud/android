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
import android.widget.ScrollView;

import com.nextcloud.client.di.Injectable;
import com.nextcloud.client.jobs.BackgroundJobManager;
import com.nextcloud.client.preferences.AppPreferences;
import com.nmc.android.utils.FileUtils;
import com.nmc.android.utils.KeyboardUtils;
import com.owncloud.android.R;
import com.owncloud.android.databinding.FragmentScanSaveBinding;
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
import androidx.fragment.app.Fragment;

public class SaveScannedDocumentFragment extends Fragment implements CompoundButton.OnCheckedChangeListener,
    Injectable, View.OnClickListener {

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

    private FragmentScanSaveBinding binding;

    public static final String SAVE_TYPE_PDF = "pdf";
    public static final String SAVE_TYPE_PNG = "png";
    public static final String SAVE_TYPE_JPG = "jpg";
    public static final String SAVE_TYPE_PDF_OCR = "pdf_ocr";
    public static final String SAVE_TYPE_TXT = "txt";

    public static final String EXTRA_SCAN_DOC_REMOTE_PATH = "scan_doc_remote_path";

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
        binding = FragmentScanSaveBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initViews();
        String remotePath = setDefaultRemotePath();
        updateSaveLocationText(remotePath);
        implementCheckListeners();
        implementClickEvent();
    }

    private void implementClickEvent() {
        binding.scanSaveFilenameInputEditBtn.setOnClickListener(this);
        binding.scanSaveLocationEditBtn.setOnClickListener(this);
        binding.saveScanBtnCancel.setOnClickListener(this);
        binding.saveScanBtnSave.setOnClickListener(this);
    }

    /**
     * create default OCFile if default Scan folder is selected
     */
    private String setDefaultRemotePath() {

        //check if user has selected scan document from sub folders
        //if yes then show that folder in location to save scanned documents
        //else check in preferences for last selected path
        //if no last path selected available then show default /Scans/ path
        if (requireActivity() instanceof ScanActivity){
            String remotePath = ((ScanActivity) requireActivity()).getRemotePath();
            //remote path should not be null and should not be root path i.e only /
            if (!TextUtils.isEmpty(remotePath) && !remotePath.equals("/")){
                setRemoteFilePath(remotePath);
                return remotePath;
            }
        }
        if (remoteFilePath == null &&
            appPreferences.getUploadScansLastPath().equalsIgnoreCase(ScanActivity.DEFAULT_UPLOAD_SCAN_PATH)) {
            setRemoteFilePath(appPreferences.getUploadScansLastPath());
        }
        return appPreferences.getUploadScansLastPath();
    }

    private void setRemoteFilePath(String remotePath) {
        remoteFilePath = new OCFile(remotePath);
        remoteFilePath.setFolder();
    }

    private void initViews() {
        binding.scanSaveFilenameInput.setText(FileUtils.scannedFileName());
        ThemeCheckableUtils.tintSwitch(binding.scanSavePdfPasswordSwitch, 0);
        ThemeCheckableUtils.tintCheckbox(0, binding.scanSaveWithoutTxtRecognitionPdfCheckbox,
                                         binding.scanSaveWithoutTxtRecognitionPngCheckbox,
                                         binding.scanSaveWithoutTxtRecognitionJpgCheckbox,
                                         binding.scanSaveWithTxtRecognitionPdfCheckbox,
                                         binding.scanSaveWithTxtRecognitionTxtCheckbox);
        ThemeButtonUtils.colorPrimaryButton(binding.saveScanBtnSave, requireContext());

        binding.scanSaveWithTxtRecognitionPdfCheckbox.setChecked(true);
        ThemeTextInputUtils.colorTextInputLayout(binding.scanSavePdfPasswordTextInput,
                                                 getResources().getColor(R.color.secondary_text_color));
        binding.scanSavePdfPasswordEt.setHighlightColor(getResources().getColor(R.color.et_highlight_color));
        binding.scanSavePdfPasswordTextInput.setDefaultHintTextColor(new ColorStateList(
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
        binding.scanSaveWithoutTxtRecognitionPdfCheckbox.setOnCheckedChangeListener(this);
        binding.scanSaveWithoutTxtRecognitionJpgCheckbox.setOnCheckedChangeListener(this);
        binding.scanSaveWithoutTxtRecognitionPngCheckbox.setOnCheckedChangeListener(this);
        binding.scanSaveWithTxtRecognitionPdfCheckbox.setOnCheckedChangeListener(this);
        binding.scanSaveWithTxtRecognitionTxtCheckbox.setOnCheckedChangeListener(this);
        binding.scanSavePdfPasswordSwitch.setOnCheckedChangeListener(this);

        binding.scanSaveFilenameInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                enableFileNameEditing();
                return true;
            }
            return false;
        });
    }

    private void enableDisablePdfPasswordSwitch() {
        binding.scanSavePdfPasswordSwitch.setEnabled(binding.scanSaveWithoutTxtRecognitionPdfCheckbox.isChecked() || binding.scanSaveWithTxtRecognitionPdfCheckbox.isChecked());
        if (!binding.scanSaveWithoutTxtRecognitionPdfCheckbox.isChecked() && !binding.scanSaveWithTxtRecognitionPdfCheckbox.isChecked()) {
            binding.scanSavePdfPasswordSwitch.setChecked(false);
        }
    }

    private void showHidePdfPasswordInput(boolean isChecked) {
        binding.scanSavePdfPasswordTextInput.setVisibility(isChecked ? View.VISIBLE : View.GONE);
        if (isChecked) {
            binding.scanSaveNestedScrollView.post(() -> binding.scanSaveNestedScrollView.fullScroll(ScrollView.FOCUS_DOWN));
        }
        if (isChecked) {
            KeyboardUtils.showSoftKeyboard(requireContext(), binding.scanSavePdfPasswordEt);
        } else {
            KeyboardUtils.hideKeyboardFrom(requireContext(), binding.scanSavePdfPasswordEt);
        }
    }

    private void enableFileNameEditing() {
        isFileNameEditable = !isFileNameEditable;
        binding.scanSaveFilenameInput.setEnabled(isFileNameEditable);
        if (isFileNameEditable) {
            binding.scanSaveFilenameInputEditBtn.setImageResource(R.drawable.ic_tick);
            KeyboardUtils.showSoftKeyboard(requireContext(), binding.scanSaveFilenameInput);
            binding.scanSaveFilenameInput.setSelection(binding.scanSaveFilenameInput.getText().toString().trim().length());
        } else {
            binding.scanSaveFilenameInputEditBtn.setImageResource(R.drawable.ic_pencil_edit);
            KeyboardUtils.hideKeyboardFrom(requireContext(), binding.scanSaveFilenameInput);
        }
    }


    @Override
    public void onClick(View view) {
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
        String fileName = binding.scanSaveFilenameInput.getText().toString().trim();
        if (TextUtils.isEmpty(fileName)) {
            DisplayUtils.showSnackMessage(requireActivity(), R.string.filename_empty);
            return;
        }

        if (!com.owncloud.android.lib.resources.files.FileUtils.isValidName(fileName)) {
            DisplayUtils.showSnackMessage(requireActivity(), R.string.filename_forbidden_charaters_from_server);
            return;
        }

        if (!binding.scanSaveWithoutTxtRecognitionPdfCheckbox.isChecked()
            && !binding.scanSaveWithoutTxtRecognitionJpgCheckbox.isChecked()
            && !binding.scanSaveWithoutTxtRecognitionPngCheckbox.isChecked()
            && !binding.scanSaveWithTxtRecognitionPdfCheckbox.isChecked()
            && !binding.scanSaveWithTxtRecognitionTxtCheckbox.isChecked()) {
            DisplayUtils.showSnackMessage(requireActivity(), R.string.scan_save_no_file_select_toast);
            return;
        }

        StringBuilder fileTypesStringBuilder = new StringBuilder();
        if (binding.scanSaveWithoutTxtRecognitionPdfCheckbox.isChecked()) {
            fileTypesStringBuilder.append(SAVE_TYPE_PDF);
            fileTypesStringBuilder.append(",");
        }
        if (binding.scanSaveWithoutTxtRecognitionJpgCheckbox.isChecked()) {
            fileTypesStringBuilder.append(SAVE_TYPE_JPG);
            fileTypesStringBuilder.append(",");
        }
        if (binding.scanSaveWithoutTxtRecognitionPngCheckbox.isChecked()) {
            fileTypesStringBuilder.append(SAVE_TYPE_PNG);
            fileTypesStringBuilder.append(",");
        }
        if (binding.scanSaveWithTxtRecognitionPdfCheckbox.isChecked()) {
            fileTypesStringBuilder.append(SAVE_TYPE_PDF_OCR);
            fileTypesStringBuilder.append(",");
        }
        if (binding.scanSaveWithTxtRecognitionTxtCheckbox.isChecked()) {
            fileTypesStringBuilder.append(SAVE_TYPE_TXT);
        }
        String pdfPassword = binding.scanSavePdfPasswordEt.getText().toString().trim();
        if (binding.scanSavePdfPasswordSwitch.isChecked() && TextUtils.isEmpty(pdfPassword)) {
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
        binding = null;
        super.onDestroyView();
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
        binding.scanSaveLocationInput.setText(path);
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
