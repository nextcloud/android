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
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.nextcloud.client.account.CurrentAccountProvider;
import com.nextcloud.client.network.ClientFactory;
import com.owncloud.android.R;
import com.owncloud.android.datamodel.Template;
import com.owncloud.android.ui.dialog.ChooseRichDocumentsTemplateDialogFragment;
import com.owncloud.android.utils.NextcloudServer;
import com.owncloud.android.utils.glide.CustomGlideStreamLoader;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import butterknife.BindView;
import butterknife.ButterKnife;

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
    }

    @NonNull
    @Override
    @NextcloudServer(max = 18) // remove entire class
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.template_button, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.setData(templateList.get(position));
    }

    public void setTemplateList(List<Template> templateList) {
        this.templateList = templateList;
    }

    @Override
    public int getItemCount() {
        return templateList.size();
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
                .into(thumbnail);

            name.setText(template.getName());
        }
    }

    public interface ClickListener {
        void onClick(Template template);
    }
}
