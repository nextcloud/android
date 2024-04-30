/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2019 Chris Narkiewicz <hello@ezaquarii.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.nextcloud.client.etm

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.owncloud.android.R

class EtmMenuFragment : EtmBaseFragment() {

    private lateinit var adapter: EtmMenuAdapter
    private lateinit var list: RecyclerView

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        adapter = EtmMenuAdapter(requireContext(), this::onClickedItem)
        adapter.pages = vm.pages
        val view = inflater.inflate(R.layout.fragment_etm_menu, container, false)
        list = view.findViewById(R.id.etm_menu_list)
        list.layoutManager = LinearLayoutManager(requireContext())
        list.adapter = adapter
        return view
    }

    override fun onResume() {
        super.onResume()
        activity?.setTitle(R.string.etm_title)
    }

    private fun onClickedItem(position: Int) {
        vm.onPageSelected(position)
    }
}
