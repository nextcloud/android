/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.owncloud.android.ui.activity

import android.content.Intent
import android.os.Bundle
import androidx.annotation.VisibleForTesting
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.commit
import com.nextcloud.client.di.Injectable
import com.owncloud.android.R
import com.owncloud.android.ui.activity.settings.SettingsFragment
import com.owncloud.android.utils.theme.ViewThemeUtils
import javax.inject.Inject

class SettingsActivity :
    AppCompatActivity(),
    Injectable {

    @Inject
    lateinit var viewThemeUtils: ViewThemeUtils

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setupActionBar()

        if (savedInstanceState == null) {
            supportFragmentManager.commit {
                replace(android.R.id.content, SettingsFragment())
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()

        return true
    }

    @VisibleForTesting
    fun handleMnemonicRequest(data: Intent?) {
        val fragment = supportFragmentManager.findFragmentById(android.R.id.content) as? SettingsFragment ?: return

        fragment.handleMnemonicRequest(data)
    }

    private fun setupActionBar() {
        val actionBar = supportActionBar ?: return

        viewThemeUtils.platform.themeStatusBar(this)
        actionBar.setDisplayHomeAsUpEnabled(true)
        actionBar.setDisplayShowTitleEnabled(true)

        val backIcon = ResourcesCompat.getDrawable(resources, R.drawable.ic_arrow_back, null) ?: return

        viewThemeUtils.androidx.themeActionBar(this, actionBar, getString(R.string.actionbar_settings), backIcon)
    }
}
