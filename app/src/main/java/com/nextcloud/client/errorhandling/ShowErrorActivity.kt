/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2019 Andy Scherzinger <info@andy-scherzinger.de>
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.nextcloud.client.errorhandling

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.snackbar.Snackbar
import com.owncloud.android.R
import com.owncloud.android.databinding.ActivityShowErrorBinding
import com.owncloud.android.utils.ClipboardUtil
import com.owncloud.android.utils.DisplayUtils

class ShowErrorActivity : AppCompatActivity() {
    private lateinit var binding: ActivityShowErrorBinding

    companion object {
        const val EXTRA_ERROR_TEXT = "error"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityShowErrorBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.textViewError.text = intent.getStringExtra(EXTRA_ERROR_TEXT)

        setSupportActionBar(binding.toolbarInclude.toolbar)
        supportActionBar!!.title = createErrorTitle()

        val snackbar = DisplayUtils.createSnackbar(
            binding.errorPageContainer,
            R.string.error_report_issue_text,
            Snackbar.LENGTH_INDEFINITE
        )
            .setAction(R.string.error_report_issue_action) { reportIssue() }

        snackbar.show()
    }

    private fun createErrorTitle() = String.format(getString(R.string.error_crash_title), getString(R.string.app_name))

    private fun reportIssue() {
        ClipboardUtil.copyToClipboard(this, binding.textViewError.text.toString(), false)
        val issueLink = getString(R.string.report_issue_link)
        DisplayUtils.startLinkIntent(this, issueLink)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.activity_show_error, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean = when (item.itemId) {
        R.id.error_share -> {
            onClickedShare()
            true
        }
        else -> super.onOptionsItemSelected(item)
    }

    private fun onClickedShare() {
        val intent = Intent(Intent.ACTION_SEND)
        intent.putExtra(Intent.EXTRA_SUBJECT, createErrorTitle())
        intent.putExtra(Intent.EXTRA_TEXT, binding.textViewError.text)
        intent.type = "text/plain"
        startActivity(intent)
    }
}
