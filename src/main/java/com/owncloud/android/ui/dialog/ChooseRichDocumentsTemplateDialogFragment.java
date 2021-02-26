/*
 *
 * Nextcloud Android client application
 *
 * @author Tobias Kaminsky
 * Copyright (C) 2019 Tobias Kaminsky
 * Copyright (C) 2019 Nextcloud GmbH
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
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager.LayoutParams;
import android.widget.Button;

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
import com.owncloud.android.utils.NextcloudServer;
import com.owncloud.android.utils.theme.ThemeButtonUtils;
import com.owncloud.android.utils.theme.ThemeColorUtils;
import com.owncloud.android.utils.theme.ThemeTextInputUtils;

import org.parceler.Parcels;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

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

    private RichDocumentsTemplateAdapter adapter;
    private OCFile parentFolder;
    private OwnCloudClient client;
    @Inject CurrentAccountProvider currentAccount;
    @Inject ClientFactory clientFactory;
    private Button positiveButton;

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

        positiveButton = alertDialog.getButton(AlertDialog.BUTTON_POSITIVE);
        ThemeButtonUtils.themeBorderlessButton(positiveButton,
                                               alertDialog.getButton(AlertDialog.BUTTON_NEUTRAL));
        positiveButton.setOnClickListener(this);
        positiveButton.setEnabled(false);

        checkEnablingCreateButton();
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

        // Inflate the layout for the dialog
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        binding = ChooseTemplateBinding.inflate(inflater, null, false);
        View view = binding.getRoot();

        binding.filename.requestFocus();
        ThemeTextInputUtils.colorTextInput(binding.filenameContainer,
                                           binding.filename,
                                           ThemeColorUtils.primaryColor(getContext()));

        Type type = Type.valueOf(arguments.getString(ARG_TYPE));
        new FetchTemplateTask(this, client).execute(type);

        binding.list.setHasFixedSize(true);
        binding.list.setLayoutManager(new GridLayoutManager(activity, 2));
        adapter = new RichDocumentsTemplateAdapter(type, this, getContext(), currentAccount, clientFactory);
        binding.list.setAdapter(adapter);

        // Build the dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setView(view)
            .setPositiveButton(R.string.create, null)
            .setNeutralButton(R.string.common_cancel, null)
            .setTitle(getTitle(type));
        Dialog dialog = builder.create();

        Window window = dialog.getWindow();

        if (window != null) {
            window.setSoftInputMode(LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        }

        return dialog;
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
            binding.filename.setText(String.format("%s.%s", template.name, template.extension));
        }
        binding.filename.setSelection(binding.filename.getText().toString().lastIndexOf('.'));
    }

    private void checkEnablingCreateButton() {
        Template selectedTemplate = adapter.getSelectedTemplate();
        String name = binding.filename.getText().toString();

        positiveButton.setEnabled(selectedTemplate != null && !name.isEmpty() &&
                                      !name.equalsIgnoreCase(DOT + selectedTemplate.getExtension()));
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
                            user.toPlatformAccount(),
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
                if (url.isEmpty()) {
                    DisplayUtils.showSnackMessage(fragment.binding.list, "Error creating file from template");
                } else {
                    Intent collaboraWebViewIntent = new Intent(MainApp.getAppContext(), RichDocumentsEditorWebView.class);
                    collaboraWebViewIntent.putExtra(ExternalSiteWebView.EXTRA_TITLE, "Collabora");
                    collaboraWebViewIntent.putExtra(ExternalSiteWebView.EXTRA_URL, url);
                    collaboraWebViewIntent.putExtra(ExternalSiteWebView.EXTRA_FILE, file);
                    collaboraWebViewIntent.putExtra(ExternalSiteWebView.EXTRA_SHOW_SIDEBAR, false);
                    collaboraWebViewIntent.putExtra(ExternalSiteWebView.EXTRA_TEMPLATE, Parcels.wrap(template));
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
                    DisplayUtils.showSnackMessage(fragment.binding.list, R.string.error_retrieving_templates);
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
