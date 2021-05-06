package com.nmc.android.ui;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.ScrollView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.nmc.android.OnDocScanListener;
import com.nmc.android.OnFragmentChangeListener;
import com.nmc.android.adapters.ViewPagerFragmentAdapter;
import com.nmc.android.utils.KeyboardUtils;
import com.owncloud.android.R;
import com.owncloud.android.lib.resources.files.FileUtils;
import com.owncloud.android.utils.DisplayUtils;
import com.owncloud.android.utils.theme.ThemeCheckableUtils;
import com.owncloud.android.utils.theme.ThemeTextInputUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
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
import io.scanbot.sdk.ScanbotSDK;
import io.scanbot.sdk.process.PDFRenderer;

public class SaveScannedDocumentFragment extends Fragment implements CompoundButton.OnCheckedChangeListener {

    protected static final String TAG = "SaveScannedDocumentFragment";

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

    private ScanbotSDK scanbotSDK;
    private PDFRenderer pdfRenderer;

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
    }

    private void initViews() {
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
    }


    @OnClick({R.id.scan_save_filename_input_edit_btn, R.id.scan_save_location_edit_btn, R.id.save_scan_btn_cancel, R.id.save_scan_btn_save})
    void onClickListener(View view) {
        switch (view.getId()) {
            case R.id.scan_save_filename_input_edit_btn:
                enableFileNameEditing();
                break;
            case R.id.scan_save_location_edit_btn:
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

        if (!FileUtils.isValidName(fileName)) {
            DisplayUtils.showSnackMessage(requireActivity(), R.string.filename_forbidden_charaters_from_server);
            return;
        }

        if (!pdfCheckBox.isChecked() && !jpgCheckBox.isChecked() && !pngCheckBox.isChecked() && !pdfOcrCheckBox.isChecked()
            && !txtFileCheckBox.isChecked()) {
            DisplayUtils.showSnackMessage(requireActivity(), R.string.scan_save_no_file_select_toast);
            return;
        }
        //do file save
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
}
