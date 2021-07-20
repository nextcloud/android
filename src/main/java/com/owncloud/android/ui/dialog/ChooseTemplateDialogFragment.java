/*
 * Nextcloud Android client application
 *
 * @author Tobias Kaminsky
 * @author Chris Narkiewicz
 *
 * Copyright (C) 2018 Tobias Kaminsky
 * Copyright (C) 2018 Nextcloud GmbH.
 * Copyright (C) 2019 Chris Narkiewicz <hello@ezaquarii.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package com.owncloud.android.ui.dialog;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager.LayoutParams;
import android.widget.Button;

import com.nextcloud.android.lib.resources.directediting.DirectEditingCreateFileRemoteOperation;
import com.nextcloud.android.lib.resources.directediting.DirectEditingObtainListOfTemplatesRemoteOperation;
import com.nextcloud.client.account.CurrentAccountProvider;
import com.nextcloud.client.account.User;
import com.nextcloud.client.di.Injectable;
import com.nextcloud.client.network.ClientFactory;
import com.owncloud.android.MainApp;
import com.owncloud.android.R;
import com.owncloud.android.databinding.ChooseTemplateBinding;
import com.owncloud.android.datamodel.FileDataStorageManager;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.lib.common.Creator;
import com.owncloud.android.lib.common.OwnCloudClient;
import com.owncloud.android.lib.common.Template;
import com.owncloud.android.lib.common.TemplateList;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.lib.resources.files.ReadFileRemoteOperation;
import com.owncloud.android.lib.resources.files.model.RemoteFile;
import com.owncloud.android.ui.activity.ExternalSiteWebView;
import com.owncloud.android.ui.activity.TextEditorWebView;
import com.owncloud.android.ui.adapter.TemplateAdapter;
import com.owncloud.android.utils.DisplayUtils;
import com.owncloud.android.utils.FileStorageUtils;
import com.owncloud.android.utils.theme.ThemeButtonUtils;
import com.owncloud.android.utils.theme.ThemeColorUtils;
import com.owncloud.android.utils.theme.ThemeTextInputUtils;

import java.lang.ref.WeakReference;

import javax.inject.Inject;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.GridLayoutManager;

/**
 * Dialog to show templates for new documents/spreadsheets/presentations.
 */
