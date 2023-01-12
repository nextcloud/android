/*
 * Nextcloud Android client application
 *
 *  @author Álvaro Brey
 *  Copyright (C) 2023 Álvaro Brey
 *  Copyright (C) 2023 Nextcloud GmbH
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU AFFERO GENERAL PUBLIC LICENSE
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU AFFERO GENERAL PUBLIC LICENSE for more details.
 *
 * You should have received a copy of the GNU Affero General Public
 * License along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
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
class InjectionTestActivity : AppCompatActivity(), Injectable {
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
