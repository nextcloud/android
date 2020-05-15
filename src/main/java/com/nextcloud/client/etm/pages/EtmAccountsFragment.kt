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
package com.nextcloud.client.etm.pages

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import com.nextcloud.client.etm.EtmBaseFragment
import com.owncloud.android.R
import kotlinx.android.synthetic.main.fragment_etm_accounts.*
import kotlinx.android.synthetic.main.fragment_etm_preferences.*

class EtmAccountsFragment : EtmBaseFragment() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_etm_accounts, container, false)
    }

    override fun onResume() {
        super.onResume()
        val builder = StringBuilder()
        vm.accounts.forEach {
            builder.append("Account: ${it.account.name}\n")
            it.userData.forEach {
                builder.append("\t${it.key}: ${it.value}\n")
            }
        }
        etm_accounts_text.text = builder.toString()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.fragment_etm_accounts, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.etm_accounts_share -> {
                onClickedShare(); true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun onClickedShare() {
        val intent = Intent(Intent.ACTION_SEND)
        intent.putExtra(Intent.EXTRA_SUBJECT, "Nextcloud accounts information")
        intent.putExtra(Intent.EXTRA_TEXT, etm_accounts_text.text)
        intent.type = "text/plain"
        startActivity(intent)
    }
}
