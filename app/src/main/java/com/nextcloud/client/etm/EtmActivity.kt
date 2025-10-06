/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2019 Chris Narkiewicz <hello@ezaquarii.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.nextcloud.client.etm

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import androidx.activity.OnBackPressedCallback
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.nextcloud.client.di.Injectable
import com.nextcloud.client.di.ViewModelFactory
import com.owncloud.android.R
import com.owncloud.android.ui.activity.ToolbarActivity
import javax.inject.Inject

class EtmActivity :
    ToolbarActivity(),
    Injectable {

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
        vm.currentPage.observe(
            this,
            Observer {
                onPageChanged(it)
            }
        )
        handleOnBackPressed()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean = when (item.itemId) {
        android.R.id.home -> {
            if (!vm.onBackPressed()) {
                finish()
            }
            true
        }
        else -> super.onOptionsItemSelected(item)
    }

    private fun handleOnBackPressed() {
        onBackPressedDispatcher.addCallback(object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val handledByVm = vm.onBackPressed()

                if (!handledByVm) {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })
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
