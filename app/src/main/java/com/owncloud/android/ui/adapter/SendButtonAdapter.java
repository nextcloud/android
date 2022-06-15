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

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.owncloud.android.databinding.SendButtonBinding;
import com.owncloud.android.ui.components.SendButtonData;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class SendButtonAdapter extends RecyclerView.Adapter<SendButtonAdapter.ViewHolder> {

    private List<SendButtonData> sendButtonDataList;
    private ClickListener clickListener;

    public SendButtonAdapter(List<SendButtonData> sendButtonDataList, ClickListener clickListener) {
        this.sendButtonDataList = sendButtonDataList;
        this.clickListener = clickListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(SendButtonBinding.inflate(LayoutInflater.from(parent.getContext()), parent,
                                                                                                 false), clickListener);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        holder.bind(sendButtonDataList.get(position));
    }

    @Override
    public int getItemCount() {
        return sendButtonDataList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        private SendButtonBinding binding;
        private ClickListener clickListener;
        private SendButtonData sendButtonDataData;

        public ViewHolder(@NonNull SendButtonBinding binding, ClickListener clickListener) {
            super(binding.getRoot());
            this.binding = binding;
            this.clickListener = clickListener;
            itemView.setOnClickListener(this);
        }

        public void bind(SendButtonData item) {
            sendButtonDataData = item;
            binding.sendButtonIcon.setImageDrawable(item.getDrawable());
            binding.sendButtonText.setText(item.getTitle());
        }

        @Override
        public void onClick(View v) {
            if (clickListener != null) {
                clickListener.onClick(sendButtonDataData);
            }
        }
    }

    public interface ClickListener {
        void onClick(SendButtonData sendButtonDataData);
    }
}
