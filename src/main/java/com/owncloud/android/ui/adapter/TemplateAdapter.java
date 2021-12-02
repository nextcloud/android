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
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package com.owncloud.android.ui.adapter;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.bumptech.glide.Glide;
import com.nextcloud.client.account.CurrentAccountProvider;
import com.nextcloud.client.network.ClientFactory;
import com.owncloud.android.R;
import com.owncloud.android.databinding.TemplateButtonBinding;
import com.owncloud.android.lib.common.Template;
import com.owncloud.android.lib.common.TemplateList;
import com.owncloud.android.utils.MimeTypeUtil;
import com.owncloud.android.utils.theme.ThemeColorUtils;
import com.owncloud.android.utils.glide.CustomGlideStreamLoader;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

/**
 * Adapter for handling Templates, used to create files out of it via RichDocuments app
 */
public class TemplateAdapter extends RecyclerView.Adapter<TemplateAdapter.ViewHolder> {

    private TemplateList templateList = new TemplateList();
    private ClickListener clickListener;
    private Context context;
    private CurrentAccountProvider currentAccountProvider;
    private ClientFactory clientFactory;
    private String mimetype;
    private Template selectedTemplate;
    private final int colorSelected;
    private final int colorUnselected;

    public TemplateAdapter(
        String mimetype,
        ClickListener clickListener,
        Context context,
        CurrentAccountProvider currentAccountProvider,
        ClientFactory clientFactory
    ) {
        this.mimetype = mimetype;
        this.clickListener = clickListener;
        this.context = context;
        this.currentAccountProvider = currentAccountProvider;
        this.clientFactory = clientFactory;
        colorSelected = ThemeColorUtils.primaryColor(context, true);
        colorUnselected = context.getResources().getColor(R.color.grey_200);
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
        holder.setData(templateList.getTemplateList().get(position));
    }

    public void setTemplateList(TemplateList templateList) {
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
        return templateList.getTemplateList().size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        private final TemplateButtonBinding binding;
        private Template template;

        public ViewHolder(@NonNull TemplateButtonBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
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

            Drawable placeholder = MimeTypeUtil.getFileTypeIcon(mimetype,
                                                                template.getTitle(),
                                                                currentAccountProvider.getUser(),
                                                                context);

            Glide.with(context).using(new CustomGlideStreamLoader(currentAccountProvider.getUser(), clientFactory))
                .load(template.getPreview())
                .placeholder(placeholder)
                .error(placeholder)
                .into(binding.template);

            binding.templateName.setText(template.getTitle());

            if (template == selectedTemplate) {
                binding.templateContainer.setStrokeColor(colorSelected);
            } else {
                binding.templateContainer.setStrokeColor(colorUnselected);
            }
        }
    }

    public interface ClickListener {
        void onClick(Template template);
    }
}
