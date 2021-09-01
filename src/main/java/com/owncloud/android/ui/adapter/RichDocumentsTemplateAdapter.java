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
import com.owncloud.android.datamodel.Template;
import com.owncloud.android.ui.dialog.ChooseRichDocumentsTemplateDialogFragment;
import com.owncloud.android.utils.NextcloudServer;
import com.owncloud.android.utils.theme.ThemeColorUtils;
import com.owncloud.android.utils.glide.CustomGlideStreamLoader;

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
    private ChooseRichDocumentsTemplateDialogFragment.Type type;
    private CurrentAccountProvider currentAccountProvider;
    private ClientFactory clientFactory;
    private Template selectedTemplate;
    private final int colorSelected;
    private final int colorUnselected;

    public RichDocumentsTemplateAdapter(
        ChooseRichDocumentsTemplateDialogFragment.Type type,
        ClickListener clickListener,
        Context context,
        CurrentAccountProvider currentAccountProvider,
        ClientFactory clientFactory
    ) {
        this.clickListener = clickListener;
        this.type = type;
        this.context = context;
        this.currentAccountProvider = currentAccountProvider;
        this.clientFactory = clientFactory;
        colorSelected = ThemeColorUtils.primaryColor(context, true);
        colorUnselected = context.getResources().getColor(R.color.grey_200);
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

            Glide.with(context).using(new CustomGlideStreamLoader(currentAccountProvider, clientFactory)).
                load(template.getThumbnailLink())
                .placeholder(placeholder)
                .error(placeholder)
                .into(binding.template);

            binding.templateName.setText(template.getName());

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
