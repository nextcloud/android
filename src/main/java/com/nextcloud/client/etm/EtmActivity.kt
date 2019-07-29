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

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.nextcloud.client.di.Injectable
import com.nextcloud.client.di.ViewModelFactory
import com.owncloud.android.R
import com.owncloud.android.ui.activity.ToolbarActivity
import javax.inject.Inject

class EtmActivity : ToolbarActivity(), Injectable {

    companion object {
        @JvmStatic
        fun launch(context: Context) {
            val etmIntent = Intent(context, EtmActivity::class.java)
            context.startActivity(etmIntent)
        }
    }

    @Inject
    lateinit var viewModelFactory: ViewModelFactory
    internal lateinit var vm: EtmViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_etm)
        setupToolbar()
        updateActionBarTitleAndHomeButtonByString(getString(R.string.etm_title))
        vm = ViewModelProvider(this, viewModelFactory).get(EtmViewModel::class.java)
        vm.currentPage.observe(this, Observer {
            onPageChanged(it)
        })
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        return when (item?.itemId) {
            android.R.id.home -> {
                if (!vm.onBackPressed()) {
                    finish()
                }
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onBackPressed() {
        if (!vm.onBackPressed()) {
            super.onBackPressed()
        }
    }

    private fun onPageChanged(page: EtmMenuEntry?) {
        if (page != null) {
            val fragment = page.pageClass.java.getConstructor().newInstance()
            supportFragmentManager.beginTransaction()
                .replace(R.id.etm_page_container, fragment)
                .commit()
            updateActionBarTitleAndHomeButtonByString("ETM - ${getString(page.titleRes)}")
        } else {
            supportFragmentManager.beginTransaction()
                .replace(R.id.etm_page_container, EtmMenuFragment())
                .commitNow()
            updateActionBarTitleAndHomeButtonByString(getString(R.string.etm_title))
        }
    }
}
