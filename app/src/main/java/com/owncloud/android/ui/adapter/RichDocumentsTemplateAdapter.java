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

package com.owncloud.android.ui.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.bumptech.glide.Glide;
import com.nextcloud.client.account.CurrentAccountProvider;
import com.nextcloud.client.network.ClientFactory;
import com.owncloud.android.R;
import com.owncloud.android.databinding.TemplateButtonBinding;
import com.owncloud.android.lib.resources.files.RichDocumentsTemplateType;
import com.owncloud.android.lib.resources.files.Template;
import com.owncloud.android.utils.NextcloudServer;
import com.owncloud.android.utils.glide.CustomGlideStreamLoader;
import com.owncloud.android.utils.theme.ViewThemeUtils;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

/**
 * Adapter for handling Templates, used to create files out of it via RichDocuments app
 */
public class RichDocumentsTemplateAdapter extends RecyclerView.Adapter<RichDocumentsTemplateAdapter.ViewHolder> {

    private List<Template> templateList = new ArrayList<>();
    private ClickListener clickListener;
    private Context context;
    private RichDocumentsTemplateType type;
    private CurrentAccountProvider currentAccountProvider;
    private ClientFactory clientFactory;
    private Template selectedTemplate;
    private ViewThemeUtils viewThemeUtils;

    public RichDocumentsTemplateAdapter(
        RichDocumentsTemplateType type,
        ClickListener clickListener,
        Context context,
        CurrentAccountProvider currentAccountProvider,
        ClientFactory clientFactory,
        ViewThemeUtils viewThemeUtils
                                       ) {
        this.clickListener = clickListener;
        this.type = type;
        this.context = context;
        this.currentAccountProvider = currentAccountProvider;
        this.clientFactory = clientFactory;
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

            int placeholder;

            switch (type) {
                case DOCUMENT:
                    placeholder = R.drawable.file_doc;
                    break;

                case SPREADSHEET:
                    placeholder = R.drawable.file_xls;
                    break;

                case PRESENTATION:
                    placeholder = R.drawable.file_ppt;
                    break;

                default:
                    placeholder = R.drawable.file;
                    break;
            }

            Glide.with(context).using(new CustomGlideStreamLoader(currentAccountProvider.getUser(), clientFactory))
                .load(template.getThumbnailLink())
                .placeholder(placeholder)
                .error(placeholder)
                .into(binding.template);

            binding.templateName.setText(template.getName());
            binding.templateContainer.setChecked(template == selectedTemplate);
        }
    }

    public interface ClickListener {
        void onClick(Template template);
    }
}
