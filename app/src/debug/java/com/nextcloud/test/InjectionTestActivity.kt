/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2023 Álvaro Brey <alvaro@alvarobrey.com>
 * SPDX-FileCopyrightText: 2023 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.nextcloud.test

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.nextcloud.client.di.Injectable
import com.nextcloud.client.preferences.AppPreferences
import com.owncloud.android.databinding.ActivityInjectionTestBinding
import javax.inject.Inject

/**
 * Sample activity to check test overriding injections
 */
class InjectionTestActivity :
    AppCompatActivity(),
    Injectable {
    @Inject
    lateinit var appPreferences: AppPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityInjectionTestBinding.inflate(layoutInflater)
        // random pref, just needs to match the one in the test
        binding.text.text = appPreferences.lastUploadPath
        setContentView(binding.root)
    }
}
