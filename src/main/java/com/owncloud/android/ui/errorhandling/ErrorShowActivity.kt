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
package com.owncloud.android.ui.errorhandling

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import com.owncloud.android.R
import kotlinx.android.synthetic.main.errorhandling_showerror.*
import kotlinx.android.synthetic.main.toolbar_standard.*

class ErrorShowActivity : AppCompatActivity() {
    companion object{
        const val EXTRA_ERROR_TEXT = "error"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.errorhandling_showerror)

        text_view_error.text = intent.getStringExtra(EXTRA_ERROR_TEXT)

        setSupportActionBar(toolbar)
        setTitle(R.string.common_error)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.activity_error_show, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        return when (item?.itemId) {
            R.id.error_share -> {
                onClickedShare(); true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun onClickedShare() {
        val intent = Intent(Intent.ACTION_SEND)
        intent.putExtra(Intent.EXTRA_SUBJECT, "Nextcloud Error")
        intent.putExtra(Intent.EXTRA_TEXT, text_view_error.text)
        intent.type = "text/plain"
        startActivity(intent)
    }
}
