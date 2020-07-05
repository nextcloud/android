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
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.nextcloud.client.account.CurrentAccountProvider;
import com.nextcloud.client.network.ClientFactory;
import com.owncloud.android.R;
import com.owncloud.android.lib.common.Template;
import com.owncloud.android.lib.common.TemplateList;
import com.owncloud.android.utils.MimeTypeUtil;
import com.owncloud.android.utils.glide.CustomGlideStreamLoader;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import butterknife.BindView;
import butterknife.ButterKnife;

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
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.template_button, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.setData(templateList.getTemplateList().get(position));
    }

    public void setTemplateList(TemplateList templateList) {
        this.templateList = templateList;
    }

    @Override
    public int getItemCount() {
        return templateList.getTemplateList().size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        @BindView(R.id.name)
        public TextView name;

        @BindView(R.id.thumbnail)
        public ImageView thumbnail;

        private Template template;

        public ViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
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

            Glide.with(context).using(new CustomGlideStreamLoader(currentAccountProvider, clientFactory))
                .load(template.getPreview())
                    .placeholder(placeholder)
                    .error(placeholder)
                    .into(thumbnail);

            name.setText(template.getTitle());
        }
    }

    public interface ClickListener {
        void onClick(Template template);
    }
}
