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

import android.content.SharedPreferences
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.nextcloud.client.etm.pages.EtmPreferencesFragment
import com.owncloud.android.R
import javax.inject.Inject

class EtmViewModel @Inject constructor(
    private val defaultPreferences: SharedPreferences
) : ViewModel() {
    val currentPage: LiveData<EtmMenuEntry?> = MutableLiveData()
    val pages: List<EtmMenuEntry> = listOf(
        EtmMenuEntry(
            iconRes = R.drawable.ic_settings,
            titleRes = R.string.etm_preferences,
            pageClass = EtmPreferencesFragment::class
        )
    )

    val preferences: Map<String, String> get() {
        return defaultPreferences.all
            .map { it.key to "${it.value}" }
            .sortedBy { it.first }
            .toMap()
    }

    init {
        (currentPage as MutableLiveData).apply {
            value = null
        }
    }

    fun onPageSelected(index: Int) {
        if (index < pages.size) {
            currentPage as MutableLiveData
            currentPage.value = pages[index]
        }
    }

    fun onBackPressed(): Boolean {
        (currentPage as MutableLiveData)
        return if (currentPage.value != null) {
            currentPage.value = null
            true
        } else {
            false
        }
    }
}
