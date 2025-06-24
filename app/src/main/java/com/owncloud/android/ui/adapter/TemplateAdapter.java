/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2024 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-FileCopyrightText: 2019 Chris Narkiewicz <hello@ezaquarii.com>
 * SPDX-FileCopyrightText: 2018 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2018 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.ui.adapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.nextcloud.client.account.CurrentAccountProvider;
import com.nextcloud.client.network.ClientFactory;
import com.nextcloud.utils.GlideHelper;
import com.owncloud.android.databinding.TemplateButtonBinding;
import com.owncloud.android.lib.common.Template;
import com.owncloud.android.lib.common.TemplateList;
import com.owncloud.android.utils.MimeTypeUtil;
import com.owncloud.android.utils.theme.ViewThemeUtils;

import java.util.ArrayList;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

/**
 * Adapter for handling Templates, used to create files out of it via RichDocuments app
 */
public class TemplateAdapter extends RecyclerView.Adapter<TemplateAdapter.ViewHolder> {

    private TemplateList templateList = new TemplateList();
    private final ClickListener clickListener;
    private final Context context;
    private final CurrentAccountProvider currentAccountProvider;
    private final ClientFactory clientFactory;
    private final String mimetype;
    private Template selectedTemplate;
    private final ViewThemeUtils viewThemeUtils;

    public TemplateAdapter(
        String mimetype,
        ClickListener clickListener,
        Context context,
        CurrentAccountProvider currentAccountProvider,
        ClientFactory clientFactory,
        ViewThemeUtils viewThemeUtils
                          ) {
        this.mimetype = mimetype;
        this.clickListener = clickListener;
        this.context = context;
        this.currentAccountProvider = currentAccountProvider;
        this.clientFactory = clientFactory;
        this.viewThemeUtils = viewThemeUtils;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new TemplateAdapter.ViewHolder(
            TemplateButtonBinding.inflate(LayoutInflater.from(parent.getContext()),
                                          parent,
                                          false)
        );
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.setData(new ArrayList<>(templateList.getTemplates().values()).get(position));
    }

    public void setTemplateList(TemplateList templateList) {
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
        return templateList.getTemplates().size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        private final TemplateButtonBinding binding;
        private Template template;

        public ViewHolder(@NonNull TemplateButtonBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
            viewThemeUtils.files.themeTemplateCardView(this.binding.templateContainer);
            binding.templateLayout.setOnClickListener(this);
            binding.templateContainer.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            if (clickListener != null) {
                clickListener.onClick(template);
            }
        }

        public void setData(Template template) {
            this.template = template;

            Drawable placeholder = MimeTypeUtil.getFileTypeIcon(mimetype,
                                                                template.getTitle(),
                                                                context,
                                                                viewThemeUtils);

            GlideHelper.INSTANCE.loadViaURLIntoImageView(context, template.getPreview(), binding.template, placeholder);
            binding.templateName.setText(template.getTitle());
            binding.templateContainer.setChecked(template == selectedTemplate);
        }
    }

    public interface ClickListener {
        void onClick(Template template);
    }
}
