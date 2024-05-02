/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2017 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2017 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.ui.adapter;

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
