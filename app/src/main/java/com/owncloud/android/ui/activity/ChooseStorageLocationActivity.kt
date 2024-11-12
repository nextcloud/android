/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2024 ZetaTom <70907959+ZetaTom@users.noreply.github.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.owncloud.android.ui.activity

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.nextcloud.ui.ChooseStorageLocationDialogFragment

class ChooseStorageLocationActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val chooseStorageLocationDialogFragment = ChooseStorageLocationDialogFragment.newInstance()
        supportFragmentManager.setFragmentResultListener(
            KEY_RESULT_STORAGE_LOCATION,
            this
        ) { _, result ->
            setResult(
                ChooseStorageLocationDialogFragment.STORAGE_LOCATION_RESULT_CODE,
                Intent().putExtra(
                    KEY_RESULT_STORAGE_LOCATION,
                    result.getString(KEY_RESULT_STORAGE_LOCATION)
                )
            )
        }
        chooseStorageLocationDialogFragment.show(supportFragmentManager, "choose_storage_location")
    }

    companion object {
        const val KEY_RESULT_STORAGE_LOCATION = ChooseStorageLocationDialogFragment.KEY_RESULT_STORAGE_LOCATION
    }
}