public class ChooseTemplateDialogFragment extends DialogFragment implements View.OnClickListener,
    TemplateAdapter.ClickListener, Injectable {

    private static final String ARG_PARENT_FOLDER = "PARENT_FOLDER";
    private static final String ARG_CREATOR = "CREATOR";
    private static final String ARG_HEADLINE = "HEADLINE";
    private static final String TAG = ChooseTemplateDialogFragment.class.getSimpleName();
    private static final String DOT = ".";
    public static final int SINGLE_TEMPLATE = 1;

    private TemplateAdapter adapter;
    private OCFile parentFolder;
    private String title;
    @Inject ClientFactory clientFactory;
    private Creator creator;
    @Inject CurrentAccountProvider currentAccount;
    private Button positiveButton;

    public enum Type {
        DOCUMENT,
        SPREADSHEET,
        PRESENTATION
    }

    ChooseTemplateBinding binding;

    public static ChooseTemplateDialogFragment newInstance(OCFile parentFolder, Creator creator, String headline) {
        ChooseTemplateDialogFragment frag = new ChooseTemplateDialogFragment();
        Bundle args = new Bundle();
        args.putParcelable(ARG_PARENT_FOLDER, parentFolder);
        args.putParcelable(ARG_CREATOR, creator);
        args.putString(ARG_HEADLINE, headline);
        frag.setArguments(args);
        return frag;
    }

    @Override
    public void onStart() {
        super.onStart();

        AlertDialog alertDialog = (AlertDialog) getDialog();

        positiveButton = alertDialog.getButton(AlertDialog.BUTTON_POSITIVE);
        ThemeButtonUtils.themeBorderlessButton(positiveButton, alertDialog.getButton(AlertDialog.BUTTON_NEUTRAL));
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

        parentFolder = arguments.getParcelable(ARG_PARENT_FOLDER);
        creator = arguments.getParcelable(ARG_CREATOR);
        title = arguments.getString(ARG_HEADLINE, getString(R.string.select_template));

        if (savedInstanceState == null) {
            title = arguments.getString(ARG_HEADLINE);
        } else {
            title = savedInstanceState.getString(ARG_HEADLINE);
        }

        // Inflate the layout for the dialog
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        binding = ChooseTemplateBinding.inflate(inflater, null, false);
        View view = binding.getRoot();

        binding.filename.requestFocus();
        ThemeTextInputUtils.colorTextInput(binding.filenameContainer,
                                           binding.filename,
                                           ThemeColorUtils.primaryColor(getContext()));

        binding.filename.setOnKeyListener((v, keyCode, event) -> {
            checkEnablingCreateButton();
            return false;
        });

        binding.filename.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // generated method stub
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // generated method stub
            }

            @Override
            public void afterTextChanged(Editable s) {
                checkEnablingCreateButton();
            }
        });

        try {
            User user = currentAccount.getUser();
            new FetchTemplateTask(this, clientFactory, user, creator).execute();
        } catch (Exception e) {
            Log_OC.e(TAG, "Loading stream url not possible: " + e);
        }

        binding.list.setHasFixedSize(true);
        binding.list.setLayoutManager(new GridLayoutManager(activity, 2));
        adapter = new TemplateAdapter(creator.getMimetype(), this, getContext(), currentAccount, clientFactory);
        binding.list.setAdapter(adapter);

        // Build the dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setView(view)
            .setPositiveButton(R.string.create, null)
            .setNeutralButton(R.string.common_cancel, null)
            .setTitle(title);
        Dialog dialog = builder.create();

        Window window = dialog.getWindow();

        if (window != null) {
            window.setSoftInputMode(LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        }

        return dialog;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        savedInstanceState.putString(ARG_HEADLINE, title);
    }

    private void createFromTemplate(Template template, String path) {
        new CreateFileFromTemplateTask(this, clientFactory, currentAccount.getUser(), template, path, creator).execute();
    }

    public void setTemplateList(TemplateList templateList) {
        adapter.setTemplateList(templateList);
        adapter.notifyDataSetChanged();
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
            binding.filename.setText(String.format("%s.%s", template.title, template.extension));
        }
        binding.filename.setSelection(binding.filename.getText().toString().lastIndexOf('.'));
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

    private void checkEnablingCreateButton() {
        Template selectedTemplate = adapter.getSelectedTemplate();
        String name = binding.filename.getText().toString();

        positiveButton.setEnabled(selectedTemplate != null && !name.isEmpty() &&
                                      !name.equalsIgnoreCase(DOT + selectedTemplate.getExtension()));
    }

    private static class CreateFileFromTemplateTask extends AsyncTask<Void, Void, String> {
        private ClientFactory clientFactory;
        private WeakReference<ChooseTemplateDialogFragment> chooseTemplateDialogFragmentWeakReference;
        private Template template;
        private String path;
        private Creator creator;
        private User user;
        private OCFile file;

        CreateFileFromTemplateTask(ChooseTemplateDialogFragment chooseTemplateDialogFragment,
                                   ClientFactory clientFactory,
                                   User user,
                                   Template template,
                                   String path,
                                   Creator creator
        ) {
            this.clientFactory = clientFactory;
            this.chooseTemplateDialogFragmentWeakReference = new WeakReference<>(chooseTemplateDialogFragment);
            this.template = template;
            this.path = path;
            this.creator = creator;
            this.user = user;
        }

        @Override
        protected String doInBackground(Void... voids) {
            try {
                OwnCloudClient client = clientFactory.create(user);

                RemoteOperationResult<String> result =
                    new DirectEditingCreateFileRemoteOperation(path,
                                                               creator.getEditor(),
                                                               creator.getId(),
                                                               template.getTitle()).execute(client);
                if (!result.isSuccess()) {
                    return "";
                }

                RemoteOperationResult newFileResult = new ReadFileRemoteOperation(path).execute(client);
                if (!newFileResult.isSuccess()) {
                    return "";
                }

                final ChooseTemplateDialogFragment fragment = chooseTemplateDialogFragmentWeakReference.get();
                if (fragment == null) {
                    return "";
                }

                final Context context = fragment.getContext();
                if (context == null) {
                    // fragment has been detached
                    return "";
                }

                FileDataStorageManager storageManager = new FileDataStorageManager(user.toPlatformAccount(),
                                                                                   context.getContentResolver());

                OCFile temp = FileStorageUtils.fillOCFile((RemoteFile) newFileResult.getData().get(0));
                storageManager.saveFile(temp);
                file = storageManager.getFileByPath(path);

                return result.getResultData();

            } catch (ClientFactory.CreationException e) {
                Log_OC.e(TAG, "Error creating file from template!", e);
                return "";
            }
        }

        @Override
        protected void onPostExecute(String url) {
            final ChooseTemplateDialogFragment fragment = chooseTemplateDialogFragmentWeakReference.get();

            if (fragment != null && fragment.isAdded()) {
                if (url.isEmpty()) {
                    DisplayUtils.showSnackMessage(fragment.binding.list, "Error creating file from template");
                } else {
                    Intent editorWebView = new Intent(MainApp.getAppContext(), TextEditorWebView.class);
                    editorWebView.putExtra(ExternalSiteWebView.EXTRA_TITLE, "Text");
                    editorWebView.putExtra(ExternalSiteWebView.EXTRA_URL, url);
                    editorWebView.putExtra(ExternalSiteWebView.EXTRA_FILE, file);
                    editorWebView.putExtra(ExternalSiteWebView.EXTRA_SHOW_SIDEBAR, false);
                    fragment.startActivity(editorWebView);

                    fragment.dismiss();
                }
            } else {
                Log_OC.e(TAG, "Error creating file from template!");
            }
        }
    }

    private static class FetchTemplateTask extends AsyncTask<Void, Void, TemplateList> {

        private User user;
        private ClientFactory clientFactory;
        private WeakReference<ChooseTemplateDialogFragment> chooseTemplateDialogFragmentWeakReference;
        private Creator creator;

        FetchTemplateTask(ChooseTemplateDialogFragment chooseTemplateDialogFragment,
                          ClientFactory clientFactory,
                          User user,
                          Creator creator) {
            this.user = user;
            this.clientFactory = clientFactory;
            this.chooseTemplateDialogFragmentWeakReference = new WeakReference<>(chooseTemplateDialogFragment);
            this.creator = creator;
        }

        @Override
        protected TemplateList doInBackground(Void... voids) {

            try {
                OwnCloudClient client = clientFactory.create(user);
                RemoteOperationResult<TemplateList> result =
                    new DirectEditingObtainListOfTemplatesRemoteOperation(creator.getEditor(),
                                                                          creator.getId())
                        .execute(client);

                if (!result.isSuccess()) {
                    return new TemplateList();
                }

                return result.getResultData();
            } catch (ClientFactory.CreationException e) {
                Log_OC.e(TAG, "Could not fetch template", e);

                return new TemplateList();
            }
        }

        @Override
        protected void onPostExecute(TemplateList templateList) {
            ChooseTemplateDialogFragment fragment = chooseTemplateDialogFragmentWeakReference.get();

            if (fragment != null && fragment.isAdded()) {
                if (templateList.templates.isEmpty()) {
                    DisplayUtils.showSnackMessage(fragment.binding.list, R.string.error_retrieving_templates);
                } else {
                    if (templateList.templates.size() == SINGLE_TEMPLATE) {
                        fragment.onTemplateChosen(templateList.templates.values().iterator().next());
                        fragment.binding.list.setVisibility(View.GONE);
                    } else {
                        String name = DOT + templateList.templates.values().iterator().next().getExtension();
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
