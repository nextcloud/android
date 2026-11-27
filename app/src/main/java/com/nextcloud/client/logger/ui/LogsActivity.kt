/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2019 Chris Narkiewicz <hello@ezaquarii.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.nextcloud.client.logger.ui

import android.app.DownloadManager
import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.widget.SearchView
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.nextcloud.client.di.ViewModelFactory
import com.nextcloud.utils.SnackbarUtil
import com.nextcloud.utils.extensions.collectWhenStarted
import com.owncloud.android.R
import com.owncloud.android.databinding.LogsActivityBinding
import com.owncloud.android.ui.activity.ToolbarActivity
import com.owncloud.android.ui.navigation.model.ActionBarStyle
import javax.inject.Inject

class LogsActivity : ToolbarActivity() {

    @Inject
    lateinit var viewModelFactory: ViewModelFactory

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
        binding = LogsActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewThemeUtils.platform.themeHorizontalProgressBar(binding.logsLoadingProgress)

        logsAdapter = LogsAdapter(this)
        binding.logsList.apply {
            layoutManager = LinearLayoutManager(this@LogsActivity)
            adapter = logsAdapter
        }

        vm.entries.observe(this) { logsAdapter.entries = it }
        vm.isLoading.observe(this) { isLoading ->
            binding.logsLoadingProgress.isVisible = isLoading == true
            binding.logsContent.isInvisible = isLoading == true
        }
        vm.status.observe(this) { binding.logsStatus.text = it }
        collectWhenStarted(vm.logsExported) { showLogsExportedSnackbar() }
        vm.load()

        showActionBar(ActionBarStyle.Plain)
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
            R.id.action_export_logs -> vm.export()
            R.id.action_refresh_logs -> vm.load()
            else -> retval = super.onOptionsItemSelected(item)
        }
        return retval
    }

    private fun showLogsExportedSnackbar() {
        Snackbar.make(binding.root, R.string.logs_exported, Snackbar.LENGTH_LONG)
            .setAction(R.string.logs_open_in_files) { openDownloadsFolder() }
            .also { viewThemeUtils.material.themeSnackbar(it) }
            .show()
    }

    private fun openDownloadsFolder() {
        try {
            startActivity(Intent(DownloadManager.ACTION_VIEW_DOWNLOADS))
        } catch (_: ActivityNotFoundException) {
            SnackbarUtil.show(this, R.string.logs_no_files_app)
        }
    }
}
