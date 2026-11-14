/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2022 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2022 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.ui.activity

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.nextcloud.client.account.User
import com.nextcloud.utils.SnackbarUtil
import com.nextcloud.utils.extensions.getParcelableArgument
import com.owncloud.android.R
import com.owncloud.android.ui.dialog.setupEncryption.SetupEncryptionDialogFragment

/**
 * Only needed for SettingsActivity
 */
class SetupEncryptionActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val user = intent?.getParcelableArgument("EXTRA_USER", User::class.java)

        if (user == null) {
            SnackbarUtil.show(this, R.string.error_showing_encryption_dialog)
            finish()
        }

        val setupEncryptionDialogFragment = SetupEncryptionDialogFragment.newInstance(user, null, null)
        supportFragmentManager.setFragmentResultListener(
            SetupEncryptionDialogFragment.RESULT_REQUEST_KEY,
            this
        ) { requestKey, result ->
            if (requestKey == SetupEncryptionDialogFragment.RESULT_REQUEST_KEY) {
                if (!result.getBoolean(SetupEncryptionDialogFragment.RESULT_KEY_CANCELLED, false)) {
                    setResult(
                        SetupEncryptionDialogFragment.SETUP_ENCRYPTION_RESULT_CODE,
                        buildResultIntentFromBundle(result)
                    )
                }
            }
            finish()
        }

        // Only show a new dialog the first time: on recreation the FragmentManager
        // restores the existing dialog with its saved passphrase. Showing a fresh
        // one here would generate a new passphrase and lose the one being copied.
        if (savedInstanceState == null) {
            setupEncryptionDialogFragment.show(supportFragmentManager, "setup_encryption")
        }
    }

    private fun buildResultIntentFromBundle(result: Bundle): Intent = Intent().apply {
        putExtra(
            SetupEncryptionDialogFragment.SUCCESS,
            result.getBoolean(SetupEncryptionDialogFragment.SUCCESS)
        )
        putExtra(
            SetupEncryptionDialogFragment.ARG_FILE_PATH,
            result.getInt(SetupEncryptionDialogFragment.ARG_FILE_PATH)
        )
    }
}
