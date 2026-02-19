/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.ui.bottomSheet

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.nextcloud.android.common.ui.theme.utils.ColorRole
import com.nextcloud.client.di.Injectable
import com.owncloud.android.R
import com.owncloud.android.utils.theme.ViewThemeUtils
import javax.inject.Inject

class ActionsBottomSheet(
    private val actions: List<ActionBottomSheetData>,
) : BottomSheetDialogFragment(), Injectable {

    @Inject
    lateinit var viewThemeUtils: ViewThemeUtils

    private var recyclerView: RecyclerView? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.bottom_sheet_actions, container, false)
        recyclerView = view.findViewById(R.id.actions_list)
        recyclerView?.layoutManager = LinearLayoutManager(context)
        recyclerView?.adapter = ActionsAdapter(requireContext(), actions)
        viewThemeUtils.platform.colorViewBackground(view, ColorRole.SURFACE)
        return view
    }

    private inner class ActionsAdapter(
        val context: Context,
        val actions: List<ActionBottomSheetData>,
    ) : RecyclerView.Adapter<ActionsAdapter.ActionViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ActionViewHolder {
            val view = LayoutInflater.from(context)
                .inflate(R.layout.item_action_list, parent, false)
            return ActionViewHolder(view)
        }

        override fun onBindViewHolder(holder: ActionViewHolder, position: Int) {
            val action = actions[position]
            holder.textView.text = action.title
            holder.itemView.setOnClickListener {
                action.onClick.invoke()
                dismiss()
            }
        }

        override fun getItemCount(): Int = actions.size

        inner class ActionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val textView: TextView = itemView.findViewById(R.id.action_text)
        }
    }
}
