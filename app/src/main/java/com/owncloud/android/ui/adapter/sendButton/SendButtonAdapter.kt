/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.owncloud.android.ui.adapter.sendButton

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.owncloud.android.databinding.SendButtonBinding

class SendButtonAdapter(
    private val sendButtonDataList: List<SendButtonData>,
    private val clickListener: ClickListener
) : RecyclerView.Adapter<SendButtonAdapter.ViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder = ViewHolder(
        SendButtonBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        ),
        clickListener
    )

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(sendButtonDataList[position])
    }

    override fun getItemCount(): Int = sendButtonDataList.size

    class ViewHolder(private val binding: SendButtonBinding, private val clickListener: ClickListener) :
        RecyclerView.ViewHolder(
            binding.getRoot()
        ),
        View.OnClickListener {
        private var sendButtonDataData: SendButtonData? = null

        init {
            itemView.setOnClickListener(this)
        }

        fun bind(item: SendButtonData) {
            sendButtonDataData = item
            binding.sendButton.icon = item.drawable
            binding.sendButton.text = item.title
        }

        override fun onClick(v: View) {
            clickListener.onSendButtonClick(sendButtonDataData)
        }
    }

    interface ClickListener {
        fun onSendButtonClick(sendButtonData: SendButtonData?)
    }
}
