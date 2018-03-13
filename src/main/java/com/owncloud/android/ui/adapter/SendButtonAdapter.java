package com.owncloud.android.ui.adapter;

/*
 * Nextcloud Android client application
 *
 * @author Tobias Kaminsky
 * Copyright (C) 2017 Tobias Kaminsky
 * Copyright (C) 2017 Nextcloud GmbH.
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

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.owncloud.android.R;
import com.owncloud.android.ui.components.SendButtonData;

import java.util.List;

public class SendButtonAdapter extends RecyclerView.Adapter<SendButtonAdapter.ViewHolder> {

    private List<SendButtonData> sendButtonDataList;
    private ClickListener clickListener;

    public SendButtonAdapter(List<SendButtonData> sendButtonDataList, ClickListener clickListener) {
        this.sendButtonDataList = sendButtonDataList;
        this.clickListener = clickListener;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.send_button, parent, false));
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        holder.setData(sendButtonDataList.get(position));
    }

    @Override
    public int getItemCount() {
        return sendButtonDataList.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        private TextView text;
        private ImageView icon;
        private SendButtonData sendButtonDataData;

        public ViewHolder(View itemView) {
            super(itemView);
            itemView.setOnClickListener(this);
            text = itemView.findViewById(R.id.send_button_text);
            icon = itemView.findViewById(R.id.send_button_icon);
        }

        @Override
        public void onClick(View v) {
            if (clickListener != null) {
                clickListener.onClick(sendButtonDataData);
            }
        }

        public void setData(SendButtonData item) {
            sendButtonDataData = item;
            icon.setImageDrawable(item.getDrawable());
            text.setText(item.getTitle());
        }
    }

    public interface ClickListener {
        void onClick(SendButtonData sendButtonDataData);
    }
}
