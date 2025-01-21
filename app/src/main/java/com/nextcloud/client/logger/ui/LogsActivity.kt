/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2019 Chris Narkiewicz <hello@ezaquarii.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.nextcloud.client.logger.ui

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.ProgressBar
import androidx.appcompat.widget.SearchView
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.nextcloud.client.di.ViewModelFactory
import com.owncloud.android.R
import com.owncloud.android.databinding.LogsActivityBinding
import com.owncloud.android.ui.activity.ToolbarActivity
import com.owncloud.android.utils.theme.ViewThemeUtils
import javax.inject.Inject

class LogsActivity : ToolbarActivity() {

    @Inject
    lateinit var viewModelFactory: ViewModelFactory

    @Inject
    lateinit var viewThemeUtils: ViewThemeUtils

    private lateinit var vm: LogsViewModel
    private lateinit var binding: LogsActivityBinding
    private lateinit var logsAdapter: LogsAdapter

    private val searchBoxListener = object : SearchView.OnQueryTextListener {
        override fun onQueryTextSubmit(query: String): Boolean = false

        override fun onQueryTextChange(newText: String): Boolean {
            vm.filter(newText)
            return false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        vm = ViewModelProvider(this, viewModelFactory).get(LogsViewModel::class.java)
        binding = DataBindingUtil.setContentView<LogsActivityBinding>(this, R.layout.logs_activity).apply {
            lifecycleOwner = this@LogsActivity
            vm = this@LogsActivity.vm
        }

        findViewById<ProgressBar>(R.id.logs_loading_progress).apply {
            viewThemeUtils.platform.themeHorizontalProgressBar(this)
        }

        logsAdapter = LogsAdapter(this)
        findViewById<RecyclerView>(R.id.logsList).apply {
            layoutManager = LinearLayoutManager(this@LogsActivity)
            adapter = logsAdapter
        }

        vm.entries.observe(this, Observer { logsAdapter.entries = it })
        vm.load()

        setupToolbar()
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        supportActionBar?.let {
            viewThemeUtils.files.themeActionBar(this, it, R.string.logs_title)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.activity_logs, menu)

        (menu.findItem(R.id.action_search).actionView as SearchView).apply {
            setOnQueryTextListener(searchBoxListener)
            viewThemeUtils.androidx.themeToolbarSearchView(this)
        }
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        var retval = true
        when (item.itemId) {
            android.R.id.home -> finish()
            R.id.action_delete_logs -> vm.deleteAll()
            R.id.action_send_logs -> vm.send()
            R.id.action_refresh_logs -> vm.load()
            else -> retval = super.onOptionsItemSelected(item)
        }
        return retval
    }
}
