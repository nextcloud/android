/*
 * Nextcloud Android client application
 *
 * @author Chris Narkiewicz
 * Copyright (C) 2019 Chris Narkiewicz <hello@ezaquarii.com>
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
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
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
        adapter = EtmMenuAdapter(context!!, this::onClickedItem)
        adapter.pages = vm.pages
        val view = inflater.inflate(R.layout.fragment_etm_menu, container, false)
        list = view.findViewById(R.id.etm_menu_list)
        list.layoutManager = LinearLayoutManager(context!!)
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
