package com.nmc.android.ui;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.nmc.android.OnDocScanListener;
import com.nmc.android.OnFragmentChangeListener;
import com.nmc.android.adapters.ViewPagerFragmentAdapter;
import com.owncloud.android.R;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatCheckBox;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.appcompat.widget.SwitchCompat;
import androidx.fragment.app.Fragment;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;

public class SaveScannedDocumentFragment extends Fragment {

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

    @BindView(R.id.scan_save_filename_input)
    TextInputEditText fileNameInput;
    @BindView(R.id.scan_save_filename_input_edit_btn)
    AppCompatImageView fileNameEditBtn;
    @BindView(R.id.scan_save_location_input)
    TextInputEditText locationInput;
    @BindView(R.id.scan_save_location_edit_btn)
    AppCompatImageView locationEditBtn;
    @BindView(R.id.scan_save_file_type_input)
    TextInputEditText fileTypeInput;
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
    }

    @OnClick({R.id.scan_save_filename_input_edit_btn, R.id.scan_save_location_edit_btn, R.id.save_scan_btn_cancel, R.id.save_scan_btn_save})
    void onClickListener(View view) {
        switch (view.getId()) {
            case R.id.scan_save_filename_input_edit_btn:
                break;
            case R.id.scan_save_location_edit_btn:
                break;
            case R.id.save_scan_btn_cancel:
                break;
            case R.id.save_scan_btn_save:
                break;

        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (unbinder != null) {
            unbinder.unbind();
        }
    }

}
