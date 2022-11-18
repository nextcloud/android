/*
 *
 * Nextcloud Android client application
 *
 * @author Tobias Kaminsky
 * Copyright (C) 2022 Tobias Kaminsky
 * Copyright (C) 2022 Nextcloud GmbH
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
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package com.owncloud.android.ui.activity

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.nextcloud.client.account.User
import com.owncloud.android.R
import com.owncloud.android.ui.dialog.SetupEncryptionDialogFragment

class SetupEncryptionActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val user = intent?.getParcelableExtra("EXTRA_USER") as User?

        if (user == null) {
            Toast.makeText(this, getString(R.string.error_showing_encryption_dialog), Toast.LENGTH_LONG).show()
            finish()
        }

        val setupEncryptionDialogFragment = SetupEncryptionDialogFragment.newInstance(user, -1)
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
        setupEncryptionDialogFragment.show(supportFragmentManager, "setup_encryption")
    }

    private fun buildResultIntentFromBundle(result: Bundle): Intent {
        val intent = Intent()
        intent.putExtra(
            SetupEncryptionDialogFragment.SUCCESS,
            result.getBoolean(SetupEncryptionDialogFragment.SUCCESS)
        )
        intent.putExtra(
            SetupEncryptionDialogFragment.ARG_POSITION,
            result.getInt(SetupEncryptionDialogFragment.ARG_POSITION)
        )
        return intent
    }
}
