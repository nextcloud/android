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

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.PorterDuff;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager.LayoutParams;
import android.widget.EditText;

import com.nextcloud.android.lib.resources.directediting.DirectEditingCreateFileRemoteOperation;
import com.nextcloud.android.lib.resources.directediting.DirectEditingObtainListOfTemplatesRemoteOperation;
import com.nextcloud.client.account.CurrentAccountProvider;
import com.nextcloud.client.account.User;
import com.nextcloud.client.di.Injectable;
import com.nextcloud.client.network.ClientFactory;
import com.owncloud.android.MainApp;
import com.owncloud.android.R;
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
import com.owncloud.android.utils.ThemeUtils;

import java.lang.ref.WeakReference;

import javax.inject.Inject;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Dialog to show templates for new documents/spreadsheets/presentations.
 */
public class ChooseTemplateDialogFragment extends DialogFragment implements DialogInterface.OnClickListener,
    TemplateAdapter.ClickListener, Injectable {

    private static final String ARG_PARENT_FOLDER = "PARENT_FOLDER";
    private static final String ARG_CREATOR = "CREATOR";
    private static final String TAG = ChooseTemplateDialogFragment.class.getSimpleName();
    private static final String DOT = ".";

    private TemplateAdapter adapter;
    private OCFile parentFolder;
    @Inject ClientFactory clientFactory;
    private Creator creator;
    @Inject CurrentAccountProvider currentAccount;

    public enum Type {
        DOCUMENT,
        SPREADSHEET,
        PRESENTATION
    }

    private Template defaultTemplate;

    @BindView(R.id.list)
    RecyclerView listView;

    @BindView(R.id.filename)
    EditText fileName;

    public static ChooseTemplateDialogFragment newInstance(OCFile parentFolder, Creator creator) {
        ChooseTemplateDialogFragment frag = new ChooseTemplateDialogFragment();
        Bundle args = new Bundle();
        args.putParcelable(ARG_PARENT_FOLDER, parentFolder);
        args.putParcelable(ARG_CREATOR, creator);
        frag.setArguments(args);
        return frag;

    }

    @Override
    public void onStart() {
        super.onStart();

        int color = ThemeUtils.primaryAccentColor(getContext());

        AlertDialog alertDialog = (AlertDialog) getDialog();

        alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(color);
        alertDialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(color);
        alertDialog.getButton(AlertDialog.BUTTON_NEGATIVE).setOnClickListener(view -> {
            onClick(defaultTemplate);
        });
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

        int accentColor = ThemeUtils.primaryAccentColor(getContext());

        parentFolder = arguments.getParcelable(ARG_PARENT_FOLDER);
        creator = arguments.getParcelable(ARG_CREATOR);

        // Inflate the layout for the dialog
        LayoutInflater inflater = activity.getLayoutInflater();
        @SuppressLint("InflateParams") View view = inflater.inflate(R.layout.choose_template, null);
        ButterKnife.bind(this, view);

        fileName.requestFocus();
        fileName.getBackground().setColorFilter(accentColor, PorterDuff.Mode.SRC_ATOP);

        try {
            User user = currentAccount.getUser();
            new FetchTemplateTask(this, clientFactory, user, creator).execute();
        } catch (Exception e) {
            Log_OC.e(TAG, "Loading stream url not possible: " + e);
        }

        listView.setHasFixedSize(true);
        listView.setLayoutManager(new GridLayoutManager(activity, 2));
        adapter = new TemplateAdapter(creator.getMimetype(), this, getContext(), currentAccount, clientFactory);
        listView.setAdapter(adapter);

        // Build the dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setView(view)
            .setNegativeButton(R.string.common_cancel, this)
            .setPositiveButton(R.string.common_save, null)
            .setTitle(R.string.select_template);
        Dialog dialog = builder.create();

        Window window = dialog.getWindow();

        if (window != null) {
            window.setSoftInputMode(LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        }

        return dialog;
    }

    private void createFromTemplate(Template template, String path) {
        new CreateFileFromTemplateTask(this, clientFactory, currentAccount.getUser(), template, path, creator).execute();
    }

    public void setTemplateList(TemplateList templateList) {
        if (templateList.getTemplateList()!=null&&templateList.getTemplateList().size()>0){
            defaultTemplate = templateList.getTemplateList().get(0);
        }
        adapter.setTemplateList(templateList);
        adapter.notifyDataSetChanged();
    }

    @Override
    public void onClick(Template template) {
        String name = fileName.getText().toString();
        String path = parentFolder.getRemotePath() + name;

        if (name.isEmpty() || name.equalsIgnoreCase(DOT + template.getExtension())) {
            DisplayUtils.showSnackMessage(listView, R.string.enter_filename);
        } else if (!name.endsWith(template.getExtension())) {
            createFromTemplate(template, path + DOT + template.getExtension());
        } else {
            createFromTemplate(template, path);
        }
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        // cancel is handled by dialog itself, no other button available
        onClick(defaultTemplate);
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

                RemoteOperationResult result =
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

                return result.getData().get(0).toString();

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
                    DisplayUtils.showSnackMessage(fragment.listView, "Error creating file from template");
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
                RemoteOperationResult result = new DirectEditingObtainListOfTemplatesRemoteOperation(creator.getEditor(),
                                                                                                     creator.getId())
                    .execute(client);

                if (!result.isSuccess()) {
                    return new TemplateList();
                }

                return (TemplateList) result.getSingleData();
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
                    DisplayUtils.showSnackMessage(fragment.listView, R.string.error_retrieving_templates);
                } else {
                    fragment.setTemplateList(templateList);

                    String name = DOT + templateList.templates.values().iterator().next().getExtension();
                    fragment.fileName.setText(name);
                }
            } else {
                Log_OC.e(TAG, "Error streaming file: no previewMediaFragment!");
            }
        }
    }
}
