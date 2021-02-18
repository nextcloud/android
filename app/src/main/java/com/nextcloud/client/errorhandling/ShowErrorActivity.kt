/*
 * Nextcloud Android client application
 *
 * @author Andy Scherzinger
 * Copyright (C) 2019 Andy Scherzinger <info@andy-scherzinger.de>
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
package com.nextcloud.client.errorhandling

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.snackbar.Snackbar
import com.owncloud.android.R
import com.owncloud.android.utils.ClipboardUtil
import com.owncloud.android.utils.DisplayUtils
import kotlinx.android.synthetic.main.activity_show_error.*
import kotlinx.android.synthetic.main.toolbar_standard.*

class ShowErrorActivity : AppCompatActivity() {
    companion object {
        const val EXTRA_ERROR_TEXT = "error"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_show_error)

        text_view_error.text = intent.getStringExtra(EXTRA_ERROR_TEXT)

        setSupportActionBar(toolbar)
        supportActionBar!!.title = createErrorTitle()

        val snackbar = DisplayUtils.createSnackbar(
            error_page_container,
            R.string.error_report_issue_text,
            Snackbar.LENGTH_INDEFINITE
        )
            .setAction(R.string.error_report_issue_action) { reportIssue() }

        snackbar.show()
    }

    private fun createErrorTitle() = String.format(getString(R.string.error_crash_title), getString(R.string.app_name))

    private fun reportIssue() {
        ClipboardUtil.copyToClipboard(this, text_view_error.text.toString(), false)
        val issueLink = getString(R.string.report_issue_link)
        if (issueLink.isNotEmpty()) {
            val uriUrl = Uri.parse(issueLink)
            val intent = Intent(Intent.ACTION_VIEW, uriUrl)
            DisplayUtils.startIntentIfAppAvailable(intent, this, R.string.no_browser_available)
        }
        Toast.makeText(this, R.string.copied_to_clipboard, Toast.LENGTH_LONG).show()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.activity_show_error, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        return when (item?.itemId) {
            R.id.error_share -> { onClickedShare(); true }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun onClickedShare() {
        val intent = Intent(Intent.ACTION_SEND)
        intent.putExtra(Intent.EXTRA_SUBJECT, createErrorTitle())
        intent.putExtra(Intent.EXTRA_TEXT, text_view_error.text)
        intent.type = "text/plain"
        startActivity(intent)
    }
}
