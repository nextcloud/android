/*
 *
 * Nextcloud Android client application
 *
 * @author Tobias Kaminsky
 * @author TSI-mc
 * Copyright (C) 2019 Tobias Kaminsky
 * Copyright (C) 2019 Nextcloud GmbH
 * Copyright (C) 2023 TSI-mc
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

package com.owncloud.android.ui.dialog;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.common.collect.Sets;
import com.nextcloud.client.account.CurrentAccountProvider;
import com.nextcloud.client.account.User;
import com.nextcloud.client.di.Injectable;
import com.nextcloud.client.network.ClientFactory;
import com.owncloud.android.MainApp;
import com.owncloud.android.R;
import com.owncloud.android.databinding.ChooseTemplateBinding;
import com.owncloud.android.datamodel.FileDataStorageManager;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.datamodel.Template;
import com.owncloud.android.files.CreateFileFromTemplateOperation;
import com.owncloud.android.files.FetchTemplateOperation;
import com.owncloud.android.lib.common.OwnCloudClient;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.lib.resources.files.ReadFileRemoteOperation;
import com.owncloud.android.lib.resources.files.model.RemoteFile;
import com.owncloud.android.ui.activity.ExternalSiteWebView;
import com.owncloud.android.ui.activity.RichDocumentsEditorWebView;
import com.owncloud.android.ui.adapter.RichDocumentsTemplateAdapter;
import com.owncloud.android.utils.DisplayUtils;
import com.owncloud.android.utils.FileStorageUtils;
import com.owncloud.android.utils.KeyboardUtils;
import com.owncloud.android.utils.NextcloudServer;
import com.owncloud.android.utils.theme.ViewThemeUtils;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import javax.inject.Inject;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.GridLayoutManager;

/**
 * Dialog to show templates for new documents/spreadsheets/presentations.
 */
