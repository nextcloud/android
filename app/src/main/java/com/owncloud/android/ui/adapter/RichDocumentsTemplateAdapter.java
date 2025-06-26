/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2019 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2019 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.ui.adapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.nextcloud.client.account.CurrentAccountProvider;
import com.nextcloud.utils.GlideHelper;
import com.owncloud.android.R;
import com.owncloud.android.databinding.TemplateButtonBinding;
import com.owncloud.android.datamodel.Template;
import com.owncloud.android.lib.common.OwnCloudClientManagerFactory;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.ui.dialog.ChooseRichDocumentsTemplateDialogFragment;
import com.owncloud.android.utils.NextcloudServer;
import com.owncloud.android.utils.theme.ViewThemeUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

/**
 * Adapter for handling Templates, used to create files out of it via RichDocuments app
 */
public class RichDocumentsTemplateAdapter extends RecyclerView.Adapter<RichDocumentsTemplateAdapter.ViewHolder> {

    private List<Template> templateList = new ArrayList<>();
    private final ClickListener clickListener;
    private final Context context;
    private final ChooseRichDocumentsTemplateDialogFragment.Type type;
    private Template selectedTemplate;
    private final ViewThemeUtils viewThemeUtils;
    private final CurrentAccountProvider currentAccount;
    private final android.os.Handler handler = new Handler(Looper.getMainLooper());

    public RichDocumentsTemplateAdapter(
        CurrentAccountProvider currentAccount,
        ChooseRichDocumentsTemplateDialogFragment.Type type,
        ClickListener clickListener,
        Context context,
        ViewThemeUtils viewThemeUtils) {
        this.currentAccount = currentAccount;
        this.clickListener = clickListener;
        this.type = type;
        this.context = context;
        this.viewThemeUtils = viewThemeUtils;
    }

    @NonNull
    @Override
    @NextcloudServer(max = 18) // remove entire class
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new RichDocumentsTemplateAdapter.ViewHolder(
            TemplateButtonBinding.inflate(LayoutInflater.from(parent.getContext()),
                                          parent,
                                          false)
        );
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.setData(templateList.get(position));
    }

    public void setTemplateList(List<Template> templateList) {
        this.templateList = templateList;
    }

    @SuppressLint("NotifyDataSetChanged")
    public void setTemplateAsActive(Template template) {
        selectedTemplate = template;
        notifyDataSetChanged();
    }

    public Template getSelectedTemplate() {
        return selectedTemplate;
    }

    @Override
    public int getItemCount() {
        return templateList.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        private final TemplateButtonBinding binding;
        private Template template;

        public ViewHolder(@NonNull TemplateButtonBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
            viewThemeUtils.files.themeTemplateCardView(this.binding.templateContainer);
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            if (clickListener != null) {
                clickListener.onClick(template);
            }
        }

        public void setData(Template template) {
            this.template = template;

            int placeholder = switch (type) {
                case DOCUMENT -> R.drawable.file_doc;
                case SPREADSHEET -> R.drawable.file_xls;
                case PRESENTATION -> R.drawable.file_ppt;
            };


            new Thread(() -> {{
                try {
                    final var client = OwnCloudClientManagerFactory.getDefaultSingleton().getNextcloudClientFor(currentAccount.getUser().toOwnCloudAccount(), context);
                    handler.post(() -> GlideHelper.INSTANCE.loadIntoImageView(context, client, template.getThumbnailLink(), binding.template, placeholder, false));
                } catch (Exception e) {
                    Log_OC.e("RichDocumentsTemplateAdapter", "Exception setData: " + e);
                }
            }}).start();

            binding.templateName.setText(template.getName());
            binding.templateContainer.setChecked(template == selectedTemplate);
        }
    }

    public interface ClickListener {
        void onClick(Template template);
    }
}
