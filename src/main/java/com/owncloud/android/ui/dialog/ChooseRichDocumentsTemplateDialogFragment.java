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

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
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

import com.nextcloud.client.account.CurrentAccountProvider;
import com.nextcloud.client.account.User;
import com.nextcloud.client.di.Injectable;
import com.nextcloud.client.network.ClientFactory;
import com.owncloud.android.MainApp;
import com.owncloud.android.R;
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
import com.owncloud.android.utils.ThemeUtils;

import org.parceler.Parcels;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

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
public class ChooseRichDocumentsTemplateDialogFragment extends DialogFragment implements DialogInterface.OnClickListener,
    RichDocumentsTemplateAdapter.ClickListener, Injectable {

    private static final String ARG_PARENT_FOLDER = "PARENT_FOLDER";
    private static final String ARG_TYPE = "TYPE";
    private static final String TAG = ChooseRichDocumentsTemplateDialogFragment.class.getSimpleName();
    private static final String DOT = ".";

    private RichDocumentsTemplateAdapter adapter;
    private OCFile parentFolder;
    private OwnCloudClient client;
    @Inject CurrentAccountProvider currentAccount;
    @Inject ClientFactory clientFactory;

    public enum Type {
        DOCUMENT,
        SPREADSHEET,
        PRESENTATION
    }

    @BindView(R.id.list)
    RecyclerView listView;

    @BindView(R.id.filename)
    EditText fileName;

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

        int color = ThemeUtils.primaryAccentColor(getContext());

        AlertDialog alertDialog = (AlertDialog) getDialog();

        alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(color);
        alertDialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(color);
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

        // Inflate the layout for the dialog
        LayoutInflater inflater = activity.getLayoutInflater();
        @SuppressLint("InflateParams") View view = inflater.inflate(R.layout.choose_template, null);
        ButterKnife.bind(this, view);

        fileName.requestFocus();
        fileName.getBackground().setColorFilter(accentColor, PorterDuff.Mode.SRC_ATOP);

        try {
            client = clientFactory.create(currentAccount.getUser());
        } catch (ClientFactory.CreationException e) {
            throw new RuntimeException(e); // we'll NPE without the client
        }

        Type type = Type.valueOf(arguments.getString(ARG_TYPE));
        new FetchTemplateTask(this, client).execute(type);

        listView.setHasFixedSize(true);
        listView.setLayoutManager(new GridLayoutManager(activity, 2));
        adapter = new RichDocumentsTemplateAdapter(type, this, getContext(), currentAccount, clientFactory);
        listView.setAdapter(adapter);

        // Build the dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setView(view)
            .setNegativeButton(R.string.common_cancel, this)
            .setTitle(R.string.select_template);
        Dialog dialog = builder.create();

        Window window = dialog.getWindow();

        if (window != null) {
            window.setSoftInputMode(LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        }

        return dialog;
    }

    private void createFromTemplate(Template template, String path) {
        new CreateFileFromTemplateTask(this, client, template, path, currentAccount.getUser()).execute();
    }

    public void setTemplateList(List<Template> templateList) {
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
                    DisplayUtils.showSnackMessage(fragment.listView, "Error creating file from template");
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
                    DisplayUtils.showSnackMessage(fragment.listView, R.string.error_retrieving_templates);
                } else {
                    fragment.setTemplateList(templateList);

                    String name = DOT + templateList.get(0).getExtension();
                    fragment.fileName.setText(name);
                }
            } else {
                Log_OC.e(TAG, "Error streaming file: no previewMediaFragment!");
            }
        }
    }
}