public class ChooseRichDocumentsTemplateDialogFragment extends DialogFragment implements View.OnClickListener,
    RichDocumentsTemplateAdapter.ClickListener, Injectable {

    private static final String ARG_PARENT_FOLDER = "PARENT_FOLDER";
    private static final String ARG_TYPE = "TYPE";
    private static final String TAG = ChooseRichDocumentsTemplateDialogFragment.class.getSimpleName();
    private static final String DOT = ".";
    public static final int SINGLE_TEMPLATE = 1;
    private static final String WAIT_DIALOG_TAG = "WAIT";

    private Set<String> fileNames;

    @Inject CurrentAccountProvider currentAccount;
    @Inject ClientFactory clientFactory;
    @Inject ViewThemeUtils viewThemeUtils;
    @Inject FileDataStorageManager fileDataStorageManager;
    @Inject KeyboardUtils keyboardUtils;
    private RichDocumentsTemplateAdapter adapter;
    private OCFile parentFolder;
    private OwnCloudClient client;
    private MaterialButton positiveButton;
    private DialogFragment waitDialog;

    public enum Type {
        DOCUMENT,
        SPREADSHEET,
        PRESENTATION
    }

    ChooseTemplateBinding binding;

    @NextcloudServer(max = 18) // will be removed in favor of generic direct editing
    public static ChooseRichDocumentsTemplateDialogFragment newInstance(OCFile parentFolder, Type type) {
        ChooseRichDocumentsTemplateDialogFragment frag = new ChooseRichDocumentsTemplateDialogFragment();
        Bundle args = new Bundle();
        args.putParcelable(ARG_PARENT_FOLDER, parentFolder);
        args.putString(ARG_TYPE, type.name());
        frag.setArguments(args);
        return frag;
    }

    @Override
    public void onStart() {
        super.onStart();

        AlertDialog alertDialog = (AlertDialog) getDialog();

        if (alertDialog != null) {
            positiveButton = (MaterialButton) alertDialog.getButton(AlertDialog.BUTTON_POSITIVE);
            viewThemeUtils.material.colorMaterialButtonPrimaryTonal(positiveButton);

            MaterialButton negativeButton = (MaterialButton) alertDialog.getButton(AlertDialog.BUTTON_NEGATIVE);
            if (negativeButton != null) {
                viewThemeUtils.material.colorMaterialButtonPrimaryBorderless(negativeButton);
            }

            positiveButton.setOnClickListener(this);
            positiveButton.setEnabled(false);
        }

        checkEnablingCreateButton();
    }

    @Override
    public void onResume() {
        super.onResume();
        keyboardUtils.showKeyboardForEditText(requireDialog().getWindow(), binding.filename);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Bundle arguments = getArguments();
        if (arguments == null) {
            throw new IllegalArgumentException("Arguments may not be null");
        }

        Activity activity = getActivity();
        if (activity == null) {
            throw new IllegalArgumentException("Activity may not be null");
        }

        try {
            client = clientFactory.create(currentAccount.getUser());
        } catch (ClientFactory.CreationException e) {
            throw new RuntimeException(e); // we'll NPE without the client
        }

        parentFolder = arguments.getParcelable(ARG_PARENT_FOLDER);
        List<OCFile> folderContent = fileDataStorageManager.getFolderContent(parentFolder, false);
        fileNames = Sets.newHashSetWithExpectedSize(folderContent.size());

        for (OCFile file : folderContent) {
            fileNames.add(file.getFileName());
        }

        // Inflate the layout for the dialog
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        binding = ChooseTemplateBinding.inflate(inflater, null, false);
        View view = binding.getRoot();

        viewThemeUtils.material.colorTextInputLayout(binding.filenameContainer);

        Type type = Type.valueOf(arguments.getString(ARG_TYPE));
        new FetchTemplateTask(this, client).execute(type);

        binding.list.setHasFixedSize(true);
        binding.list.setLayoutManager(new GridLayoutManager(activity, 2));
        adapter = new RichDocumentsTemplateAdapter(type,
                                                   this,
                                                   getContext(),
                                                   currentAccount,
                                                   clientFactory,
                                                   viewThemeUtils);
        binding.list.setAdapter(adapter);

        binding.filename.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // not needed
            }

            @Override
            public void afterTextChanged(Editable s) {
                checkEnablingCreateButton();
            }
        });

        int titleTextId = getTitle(type);

        // Build the dialog
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(activity);
        builder.setView(view)
            .setPositiveButton(R.string.create, null)
            .setNegativeButton(R.string.common_cancel, null)
            .setTitle(titleTextId);

        viewThemeUtils.dialog.colorMaterialAlertDialogBackground(activity, builder);

        return builder.create();
    }

    private int getTitle(Type type) {
        if (type == Type.DOCUMENT) {
            return R.string.create_new_document;
        } else if (type == Type.SPREADSHEET) {
            return R.string.create_new_spreadsheet;
        } else if (type == Type.PRESENTATION) {
            return R.string.create_new_presentation;
        }

        return R.string.select_template;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private void createFromTemplate(Template template, String path) {
        waitDialog = IndeterminateProgressDialog.newInstance(R.string.wait_a_moment, false);
        waitDialog.show(getParentFragmentManager(), WAIT_DIALOG_TAG);
        new CreateFileFromTemplateTask(this, client, template, path, currentAccount.getUser()).execute();
    }

    public void setTemplateList(List<Template> templateList) {
        adapter.setTemplateList(templateList);
        adapter.notifyDataSetChanged();
    }

    @Override
    public void onClick(View v) {
        String name = binding.filename.getText().toString();
        String path = parentFolder.getRemotePath() + name;

        Template selectedTemplate = adapter.getSelectedTemplate();

        if (selectedTemplate == null) {
            DisplayUtils.showSnackMessage(binding.list, R.string.select_one_template);
        } else if (name.isEmpty() || name.equalsIgnoreCase(DOT + selectedTemplate.getExtension())) {
            DisplayUtils.showSnackMessage(binding.list, R.string.enter_filename);
        } else if (!name.endsWith(selectedTemplate.getExtension())) {
            createFromTemplate(selectedTemplate, path + DOT + selectedTemplate.getExtension());
        } else {
            createFromTemplate(selectedTemplate, path);
        }
    }

    @Override
    public void onClick(Template template) {
        onTemplateChosen(template);
    }

    private void onTemplateChosen(Template template) {
        adapter.setTemplateAsActive(template);
        prefillFilenameIfEmpty(template);
        checkEnablingCreateButton();
    }

    private void prefillFilenameIfEmpty(Template template) {
        String name = binding.filename.getText().toString();
        if (name.isEmpty() || name.equalsIgnoreCase(DOT + template.getExtension())) {
            binding.filename.setText(String.format("%s.%s", template.getName(), template.getExtension()));
        }

        final int dotIndex = binding.filename.getText().toString().lastIndexOf('.');
        if (dotIndex >= 0) {
            binding.filename.setSelection(dotIndex);
        }
    }

    private void checkEnablingCreateButton() {
        if (positiveButton != null) {
            Template selectedTemplate = adapter.getSelectedTemplate();
            String name = Objects.requireNonNull(binding.filename.getText()).toString();
            boolean isNameJustExtension = selectedTemplate != null && name.equalsIgnoreCase(
                DOT + selectedTemplate.getExtension());
            boolean isNameEmpty = name.isEmpty() || isNameJustExtension;
            boolean state = selectedTemplate != null && !isNameEmpty && !fileNames.contains(name);

            positiveButton.setEnabled(selectedTemplate != null && !name.isEmpty() &&
                                          !name.equalsIgnoreCase(DOT + selectedTemplate.getExtension()));
            positiveButton.setEnabled(state);
            positiveButton.setClickable(state);
            binding.filenameContainer.setErrorEnabled(!state);

            if (!state) {
                if (isNameEmpty) {
                    binding.filenameContainer.setError(getText(R.string.filename_empty));
                } else {
                    binding.filenameContainer.setError(getText(R.string.file_already_exists));
                }
            }
        }
    }

    private static class CreateFileFromTemplateTask extends AsyncTask<Void, Void, String> {
        private OwnCloudClient client;
        private WeakReference<ChooseRichDocumentsTemplateDialogFragment> chooseTemplateDialogFragmentWeakReference;
        private Template template;
        private String path;
        private User user;
        private OCFile file;

        CreateFileFromTemplateTask(ChooseRichDocumentsTemplateDialogFragment chooseTemplateDialogFragment,
                                   OwnCloudClient client,
                                   Template template,
                                   String path,
                                   User user
                                  ) {
            this.client = client;
            this.chooseTemplateDialogFragmentWeakReference = new WeakReference<>(chooseTemplateDialogFragment);
            this.template = template;
            this.path = path;
            this.user = user;
        }

        @Override
        protected String doInBackground(Void... voids) {
            RemoteOperationResult result = new CreateFileFromTemplateOperation(path, template.getId()).execute(client);

            if (result.isSuccess()) {
                // get file
                RemoteOperationResult newFileResult = new ReadFileRemoteOperation(path).execute(client);

                if (newFileResult.isSuccess()) {
                    OCFile temp = FileStorageUtils.fillOCFile((RemoteFile) newFileResult.getData().get(0));

                    if (chooseTemplateDialogFragmentWeakReference.get() != null) {
                        FileDataStorageManager storageManager = new FileDataStorageManager(
                            user,
                            chooseTemplateDialogFragmentWeakReference.get().requireContext().getContentResolver());
                        storageManager.saveFile(temp);
                        file = storageManager.getFileByPath(path);

                        return result.getData().get(0).toString();
                    } else {
                        return "";
                    }
                } else {
                    return "";
                }
            } else {
                return "";
            }
        }

        @Override
        protected void onPostExecute(String url) {
            ChooseRichDocumentsTemplateDialogFragment fragment = chooseTemplateDialogFragmentWeakReference.get();

            if (fragment != null && fragment.isAdded()) {
                if (fragment.waitDialog != null) {
                    fragment.waitDialog.dismiss();
                }

                if (url.isEmpty()) {
                    fragment.dismiss();
                    DisplayUtils.showSnackMessage(fragment.requireActivity(), R.string.error_creating_file_from_template);
                } else {
                    Intent collaboraWebViewIntent = new Intent(MainApp.getAppContext(), RichDocumentsEditorWebView.class);
                    collaboraWebViewIntent.putExtra(ExternalSiteWebView.EXTRA_TITLE, "Collabora");
                    collaboraWebViewIntent.putExtra(ExternalSiteWebView.EXTRA_URL, url);
                    collaboraWebViewIntent.putExtra(ExternalSiteWebView.EXTRA_FILE, file);
                    collaboraWebViewIntent.putExtra(ExternalSiteWebView.EXTRA_SHOW_SIDEBAR, false);
                    collaboraWebViewIntent.putExtra(ExternalSiteWebView.EXTRA_TEMPLATE, template);
                    fragment.startActivity(collaboraWebViewIntent);

                    fragment.dismiss();
                }
            } else {
                Log_OC.e(TAG, "Error creating file from template!");
            }
        }
    }

    private static class FetchTemplateTask extends AsyncTask<Type, Void, List<Template>> {

        private OwnCloudClient client;
        private WeakReference<ChooseRichDocumentsTemplateDialogFragment> chooseTemplateDialogFragmentWeakReference;

        FetchTemplateTask(ChooseRichDocumentsTemplateDialogFragment chooseTemplateDialogFragment, OwnCloudClient client) {
            this.client = client;
            this.chooseTemplateDialogFragmentWeakReference = new WeakReference<>(chooseTemplateDialogFragment);
        }

        @Override
        protected List<Template> doInBackground(Type... type) {
            FetchTemplateOperation fetchTemplateOperation = new FetchTemplateOperation(type[0]);
            RemoteOperationResult result = fetchTemplateOperation.execute(client);

            if (!result.isSuccess()) {
                return new ArrayList<>();
            }

            List<Template> templateList = new ArrayList<>();
            for (Object object : result.getData()) {
                templateList.add((Template) object);
            }

            return templateList;
        }

        @Override
        protected void onPostExecute(List<Template> templateList) {
            ChooseRichDocumentsTemplateDialogFragment fragment = chooseTemplateDialogFragmentWeakReference.get();

            if (fragment != null) {
                if (templateList.isEmpty()) {
                    fragment.dismiss();
                    DisplayUtils.showSnackMessage(fragment.requireActivity(), R.string.error_retrieving_templates);
                } else {
                    if (templateList.size() == SINGLE_TEMPLATE) {
                        fragment.onTemplateChosen(templateList.get(0));
                        fragment.binding.list.setVisibility(View.GONE);
                    } else {
                        String name = DOT + templateList.get(0).getExtension();
                        fragment.binding.filename.setText(name);
                        fragment.binding.helperText.setVisibility(View.VISIBLE);
                    }

                    fragment.setTemplateList(templateList);
                }
            } else {
                Log_OC.e(TAG, "Error streaming file: no previewMediaFragment!");
            }
        }
    }
}
